package com.example.codename

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCreateRoom.setOnClickListener {

            soundPool?.play2(soundIdButtonClicked)
            status = Status.CREATE_ROOM
            createRoom()

        }

        btnJoinRoom.setOnClickListener {

            soundPool?.play2(soundIdButtonClicked)
            status = Status.JOIN_ROOM
            createRoom()
        }

    }

    private fun createRoom() {

        supportFragmentManager.beginTransaction()
            .replace(R.id.container_type_room_info, SetRoomInfoFragment()).commit()


    }


}