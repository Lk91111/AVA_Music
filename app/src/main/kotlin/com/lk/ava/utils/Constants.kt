/*
    Copyright 2020 LK Verma <laxmikant91111@gmail.com>

    This file is part of AVAMusicPlayer.

    AVAMusicPlayer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, version 3 of the License.

    AVAMusicPlayer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with AVAMusicPlayer.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.lk.ava.utils

import android.os.Environment
import java.io.File

class Constants {
    companion object {
        /** a File object pointing to the folder where playlist JSONs will be stored */
        @Suppress("DEPRECATION")
        val playlistFolder = File(
            Environment.getExternalStorageDirectory(),
            "AVAMusic/playlists")

        /**
         * a File object pointing to the folder where all AVAMusic related files
         * will be stored.
         */
        @Suppress("DEPRECATION")
        val ableSongDir = File(
            Environment.getExternalStorageDirectory(),
            "AVAMusic")

        /**
         * a File object pointing to the folder where all songs imported from Spotify
         * will be stored.
         */
        val playlistSongDir = File(ableSongDir.absolutePath + "/playlist_songs")

        /** a File object pointing to the folder where album art JPGs will be stored */
        val albumArtDir = File(ableSongDir.absolutePath + "/album_art")

        /** a File object pointing to the folder where temporary items will be stored */
        val cacheDir = File(ableSongDir.absolutePath + "/cache")

        /**
         * API keys and version code names which *should* be replaced during compilation.
         */
        const val FLURRY_KEY = "INSERT_FLURRY_KEY"
        const val RAPID_API_KEY= "INSERT_RAPID_KEY"
        const val VERSION = "Debug"

        const val DEEZER_API = "https://deezerdevs-deezer.p.rapidapi.com/search?q="
        const val CHANNEL_ID = "AVAMusicDownload"
    }
}