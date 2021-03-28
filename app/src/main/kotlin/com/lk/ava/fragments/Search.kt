package com.lk.ava.fragments

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lk.ava.R
import com.lk.ava.adapters.YtResultAdapter
import com.lk.ava.adapters.YtmResultAdapter
import com.lk.ava.models.Song
import com.lk.ava.utils.Shared
import com.lk.ava.utils.SwipeController
import kotlinx.android.synthetic.main.search.*
import kotlinx.coroutines.*
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.lang.ref.WeakReference
import java.util.Collections.singletonList

/**
 * The second fragment. Used to search for songs.
 */
@ExperimentalCoroutinesApi
class Search : Fragment(), CoroutineScope {
    private lateinit var itemPressed: SongCallback
    private lateinit var sp: SharedPreferences
    companion object {
        val resultArray = ArrayList<Song>()
    }
    interface SongCallback {
        fun sendItem(song: Song , mode:String = "")
    }
    
    override val coroutineContext = Dispatchers.Main + SupervisorJob()

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.search, container, false)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            itemPressed = context as Activity as SongCallback
        } catch (e: ClassCastException) {
            throw ClassCastException(
                activity.toString()
                        + " must implement SongCallback"
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sp = requireContext().getSharedPreferences("search", 0)

        when(sp.getString("mode", "Music")){
            "Album" -> {
                search_mode.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.mode_album))
            }

            "Playlists" -> {
                search_mode.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.mode_playlist))
            }
        }

        View.OnClickListener {
            when (sp.getString("mode", "Music")) {
                "Music" -> {
                    sp.edit().putString("mode", "Album").apply()
                    search_mode.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.mode_album
                        )
                    )
                }

                "Album" -> {
                    sp.edit().putString("mode", "Playlists").apply()
                    search_mode.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.mode_playlist
                        )
                    )
                }

                "Playlists" -> {
                    sp.edit().putString("mode", "Music").apply()
                    search_mode.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.mode_music
                        )
                    )
                }
            }
        }.also {
            search_mode.setOnClickListener(it)
            search_mode_pr.setOnClickListener(it)
        }
        loading_view.enableMergePathsForKitKatAndAbove(true)
        getItems(view.findViewById(R.id.search_bar),view.findViewById(R.id.search_rv))
    }

    private fun getItems(searchBar:EditText, searchRv:RecyclerView){
        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == 6) {
                if(Shared.isInternetConnected(requireContext())){
                    loading_view.progress = 0.3080229f
                    loading_view.playAnimation()

                    if (searchRv.visibility == View.VISIBLE) {
                        searchRv.animate().alpha(0f).duration = 200
                        searchRv.visibility = View.GONE
                    }
                    val text = searchBar.text
                    if (loading_view.visibility == View.GONE) {
                        loading_view.alpha = 0f
                        loading_view.visibility = View.VISIBLE
                        loading_view.animate().alpha(1f).duration = 200
                    }

                    hideKeyboard(activity as Activity)
                    resultArray.clear()
                    try {
                        var query = text.toString()
                        if (query.isEmpty())
                            query = "songs"
                        val useYtMusic: Boolean = when {
                            text.startsWith("!") -> {
                                query = text.toString().replaceFirst(Regex("^!\\s*"), "")
                                true
                            }

                            text.startsWith("?") -> {
                                query = text.toString().replaceFirst(Regex("^?\\s*"), "")
                                false
                            }

                            else -> (PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .getString("source_key", "Youtube Music") == "Youtube Music")
                        }

                        launch(Dispatchers.IO) {
                            if (useYtMusic) {
                                when (sp.getString("mode", "Music")) {
                                    "Music" -> {
                                        val extractor = YouTube.getSearchExtractor(
                                            query, singletonList(
                                                YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
                                            ), ""
                                        )

                                        extractor.fetchPage()

                                        for (song in extractor.initialPage.items) {
                                            val ex = song as StreamInfoItem
                                            if(song.thumbnailUrl.contains("ytimg")) {
                                                val songId = Shared.getIdFromLink(ex.url)
                                                song.thumbnailUrl = "https://i.ytimg.com/vi/$songId/maxresdefault.jpg"
                                            }
                                            resultArray.add(
                                                Song(
                                                    name = ex.name,
                                                    artist = ex.uploaderName,
                                                    youtubeLink = ex.url,
                                                    ytmThumbnail = song.thumbnailUrl
                                                )
                                            )
                                        }
                                    }

                                    "Album" -> {
                                        val extractor = YouTube.getSearchExtractor(
                                            query, singletonList(
                                                YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS
                                            ), ""
                                        )

                                        extractor.fetchPage()

                                        for (song in extractor.initialPage.items) {
                                            val ex = song as PlaylistInfoItem
                                            if(song.thumbnailUrl.contains("ytimg")) {
                                                val songId = Shared.getIdFromLink(ex.url)
                                                song.thumbnailUrl = "https://i.ytimg.com/vi/$songId/maxresdefault.jpg"
                                            }
                                            resultArray.add(
                                                Song(
                                                    name = ex.name,
                                                    artist = ex.uploaderName,
                                                    youtubeLink = ex.url,
                                                    ytmThumbnail = song.thumbnailUrl
                                                )
                                            )
                                        }
                                    }

                                    "Playlists" -> {
                                        val extractor = if (query.startsWith("https://"))
                                            YouTube.getPlaylistExtractor(query)
                                        else
                                            YouTube.getSearchExtractor(
                                                query, singletonList(
                                                    YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS
                                                ), ""
                                            )

                                        extractor.fetchPage()

                                        for (song in extractor.initialPage.items) {
                                            val ex = song as PlaylistInfoItem
                                            resultArray.add(
                                                Song(
                                                    name = ex.name,
                                                    artist = ex.uploaderName,
                                                    youtubeLink = ex.url,
                                                    ytmThumbnail = song.thumbnailUrl
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                val extractor = YouTube.getSearchExtractor(
                                    query, singletonList(
                                        YoutubeSearchQueryHandlerFactory.VIDEOS
                                    ), ""
                                )

                                extractor.fetchPage()

                                for (song in extractor.initialPage.items) {
                                    val ex = song as StreamInfoItem
                                    if(song.thumbnailUrl.contains("ytimg")) {
                                        val songId = Shared.getIdFromLink(ex.url)
                                        song.thumbnailUrl = "https://i.ytimg.com/vi/$songId/maxresdefault.jpg"
                                    }
                                    resultArray.add(
                                        Song(
                                            name = ex.name,
                                            artist = ex.uploaderName,
                                            youtubeLink = ex.url,
                                            ytmThumbnail = song.thumbnailUrl
                                        )
                                    )
                                }
                            }

                            launch(Dispatchers.Main) {
                                if (useYtMusic)
                                    searchRv.adapter =
                                        YtmResultAdapter(
                                            resultArray,
                                            WeakReference(this@Search),
                                            sp.getString("mode", "Music") ?: "Music"
                                        )
                                else
                                    searchRv.adapter =
                                        YtResultAdapter(resultArray, WeakReference(this@Search))
                                searchRv.layoutManager = LinearLayoutManager(requireContext())
                                loading_view.visibility = View.GONE
                                loading_view.pauseAnimation()
                                searchRv.alpha = 0f
                                searchRv.visibility = View.VISIBLE
                                searchRv.animate().alpha(1f).duration = 200
                                val itemTouchHelper= ItemTouchHelper(SwipeController(
                                    context,
                                    "Search",
                                    null
                                ))
                                itemTouchHelper.attachToRecyclerView(searchRv)
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Something failed!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                else
                    Toast.makeText(requireContext(),"No Internet Connection", Toast.LENGTH_LONG).show()
            }
            false
        }
    }

    fun itemPressed(song: Song) {
        if(Shared.isInternetConnected(requireContext()))
            itemPressed.sendItem(song)
        else
            Toast.makeText(requireContext(),"No Internet Connection", Toast.LENGTH_LONG).show()
    }

    private fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus ?: View(activity)

        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}