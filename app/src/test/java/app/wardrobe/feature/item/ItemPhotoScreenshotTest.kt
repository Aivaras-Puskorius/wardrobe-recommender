package app.wardrobe.feature.item

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.PhotoStore
import app.wardrobe.data.SampleData
import app.wardrobe.testing.captureInTheme
import app.wardrobe.ui.components.LocalPhotoStore
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.request.ErrorResult
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.io.File
import java.io.FileNotFoundException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Items with photos. A fake image loader stands in for decoding, so the photo is a flat color. */
@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ItemPhotoScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val jacket = SampleData.items.first { it.id == "olive-rain-jacket" }

    @Before
    fun fakeImages() {
        val engine = FakeImageLoaderEngine.Builder()
            .intercept({ it is File && it.name == PHOTO }, ColorImage(PHOTO_COLOR))
            .intercept({ it is File && it.name == MISSING }) { chain ->
                ErrorResult(null, chain.request, FileNotFoundException(MISSING))
            }
            .build()
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(RuntimeEnvironment.getApplication()).components { add(engine) }.build(),
        )
    }

    @After
    fun resetImages() {
        SingletonImageLoader.reset()
    }

    private fun detail(photo: String) = composeRule.captureInTheme {
        CompositionLocalProvider(LocalPhotoStore provides PhotoStore(folder.root)) {
            ItemDetailContent(
                ItemDetailUiState.Loaded(jacket.copy(photo = photo)),
                onBack = {},
                onEdit = {},
                onStyle = {},
                onDelete = {},
            )
        }
    }

    @Test
    fun detailShowsThePhoto() = detail(PHOTO)

    @Test
    fun detailFallsBackToTheIllustration() = detail(MISSING)

    private companion object {
        const val PHOTO = "photo.jpg"
        const val MISSING = "missing.jpg"
        const val PHOTO_COLOR = 0xFF6F8A9C.toInt()
    }
}
