package com.example.videocallingapp

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.videocallingapp.databinding.ActivityCallBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    var username = ""
    var friendsUsername = ""
    var isPeerConnected = false
    var firebaseRef = Firebase.database.getReference("users")
    var historyRef = Firebase.database.getReference("history")
    var isAudio  = true
    var isVideo = true
    var cameraId = true
    private lateinit var recyclerView : RecyclerView
    private var history_usernames: ArrayList<User>? = ArrayList()
    private lateinit var adapter : RecyclerViewAdapter
    private var username_map : HashMap<String, String>? = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        Log.e("r", "call activity")

        recyclerView =findViewById(R.id.contact_recycler_view)

        username = intent.getStringExtra("username")!!

        adapter = RecyclerViewAdapter(history_usernames!!, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        Log.e("0","Called")
        fetchListToHashMap()

        binding.callBtn.setOnClickListener {
            friendsUsername = binding.friendNameEdit.text.toString()
            if(!username_map!!.containsKey(friendsUsername)) {
                username_map!![friendsUsername] = friendsUsername
                historyRef.child(username).setValue(username_map)
                HashMaptoList()
                Log.e("not", "here")
            }
            sendCallRequest()
        }

        binding.toggleAudioBtn.setOnClickListener {
            isAudio = !isAudio
            callJavascriptFunction("javascript:toggleAudio(\"${isAudio}\")")
            binding.toggleAudioBtn.setImageResource(if (isAudio) R.drawable.ic_baseline_mic_24 else R.drawable.ic_baseline_mic_off_24)
        }

        binding.toggleVideoBtn.setOnClickListener {
            isVideo = !isVideo
            callJavascriptFunction("javascript:toggleVideo(\"${isVideo}\")")
            binding.toggleVideoBtn.setImageResource(if (isVideo) R.drawable.ic_baseline_videocam_24 else R.drawable.ic_baseline_videocam_off_24)
        }


        binding.endCall.setOnClickListener {
            binding.webView.loadUrl("about:blank")
            firebaseRef.child(username).setValue(null)
            firebaseRef.child(friendsUsername).setValue(null)
            jup()
        }

        binding.toggleCameraBtn.setOnClickListener {
            cameraId = !cameraId
            TODO()
            callJavascriptFunction("javascript:toggleCamera(\"${cameraId}\")")
        }


        setupWebView()
    }

    private fun HashMaptoList() {
        history_usernames!!.clear()
        username_map!!.forEach {
            history_usernames!!.add(User(it.key))
            Log.e("3 in for",history_usernames!!.size.toString())
        }
        adapter.notifyDataSetChanged()
        Log.e("3",history_usernames!!.size.toString())
        if(history_usernames!!.size == 0)
        {
            binding.conditionNofrnd.visibility = View.VISIBLE
        }
    }

    private fun fetchListToHashMap() {
        historyRef.child(username).addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(i in snapshot.children)
                {
                    var s: String = i.value.toString()
                    username_map!![s] = s
                    Log.e("1", s + "added")
                }
                HashMaptoList()
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
        Log.e("2","added")
    }

    private fun checkForFriend(){
        firebaseRef.child(friendsUsername).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == null) {
                    jup()
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun jup() {
        binding.webView.loadUrl("about:blank")
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun sendCallRequest() {
        if(!isPeerConnected)
        {
            Toast.makeText(this, "Check Internet", Toast.LENGTH_LONG).show()
            return
        }
        firebaseRef.child(friendsUsername).child("incoming").setValue(username)
        firebaseRef.child(friendsUsername).child("isAvailable").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value.toString() == "true") {
                    listenForConnId()
                    Log.e("at","Peer connected")
                    checkForFriend()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun listenForConnId() {
        firebaseRef.child(friendsUsername).child("connId").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == null) {
                    return
                }
                switchToControls()
                callJavascriptFunction("javascript:startCall(\"${snapshot.value}\")")
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun setupWebView() {
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false
        binding.webView.addJavascriptInterface(JavascriptInterface(this), "Android")

        loadVideoCall()
    }

    private fun loadVideoCall() {
        val filePath = "file:android_asset/call.html"
        binding.webView.loadUrl(filePath)
        binding.webView.webViewClient = object : WebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                initializePeer()
            }
        }

    }

    fun onClickCalled(usernameToCall : String){
        friendsUsername = usernameToCall
        sendCallRequest()
    }

    var uniqueId = ""

    private fun initializePeer() {
        uniqueId = getUniqueID()
        callJavascriptFunction("javascript:init(\"${uniqueId}\")")
        firebaseRef.child(username).child("incoming").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onCallRequest(snapshot.value as? String)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun onCallRequest(caller: String?) {
        if(caller == null) return
        val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        // Ringtone Manager Object
        val ringMan = RingtoneManager(applicationContext)
        ringMan.stopPreviousRingtone()
        // Ringtone Object
        val r = RingtoneManager.getRingtone(applicationContext, notification)
        r.play()
        binding.callLayout.visibility = View.VISIBLE
        binding.incomingCallTxt.text = "$caller is calling..."
        

        binding.acceptBtn.setOnClickListener {

            r.stop()
            firebaseRef.child(username).child("connId").setValue(uniqueId)
            firebaseRef.child(username).child("isAvailable").setValue(true)

            binding.contactRecyclerView.visibility = View.GONE
            binding.callLayout.visibility = View.GONE
            
            Log.e("at","Peer connected")
            checkForFriend()
            switchToControls()
        }

        binding.rejectBtn.setOnClickListener {
            r.stop()
            binding.contactRecyclerView.visibility = View.VISIBLE
            
            firebaseRef.child(username).child("incoming").setValue(null)
            binding.callLayout.visibility = View.GONE
        }
    }

    private fun switchToControls() {
        binding.inputLayout.visibility = View.GONE
        binding.callControlLayout.visibility = View.VISIBLE
        binding.txtViewContacts.visibility = View.GONE
        binding.contactRecyclerView.visibility = View.GONE

    }

    private fun getUniqueID(): String{
        return UUID.randomUUID().toString()
    }

    private fun callJavascriptFunction(functionString: String){
        binding.webView.post{
            binding.webView.evaluateJavascript(functionString, null)
        }
    }

    fun onPeerConnected() {
        isPeerConnected = true
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseRef.child(username).setValue(null)
        firebaseRef.child(friendsUsername).setValue(null)

    }
}