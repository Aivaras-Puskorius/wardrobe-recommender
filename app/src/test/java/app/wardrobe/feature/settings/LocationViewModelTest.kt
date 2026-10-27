package app.wardrobe.feature.settings

import app.wardrobe.data.SampleData
import app.wardrobe.data.SampleWeather
import app.wardrobe.data.Weather
import app.wardrobe.data.WeatherState
import app.wardrobe.testing.FakeWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    private val vilnius = SampleData.places.first { it.name == "Vilnius" }

    @Before
    fun setUp() {
        // runTest picks up this dispatcher's clock, so advancing time also moves the view model's debounce.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(weather: Weather = SampleWeather()) =
        LocationViewModel(weather).also { viewModel -> backgroundScope.launch { viewModel.search.collect {} } }

    @Test
    fun startsWithNothingSearched() = runTest {
        assertEquals(LocationSearch.Idle, viewModel().search.value)
    }

    @Test
    fun searchesAfterTypingPauses() = runTest {
        val viewModel = viewModel()
        viewModel.setQuery("Vil")
        advanceTimeBy(SEARCH_DELAY_MILLIS - 1)
        assertEquals(LocationSearch.Idle, viewModel.search.value)
        advanceTimeBy(2)
        assertEquals(LocationSearch.Results(listOf(vilnius)), viewModel.search.value)
    }

    @Test
    fun oneLetterSearchesNothing() = runTest {
        val viewModel = viewModel()
        viewModel.setQuery("V")
        advanceUntilIdle()
        assertEquals(LocationSearch.Idle, viewModel.search.value)
    }

    @Test
    fun noMatchesSaysSo() = runTest {
        val viewModel = viewModel()
        viewModel.setQuery("Xqzzv")
        advanceUntilIdle()
        assertEquals(LocationSearch.NoMatches, viewModel.search.value)
    }

    @Test
    fun aFailedSearchSaysSo() = runTest {
        val weather = FakeWeather(WeatherState.Loading).apply { searchFails = true }
        val viewModel = viewModel(weather)
        viewModel.setQuery("Vilnius")
        advanceUntilIdle()
        assertEquals(LocationSearch.Failed, viewModel.search.value)
    }

    @Test
    fun choosingAPlaceMakesItTheForecastsPlace() = runTest {
        val weather = SampleWeather()
        viewModel(weather).choose(vilnius)
        assertEquals(vilnius, weather.place.value)
        assertEquals(vilnius, viewModel(weather).place.value)
    }
}
