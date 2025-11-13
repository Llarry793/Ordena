package miguel.oscar.uv.ordena

import android.os.Parcel
import android.os.Parcelable

// Data class que representa un restaurante y implementa Parcelable para permitir su paso entre Activities
data class Restaurant(
    val id: Long,                    // ID único del restaurante
    val name: String,                // Nombre del restaurante
    val description: String,         // Descripción detallada
    val imageResId: Int = R.mipmap.ic_launcher,  // Recurso de imagen por defecto
    val imagePath: String? = null,   // Ruta de imagen personalizada (opcional)
    val address: String? = null      // Dirección física (opcional)
) : Parcelable {

    // Constructor secundario para reconstruir el objeto desde un Parcel
    constructor(parcel: Parcel) : this(
        parcel.readLong(),           // Leer ID
        parcel.readString()!!,       // Leer nombre (no nulo)
        parcel.readString()!!,       // Leer descripción (no nulo)
        parcel.readInt(),            // Leer recurso de imagen
        parcel.readString(),         // Leer ruta de imagen (nullable)
        parcel.readString()          // Leer dirección (nullable)
    )

    // Método para escribir el objeto en un Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeInt(imageResId)
        parcel.writeString(imagePath)
        parcel.writeString(address)
    }

    // Método requerido por Parcelable (no se usa en esta implementación)
    override fun describeContents(): Int = 0

    // Companion object que implementa el creador de instancias desde Parcel
    companion object CREATOR : Parcelable.Creator<Restaurant> {
        // Crea una instancia de Restaurant desde un Parcel
        override fun createFromParcel(parcel: Parcel): Restaurant = Restaurant(parcel)

        // Crea un array de Restaurant del tamaño especificado
        override fun newArray(size: Int): Array<Restaurant?> = arrayOfNulls(size)
    }
}
