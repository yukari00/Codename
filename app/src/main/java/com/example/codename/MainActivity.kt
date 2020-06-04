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

            createRoom(Status.CREATE_ROOM)

        }

        btnJoinRoom.setOnClickListener {
            createRoom(Status.JOIN_ROOM)
        }

    }

    private fun joinRoom() {

    }

    private fun createRoom(status: Status) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.container_type_room_info, SetRoomInfoFragment.newInstance(status)).commit()


    }


}