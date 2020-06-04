package com.example.codename

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_type_room_info.*

class MainActivity : AppCompatActivity() {

    val database = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCreateRoom.setOnClickListener {
            //Todo Create a room
            createRoom()

        }

        btnJoinRoom.setOnClickListener {
            createRoom()
        }
    }

    private fun createRoom() {

        supportFragmentManager.beginTransaction().add(R.id.container_type_room_info, SetRoomInfoFragment()).commit()


    }


}