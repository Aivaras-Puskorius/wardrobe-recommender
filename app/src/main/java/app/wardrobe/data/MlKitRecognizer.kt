package app.wardrobe.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.ItemSuggestion
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Recognizes a garment on the phone with two ML Kit labelers, and its color from the photo's pixels.
 *
 * The standard labeler's model comes through Google Play services, which download it on first use.
 * Until then, and on phones without Play services, only the ImageNet labeler and the color work.
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

    /** Asks Google Play services to download the standard model now, so the first photo doesn't wait for it. */
    fun requestModelDownload() {
        ModuleInstall.getClient(context).installModules(ModuleInstallRequest.newBuilder().addApi(standard).build())
            .addOnSuccessListener { Log.d(TAG, "Standard model ready: ${it.areModulesAlreadyInstalled()}") }
            .addOnFailureListener { Log.w(TAG, "Standard model download failed: ${it.message}") }
    }

    override suspend fun recognize(photo: File): ItemSuggestion = withContext(Dispatchers.IO) {
        val image = try {
            InputImage.fromFilePath(context, Uri.fromFile(photo))
        } catch (e: IOException) {
            return@withContext ItemSuggestion(garment = null, color = null)
        }
        val standardLabels = labels(standard, image)
        val imageNetLabels = labels(imageNet, image)
        // The raw labels, for tuning the thresholds in PhotoLabels.kt: adb logcat -s MlKitRecognizer
        Log.d(TAG, "standard $standardLabels, ImageNet $imageNetLabels")
        ItemSuggestion(
            garment = garmentFromLabels(standardLabels, imageNetLabels),
            color = colorOf(photo),
            material = materialFromLabels(standardLabels, imageNetLabels),
        )
    }

    /** The labeler's labels, or none when it fails, as the standard one does before its model is downloaded. */
    private suspend fun labels(labeler: ImageLabeler, image: InputImage): List<PhotoLabel> = try {
        labeler.process(image).await().map { PhotoLabel(it.text, it.confidence) }
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
        const val COLOR_SIZE = 64
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
