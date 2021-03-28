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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.lk.ava.fragments.Home
import com.lk.ava.fragments.Search
import com.lk.ava.fragments.Playlists
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Adapter for ViewPager on MainActivity.
 */
@ExperimentalCoroutinesApi
class ViewPagerAdapter(fm: FragmentManager, private val home: Home):
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> home
            1 -> Search()
            else -> Playlists()
        }
    }

    override fun getCount() = 3
}