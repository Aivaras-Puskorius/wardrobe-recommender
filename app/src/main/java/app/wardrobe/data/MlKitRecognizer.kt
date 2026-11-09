package app.wardrobe.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import android.util.Log
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.ItemSuggestion
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Recognizes a garment on the phone with three ML Kit labelers, and its color from the photo's pixels.
 * A clothes classifier trained for this app decides the type. ML Kit's standard model and an ImageNet model
 * refine it and suggest the material.
 *
 * The standard labeler's model comes through Google Play services, which download it on first use.
 * Until then, and on phones without Play services, only the ImageNet labeler and the color work.
 * The subject segmenter, a beta model also from Play services, cuts the item out of the photo.
 */
class MlKitRecognizer(private val context: Context) : Recognizer {

    // Low thresholds here, because garmentFromLabels applies each model's own threshold.
    private val standard = ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.1f).build())

    private val imageNet = ImageLabeling.getClient(
        CustomImageLabelerOptions.Builder(LocalModel.Builder().setAssetFilePath(IMAGENET_MODEL).build())
            .setConfidenceThreshold(0.1f)
            .setMaxResultCount(10)
            .build(),
    )

    /** Trained by tools/train_garment_model.py. It has no metadata, so its labels come from a file by index. */
    private val clothes = ImageLabeling.getClient(
        CustomImageLabelerOptions.Builder(LocalModel.Builder().setAssetFilePath(CLOTHES_MODEL).build())
            .setConfidenceThreshold(0.1f)
            .setMaxResultCount(3)
            .build(),
    )

    private val clothesLabelNames: List<String> by lazy {
        context.assets.open(CLOTHES_LABELS).bufferedReader().use { it.readLines() }
    }

    private fun clothesName(label: ImageLabel) = clothesLabelNames.getOrElse(label.index) { "" }

    private val segmenter = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder().enableForegroundBitmap().build(),
    )

    /** Asks Google Play services to download its models now, so the first photo doesn't wait for them. */
    fun requestModelDownload() {
        val request = ModuleInstallRequest.newBuilder().addApi(standard).addApi(segmenter).build()
        ModuleInstall.getClient(context).installModules(request)
            .addOnSuccessListener { Log.d(TAG, "Play services models ready: ${it.areModulesAlreadyInstalled()}") }
            .addOnFailureListener { Log.w(TAG, "Play services model download failed: ${it.message}") }
    }

    override suspend fun recognize(photo: File): ItemSuggestion = withContext(Dispatchers.IO) {
        val image = try {
            InputImage.fromFilePath(context, Uri.fromFile(photo))
        } catch (e: IOException) {
            return@withContext ItemSuggestion(garment = null, color = null)
        }
        val clothesLabels = labels(clothes, image, ::clothesName)
        val standardLabels = labels(standard, image)
        val imageNetLabels = labels(imageNet, image)
        // The raw labels, for tuning the thresholds in PhotoLabels.kt: adb logcat -s MlKitRecognizer
        val cutout = cutOut(photo)
        Log.d(
            TAG,
            "clothes $clothesLabels, on the cut-out ${cutout?.clothes}, " +
                "standard $standardLabels, ImageNet $imageNetLabels",
        )
        val classifier = moreConfident(clothesLabels, cutout?.clothes.orEmpty())
        ItemSuggestion(
            garment = garmentFor(classifier, standardLabels, imageNetLabels),
            color = cutout?.color ?: colorOf(photo),
            material = materialFromLabels(standardLabels, imageNetLabels),
        )
    }

    /** What a photo's cut-out shows: its color, null when too little of it is item, and the clothes model's labels. */
    private class Cutout(val color: ItemColor?, val clothes: List<PhotoLabel>)

    /**
     * Cuts the item out of [photo] and saves it as [cutoutOf] the photo, which the item's pictures then show.
     * Null when the segmenter fails, as it does until Play services has downloaded its model.
     */
    private suspend fun cutOut(photo: File): Cutout? {
        val started = SystemClock.elapsedRealtime()
        val upright = uprightBitmap(photo, CUTOUT_SIZE) ?: return null
        val foreground = try {
            segmenter.process(InputImage.fromBitmap(upright, 0)).await().foregroundBitmap
        } catch (e: MlKitException) {
            Log.w(TAG, "Segmenter failed: ${e.message}")
            null
        }
        upright.recycle()
        foreground ?: return null
        cutoutOf(photo).outputStream().use { foreground.compress(WEBP_WITH_ALPHA, CUTOUT_QUALITY, it) }

        // The color from a copy 128 pixels wide, as many pixels as the photo's own color reads.
        val small = foreground.scale(COLOR_SIZE * 2, COLOR_SIZE * 2 * foreground.height / foreground.width)
        val onWhite = Bitmap.createBitmap(onWhite(foreground.pixels()), foreground.width, foreground.height, ARGB_8888)
        val clothesOnCutout = labels(clothes, InputImage.fromBitmap(onWhite, 0), ::clothesName)
        val cutout = Cutout(cutoutColor(small.pixels()), clothesOnCutout)
        Log.d(TAG, "cut out in ${SystemClock.elapsedRealtime() - started} ms, color ${cutout.color}")
        listOf(foreground, small, onWhite).forEach { it.recycle() }
        return cutout
    }

    /** The photo turned the way its camera says, about [size] to twice that across, or null if it can't be read. */
    private fun uprightBitmap(photo: File, size: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photo.path, bounds)
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= size) sampleSize *= 2
        val bitmap = BitmapFactory.decodeFile(photo.path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            ?: return null
        val degrees = when (ExifInterface(photo.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { bitmap.recycle() }
    }

    /** The labeler's labels, or none when it fails, as the standard one does before its model is downloaded. */
    private suspend fun labels(
        labeler: ImageLabeler,
        image: InputImage,
        name: (ImageLabel) -> String = { it.text },
    ): List<PhotoLabel> = try {
        labeler.process(image).await().map { PhotoLabel(name(it), it.confidence) }
    } catch (e: MlKitException) {
        Log.w(TAG, "Labeler failed: ${e.message}")
        emptyList()
    }

    /** The main color, from a copy of the photo about 64 to 128 pixels across. */
    private fun colorOf(photo: File): ItemColor? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photo.path, bounds)
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= COLOR_SIZE) sampleSize *= 2
        val bitmap = BitmapFactory.decodeFile(photo.path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            ?: return null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return dominantColor(pixels, bitmap.width, bitmap.height).also { bitmap.recycle() }
    }

    private companion object {
        const val TAG = "MlKitRecognizer"
        /** EfficientNet-Lite0, int8, trained on ImageNet. Apache License 2.0, from tensorflow/efficientnet on Kaggle. */
        const val IMAGENET_MODEL = "models/imagenet_efficientnet_lite0_int8.tflite"

        /** MobileNetV2 fine-tuned on the CC0 clothing dataset by alexeygrigorev, see tools/train_garment_model.py. */
        const val CLOTHES_MODEL = "models/garments_mobilenetv2_int8.tflite"

        // WARN: one label per line, in the model's output order. Retraining writes both files together.
        const val CLOTHES_LABELS = "models/garments_labels.txt"
        const val COLOR_SIZE = 64

        /** Google asks for at least 512 by 512 pixels for the segmenter. */
        const val CUTOUT_SIZE = 1024

        /** Lossy WebP keeps transparency. The PNGs of the third S25 test took 0.9 to 3.5 MB each. */
        const val CUTOUT_QUALITY = 90

        val WEBP_WITH_ALPHA: Bitmap.CompressFormat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                // Lossy below quality 100 on the Android versions before WEBP_LOSSY.
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
    }
}

private fun Bitmap.pixels(): IntArray = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
