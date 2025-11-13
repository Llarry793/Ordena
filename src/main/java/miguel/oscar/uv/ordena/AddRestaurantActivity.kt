package miguel.oscar.uv.ordena

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest

class AddRestaurantActivity : AppCompatActivity() {
    // Declaración de componentes UI
    private lateinit var imageView: ImageView
    private lateinit var nameInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var saveButton: Button
    private lateinit var cameraButton: Button
    private lateinit var addressInput: EditText

    // Ruta temporal para almacenar la foto capturada
    private var currentPhotoPath: String? = null
    private lateinit var photoFile: File

    // Lanzador para la actividad de la cámara
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                // Cargar la imagen capturada en el ImageView
                val bitmap = BitmapFactory.decodeFile(path)
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    // Lanzador para la solicitud de permisos de la cámara
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(this, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_restaurant)

        // Inicialización de vistas mediante findViewById
        imageView = findViewById(R.id.imageView)
        nameInput = findViewById(R.id.editTextName)
        descriptionInput = findViewById(R.id.editTextDescription)
        saveButton = findViewById(R.id.btnSave)
        cameraButton = findViewById(R.id.btnCamera)
        addressInput = findViewById(R.id.editTextAddress)

        // Configurar listeners de los botones
        setupButtonListeners()
    }

    // Configuración de los eventos click para los botones
    private fun setupButtonListeners() {
        cameraButton.setOnClickListener {
            checkCameraPermission()
        }

        saveButton.setOnClickListener {
            saveRestaurant()
        }
    }

    // Verificación de permisos de la cámara
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            dispatchTakePictureIntent()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Metodo para guardar el restaurante
    private fun saveRestaurant() {
        val name = nameInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val address = addressInput.text.toString().trim()

        if (validateInputs(name, description, address)) {
            val resultIntent = Intent().apply {
                putExtra("name", name)
                putExtra("description", description)
                putExtra("address", address)
                currentPhotoPath?.let { putExtra("photoPath", it) }
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    // Validación de campos de entrada
    private fun validateInputs(name: String, description: String, address: String): Boolean {
        return when {
            name.isEmpty() -> {
                nameInput.error = "Nombre requerido"
                false
            }
            description.isEmpty() -> {
                descriptionInput.error = "Descripción requerida"
                false
            }
            address.isEmpty() -> {
                addressInput.error = "Dirección requerida"
                false
            }
            else -> true
        }
    }

    // Inicia la intent de la cámara con el archivo temporal creado
    private fun dispatchTakePictureIntent() {
        try {
            photoFile = createImageFile() ?: return
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(photoURI)
        } catch (ex: IOException) {
            Toast.makeText(this, "Error al abrir cámara", Toast.LENGTH_SHORT).show()
        }
    }

    // Crea un archivo temporal único para almacenar la foto
    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_$timeStamp"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            ).apply {
                currentPhotoPath = absolutePath
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            Toast.makeText(this, "Error al crear archivo", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
