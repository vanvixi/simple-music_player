package com.vanvixi.simple_music_player.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SongModel(val title: String, val artist: String, val album: String, val duration: Int) : Parcelable

