package app.wardrobe.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Warmth
import kotlinx.coroutines.flow.Flow

/** One row of the `items` table. Enums are stored by name. */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val garment: Garment,
    val color: ItemColor,
    val warmth: Warmth,
    val seasons: Set<Season>,
)

@Dao
interface ItemDao {
    /** All items in the order they were added. */
    @Query("SELECT * FROM items ORDER BY rowid")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observe(id: String): Flow<ItemEntity?>

    /** @throws android.database.sqlite.SQLiteConstraintException if the id exists. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ItemEntity)

    /** @return the number of rows changed, 0 when no item has this id. */
    @Update
    suspend fun update(item: ItemEntity): Int

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun delete(id: String)
}

/** Seasons as one column, such as "AUTUMN,WINTER". */
class Converters {
    @TypeConverter
    fun seasonsToText(seasons: Set<Season>): String = seasons.sorted().joinToString(",") { it.name }

    @TypeConverter
    fun textToSeasons(text: String): Set<Season> =
        if (text.isEmpty()) emptySet() else text.split(",").map(Season::valueOf).toSet()
}

/** The wardrobe on the phone. Room creates it on first launch and adds the sample items then. */
@Database(entities = [ItemEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class WardrobeDatabase : RoomDatabase() {
    abstract fun items(): ItemDao

    companion object {
        fun build(context: Context, name: String = "wardrobe.db"): WardrobeDatabase =
            Room.databaseBuilder(context, WardrobeDatabase::class.java, name)
                .addCallback(SeedSampleItems)
                .build()
    }
}

/**
 * Fills a new database with the sample wardrobe. Runs once, when the database file is created.
 *
 * WARN: the column names here must match [ItemEntity]. A new column needs a value here too, and a migration.
 */
private object SeedSampleItems : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val converters = Converters()
        SampleData.items.forEach { item ->
            val row = ContentValues().apply {
                put("id", item.id)
                put("name", item.name)
                put("garment", item.garment.name)
                put("color", item.color.name)
                put("warmth", item.warmth.name)
                put("seasons", converters.seasonsToText(item.seasons))
            }
            db.insert("items", SQLiteDatabase.CONFLICT_ABORT, row)
        }
    }
}

fun ItemEntity.toDomain() = ClothingItem(id, name, garment, color, warmth, seasons)

fun ClothingItem.toEntity() = ItemEntity(id, name, garment, color, warmth, seasons)
