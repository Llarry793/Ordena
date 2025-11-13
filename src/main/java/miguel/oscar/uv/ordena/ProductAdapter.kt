package miguel.oscar.uv.ordena

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(
    private var productList: MutableList<Product>,
    private val onProductDeleted: (Product) -> Unit,
    private val onProductQuantityChanged: (Product, Double) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // Clase interna ViewHolder para contener las vistas de cada elemento
    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referencias a los elementos del layout
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productUnit: TextView = itemView.findViewById(R.id.productUnit)
        val productQuantity: TextView = itemView.findViewById(R.id.productQuantity)
        val deleteButton: TextView = itemView.findViewById(R.id.deleteButton)
        val incrementButton: TextView = itemView.findViewById(R.id.incrementButton)
        val decrementButton: TextView = itemView.findViewById(R.id.decrementButton)
    }

    // Metodo para crear nuevas instancias de ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    // Metodo para vincular datos a las vistas
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        // Configurar los textos en las vistas
        holder.productName.text = product.name
        holder.productUnit.text = product.unit
        holder.productQuantity.text = "%.2f".format(product.quantity)

        // Listener para el botón de eliminar
        holder.deleteButton.setOnClickListener {
            onProductDeleted(product)
            productList.removeAt(position)
            notifyItemRemoved(position)
        }

        // Listener para el botón de incrementar cantidad
        holder.incrementButton.setOnClickListener {
            val newQuantity = product.quantity + 1.0
            updateProductQuantity(product, newQuantity, position)
        }

        // Listener para el botón de decrementar cantidad
        holder.decrementButton.setOnClickListener {
            if (product.quantity > 0) {
                val newQuantity = product.quantity - 1.0
                updateProductQuantity(product, newQuantity, position)
            }
        }
    }

    // Metodo para actualizar la cantidad de un producto
    private fun updateProductQuantity(product: Product, newQuantity: Double, position: Int) {
        val updatedProduct = product.copy(quantity = newQuantity)
        onProductQuantityChanged(updatedProduct, newQuantity)
        productList[position] = updatedProduct
        notifyItemChanged(position)
    }

    // Metodo para obtener el número de elementos en la lista
    override fun getItemCount() = productList.size

    // Metodo para añadir un nuevo producto a la lista
    fun addProduct(product: Product) {
        productList.add(product)
        notifyItemInserted(productList.size - 1)
    }

    // Metodo para actualizar toda la lista de productos
    fun updateProducts(newProducts: List<Product>) {
        productList.clear()
        productList.addAll(newProducts)
        notifyDataSetChanged()
    }
}
