package com.lk.ava.models

import kotlinx.coroutines.sync.Mutex

class Song(
    val name: String,
    var artist: String="",
    var youtubeLink: String = "",
    var filePath: String = "",
    var placeholder: Boolean = false,
    var ytmThumbnail: String = "",
    val albumId: Long = -1,
    var isLocal: Boolean = false,
    var cacheStatus: CacheStatus = CacheStatus.NULL,
    var streamProg: Int = 0
) {
    lateinit var streamMutexes: Array<Mutex>
    lateinit var internalStream: ByteArray // SHOULDN'T BE USED FOR PLAYING
    lateinit var streams: Array<ByteArray>
}