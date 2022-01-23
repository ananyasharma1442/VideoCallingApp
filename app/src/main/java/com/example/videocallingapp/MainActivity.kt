package com.example.videocallingapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.core.app.ActivityCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

class MainActivity : AppCompatActivity() {
    private lateinit var usernameEdtxt : EditText
    private lateinit var loginBtn : Button
    val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    val requestCode= 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(!isPermissionGranted())
        {
            askPermission()
        }

        Firebase.initialize(this)
        loginBtn = findViewById(R.id.login_btn)
        usernameEdtxt = findViewById(R.id.username_edtxt)
        loginBtn.setOnClickListener {
            val username = usernameEdtxt.text.toString()
            val intent = Intent(this, CallActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }
    }

    private fun askPermission() {
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    private fun isPermissionGranted(): Boolean {
        permissions.forEach {
            if(ActivityCompat.checkSelfPermission(this,it) != PackageManager.PERMISSION_GRANTED)
                return false
        }
        return true
    }
}