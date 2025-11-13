package miguel.oscar.uv.ordena

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.Locale

class RestaurantMapActivity : AppCompatActivity() {
    private lateinit var restaurants: List<Restaurant>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restaurants = intent.getParcelableArrayListExtra("restaurants") ?: emptyList()

        if (restaurants.isEmpty()) {
            Toast.makeText(this, "No hay restaurantes para mostrar", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showAllOnMap()
    }

    private fun showAllOnMap() {
        val addresses = restaurants.mapNotNull { it.address }
        if (addresses.isEmpty()) {
            Toast.makeText(this, "No hay direcciones válidas", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Crear un URI con todas las direcciones como marcadores
        val uriBuilder = StringBuilder("geo:0,0?q=")
        for ((index, address) in addresses.withIndex()) {
            if (index > 0) uriBuilder.append("|")
            uriBuilder.append(Uri.encode(address))
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString()))
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Google Maps no está instalado", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}