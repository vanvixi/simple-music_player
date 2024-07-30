package com.vanvixi.simple_music_player

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.vanvixi.simple_music_player.data.SongModel
import com.vanvixi.simple_music_player.databinding.ActivityMainBinding
import com.vanvixi.simple_music_player.service.MusicService
import com.vanvixi.simple_music_player.service.MusicServiceBroadcastAction
import com.vanvixi.simple_music_player.service.MusicServiceReceiverAction

class MainActivity : AppCompatActivity() {
    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val readStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onPickSong()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val selectSongIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val musicUri = result.data?.data ?: return@registerForActivityResult
            onSongPicked(musicUri)
        } else {
            Toast.makeText(this, "Song not selected", Toast.LENGTH_SHORT).show()
        }
    }

    private val mediaServiceStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicServiceBroadcastAction.PLAYING.name -> {
                    binding.btnPlayOrPause.text = getString(R.string.pause)
                }

                MusicServiceBroadcastAction.PAUSED.name -> {
                    binding.btnPlayOrPause.text = getString(R.string.play)
                }

//                MusicServiceBroadcastAction.SEEK_UPDATE.name -> {
//                    val currentPosition = intent.getIntExtra("currentPosition", 0)
//
//                    binding.seekBar.progress = currentPosition
//                }

                MusicServiceBroadcastAction.STOPPED.name -> {
                    binding.btnPlayOrPause.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnPlayOrPause.visibility = View.GONE
//        binding.seekBar.visibility = View.GONE
        setUpClickListener()
//        setUpSeekBarChangeListener();
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(MusicServiceBroadcastAction.PLAYING.name)
            addAction(MusicServiceBroadcastAction.PAUSED.name)
            addAction(MusicServiceBroadcastAction.STOPPED.name)
            addAction(MusicServiceBroadcastAction.SEEK_UPDATE.name)
        }
        registerReceiver(mediaServiceStatusReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(mediaServiceStatusReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent(this, MusicService::class.java)
        stopService(intent)
    }

    private fun setUpClickListener() {
        binding.btnPickSong.setOnClickListener { onCheckPermissionOrPickSong() }

        binding.btnPlayOrPause.setOnClickListener { onPlayOrPause() }

//        binding.btnPauseMusic.setOnClickListener { onPauseMusic() }
    }

//    private fun setUpSeekBarChangeListener() {
//        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                if (fromUser) {
//                    sendBroadcast(Intent(MusicServiceReceiverAction.SEEK_TO.name).apply {
//                        putExtra("position", progress)
//                    })
//                }
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
//        })
//    }

    private fun onCheckPermissionOrPickSong() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            readStoragePermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            return
        }

        onPickSong()
    }

    private fun onPickSong() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
        }

        selectSongIntentLauncher.launch(intent)
    }

    private fun onSongPicked(musicUri: Uri) {
        MediaMetadataRetriever().use {
            it.setDataSource(this, musicUri)
            val title = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown"
            val artist = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown"
            val album = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown"
            val duration = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0
            binding.txtSongInfo.text = "Title: $title\nArtist: $artist\nAlbum: $album\nDuration: ${formatDuration(duration)}"
            val songModel: SongModel = SongModel(title, artist, album, duration)

            if (!MusicService.isRunning) {
                startForegroundService(Intent(this, MusicService::class.java).apply {
                    putExtra("musicUri", musicUri)
                    putExtra("songModel", songModel)
                })
            } else {
                sendBroadcast(Intent(MusicServiceReceiverAction.SET_URI.name).apply {
                    putExtra("musicUri", musicUri)
                })
            }

            if (binding.btnPlayOrPause.visibility == View.GONE) {
                binding.btnPlayOrPause.visibility = View.VISIBLE
            }

//            if (binding.seekBar.visibility == View.GONE) {
//                binding.seekBar.visibility = View.VISIBLE
//                binding.seekBar.max = duration
//            }
        }

    }

    private fun formatDuration(duration: Int): String {
        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun onPlayOrPause() {
        sendBroadcast(Intent(MusicServiceReceiverAction.PLAY_OR_PAUSE.name))
    }

    private fun onPauseMusic() {
        sendBroadcast(Intent(MusicServiceReceiverAction.PAUSE.name))
    }
}