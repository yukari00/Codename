package com.example.codename

import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_type_room_info.*
import java.io.IOException
import java.io.InputStreamReader
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    val database = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCreateRoom.setOnClickListener {

            status = Status.CREATE_ROOM
            createRoom()

        }

        btnJoinRoom.setOnClickListener {
            status = Status.JOIN_ROOM
            createRoom()
        }

    }

    private fun createRoom() {

        supportFragmentManager.beginTransaction()
            .replace(R.id.container_type_room_info, SetRoomInfoFragment()).commit()


    }


}