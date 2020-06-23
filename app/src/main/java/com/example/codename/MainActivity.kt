package com.example.codename

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        Log.d("currentUser", "$currentUser")
        if(currentUser == null){
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("TAG", "signInAnonymously:success")
                        val user = auth.currentUser ?: return@addOnCompleteListener
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("TAG", "signInAnonymously:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                    }

                }
        } else {
            updateUI(currentUser)
        }
    }

    private fun updateUI(user: FirebaseUser) {
        val uid = user.uid
        btnCreateRoom.setOnClickListener {

            soundPool?.play2(soundIdButtonClicked)
            status = Status.CREATE_ROOM
            createRoom(uid)

        }

        btnJoinRoom.setOnClickListener {

            soundPool?.play2(soundIdButtonClicked)
            status = Status.JOIN_ROOM
            createRoom(uid)
        }

        btn_explain_main.setOnClickListener {

            soundPool?.play2(soundIdButtonClicked)
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_type_room_info, ExplanationFragment()).commit()
        }
    }


    private fun createRoom(uid: String) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.container_type_room_info, SetRoomInfoFragment.newInstance(uid)).commit()

    }


}