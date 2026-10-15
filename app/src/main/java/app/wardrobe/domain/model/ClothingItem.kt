package app.wardrobe.domain.model

/** One piece of clothing in the user's wardrobe. */
data class ClothingItem(
    val id: String,
    val name: String,
    val garment: Garment,
    val color: ItemColor,
    val material: Material,
    val warmth: Warmth,
    val seasons: Set<Season>,
) {
    val category: Category get() = garment.category
}

/** The four categories an outfit is built from. */
enum class Category { TOP, BOTTOM, FOOTWEAR, OUTERWEAR }

/**
 * The kind of garment. It decides the category and the illustration shown until real photos exist.
 *
 * @param defaultMaterial what a new item of this garment starts with in the form.
 */
enum class Garment(val category: Category, val defaultMaterial: Material) {
    T_SHIRT(Category.TOP, Material.COTTON),
    SHIRT(Category.TOP, Material.COTTON),
    SWEATER(Category.TOP, Material.WOOL),
    JEANS(Category.BOTTOM, Material.DENIM),
    TROUSERS(Category.BOTTOM, Material.COTTON),
    SHORTS(Category.BOTTOM, Material.COTTON),
    SKIRT(Category.BOTTOM, Material.COTTON),
    SNEAKERS(Category.FOOTWEAR, Material.SYNTHETIC),
    BOOTS(Category.FOOTWEAR, Material.LEATHER),
    JACKET(Category.OUTERWEAR, Material.SYNTHETIC),
    COAT(Category.OUTERWEAR, Material.WOOL),
}

/** The main fabric. The user sets it, since recognition doesn't guess it. */
enum class Material { COTTON, LINEN, WOOL, DENIM, LEATHER, SYNTHETIC, WATERPROOF }

/** How warm a garment is. The user sets it, since a photo can't show it. */
enum class Warmth { LIGHT, MEDIUM, WARM }

enum class Season { SPRING, SUMMER, AUTUMN, WINTER }
