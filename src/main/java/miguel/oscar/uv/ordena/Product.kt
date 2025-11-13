package miguel.oscar.uv.ordena

data class Product(
    val id: Long,
    val name: String,
    val unit: String,
    val quantity: Double,
    val restaurantId: Long
)