package miguel.oscar.uv.ordena

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import java.io.File

class MainActivity : AppCompatActivity() {
    // Componentes de la UI
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RestaurantAdapter
    private val restaurantList = mutableListOf<Restaurant>()
    private lateinit var dbHelper: RestaurantDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configuración inicial de la actividad
        setTheme(R.style.Theme_Ordena) // Aplicar tema personalizado
        enableEdgeToEdge() // Habilitar diseño edge-to-edge
        setContentView(R.layout.activity_main)

        // Inicialización de la base de datos
        dbHelper = RestaurantDbHelper(this)

        // Configuración de los márgenes del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configuración del RecyclerView y su adaptador
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RestaurantAdapter(restaurantList) { restaurant ->
            // Click listener para abrir la actividad de productos
            val intent = Intent(this, ProductActivity::class.java).apply {
                putExtra("restaurant_id", restaurant.id)
                putExtra("restaurant_name", restaurant.name)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Cargar datos iniciales desde la base de datos
        loadRestaurantsFromDatabase()

        // Configurar el gesto de deslizar para eliminar
        setupSwipeToDelete()

        // Configuración de la navegación inferior
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_anadir -> {
                    // Lanzar actividad para añadir nuevo restaurante
                    val intent = Intent(this, AddRestaurantActivity::class.java)
                    startActivityForResult(intent, ADD_RESTAURANT_REQUEST)
                    true
                }
                R.id.nav_map -> {
                    // Lanzar actividad del mapa con la lista de restaurantes
                    val intent = Intent(this, RestaurantMapActivity::class.java).apply {
                        putParcelableArrayListExtra("restaurants", ArrayList(restaurantList))
                    }
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    // Configura el gesto de deslizar para eliminar elementos
    private fun setupSwipeToDelete() {
        val swipeToDeleteCallback = object : ItemTouchHelper.SimpleCallback(
            0, // No permitir arrastrar
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // Direcciones de deslizamiento
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val restaurant = restaurantList[position]

                // Eliminar de la base de datos
                val db = dbHelper.writableDatabase
                db.delete(
                    RestaurantContract.TABLE_NAME,
                    "${RestaurantContract.COLUMN_ID} = ?",
                    arrayOf(restaurant.id.toString())
                )

                // Eliminar de la lista y actualizar UI
                restaurantList.removeAt(position)
                adapter.notifyItemRemoved(position)

                // Mostrar Snackbar con opción de deshacer
                Snackbar.make(
                    recyclerView,
                    "Restaurante eliminado",
                    Snackbar.LENGTH_LONG
                ).setAction("Deshacer") {
                    // Restaurar elemento eliminado
                    val values = ContentValues().apply {
                        put(RestaurantContract.COLUMN_NAME, restaurant.name)
                        put(RestaurantContract.COLUMN_DESCRIPTION, restaurant.description)
                        put(RestaurantContract.COLUMN_IMAGE_RES_ID, restaurant.imageResId)
                        restaurant.imagePath?.let { put(RestaurantContract.COLUMN_IMAGE_PATH, it) }
                        put(RestaurantContract.COLUMN_ADDRESS, restaurant.address)
                    }

                    // Reinsertar en la base de datos
                    val newRowId = db.insert(RestaurantContract.TABLE_NAME, null, values)
                    if (newRowId != -1L) {
                        restaurantList.add(position, restaurant.copy(id = newRowId))
                        adapter.notifyItemInserted(position)
                    }
                }.show()
            }
        }

        // Adjuntar el helper al RecyclerView
        ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(recyclerView)
    }

    // Carga los restaurantes desde la base de datos
    private fun loadRestaurantsFromDatabase() {
        val db = dbHelper.readableDatabase
        val projection = arrayOf(
            RestaurantContract.COLUMN_ID,
            RestaurantContract.COLUMN_NAME,
            RestaurantContract.COLUMN_DESCRIPTION,
            RestaurantContract.COLUMN_IMAGE_RES_ID,
            RestaurantContract.COLUMN_IMAGE_PATH,
            RestaurantContract.COLUMN_ADDRESS
        )

        val cursor = db.query(
            RestaurantContract.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            null
        )

        restaurantList.clear()
        with(cursor) {
            while (moveToNext()) {
                // Mapear datos del cursor a objetos Restaurant
                val id = getLong(getColumnIndexOrThrow(RestaurantContract.COLUMN_ID))
                val name = getString(getColumnIndexOrThrow(RestaurantContract.COLUMN_NAME))
                val description = getString(getColumnIndexOrThrow(RestaurantContract.COLUMN_DESCRIPTION))
                val imageResId = getInt(getColumnIndexOrThrow(RestaurantContract.COLUMN_IMAGE_RES_ID))
                val imagePath = getString(getColumnIndexOrThrow(RestaurantContract.COLUMN_IMAGE_PATH))
                val address = getString(getColumnIndexOrThrow(RestaurantContract.COLUMN_ADDRESS))

                restaurantList.add(
                    Restaurant(
                        id,
                        name,
                        description,
                        imageResId,
                        imagePath,
                        address
                    )
                )
            }
            close()
        }
        adapter.notifyDataSetChanged()
    }

    // Maneja los resultados de actividades lanzadas
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_RESTAURANT_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.let { intent ->
                try {
                    // Extraer datos del nuevo restaurante
                    val name = intent.getStringExtra("name") ?: ""
                    val description = intent.getStringExtra("description") ?: ""
                    val address = intent.getStringExtra("address") ?: ""
                    val photoPath = intent.getStringExtra("photoPath")

                    // Insertar en la base de datos
                    val db = dbHelper.writableDatabase
                    val values = ContentValues().apply {
                        put(RestaurantContract.COLUMN_NAME, name)
                        put(RestaurantContract.COLUMN_DESCRIPTION, description)
                        put(RestaurantContract.COLUMN_ADDRESS, address)
                        put(RestaurantContract.COLUMN_IMAGE_RES_ID, R.mipmap.ic_launcher)
                        photoPath?.let { put(RestaurantContract.COLUMN_IMAGE_PATH, it) }
                    }

                    val newRowId = db.insert(RestaurantContract.TABLE_NAME, null, values)
                    if (newRowId != -1L) {
                        // Añadir a la lista y actualizar UI
                        restaurantList.add(
                            Restaurant(
                                newRowId,
                                name,
                                description,
                                R.mipmap.ic_launcher,
                                photoPath,
                                address
                            )
                        )
                        adapter.notifyItemInserted(restaurantList.size - 1)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val ADD_RESTAURANT_REQUEST = 1
    }
}
