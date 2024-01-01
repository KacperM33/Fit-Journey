package com.example.projektpum

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MusicPlayer : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_music)

        findViewById<Button>(R.id.back_button).setOnClickListener {
            val musicIntent = Intent(applicationContext, MainActivity::class.java)
            startActivity(musicIntent)
        }
    }
}