package miguel.oscar.uv.ordena

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts

class MapPickerActivity : AppCompatActivity() {

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    // Actualizamos a la API moderna de resultados de actividad
    private val mapLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // La aplicación de mapas no devuelve un resultado con la ubicación seleccionada
            // El usuario tendrá que seleccionarla manualmente
            Toast.makeText(this, "Por favor selecciona manualmente la ubicación", Toast.LENGTH_LONG).show()
        } else {
            // Resultado cancelado o con error
            Toast.makeText(this, "Selección de ubicación cancelada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        // Configurar botón de confirmación
        findViewById<Button>(R.id.btnConfirmLocation).setOnClickListener {
            if (selectedLatitude != 0.0 || selectedLongitude != 0.0) {
                val resultIntent = Intent().apply {
                    putExtra("latitude", selectedLatitude)
                    putExtra("longitude", selectedLongitude)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Por favor selecciona una ubicación", Toast.LENGTH_SHORT).show()
            }
        }

        // Cargar mapa del sistema usando un intent genérico
        val gmmIntentUri = Uri.parse("geo:0,0?z=15")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

        if (mapIntent.resolveActivity(packageManager) != null) {
            mapLauncher.launch(mapIntent)
        } else {
            Toast.makeText(this, "No hay aplicación de mapas disponible", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // Este método permite al usuario seleccionar manualmente la ubicación después de que
    // se haya mostrado el mapa
    fun setLocation(latitude: Double, longitude: Double) {
        selectedLatitude = latitude
        selectedLongitude = longitude
        Toast.makeText(this, "Ubicación seleccionada", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val MAP_REQUEST_CODE = 1001
    }
}
