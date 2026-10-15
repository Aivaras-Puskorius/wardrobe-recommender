package app.wardrobe.data

import android.database.sqlite.SQLiteConstraintException
import app.wardrobe.domain.model.ClothingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Keeps items in the phone's [WardrobeDatabase], so they survive restarts. */
class RoomWardrobeRepository(private val dao: ItemDao) : WardrobeRepository {

    override val items: Flow<List<ClothingItem>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun item(id: String): Flow<ClothingItem?> = dao.observe(id).map { it?.toDomain() }.distinctUntilChanged()

    override suspend fun add(item: ClothingItem) {
        try {
            dao.insert(item.toEntity())
        } catch (e: SQLiteConstraintException) {
            throw IllegalArgumentException("Item ${item.id} already exists", e)
        }
    }

    override suspend fun update(item: ClothingItem) {
        require(dao.update(item.toEntity()) == 1) { "No item ${item.id}" }
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}
