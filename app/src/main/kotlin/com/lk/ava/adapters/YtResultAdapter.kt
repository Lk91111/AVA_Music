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

package com.lk.ava.adapters

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lk.ava.R
import com.lk.ava.fragments.Search
import com.lk.ava.models.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.ref.WeakReference

/**
 * Shows results in the search fragment when the search mode is set to regular YouTube.
 */
@ExperimentalCoroutinesApi
class YtResultAdapter(private val songList: ArrayList<Song>, private val wr: WeakReference<Search>): RecyclerView.Adapter<YtResultAdapter.RVVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RVVH =
        RVVH(LayoutInflater.from(parent.context).inflate(R.layout.rv_result, parent, false))

    override fun getItemCount() = songList.size

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RVVH, position: Int) {
        val current = songList[position]
        holder.vidName.text = " " + current.name
        holder.vidChannel.text = " " + current.artist

        holder.vidName.typeface =
            Typeface.createFromAsset(holder.vidName.context.assets, "fonts/inter.otf")

        holder.vidChannel.typeface =
            Typeface.createFromAsset(holder.vidName.context.assets, "fonts/inter.otf")

        holder.titleTxt.typeface =
            Typeface.createFromAsset(holder.vidName.context.assets, "fonts/interbold.otf")

        holder.uploaderTxt.typeface =
            Typeface.createFromAsset(holder.vidName.context.assets, "fonts/interbold.otf")

        holder.titleTxt.isSelected = true
        holder.uploaderTxt.isSelected = true
        holder.itemView.setOnClickListener {
            wr.get()?.itemPressed(songList[position])
        }
        /* Will Show Full Name for the Song held */
        holder.itemView.setOnLongClickListener {
            holder.vidName.isSingleLine = false
           // holder.vidName.setLines(Int.MAX_VALUE)
            notifyItemChanged(position)
            true
        }
    }

    class RVVH(itemView: View): RecyclerView.ViewHolder(itemView) {
        val vidName = itemView.findViewById<TextView>(R.id.vid_name)!!
        val vidChannel = itemView.findViewById<TextView>(R.id.vid_uploader)!!
        val titleTxt = itemView.findViewById<TextView>(R.id.title_txt)!!
        val uploaderTxt = itemView.findViewById<TextView>(R.id.uploader_txt)!!
    }
}

