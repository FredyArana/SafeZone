package com.fredy.safezone

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions
import android.widget.Toast
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import android.app.AlertDialog
import java.util.Random

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var origen: EditText
    private lateinit var destino: EditText
    private lateinit var placeFields: List<Place.Field>

    // Lista de zonas peligrosas con coordenadas y nombre
    private val dangerZones = listOf(
        LatLng(-9.071295, -78.592196), // Malecón Chimbote
        LatLng(-9.068455, -78.589245), // Clínica Primavera
        LatLng(-9.070255, -78.591415), // Plaza de Armas de Chimbote
        LatLng(-9.066630, -78.590980)  // Mercado de Chimbote
    )

    private val circles = mutableListOf<Circle>()  // Lista para almacenar los círculos
    private val markers = mutableListOf<com.google.android.gms.maps.model.Marker>()  // Lista para almacenar los marcadores

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyBibbnQm5zZW-ABNo3DecclXIvL267TSKc")
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        origen = findViewById(R.id.editTextOrigen)
        destino = findViewById(R.id.editTextDestino)

        // Configurar Google Places Autocomplete
        placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

        // Configura Autocomplete para origen
        val autocompleteOrigen = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, placeFields)
            .build(this)

        // Detectar clic en el EditText de origen
        origen.setOnClickListener {
            startActivityForResult(autocompleteOrigen, 1)
        }

        // Configura Autocomplete para destino
        val autocompleteDestino = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, placeFields)
            .build(this)

        // Detectar clic en el EditText de destino
        destino.setOnClickListener {
            startActivityForResult(autocompleteDestino, 2)
        }

        // Agregar botones de zoom
        val zoomInButton = findViewById<Button>(R.id.btn_zoom_in)
        val zoomOutButton = findViewById<Button>(R.id.btn_zoom_out)

        zoomInButton.setOnClickListener {
            val currentZoomLevel = googleMap.cameraPosition.zoom
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoomLevel + 1))
        }

        zoomOutButton.setOnClickListener {
            val currentZoomLevel = googleMap.cameraPosition.zoom
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoomLevel - 1))
        }

        // Configura el botón de limpiar
        val btnClean = findViewById<Button>(R.id.btnClean)
        btnClean.setOnClickListener {
            cleanData()
        }
    }

    // Maneja el resultado de la selección del lugar
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val place = Autocomplete.getPlaceFromIntent(data)
            val latLng = place.latLng

            if (latLng != null) {
                if (requestCode == 1) { // Origen
                    // Crear y agregar el marcador para origen
                    val marker = googleMap.addMarker(MarkerOptions().position(latLng).title("Origen"))
                    marker?.let { markers.add(it) }
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    origen.setText(place.name)

                    // Dibujar zonas peligrosas en el mapa
                    drawDangerZones()

                } else if (requestCode == 2) { // Destino
                    // Crear y agregar el marcador para destino
                    val marker = googleMap.addMarker(MarkerOptions().position(latLng).title("Destino"))
                    marker?.let { markers.add(it) }
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    destino.setText(place.name)

                    // Dibujar zonas peligrosas en el mapa
                    drawDangerZones()
                }
            }
        } else {
            Toast.makeText(this, "Error al seleccionar lugar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        // Configura el listener de clics en los círculos
        googleMap.setOnCircleClickListener { circle ->
            showDangerZoneDialog()
        }
    }

    // Función para dibujar las zonas peligrosas en ubicaciones predefinidas
    private fun drawDangerZones() {
        for (zone in dangerZones) {
            // Crear un círculo rojo en cada zona peligrosa
            val circleOptions = CircleOptions()
                .center(zone)
                .radius(100.0) // Radio de 100 metros
                .strokeColor(0xFFFF0000.toInt()) // Rojo
                .fillColor(0x55FF0000) // Rojo translúcido

            val circle = googleMap.addCircle(circleOptions)

            // Añadir cada círculo a la lista para eliminarlo luego
            circles.add(circle)

            // Habilitar clic en el círculo para mostrar el diálogo
            circle.isClickable = true
        }
    }

    // Función para limpiar los EditText, círculos y marcadores
    private fun cleanData() {
        // Limpiar los EditText
        origen.setText("")
        destino.setText("")

        // Eliminar los círculos del mapa
        for (circle in circles) {
            circle.remove()
        }
        // Limpiar la lista de círculos
        circles.clear()

        // Eliminar los marcadores del mapa
        for (marker in markers) {
            marker.remove()
        }
        // Limpiar la lista de marcadores
        markers.clear()
    }

    // Mostrar un dialogo de alerta cuando el usuario entra en una zona peligrosa
    private fun showDangerZoneDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Zona peligrosa a robos")
            .setCancelable(false)
            .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }
}