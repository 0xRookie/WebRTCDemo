package com.rookie.webrtc

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.google.gson.Gson
import com.rookie.webrtc.databinding.ActivityMainBinding
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.webrtc.*
import java.net.URI

class MainActivity : AppCompatActivity() {
    lateinit var mBinding: ActivityMainBinding
    lateinit var websocket: WebSocketClient
    lateinit var eglBase: EglBase
    lateinit var peerConnectionFactory: PeerConnectionFactory
    lateinit var iceServers: MutableList<PeerConnection.IceServer>
    lateinit var peerConnection: PeerConnection
    lateinit var videoTrack: VideoTrack
    lateinit var audioTrack: AudioTrack
    lateinit var streamList: MutableList<String>
    lateinit var channel: DataChannel
    lateinit var sdpObserver: MySdpObserver

    val renderList = ArrayList<SurfaceViewRenderer>()

    lateinit var from: String
    lateinit var to: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mBinding.remotes.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val length = mBinding.remotes.measuredWidth / 3
                mBinding.remotes.children.iterator().forEach {
                    it.layoutParams.height = length
                    it.layoutParams.width = length
                    it.requestLayout()
                }
                mBinding.remotes.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        renderList.add(mBinding.local)
        renderList.add(mBinding.remote2)
        renderList.add(mBinding.remote3)
        renderList.add(mBinding.remote4)
        renderList.add(mBinding.remote5)
        renderList.add(mBinding.remote6)

        streamList = ArrayList()
        iceServers = ArrayList()
        val split = mBinding.stun.text.toString().split(";")
        split.forEach {
            iceServers.add(
                PeerConnection.IceServer.builder(it)
                    .createIceServer()
            )
        }

