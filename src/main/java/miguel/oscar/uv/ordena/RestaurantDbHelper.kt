package miguel.oscar.uv.ordena

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Clase helper para la creación y gestión de la base de datos SQLite
class RestaurantDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // Versión de la base de datos (incrementar para forzar actualización)
        const val DATABASE_VERSION = 3
        // Nombre del archivo de base de datos
        const val DATABASE_NAME = "Restaurants.db"
    }

    // Método llamado al crear la base de datos por primera vez
    override fun onCreate(db: SQLiteDatabase) {
        // Script SQL para crear la tabla de restaurantes
        val SQL_CREATE_RESTAURANTS = """
            CREATE TABLE ${RestaurantContract.TABLE_NAME} (
                ${RestaurantContract.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${RestaurantContract.COLUMN_NAME} TEXT NOT NULL,
                ${RestaurantContract.COLUMN_DESCRIPTION} TEXT NOT NULL,
                ${RestaurantContract.COLUMN_IMAGE_RES_ID} INTEGER,
                ${RestaurantContract.COLUMN_IMAGE_PATH} TEXT,
                ${RestaurantContract.COLUMN_ADDRESS} TEXT
            )
        """.trimIndent()

        // Script SQL para crear la tabla de productos con clave foránea
        val SQL_CREATE_PRODUCTS = """
            CREATE TABLE ${ProductContract.TABLE_NAME} (
                ${ProductContract.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${ProductContract.COLUMN_NAME} TEXT NOT NULL,
                ${ProductContract.COLUMN_UNIT} TEXT NOT NULL,
                ${ProductContract.COLUMN_QUANTITY} TEXT NOT NULL,
                ${ProductContract.COLUMN_RESTAURANT_ID} INTEGER NOT NULL,
                FOREIGN KEY(${ProductContract.COLUMN_RESTAURANT_ID}) 
                REFERENCES ${RestaurantContract.TABLE_NAME}(${RestaurantContract.COLUMN_ID})
            )
        """.trimIndent()

        // Ejecutar los scripts de creación
        db.execSQL(SQL_CREATE_RESTAURANTS)
        db.execSQL(SQL_CREATE_PRODUCTS)
    }

    // Método para actualizar la base de datos cuando cambia la versión
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migración incremental: añade columna de dirección si no existe
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE ${RestaurantContract.TABLE_NAME} ADD COLUMN ${RestaurantContract.COLUMN_ADDRESS} TEXT DEFAULT ''")
        }
        // NOTA: Evita DROP TABLE para preservar datos existentes
    }

    // Método para inserción masiva de productos usando transacción
    fun bulkInsertProducts(db: SQLiteDatabase, products: List<Product>) {
        db.beginTransaction()
        try {
            products.forEach { product ->
                val values = ContentValues().apply {
                    put(ProductContract.COLUMN_NAME, product.name)
                    put(ProductContract.COLUMN_UNIT, product.unit)
                    put(ProductContract.COLUMN_QUANTITY, product.quantity)
                    put(ProductContract.COLUMN_RESTAURANT_ID, product.restaurantId)
                }
                db.insert(ProductContract.TABLE_NAME, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
