package com.lk.ava.models

import android.os.ResultReceiver

class DownloadableSong(val name: String, val artist: String,
                       val youtubeLink: String, val ytmThumbnailLink: String,
                       val resultReceiver: ResultReceiver)