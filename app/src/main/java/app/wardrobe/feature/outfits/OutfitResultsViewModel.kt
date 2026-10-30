package app.wardrobe.feature.outfits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wardrobe.data.RatingRepository
import app.wardrobe.data.WardrobeRepository
import app.wardrobe.data.Weather
import app.wardrobe.data.WeatherState
import app.wardrobe.data.itemIdsOf
import app.wardrobe.data.outfitKey
import app.wardrobe.data.toggle
import app.wardrobe.domain.engine.EngineResult
import app.wardrobe.domain.engine.OutfitEngine
import app.wardrobe.domain.engine.Reason
import app.wardrobe.domain.engine.ScoreTerm
import app.wardrobe.domain.engine.ScoredOutfit
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Rating
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How an outfit's colors work together. It names the outfit. */
enum class ColorStory { ALL_NEUTRALS, ACCENT, TONAL, NEIGHBORING, OPPOSITE, MIXED }

/** An outfit's name: its [story], the [color] the name mentions, and a [number] when an earlier outfit has the same name. */
data class OutfitTitle(val story: ColorStory, val color: ItemColor? = null, val number: Int? = null)

/** An outfit as the results screen shows it, with [items] in display order and [why] strongest first. */
data class OutfitCard(
    val id: String,
    val title: OutfitTitle,
    val items: List<ClothingItem>,
    val why: List<ScoreTerm>,
    val rating: Rating?,
)

sealed interface OutfitResultsUiState {
    data object Loading : OutfitResultsUiState
    data object NotFound : OutfitResultsUiState

    /** No forecast was saved and fetching one failed. */
    data object NoForecast : OutfitResultsUiState

    /**
     * @param outfits empty when no outfit fits [anchor] on [day].
     * @param missing the category the wardrobe lacks when that is why [outfits] is empty, else null.
     * @param offlineSince when the forecast was fetched, if fetching a newer one failed. Null otherwise.
     * @param canShowMore the engine has more outfits than [outfits] shows.
     */
    data class Ready(
        val anchor: ClothingItem,
        val location: String,
        val today: LocalDate,
        val day: DayForecast,
        val outfits: List<OutfitCard>,
        val missing: Category? = null,
        val offlineSince: LocalDateTime? = null,
        val canShowMore: Boolean = false,
    ) : OutfitResultsUiState
}

/**
 * Outfits built around one item for one forecast day, by [OutfitEngine]. A [date] the forecast doesn't have, such as
 * yesterday after midnight, falls back to the forecast's first day.
 *
 * INFO(limit): ratings are saved but don't change the suggestions yet, and the engine gets no history of shown
 * outfits. Milestone 3's taste learning will use the ratings.
 */
class OutfitResultsViewModel(
    itemId: String,
    date: LocalDate,
    repository: WardrobeRepository,
    private val weather: Weather,
    private val ratings: RatingRepository,
) : ViewModel() {

    /** How many outfits to show. More outfits adds [MORE_OUTFITS]. */
    private val shown = MutableStateFlow(MORE_OUTFITS)

    val uiState: StateFlow<OutfitResultsUiState> =
        combine(repository.items, ratings.ratings, weather.state, shown) { items, rated, weatherState, count ->
            val ready = when (weatherState) {
                WeatherState.Loading -> return@combine OutfitResultsUiState.Loading
                WeatherState.Unavailable -> return@combine OutfitResultsUiState.NoForecast
                is WeatherState.Ready -> weatherState
            }
            val forecast = ready.forecast
            val anchor = items.firstOrNull { it.id == itemId } ?: return@combine OutfitResultsUiState.NotFound
            val day = forecast.days.firstOrNull { it.date == date } ?: forecast.days.first()
            // One more than shown, to know whether More outfits has anything to add.
            val result = OutfitEngine.suggest(items, anchor, day, limit = count + 1)
            val outfits = (result as? EngineResult.Outfits)?.outfits.orEmpty()
            OutfitResultsUiState.Ready(
                anchor = anchor,
                location = forecast.location,
                today = forecast.days.first().date,
                day = day,
                outfits = outfitCards(outfits.take(count), rated.associate { outfitKey(it.itemIds) to it.rating }),
                missing = (result as? EngineResult.Missing)?.category,
                offlineSince = ready.offlineSince,
                canShowMore = outfits.size > count,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OutfitResultsUiState.Loading)

    /** Adds the next outfits after the ones shown. */
    fun showMore() {
        shown.update { it + MORE_OUTFITS }
    }

    /** Fetches the forecast again after it failed. */
    fun retry() {
        viewModelScope.launch { weather.refresh(force = true) }
    }

    /** Sets [rating] on the outfit, or clears it if the outfit already has that rating. */
    fun rate(outfitId: String, rating: Rating) {
        viewModelScope.launch { ratings.toggle(itemIdsOf(outfitId), rating) }
    }

    private fun outfitCards(outfits: List<ScoredOutfit>, ratings: Map<String, Rating>): List<OutfitCard> =
        outfits.zip(outfitTitles(outfits)) { outfit, title ->
            val id = outfitKey(outfit.items.map { it.id }.toSet())
            OutfitCard(id = id, title = title, items = outfit.items, why = outfit.why, rating = ratings[id])
        }

    private companion object {
        /** Outfits shown at first and added by each More outfits. */
        const val MORE_OUTFITS = 3
    }
}

/** Names each outfit by its color story. A name used by an earlier outfit gets a number, from 2. */
internal fun outfitTitles(outfits: List<ScoredOutfit>): List<OutfitTitle> {
    val titles = outfits.map { it.title() }
    return titles.mapIndexed { index, title ->
        val seen = titles.take(index + 1).count { it == title }
        if (seen > 1) title.copy(number = seen) else title
    }
}

private fun ScoredOutfit.title(): OutfitTitle {
    val term = colorTerm ?: return OutfitTitle(ColorStory.MIXED)
    return when (term.reason) {
        Reason.NEUTRAL_BASE -> OutfitTitle(ColorStory.ALL_NEUTRALS)
        Reason.SINGLE_ACCENT -> OutfitTitle(ColorStory.ACCENT, term.items.first().color)
        Reason.TONAL -> OutfitTitle(ColorStory.TONAL, term.items.first().color)
        Reason.ANALOGOUS -> OutfitTitle(ColorStory.NEIGHBORING)
        Reason.COMPLEMENTARY -> OutfitTitle(ColorStory.OPPOSITE)
        else -> OutfitTitle(ColorStory.MIXED)
    }
}
