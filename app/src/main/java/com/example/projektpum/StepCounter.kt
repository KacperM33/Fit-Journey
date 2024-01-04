package com.example.projektpum

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat

class StepCounter : Service(), SensorEventListener{

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private val STEP_THRESHOLD = 25.0f
    private var isStepCounting = false
    var stepCount = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(1, createNotification())
        initializeSensors()
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Toast.makeText(this, "Brak akcelerometru.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = Math.abs(x + y + z - SensorManager.GRAVITY_EARTH)
            if (acceleration > STEP_THRESHOLD) {
                if (!isStepCounting) {
                    isStepCounting = true
                    stepCount++
                    broadcastStepCount(applicationContext)
                }
            } else {
                isStepCounting = false
            }
        }
    }

    private fun broadcastStepCount(context: Context) {
        val intent = Intent("step_count_updated")
        intent.putExtra("step_count", stepCount)
        sendBroadcast(intent)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // tu nic nie trzeba, ale musi byc
    }

    // NOTIFICATIONS
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("FitJourney").setContentText("Counting steps...")
            .setContentIntent(pendingIntent).build()
    }

    // do usuniecia jak nie chce resetowania (do edycji)
    fun resetStepCount() {
        stepCount = 0
        broadcastStepCount(applicationContext)
    }

    companion object {
        private const val CHANNEL_ID = "StepCounterForegroundServiceChannel"
    }
}