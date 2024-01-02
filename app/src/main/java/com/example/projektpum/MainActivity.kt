package com.example.projektpum

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val camera_permission = 100
    private val storage_permission = 101
    private val location_permission = 103
    private val location_permission2 = 104

    private var stepCount = 0

    private lateinit var myMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.layout)

        // to tez do usuniecia jak nie chce resetowania
        resetSteps()

        val serviceIntent = Intent(this, StepCounter::class.java)
        startService(serviceIntent)

        registerReceiver(stepReceiver, IntentFilter("step_count_updated"))

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
    private fun resetSteps() {
        findViewById<TextView>(R.id.steps).setOnClickListener{
            Toast.makeText(this, "Przytrzymaj by zresetować", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.steps).setOnLongClickListener {
            stepCount = 0

            // linijka do usuniecia jak nie chce resetowania
            StepCounter().resetStepCount()
            // ======

            findViewById<TextView>(R.id.steps).text = StepCounter().stepCount.toString()

            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(stepReceiver)
    }

    // MAPS
    override fun onMapReady(googleMap: GoogleMap) {
        myMap = googleMap
//        val currentLatLng: LatLng? = null

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            // Pobieranie obecnej lokalizacji
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        myMap.addMarker(MarkerOptions().position(currentLatLng).title("My Location"))
                        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16.5f))
                    }
                }
        } else {
            // Jeśli nie masz uprawnień, poproś użytkownika o nie
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), location_permission)
        }

        findViewById<Button>(R.id.resetC_button).setOnClickListener {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    myMap.addMarker(MarkerOptions().position(currentLatLng).title("My Location"))
                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16.5f))
                }
            }
        }
    }
}
