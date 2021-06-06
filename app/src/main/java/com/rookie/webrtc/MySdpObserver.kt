package com.rookie.webrtc

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class MySdpObserver:SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {
        Log.e("SdpObserver","onCreateSuccess() ${p0?.type?.canonicalForm()}")
    }

    override fun onSetSuccess() {
        Log.e("SdpObserver","onSetSuccess()")
    }

    override fun onCreateFailure(p0: String?) {
        Log.e("SdpObserver","onCreateFailure() ${p0}")
    }

    override fun onSetFailure(p0: String?) {
        Log.e("SdpObserver","onSetFailure() ${p0}")
    }
}