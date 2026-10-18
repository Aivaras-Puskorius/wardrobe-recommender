package app.wardrobe.navigation

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import app.wardrobe.AppContainer
import app.wardrobe.data.PhotoStore
import app.wardrobe.data.SampleData
import app.wardrobe.data.SampleRecognizer
import app.wardrobe.ui.theme.WardrobeTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Adds an item through the photo flow, and discards one. Recognition answers without delay.
 * A fake result registry stands in for the camera app and the photo picker.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class AddItemFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val photoStore = PhotoStore(app.filesDir)
    private val container = AppContainer(
        recognizer = SampleRecognizer(SampleData.photoSuggestions, delayMillis = 0),
        photoStore = photoStore,
    )

    /** What the camera app reports: true when it saved a photo. */
    private var cameraSaves = true

    /** The photo the picker returns. */
    private val pickedPhoto = File(app.cacheDir, "picked.jpg").apply { writeBytes(byteArrayOf(7, 8, 9)) }

    private val results = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                when (contract) {
                    is ActivityResultContracts.TakePicture -> {
                        // Like a camera app, write the photo through the URI the app handed over.
                        if (cameraSaves) app.contentResolver.openOutputStream(input as Uri)!!.use { it.write(CAMERA_PHOTO) }
                        dispatchResult(requestCode, cameraSaves)
                    }
                    is ActivityResultContracts.PickVisualMedia -> dispatchResult(requestCode, Uri.fromFile(pickedPhoto))
                    else -> error("Unexpected contract $contract")
                }
            }
        }
    }

    @Before
    fun setUp() {
        // HACK: FileProvider caches its folders in a static map, and Robolectric gives each test a new files
        // folder in the same class loader. Attaching the provider again clears the cache for this test.
        val provider = app.packageManager.resolveContentProvider("${app.packageName}.photos", PackageManager.GET_META_DATA)
        Robolectric.buildContentProvider(FileProvider::class.java).create(provider)
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides results) {
                WardrobeTheme { WardrobeApp(container) }
            }
        }
        composeRule.onNodeWithContentDescription("Add item").performClick()
    }

    private fun savedSweater() =
        runBlocking { container.wardrobeRepository.items.first() }.single { it.name == "Green sweater" }

    @Test
    fun savedItemAppearsInTheWardrobe() {
        composeRule.onNodeWithText("Take a photo").performClick()
        composeRule.onNodeWithText("Suggested from your photo. Change anything that is wrong.").assertIsDisplayed()
        composeRule.onNodeWithText("Autumn").performScrollTo().performClick()
        composeRule.onNodeWithText("Save to wardrobe").performClick()

        composeRule.onNodeWithText("Green sweater added").assertIsDisplayed()
        composeRule.onNodeWithText("Green sweater").assertIsDisplayed()
        assertArrayEquals(CAMERA_PHOTO, photoStore.file(savedSweater().photo!!).readBytes())
    }

    @Test
    fun aGalleryPhotoIsCopiedAndSavedWithTheItem() {
        composeRule.onNodeWithText("Choose from gallery").performClick()
        // The copy runs on the IO dispatcher, which the test clock doesn't wait for.
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Save to wardrobe").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Autumn").performScrollTo().performClick()
        composeRule.onNodeWithText("Save to wardrobe").performClick()

        composeRule.onNodeWithText("Green sweater added").assertIsDisplayed()
        assertArrayEquals(byteArrayOf(7, 8, 9), photoStore.file(savedSweater().photo!!).readBytes())
    }

    @Test
    fun backingOutOfTheCameraStaysOnTheSourceChoice() {
        cameraSaves = false
        composeRule.onNodeWithText("Take a photo").performClick()
        composeRule.onNodeWithText("Choose from gallery").assertIsDisplayed()
    }

    @Test
    fun saveWithoutASeasonShowsAnError() {
        composeRule.onNodeWithText("Take a photo").performClick()
        composeRule.onNodeWithText("Save to wardrobe").performClick()
        composeRule.onNodeWithText("Pick at least one season").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun discardLeavesTheWardrobeUnchanged() {
        composeRule.onNodeWithText("Take a photo").performClick()
        composeRule.onNodeWithContentDescription("Close").performClick()
        composeRule.onNodeWithText("Discard").performClick()

        composeRule.onNodeWithText("Black tee").assertIsDisplayed()
        composeRule.onNodeWithText("Green sweater").assertDoesNotExist()
    }

    private companion object {
        val CAMERA_PHOTO = byteArrayOf(1, 2, 3)
    }
}
