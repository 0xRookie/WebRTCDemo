package com.rookie.webrtc

import android.util.Log
import org.webrtc.*

open class PeerConnectionObserver : PeerConnection.Observer, DataChannel.Observer {
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        Log.e("PeerConnectObserver", "onSignalingChange() ${p0?.name}")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        Log.e("PeerConnectObserver", "onIceConnectionChange() ${p0?.name}")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Log.e("PeerConnectObserver", "onIceConnectionReceivingChange() ${p0}")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        Log.e("PeerConnectObserver", "onIceGatheringChange() ${p0?.name}")
    }

    override fun onIceCandidate(p0: IceCandidate?) {
        Log.e("PeerConnectObserver", "onIceCandidate() ${p0}")

    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        Log.e("PeerConnectObserver", "onIceCandidatesRemoved() ${p0}")
    }

    override fun onAddStream(p0: MediaStream?) {
        Log.e("PeerConnectObserver", "onAddStream() ${p0}")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        Log.e("PeerConnectObserver", "onRemoveStream() ${p0}")
    }

    override fun onDataChannel(p0: DataChannel?) {
        Log.e("PeerConnectObserver", "onDataChannel() ${p0}")
        p0?.registerObserver(this)
    }

    override fun onRenegotiationNeeded() {
        Log.e("PeerConnectObserver", "onRenegotiationNeeded()")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        Log.e("PeerConnectObserver", "onAddTrack() ${p0}\n${p1}")
    }

    override fun onBufferedAmountChange(p0: Long) {
        Log.e("DataChannelObserver", "onBufferedAmountChange() ${p0}")

    }

    override fun onStateChange() {
        Log.e("DataChannelObserver", "onStateChange()")
    }

    override fun onMessage(p0: DataChannel.Buffer?) {
        Log.e("DataChannelObserver", "onMessage() ${p0}")
    }

}