        mBinding.connect.setOnClickListener {
            connectSignaling(mBinding.signalingAddress.text.toString())
        }
        mBinding.call.setOnClickListener {
            call()
        }
        mBinding.reject.setOnClickListener {
            reject()
        }
        mBinding.accept.setOnClickListener {
            accept()
        }
        mBinding.mute.setOnClickListener {
            mute()
        }
        mBinding.hangUp.setOnClickListener {
            hangup()
        }
        mBinding.switchCamera.setOnClickListener {
            switchCamera()
        }
    }

    private fun switchCamera() {

    }

    private fun hangup() {
        finish()
    }

    private fun mute() {

    }

    private fun accept() {
        val command = Command(Command.INCALL, from, to, to)
        command.code = Command.SUCCEED
        val json = Gson().toJson(command)
        websocket.send(json)
        Log.e("WebRTC", "accept() ${command}")
    }

    private fun reject() {
        val command = Command(Command.INCALL, from, to, to)
        command.code = Command.FAILURE
        val json = Gson().toJson(command)
        websocket.send(json)
        Log.e("WebRTC", "reject() ${command}")
    }

    private fun call() {
        val command = Command(Command.CALL, from, to, to)
        val json = Gson().toJson(command)
        websocket.send(json)
        Log.e("WebRTC", "call() ${command}")
    }

    fun connectSignaling(url: String) {
        websocket = object : WebSocketClient(URI.create(url)) {

            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.e(
                    "WebRTC",
                    "onOpen() ${handshakedata?.httpStatus}:${handshakedata?.httpStatusMessage}"
                )
                val command = Command(Command.REGISTER, from, to, to)
                val json = Gson().toJson(command)
                websocket.send(json)

                Log.e("WebRTC", "send() ${json}")

                mBinding.connectView.visibility = View.GONE
                mBinding.callView.visibility = View.GONE
                mBinding.answerView.visibility = View.GONE
            }

            override fun onMessage(message: String?) {
                Log.e("WebRTC", "onMessage() ${message}")
                message?.let {
                    val command = Gson().fromJson(it, Command::class.java)
                    command?.let { com ->
                        when (com.id) {
                            Command.REGISTER_RESPONSE -> {
                                if (com.code == Command.SUCCEED) {
                                    createPeerConnection()
                                    Log.e("WebSocket", "连接成功")
                                } else {
                                    Log.e("WebSocket", "注册失败")
                                }
                            }
                            Command.CALL_RESPONSE -> {
                                if (com.code == Command.SUCCEED) {
                                    Log.e("WebSocket", "${com.from}在线,创建SDP OFFER")
                                    createOffer()
                                } else {
                                    Log.e("WebSocket", "${com.from}不在线")
                                }
                            }
                            Command.INCALL -> {
                                Log.e("WebSocket", "收到电话")
                                mBinding.connectView.visibility = View.GONE
                                mBinding.callView.visibility = View.GONE
                                mBinding.answerView.visibility = View.VISIBLE
                            }
                            Command.INCALL_REPONSE -> {
                                if (com.code == Command.SUCCEED) {
                                    Log.e("WebSocket", "${com.from}同意接听")
                                    createOffer()
                                } else {
                                    Log.e("WebSocket", "${com.from}拒绝接听")
                                }
                            }
                            Command.OFFER -> {
                                Log.e("WebSocket", "收到${com.from} SDP ANSWER")
                                peerConnection.setRemoteDescription(
                                    sdpObserver,
                                    com.sessionDescription
                                )
                                createAnswer()
                            }
                            Command.CANDIDATE -> {
                                if (com.iceCandidate != null) {
                                    Log.e("WebSocket", "收到${com.from} Ice Candidate")
                                    peerConnection.addIceCandidate(com.iceCandidate)
                                }
                            }
                            else -> {
                                Log.e("WebSocket", "未注册ID")
                            }

                        }
                        com
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.e("WebRTC", "onClose() ${code}:${reason}:${remote}")
                mBinding.connectView.visibility = View.VISIBLE
                mBinding.callView.visibility = View.GONE
                mBinding.answerView.visibility = View.GONE
            }

            override fun onError(ex: Exception?) {
                Log.e("WebRTC", "onError()", ex)
                mBinding.connectView.visibility = View.VISIBLE
                mBinding.callView.visibility = View.GONE
                mBinding.answerView.visibility = View.GONE
            }


        }
    }

    private fun createAnswer() {
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        peerConnection.createAnswer(sdpObserver, mediaConstraints)
    }

    private fun createOffer() {
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        peerConnection.createOffer(sdpObserver, mediaConstraints)
    }

    fun createPeerConnection() {
        val initialOption = PeerConnectionFactory.InitializationOptions.builder(this)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(initialOption);

        eglBase = EglBase.create()
        val options = PeerConnectionFactory.Options()
        options.disableEncryption = true
        options.disableNetworkMonitor = true
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setOptions(options)
            .createPeerConnectionFactory()

        val configuration = PeerConnection.RTCConfiguration(iceServers)

        peerConnection = peerConnectionFactory.createPeerConnection(configuration,
            object : PeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                    val command = Command(Command.CANDIDATE, from, to, to)
                    command.iceCandidate = p0
                    val json = Gson().toJson(command)
                    websocket.send(json)
                    Log.e("PeerConnectionObserver", "sendIceCandidate() ${json}")
                }

                override fun onAddStream(stream: MediaStream?) {
                    super.onAddStream(stream)

                    if (stream != null) {
                        streamList.add(stream.id)
                        val videoTracks = stream.videoTracks
                        if (videoTracks.size > 0) {
                            videoTracks[0].addSink(renderList[streamList.size])
                        }

                        val audioTracks = stream.audioTracks
                        if (audioTracks.size > 0) {
                            audioTracks[0].setVolume(50.0)
                        }
                    }
                }

                override fun onRemoveStream(p0: MediaStream?) {
                    super.onRemoveStream(p0)

                }

            })!!

        val init = DataChannel.Init()
        channel = peerConnection.createDataChannel("Channel1", init)

        initView()
        initObserver()
    }

    private fun initObserver() {
        sdpObserver = object : MySdpObserver() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                super.onCreateSuccess(p0)
                peerConnection.setLocalDescription(this, p0)
                if (peerConnection.localDescription.type == SessionDescription.Type.OFFER) {
                    offer(p0)
                } else if (peerConnection.localDescription.type == SessionDescription.Type.ANSWER) {
                    answer(p0)
                } else if (peerConnection.localDescription.type == SessionDescription.Type.PRANSWER) {

                }
            }

        }
    }

    private fun answer(p0: SessionDescription?) {
        val command = Command(Command.OFFER, from, to, to)
        command.sessionDescription = p0
        val json = Gson().toJson(command)
        Log.e("WebRTC", "answer() ${json}")
        websocket.send(json)
    }

    private fun offer(p0: SessionDescription?) {
        val command = Command(Command.OFFER, from, to, to)
        command.sessionDescription = p0
        val json = Gson().toJson(command)
        Log.e("WebRTC", "offer() ${json}")
        websocket.send(json)
    }

    private fun initView() {
        initSurfaceView(mBinding.local)
        initSurfaceView(mBinding.remote2)
        initSurfaceView(mBinding.remote3)
        initSurfaceView(mBinding.remote4)
        initSurfaceView(mBinding.remote5)
        initSurfaceView(mBinding.remote6)
        startVideoCapture(mBinding.local)
        startAudioCapture()
    }


    private fun initSurfaceView(surfaceViewRenderer: SurfaceViewRenderer) {
        surfaceViewRenderer.init(eglBase.eglBaseContext, null)
        surfaceViewRenderer.setMirror(true)
        surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
        surfaceViewRenderer.keepScreenOn = true
        surfaceViewRenderer.setZOrderMediaOverlay(true)
        surfaceViewRenderer.setEnableHardwareScaler(true)
    }

    fun startVideoCapture(surfaceViewRenderer: SurfaceViewRenderer) {
        val videoSource = peerConnectionFactory.createVideoSource(true)
        val surfaceTextureHelper = SurfaceTextureHelper.create("RTCRender", eglBase.eglBaseContext)
        val videoCapturer = createVideoCapturer()

        if (videoCapturer != null) {
            videoCapturer.initialize(surfaceTextureHelper, this, videoSource.capturerObserver)
            videoCapturer.startCapture(1280, 720, 25)

            val videoStream = peerConnectionFactory.createLocalMediaStream("videostream")
            videoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource)
            videoTrack.addSink(surfaceViewRenderer)
            videoStream.addTrack(videoTrack)
            peerConnection.addTrack(videoTrack)
            peerConnection.addStream(videoStream)
        }
    }

    fun startAudioCapture() {
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googEchoCancellation",
                "true"
            )
        )
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        mediaConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googNoiseSuppression",
                "true"
            )
        )

        val audioSource = peerConnectionFactory.createAudioSource(mediaConstraints)
        audioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource)

        val audioStream = peerConnectionFactory.createLocalMediaStream("audio_stream")
        audioStream.addTrack(audioTrack)
        audioTrack.setVolume(50.0)
        peerConnection.addTrack(audioTrack)
        peerConnection.addStream(audioStream)


    }

    private fun close() {
        if (peerConnection != null) {
            peerConnection.close()
        }
        if (websocket != null) {
            websocket.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        close()
    }
    /**
     * 准备摄像头
     *
     * @return
     */
    private fun createVideoCapturer(): VideoCapturer? {
        return if (Camera2Enumerator.isSupported(this)) {
            createCameraCapturer(Camera2Enumerator(this))
        } else {
            createCameraCapturer(Camera1Enumerator(true))
        }
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera
        Log.d("WebRTC", "Looking for front facing cameras.")
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(
                    "WebRTC",
                    "Creating front facing camera capturer."
                )
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else
        Log.d("WebRTC", "Looking for other cameras.")
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d("WebRTC", "Creating other camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }
}