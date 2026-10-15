package app.wardrobe.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Warmth
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
                warmth = Warmth.WARM,
                seasons = setOf(Season.AUTUMN, Season.WINTER),
            )
        }
        assertEquals(expected, items)
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
