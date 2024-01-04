package com.example.projektpum

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val camera_permission = 100
    private val storage_permission = 101
    private val location_permission = 103

    private var stepCount = 0

    private lateinit var myMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var isRecording = false
    private var polyline: Polyline? = null
    private val pathPoints = mutableListOf<LatLng>()

    private var lastLocation: LatLng? = null
    private var totalDistanceInMeters = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.layout)

        registerReceiver(stepReceiver, IntentFilter("step_count_updated"))

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<Button>(R.id.start_button).setOnClickListener {
            startRecording()
        }

        findViewById<Button>(R.id.stop_button).setOnClickListener {
            stopRecording()
        }

        findViewById<Button>(R.id.music_button).setOnClickListener {
            val musicIntent = Intent(applicationContext, MusicPlayer::class.java)
            startActivity(musicIntent)
        }

        findViewById<Button>(R.id.camera_button).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), camera_permission)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), storage_permission)
            }

            val intentCapture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intentCapture, 123)
        }
    }

    //  PERMISSIONS
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == camera_permission) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Uprawnienia do kamery są wymagane.", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == storage_permission) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Uprawnienia do folderu są wymagane.", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == location_permission) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Uprawnienia do lokalizacji są wymagane.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //  CAMERA
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && requestCode == 123) {
            val bmp = data.extras?.get("data") as Bitmap

            bmp?.let {
                saveImageToMediaStore(it)
            }
        }
    }

    // SAVE PHOTOS
    private fun saveImageToMediaStore(imageBitmap: Bitmap) {
        val displayName = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        val mimeType = "image/jpeg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
            }
        }
    }

    // STEP COUNTER
    private val stepReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "step_count_updated") {
                stepCount = intent.getIntExtra("step_count", stepCount)
                updateStepCountOnUI()
            }
        }
    }
    private fun updateStepCountOnUI() {
        val stepCountTextView = findViewById<TextView>(R.id.steps)
        stepCountTextView.text = stepCount.toString()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(stepReceiver)
    }

    // MAPS
    override fun onMapReady(googleMap: GoogleMap) {
        myMap = googleMap

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Pobieranie obecnej lokalizacji
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        myMap.addMarker(MarkerOptions().position(currentLatLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16.5f))
                    }
                }
        } else {
            // Jeśli nie masz uprawnień, poproś użytkownika o nie
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), location_permission)
        }

        startLocationUpdates()

        findViewById<Button>(R.id.resetC_button).setOnClickListener {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16.5f))
                }
            }
        }
    }

    private fun startRecording() {
        isRecording = true
        val serviceIntent = Intent(this, StepCounter::class.java)
        startService(serviceIntent)
    }

    private fun stopRecording() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Potwierdź").setMessage("Czy na pewno chcesz zatrzymać? Spowoduje to zamknięcie aplikacji")

        builder.setPositiveButton("Tak") { dialog, which ->
            isRecording = false
            myMap.clear()
            StepCounter().resetStepCount()
        }

        builder.setNegativeButton("Nie") { dialog, which ->
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun drawPolyline() {
        // Rysuj linię na mapie
        if (pathPoints.size >= 2) {
            polyline = myMap.addPolyline(PolylineOptions().addAll(pathPoints).color(Color.parseColor("#5F00BA")).width(10.0f))
        }
    }

    // Funkcja wywoływana przy każdej aktualizacji lokalizacji
    private fun updateLocation(location: LatLng) {
        if (isRecording) {
            // Dodaj aktualną lokalizację do listy
            pathPoints.add(location)

            // licznik kilometrów (poprawa / usuniecie)
            if (lastLocation != null) {
                val distance = calculateDistance(lastLocation!!, location)
                totalDistanceInMeters += distance
                // Aktualizuj widok licznika kilometrów
                updateDistanceView()
            }

            myMap.clear()
            drawPolyline()
            myMap.addMarker(MarkerOptions().position(location).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
            myMap.moveCamera(CameraUpdateFactory.newLatLng(location))

            lastLocation = location
        } else {
            myMap.clear()
            myMap.addMarker(MarkerOptions().position(location).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
            myMap.moveCamera(CameraUpdateFactory.newLatLng(location))
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(1000) // Interwał aktualizacji w milisekundach

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), location_permission)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    updateLocation(currentLatLng)
                }
        }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)

        return results[0]
    }

    private fun updateDistanceView() {
        val distanceKm = totalDistanceInMeters / 1000.0
        val formattedDistance = String.format("%.2f km", distanceKm)

        findViewById<TextView>(R.id.km).text = formattedDistance
    }
}
