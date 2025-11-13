package miguel.oscar.uv.ordena

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.Manifest

class ProductActivity : AppCompatActivity() {
    // Componentes de la UI y variables de instancia
    private lateinit var dbHelper: RestaurantDbHelper
    private lateinit var db: SQLiteDatabase
    private lateinit var adapter: ProductAdapter
    private lateinit var restaurantName: String
    private lateinit var unit: String
    private var quantity: Double = 0.0
    private var restaurantId: Long = -1L
    private lateinit var bottomNavigation: BottomNavigationView
    private var currentRestaurantId: Long = -1L
    private var currentRestaurantName: String = ""
    private var currentRestaurantAddress: String? = null

    companion object {
        // Constantes para notificaciones
        private const val CHANNEL_ID = "STOCK_ALERT_CHANNEL"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        // Configuración inicial de la actividad
        createNotificationChannel() // Crear canal de notificaciones
        checkNotificationPermission() // Verificar permisos en Android 13+

        // Inicialización de la base de datos
        dbHelper = RestaurantDbHelper(this)
        db = dbHelper.writableDatabase

        // Obtener datos del Intent
        restaurantId = intent.getLongExtra("restaurant_id", -1L)
        restaurantName = intent.getStringExtra("restaurant_name") ?: ""
        currentRestaurantId = restaurantId
        currentRestaurantName = restaurantName

        // Validar ID del restaurante
        if (restaurantId == -1L) {
            Toast.makeText(this, "Error: ID de restaurante no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Obtener dirección del restaurante desde la base de datos
        currentRestaurantAddress = getRestaurantAddress(currentRestaurantId)

        // Configurar título de la actividad
        val title = findViewById<TextView>(R.id.restaurantTitle)
        title.text = restaurantName

        // Configurar RecyclerView para lista de productos
        setupRecyclerView()
        loadProducts()

        // Referencias a elementos de UI
        val addButton = findViewById<Button>(R.id.addProductButton)
        val productInput = findViewById<EditText>(R.id.productInput)
        val unitSpinner = findViewById<Spinner>(R.id.unitSpinner)
        val quantityInput = findViewById<EditText>(R.id.quantityInput)

        // Listener para añadir nuevos productos
        addButton.setOnClickListener {
            val productName = productInput.text.toString().trim()
            val unit = unitSpinner.selectedItem.toString()
            val quantityText = quantityInput.text.toString()

            // Validar entrada
            if (productName.isEmpty()) {
                Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convertir cantidad a Double
            val quantity = try {
                quantityText.toDouble().coerceAtLeast(0.0)
            } catch (e: NumberFormatException) {
                0.0
            }

            // Insertar en base de datos y actualizar UI
            val productId = insertProduct(productName, unit, quantity)
            if (productId != -1L) {
                adapter.addProduct(Product(productId, productName, unit, quantity, restaurantId))
                productInput.text.clear()
                quantityInput.text.clear()
            }
        }

        // Configurar navegación inferior
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> {
                    // Navegar al mapa con la ubicación del restaurante actual
                    if (currentRestaurantAddress != null) {
                        val currentRestaurant = Restaurant(
                            currentRestaurantId,
                            currentRestaurantName,
                            "",
                            R.mipmap.ic_launcher,
                            null,
                            currentRestaurantAddress
                        )
                        val restaurantList = ArrayList<Restaurant>().apply {
                            add(currentRestaurant)
                        }
                        val intent = Intent(this, RestaurantMapActivity::class.java).apply {
                            putParcelableArrayListExtra("restaurants", restaurantList)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Este restaurante no tiene dirección", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
    }

    // Verificar permiso para notificaciones (Android 13+)
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permiso ya concedido
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    Toast.makeText(
                        this,
                        "Las notificaciones son necesarias para alertas de stock bajo",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    // Crear canal de notificaciones
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Stock"
            val description = "Notificaciones para productos con stock bajo"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Enviar notificación de stock bajo
    private fun sendLowStockNotification(product: Product) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Stock bajo en ${product.name}")
            .setContentText("Quedan ${product.quantity} ${product.unit}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@ProductActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(product.id.toInt(), builder.build())
        }
    }

    // Obtener dirección del restaurante desde la base de datos
    private fun getRestaurantAddress(restaurantId: Long): String? {
        var address: String? = null
        try {
            val cursor = db.query(
                RestaurantContract.TABLE_NAME,
                arrayOf(RestaurantContract.COLUMN_ADDRESS),
                "${RestaurantContract.COLUMN_ID} = ?",
                arrayOf(restaurantId.toString()),
                null, null, null
            )
            if (cursor.moveToFirst()) {
                address = cursor.getString(cursor.getColumnIndexOrThrow(RestaurantContract.COLUMN_ADDRESS))
            }
            cursor.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al obtener la dirección: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return address
    }

    // Configurar RecyclerView y su adaptador
    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.productRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter(
            mutableListOf(),
            ::deleteProduct,
            ::updateProductQuantity
        )
        recyclerView.adapter = adapter
    }

    // Cargar productos desde la base de datos
    private fun loadProducts() {
        try {
            val cursor = db.query(
                ProductContract.TABLE_NAME,
                arrayOf(
                    ProductContract.COLUMN_ID,
                    ProductContract.COLUMN_NAME,
                    ProductContract.COLUMN_UNIT,
                    ProductContract.COLUMN_QUANTITY,
                    ProductContract.COLUMN_RESTAURANT_ID
                ),
                "${ProductContract.COLUMN_RESTAURANT_ID} = ?",
                arrayOf(restaurantId.toString()),
                null, null, null
            )

            val products = mutableListOf<Product>()
            with(cursor) {
                while (moveToNext()) {
                    val product = Product(
                        id = getLong(getColumnIndexOrThrow(ProductContract.COLUMN_ID)),
                        name = getString(getColumnIndexOrThrow(ProductContract.COLUMN_NAME)),
                        unit = getString(getColumnIndexOrThrow(ProductContract.COLUMN_UNIT)),
                        quantity = getDouble(getColumnIndexOrThrow(ProductContract.COLUMN_QUANTITY)),
                        restaurantId = restaurantId
                    )
                    products.add(product)
                    // Verificar y notificar stock bajo
                    if (product.quantity <= 2.0) {
                        sendLowStockNotification(product)
                    }
                }
                cursor.close()
            }
            adapter.updateProducts(products)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar productos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Insertar nuevo producto en la base de datos
    private fun insertProduct(name: String, unit: String, quantity: Double): Long {
        val values = ContentValues().apply {
            put(ProductContract.COLUMN_NAME, name)
            put(ProductContract.COLUMN_UNIT, unit)
            put(ProductContract.COLUMN_QUANTITY, quantity)
            put(ProductContract.COLUMN_RESTAURANT_ID, restaurantId)
        }
        return db.insert(ProductContract.TABLE_NAME, null, values)
    }

    // Eliminar producto de la base de datos
    private fun deleteProduct(product: Product) {
        db.delete(
            ProductContract.TABLE_NAME,
            "${ProductContract.COLUMN_ID} = ?",
            arrayOf(product.id.toString())
        )
    }

    // Actualizar cantidad de producto en la base de datos
    private fun updateProductQuantity(product: Product, newQuantity: Double) {
        val values = ContentValues().apply {
            put(ProductContract.COLUMN_QUANTITY, newQuantity)
        }
        db.update(
            ProductContract.TABLE_NAME,
            values,
            "${ProductContract.COLUMN_ID} = ?",
            arrayOf(product.id.toString())
        )
    }

    // Manejar resultado de solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    loadProducts() // Recargar productos si se concedió permiso
                }
            }
        }
    }
}
