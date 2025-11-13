package miguel.oscar.uv.ordena

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adaptador para la lista de restaurantes en el RecyclerView
class RestaurantAdapter(
    private val restaurantList: List<Restaurant>, // Lista de restaurantes a mostrar
    private val onItemClick: (Restaurant) -> Unit // Callback para clicks en elementos
) : RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

    // ViewHolder que contiene las vistas de cada elemento de la lista
    class RestaurantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referencias a los elementos del layout item_restaurant
        val imageView: ImageView = itemView.findViewById(R.id.restaurantImage)
        val nameView: TextView = itemView.findViewById(R.id.restaurantName)
        val descriptionView: TextView = itemView.findViewById(R.id.restaurantDescription)
    }

    // Crea nuevas instancias de ViewHolder (invocado por el layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        // Infla el layout XML del item del restaurante
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_restaurant, parent, false)
        return RestaurantViewHolder(view)
    }

    // Actualiza los contenidos de un ViewHolder (invocado por el layout manager)
    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        val restaurant = restaurantList[position]

        // Carga la imagen desde archivo si existe una ruta definida
        restaurant.imagePath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            holder.imageView.setImageBitmap(bitmap)
        } ?: run { // Si no hay ruta de imagen, usa el recurso por defecto
            holder.imageView.setImageResource(restaurant.imageResId)
        }

        // Configura los textos
        holder.nameView.text = restaurant.name
        holder.descriptionView.text = restaurant.description

        // Configura el click listener para el elemento completo
        holder.itemView.setOnClickListener {
            onItemClick(restaurant)
        }
    }

    // Devuelve el tamaño de la lista de datos
    override fun getItemCount() = restaurantList.size

    // Método para eliminar elementos de la lista (solo si es mutable)
    fun removeItem(position: Int) {
        if (restaurantList is MutableList) {
            (restaurantList as MutableList).removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
