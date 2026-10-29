package app.wardrobe.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.OutfitRating
import app.wardrobe.domain.model.Rating
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Warmth
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Upgrades of a saved database, checked against the schemas in app/schemas. */
@RunWith(RobolectricTestRunner::class)
class WardrobeMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), WardrobeDatabase::class.java)

    /** Every version 1 garment and the material the upgrade gives it. */
    private val version1Materials = mapOf(
        Garment.T_SHIRT to Material.COTTON,
        Garment.SHIRT to Material.COTTON,
        Garment.SWEATER to Material.WOOL,
        Garment.JEANS to Material.DENIM,
        Garment.TROUSERS to Material.COTTON,
        Garment.SHORTS to Material.COTTON,
        Garment.SKIRT to Material.COTTON,
        Garment.SNEAKERS to Material.SYNTHETIC,
        Garment.BOOTS to Material.LEATHER,
        Garment.JACKET to Material.SYNTHETIC,
        Garment.COAT to Material.WOOL,
    )

    @Test
    fun version1ItemsKeepTheirValuesAndGetTheirGarmentsMaterial() {
        helper.createDatabase(NAME, 1).apply {
            version1Materials.keys.forEach { garment ->
                execSQL(
                    "INSERT INTO items (id, name, garment, color, warmth, seasons) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf(garment.name, "My ${garment.name}", garment.name, "NAVY", "WARM", "AUTUMN,WINTER"),
                )
            }
            close()
        }

        helper.runMigrationsAndValidate(NAME, 2, true, WardrobeDatabase.MIGRATION_1_2).close()

        val database = WardrobeDatabase.build(RuntimeEnvironment.getApplication(), NAME)
        val items = runBlocking { RoomWardrobeRepository(database.items()).items.first() }
        database.close()
        val expected = version1Materials.map { (garment, material) ->
            ClothingItem(
                id = garment.name,
                name = "My ${garment.name}",
                garment = garment,
                color = ItemColor.NAVY,
                material = material,
                fit = Fit.REGULAR,
                warmth = Warmth.WARM,
                seasons = setOf(Season.AUTUMN, Season.WINTER),
                photo = null,
            )
        }
        assertEquals(expected, items)
    }

    @Test
    fun version2ItemsKeepTheirValuesAndBecomeRegularFit() {
        helper.createDatabase(NAME, 2).apply {
            execSQL(
                "INSERT INTO items (id, name, garment, color, material, warmth, seasons) VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf("rain-jacket", "Rain jacket", "JACKET", "OLIVE", "WATERPROOF", "MEDIUM", "AUTUMN,SPRING"),
            )
            execSQL(
                "INSERT INTO items (id, name, garment, color, material, warmth, seasons) VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf("boots", "Boots", "BOOTS", "BROWN", "LEATHER", "WARM", "WINTER"),
            )
            close()
        }

        helper.runMigrationsAndValidate(NAME, 3, true, WardrobeDatabase.MIGRATION_2_3).close()

        val database = WardrobeDatabase.build(RuntimeEnvironment.getApplication(), NAME)
        val items = runBlocking { RoomWardrobeRepository(database.items()).items.first() }
        database.close()
        assertEquals(
            listOf(
                ClothingItem(
                    "rain-jacket", "Rain jacket", Garment.JACKET, ItemColor.OLIVE, Material.WATERPROOF, Fit.REGULAR,
                    Warmth.MEDIUM, setOf(Season.SPRING, Season.AUTUMN), photo = null,
                ),
                ClothingItem(
                    "boots", "Boots", Garment.BOOTS, ItemColor.BROWN, Material.LEATHER, Fit.REGULAR, Warmth.WARM,
                    setOf(Season.WINTER), photo = null,
                ),
            ),
            items,
        )
    }

    @Test
    fun version3ItemsKeepTheirValuesAndHaveNoPhoto() {
        helper.createDatabase(NAME, 3).apply {
            execSQL(
                "INSERT INTO items (id, name, garment, color, material, fit, warmth, seasons) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf("jeans", "Jeans", "JEANS", "DENIM", "DENIM", "FITTED", "MEDIUM", "AUTUMN"),
            )
            close()
        }

        helper.runMigrationsAndValidate(NAME, 4, true, WardrobeDatabase.MIGRATION_3_4).close()

        val database = WardrobeDatabase.build(RuntimeEnvironment.getApplication(), NAME)
        val items = runBlocking { RoomWardrobeRepository(database.items()).items.first() }
        database.close()
        assertEquals(
            listOf(
                ClothingItem(
                    "jeans", "Jeans", Garment.JEANS, ItemColor.DENIM, Material.DENIM, Fit.FITTED, Warmth.MEDIUM,
                    setOf(Season.AUTUMN), photo = null,
                ),
            ),
            items,
        )
    }

    @Test
    fun version4ItemsStayAndRatingsCanBeSaved() {
        helper.createDatabase(NAME, 4).apply {
            execSQL(
                "INSERT INTO items (id, name, garment, color, material, fit, warmth, seasons, photo) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf("jeans", "Jeans", "JEANS", "DENIM", "DENIM", "FITTED", "MEDIUM", "AUTUMN", "jeans.jpg"),
            )
            close()
        }

        helper.runMigrationsAndValidate(NAME, 5, true, WardrobeDatabase.MIGRATION_4_5).close()

        val database = WardrobeDatabase.build(RuntimeEnvironment.getApplication(), NAME)
        val ratings = RoomRatingRepository(database.ratings()) { Instant.EPOCH }
        val (items, saved) = runBlocking {
            ratings.rate(setOf("jeans", "tee"), Rating.LIKED)
            RoomWardrobeRepository(database.items()).items.first() to ratings.ratings.first()
        }
        database.close()
        assertEquals(listOf("jeans.jpg"), items.map { it.photo })
        assertEquals(listOf(OutfitRating(setOf("jeans", "tee"), Rating.LIKED, Instant.EPOCH)), saved)
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
