package com.rookie.webrtc

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class Command(
    val id: String,
    val from: String,
    val to: String,
    val toName: String
) {

    companion object{
        val REGISTER="register"
        val CALL="call"
        val REGISTER_RESPONSE="register_response"
        val CALL_RESPONSE="call_response"
        val INCALL="incall"
        val INCALL_REPONSE="incall_response"
        val OFFER="offer"
        val CANDIDATE="candidate"

        val SUCCEED = 1
        val FAILURE=2
    }

    var sessionDescription: SessionDescription? = null
    var iceCandidate: IceCandidate? = null
    var code = 0
}
