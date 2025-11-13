package miguel.oscar.uv.ordena

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.Locale

class GeocodeActivity : AppCompatActivity() {
    private lateinit var addressInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geocode)

        addressInput = findViewById(R.id.editTextAddress)
        val searchButton = findViewById<Button>(R.id.btnSearch)

        searchButton.setOnClickListener {
            val address = addressInput.text.toString()
            if (address.isNotEmpty()) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses: MutableList<Address>? = geocoder.getFromLocationName(address, 1)

                    if (addresses != null) {
                        if (addresses.isNotEmpty()) {
                            val latitude = addresses?.get(0)?.latitude
                            val longitude = addresses?.get(0)?.longitude

                            val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(address)})")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")

                            if (mapIntent.resolveActivity(packageManager) != null) {
                                startActivity(mapIntent)
                            } else {
                                Toast.makeText(this, "Google Maps no está instalado", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: IOException) {
                    Toast.makeText(this, "Error al geocodificar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor ingrese una dirección", Toast.LENGTH_SHORT).show()
            }
        }
    }
}