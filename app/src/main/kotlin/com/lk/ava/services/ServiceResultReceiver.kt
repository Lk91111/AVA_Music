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

package com.lk.ava.services

import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver

class ServiceResultReceiver(handler: Handler?) : ResultReceiver(handler) {
    private var mReceiver: Receiver? = null

    fun setReceiver(receiver: Receiver?) {
        mReceiver = receiver
    }

    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
        mReceiver?.onReceiveResult(resultCode)
    }

    interface Receiver {
        fun onReceiveResult(resultCode: Int)
    }
}