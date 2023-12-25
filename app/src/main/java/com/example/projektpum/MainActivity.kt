package com.example.projektpum

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.projektpum.ui.theme.ProjektPUMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.layout)

//        findViewById<Button>(R.id.button).setOnClickListener {
//            findViewById<TextView>(R.id.textView).setText("Cześć!")
//            var message = Toast.makeText(applicationContext, "Cześć!", Toast.LENGTH_SHORT)
//            message.show()
//        }
//        findViewById<Button>(R.id.button2).setOnClickListener {
//            findViewById<TextView>(R.id.textView).setText("Dzień dobry!")
//        }
//
//        findViewById<Button>(R.id.buttonYT).setOnClickListener {
//            val address = "https://www.youtube.com/watch?v=uiEz0_kMHaY&t"
//            val intentYT = Intent(Intent.ACTION_VIEW, Uri.parse(address))
//            startActivity(intentYT)
//        }
    }
}
