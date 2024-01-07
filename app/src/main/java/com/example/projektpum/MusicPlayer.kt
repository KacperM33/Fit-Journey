package com.example.projektpum

import android.content.Context
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton

class MusicPlayer : AppCompatActivity() {

    data class Song(val title: String, val artist: String, val path: String)

    private var mediaPlayer: MediaPlayer? = null

    lateinit var runnable: Runnable
    private var handler = Handler()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_music)

        val songList = getSongList(this)
        val songAdapter = SongAdapter(this, songList)

        val songListView: ListView = findViewById(R.id.songlistView)
        songListView.adapter = songAdapter

        songListView.setOnItemClickListener { _, _, position, _ ->
            val selectedSong = songList[position]
            playSong(selectedSong, position)
        }
    }

    private fun playSong(song: Song, position: Int) {
        // Zatrzymaj odtwarzacz, jeśli jest już uruchomiony
        mediaPlayer?.release()

        // Utwórz nowy obiekt MediaPlayer i ustaw ścieżkę dźwięku
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(song.path)
        mediaPlayer?.prepare()
        mediaPlayer?.start()

        var songPosition = position
        val songList = getSongList(this)

        mediaPlayer?.setOnCompletionListener {
            if (songPosition < songList.size - 1) {
                songPosition++
                val nextSong = songList[songPosition]
                playSong(nextSong, songPosition)
            } else {
                songPosition = 0
                val nextSong = songList[songPosition]
                playSong(nextSong, songPosition)
            }
        }

        findViewById<TextView>(R.id.music_TV).text = song.title

        var toggle = true
        findViewById<Button>(R.id.pause_BT).setOnClickListener{
            toggle = !toggle
            if(!toggle) {
                mediaPlayer?.pause()
                findViewById<ToggleButton>(R.id.pause_BT).setBackgroundResource(R.drawable.play_btn)
            } else {
                mediaPlayer?.start()
                findViewById<ToggleButton>(R.id.pause_BT).setBackgroundResource(R.drawable.play_btn)
            }
        }

        findViewById<Button>(R.id.next_BT).setOnClickListener{
            if (songPosition < songList.size - 1) {
                songPosition += 1
                val nextSong = songList[songPosition]
                playSong(nextSong, songPosition)
            } else {
                songPosition = 0
                val nextSong = songList[songPosition]
                playSong(nextSong, songPosition)
            }
        }

        findViewById<Button>(R.id.prev_BT).setOnClickListener{
            if (songPosition > 0) {
                songPosition -= 1
                val nextSong = songList[songPosition]
                playSong(nextSong, songPosition)
            } else {
                songPosition = songList.size - 1
                val nextSong = songList[songPosition]
                playSong(nextSong, songPosition)
            }
        }

        val seekbar = findViewById<SeekBar>(R.id.seekBar)
        seekbar.progress = 0
        seekbar.max = mediaPlayer?.duration!!
        seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser){
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                mediaPlayer?.pause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mediaPlayer?.start()
            }
        })

        runnable = Runnable {
            seekbar.progress = mediaPlayer?.currentPosition!!
            handler.postDelayed(runnable, 1000)
        }
        handler.postDelayed(runnable, 1000)
    }

    fun getSongList(context: Context): List<Song> {
        val songList = mutableListOf<Song>()

        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA)

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val cursor = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val song = Song(title, artist, path)
                songList.add(song)
            }
        }

        return songList
    }

    class SongAdapter(context: Context, val songs: List<MusicPlayer.Song>) : ArrayAdapter<MusicPlayer.Song>(context, R.layout.item_song, songs) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var itemView = convertView
            if (itemView == null) {
                val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                itemView = inflater.inflate(R.layout.item_song, parent, false)
            }

            val titleTextView: TextView = itemView!!.findViewById(R.id.titleTextView)
            val artistTextView: TextView = itemView.findViewById(R.id.artistTextView)

            if(titleTextView != null){
                val song = songs[position]
                titleTextView.text = song.title
                artistTextView.text = song.artist
            }

            return itemView
        }

        override fun getCount(): Int {
            return songs.size
        }
    }
}