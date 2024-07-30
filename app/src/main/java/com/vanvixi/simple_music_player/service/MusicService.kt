package com.vanvixi.simple_music_player.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vanvixi.simple_music_player.MainActivity
import com.vanvixi.simple_music_player.R
import com.vanvixi.simple_music_player.data.SongModel

enum class MusicServiceBroadcastAction {
    PLAYING, PAUSED, STOPPED, SEEK_UPDATE
}

enum class MusicServiceReceiverAction {
    SET_URI, PLAY, PAUSE, PLAY_OR_PAUSE, SEEK_TO, STOP
}

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        var isRunning = false
        private const val NOTIFICATION_CHANNEL_ID = "MusicServiceNotificationChannel"
        private const val NOTIFICATION_CHANNEL_NAME = "Music Channel"
    }

    private val musicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicServiceReceiverAction.SET_URI.name -> {
                    val uri: Uri? = intent.getParcelableExtra("musicUri")
                    val songModel: SongModel? = intent.getParcelableExtra("songModel") as? SongModel
                    onSetSource(uri, songModel)
                }

                MusicServiceReceiverAction.PLAY.name -> {
                    onPlay()
                }

                MusicServiceReceiverAction.PAUSE.name -> {
                    onPause()
                }

                MusicServiceReceiverAction.PLAY_OR_PAUSE.name -> {
                    onPlayOrPause()
                }

                MusicServiceReceiverAction.SEEK_TO.name -> {
                    val position = intent.getIntExtra("position", 0)
                    onSeekTo(position)
                }

                MusicServiceReceiverAction.STOP.name -> {
                    onStop()
                }
            }
        }
    }

//    private val handlerSeekUpdate = Handler(Looper.getMainLooper())
//    private val updateSeekBarRunnable = object : Runnable {
//        override fun run() {
//            if (mediaPlayer == null || !isRunning) return
//
//            if (!mediaPlayer!!.isPlaying) return
//
//            sendBroadcast(Intent(MusicServiceBroadcastAction.SEEK_UPDATE.name).apply {
//                putExtra("currentPosition", mediaPlayer!!.currentPosition)
//            })
//            handlerSeekUpdate.postDelayed(this, 1000)
//        }
//    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val filter = IntentFilter().apply {
            addAction(MusicServiceReceiverAction.SET_URI.name)
            addAction(MusicServiceReceiverAction.PLAY.name)
            addAction(MusicServiceReceiverAction.PAUSE.name)
            addAction(MusicServiceReceiverAction.PLAY_OR_PAUSE.name)
            addAction(MusicServiceReceiverAction.SEEK_TO.name)
            addAction(MusicServiceReceiverAction.STOP.name)
        }
        registerReceiver(musicReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri: Uri? = intent?.getParcelableExtra("musicUri")
        val songModel: SongModel? = intent?.getParcelableExtra("songModel") as? SongModel
        onSetSource(uri, songModel)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        mediaPlayer?.release()
        mediaPlayer = null
        unregisterReceiver(musicReceiver)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun onSetSource(uri: Uri?, songModel: SongModel?) {
        if (uri == null || songModel == null) return

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, uri)
        mediaPlayer?.setOnCompletionListener {
            sendBroadcast(Intent(MusicServiceBroadcastAction.STOPPED.name))
//            handlerSeekUpdate.removeCallbacks(updateSeekBarRunnable)
            stopSelf()
        }

        createNotificationChannel()
        val notification = createNotification(songModel.title, songModel.artist)
        startForeground(1, notification)
    }

    private fun onPlay() {
        if (mediaPlayer == null) return

        mediaPlayer!!.start()
        sendBroadcast(Intent(MusicServiceBroadcastAction.PLAYING.name))
//        handlerSeekUpdate.post(updateSeekBarRunnable)
    }

    private fun onPause() {
        if (mediaPlayer == null) return

        mediaPlayer!!.pause()
        sendBroadcast(Intent(MusicServiceBroadcastAction.PAUSED.name))
    }

    private fun onPlayOrPause() {
        if (mediaPlayer == null) return

        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            sendBroadcast(Intent(MusicServiceBroadcastAction.PAUSED.name))
            return
        }

        mediaPlayer!!.start()
        sendBroadcast(Intent(MusicServiceBroadcastAction.PLAYING.name))
//        handlerSeekUpdate.post(updateSeekBarRunnable)
    }

    private fun onSeekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    private fun onStop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
//        handlerSeekUpdate.removeCallbacks(updateSeekBarRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)

        stopSelf()
    }

    private fun createNotification(title: String, artist: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Channel for playing music"
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}