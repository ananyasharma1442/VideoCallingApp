let videoLocal = document.getElementById("local-video")
let videoRemote = document.getElementById("remote-video")

videoLocal.style.opacity = 0
videoRemote.style.opacity = 0

videoLocal.onplaying = () => { videoLocal.style.opacity = 1 }
videoRemote.onplaying = () => { videoRemote.style.opacity = 1 }

let peer
async function init(userId) {
    peer = new Peer(userId, {
        host: '192.168.0.103',
        port: 9000,
        path: '/VideoCallingApp'
    })

    peer.on('open', ()=>{
        Android.onPeerConnected()
    })
    listen()
}

let localStream
async function listen() {
    peer.on('call', (call) => {
        navigator.getUserMedia({
            audio: true, 
            video: true
        }, (stream) => {
            videoLocal.srcObject = stream
            localStream = stream
            call.answer(stream)
            call.on('stream', (remoteStream) => {
                videoRemote.srcObject = remoteStream
                videoRemote.className = "primary-video"
                videoLocal.className = "secondary-video"
            })
        })
    })
}

async function startCall(otherUserId) {
    navigator.getUserMedia({
        audio: true,
        video: true
    }, (stream) => {

        videoLocal.srcObject = stream
        localStream = stream

        const call = peer.call(otherUserId, stream)
        call.on('stream', (remoteStream) => {
            videoRemote.srcObject = remoteStream
            videoRemote.className = "primary-video"
            videoLocal.className = "secondary-video"
        })

    })
}

async function toggleVideo(b) {
    if (b == "true") {
        localStream.getVideoTracks()[0].enabled = true
    } else {
        localStream.getVideoTracks()[0].enabled = false
    }
}

async function toggleAudio(b) {
    if (b == "true") {
        localStream.getAudioTracks()[0].enabled = true
    } else {
        localStream.getAudioTracks()[0].enabled = false
    }
}

async function toggleCamera(b){
}




