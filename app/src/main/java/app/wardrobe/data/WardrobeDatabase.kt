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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.flow.Flow

/** One row of the `items` table. Enums are stored by name. */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val garment: Garment,
    val color: ItemColor,
    val material: Material,
    val fit: Fit,
    val warmth: Warmth,
    val seasons: Set<Season>,
    val photo: String?,
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

/**
 * One row of the `ratings` table: the rating of the outfit whose sorted item ids [outfit] joins with "+".
 *
 * @param ratedAt milliseconds since 1970.
 */
@Entity(tableName = "ratings")
data class RatingEntity(
    @PrimaryKey val outfit: String,
    val rating: Rating,
    val ratedAt: Long,
)

@Dao
interface RatingDao {
    /** All ratings, the newest first. */
    @Query("SELECT * FROM ratings ORDER BY ratedAt DESC, rowid DESC")
    fun observeAll(): Flow<List<RatingEntity>>

    /** Saves [rating], replacing an earlier one for the same outfit. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rating: RatingEntity)

    @Query("DELETE FROM ratings WHERE outfit = :outfit")
    suspend fun delete(outfit: String)
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
@Database(entities = [ItemEntity::class, RatingEntity::class], version = 5)
@TypeConverters(Converters::class)
abstract class WardrobeDatabase : RoomDatabase() {
    abstract fun items(): ItemDao

    abstract fun ratings(): RatingDao

    companion object {
        /**
         * Version 2 adds the material. Saved items get their garment's default material.
         *
         * WARN: a migration describes the past, so it lists the version 1 garments and their defaults
         * as plain SQL. Building it from [Garment] would change it whenever a default changes.
         * INFO(limit): saved sample items get the defaults too, so an upgraded olive rain jacket is synthetic
         * until the user edits it. Only builds from cbb5cff up to this change made version 1 databases.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN material TEXT NOT NULL DEFAULT 'SYNTHETIC'")
                db.execSQL(
                    """
                    UPDATE items SET material = CASE garment
                        WHEN 'T_SHIRT' THEN 'COTTON'
                        WHEN 'SHIRT' THEN 'COTTON'
                        WHEN 'SWEATER' THEN 'WOOL'
                        WHEN 'JEANS' THEN 'DENIM'
                        WHEN 'TROUSERS' THEN 'COTTON'
                        WHEN 'SHORTS' THEN 'COTTON'
                        WHEN 'SKIRT' THEN 'COTTON'
                        WHEN 'SNEAKERS' THEN 'SYNTHETIC'
                        WHEN 'BOOTS' THEN 'LEATHER'
                        WHEN 'JACKET' THEN 'SYNTHETIC'
                        WHEN 'COAT' THEN 'WOOL'
                        ELSE material
                    END
                    """.trimIndent(),
                )
            }
        }

        /**
         * Version 3 adds the fit. Saved items become Regular, the default.
         *
         * INFO(limit): saved sample items become Regular too, so the sample fits only appear in a new database,
         * for example after Clear data. Setting them by id would also hit sample items the user edited into
         * something else, since an edit keeps the id.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN fit TEXT NOT NULL DEFAULT 'REGULAR'")
            }
        }

        /** Version 4 adds the photo file name. Saved items have none and keep their illustration. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN photo TEXT")
            }
        }

        /** Version 5 adds the outfit ratings, which earlier versions kept only while the results screen was open. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ratings` (`outfit` TEXT NOT NULL, `rating` TEXT NOT NULL, " +
                        "`ratedAt` INTEGER NOT NULL, PRIMARY KEY(`outfit`))",
                )
            }
        }

        fun build(context: Context, name: String = "wardrobe.db"): WardrobeDatabase =
            Room.databaseBuilder(context, WardrobeDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
                put("material", item.material.name)
                put("fit", item.fit.name)
                put("warmth", item.warmth.name)
                put("seasons", converters.seasonsToText(item.seasons))
            }
            db.insert("items", SQLiteDatabase.CONFLICT_ABORT, row)
        }
    }
}

/**
 * The key of the outfit made of [itemIds], the same whatever their order.
 *
 * WARN: item ids must not contain "+", or two outfits could share a key. Sample ids use "-", new ones are UUIDs.
 */
fun outfitKey(itemIds: Set<String>): String = itemIds.sorted().joinToString("+")

/** The item ids in an [outfitKey]. */
fun itemIdsOf(outfitKey: String): Set<String> = outfitKey.split("+").toSet()

fun RatingEntity.toDomain() = OutfitRating(itemIdsOf(outfit), rating, Instant.ofEpochMilli(ratedAt))

fun ItemEntity.toDomain() = ClothingItem(id, name, garment, color, material, fit, warmth, seasons, photo)

fun ClothingItem.toEntity() = ItemEntity(id, name, garment, color, material, fit, warmth, seasons, photo)
