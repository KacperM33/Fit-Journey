package com.example.projektpum

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import java.io.File

class GalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_gallery)

        val allImages = getImages()
        var currentImgIndex = 0

        val prev_btn = findViewById<Button>(R.id.prev_button)
        val next_btn = findViewById<Button>(R.id.next_button)

        if (allImages.size > 0) {
            findViewById<ImageView>(R.id.imageView).setImageURI(Uri.parse(allImages.get(0)))
        } else {
            findViewById<ImageView>(R.id.imageView).setImageURI(Uri.parse(allImages.get(currentImgIndex)))
        }

        prev_btn.isEnabled = false
        if (allImages.size <= 1) {
            next_btn.isEnabled = false
        }

        prev_btn.setOnClickListener {
            if (currentImgIndex > 1) {
                findViewById<ImageView>(R.id.imageView).setImageURI(Uri.parse(allImages.get(currentImgIndex - 1)))
                currentImgIndex--
                if (!next_btn.isEnabled)
                    next_btn.isEnabled = true
            }
            if (currentImgIndex == 1) {
                prev_btn.isEnabled = false
            }
        }

        next_btn.setOnClickListener {
            if (currentImgIndex < allImages.size - 1) {
                findViewById<ImageView>(R.id.imageView).setImageURI(Uri.parse(allImages.get(currentImgIndex + 1)))
                currentImgIndex++
                if (!prev_btn.isEnabled)
                    prev_btn.isEnabled = true
            }
            if (currentImgIndex == allImages.size - 1) {
                next_btn.isEnabled = false
            }
        }
    }

    fun getImages(): ArrayList<String>{
        val fileList: ArrayList<String> = ArrayList()
        val folderName = "FitJourney"

        var path: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + File.separator + folderName

        File(path).walk().forEach {
            fileList.add(it.toString())
        }

        return fileList
    }
}