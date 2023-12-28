package com.example.projektpum

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity(), SensorEventListener{
    private val camera_permission = 100
    private val storage_permission = 101

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private val STEP_THRESHOLD = 12.0f
    private var isStepCounting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.layout)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        resetSteps()

        if (accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            // Obsługa braku akcelerometru na urządzeniu
            Toast.makeText(this, "Device does not have accelerometer.", Toast.LENGTH_SHORT).show()
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
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = Math.abs(x + y + z - SensorManager.GRAVITY_EARTH)
            if (acceleration > STEP_THRESHOLD) {
                if (!isStepCounting) {
                    isStepCounting = true
                    updateStepCount()
                }
            } else {
                isStepCounting = false
            }
        }
    }

    private fun updateStepCount() {
        val stepCountTextView = findViewById<TextView>(R.id.steps)
        val currentStepCount = stepCountTextView.text.toString().toInt()
        val updatedStepCount = currentStepCount + 1

        stepCountTextView.text = "$updatedStepCount"
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun resetSteps() {
        findViewById<TextView>(R.id.steps).setOnClickListener{
            Toast.makeText(this, "Przytrzymaj by zresetować", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.steps).setOnLongClickListener {
            findViewById<TextView>(R.id.steps).text = 0.toString()

            true
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//        nie potrzebne
    }
}
