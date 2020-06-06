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

        importWordsFromCSV()

        btnCreateRoom.setOnClickListener {

            createRoom(Status.CREATE_ROOM)

        }

        btnJoinRoom.setOnClickListener {
            createRoom(Status.JOIN_ROOM)
        }

    }

    private fun importWordsFromCSV() {

        val reader = setCSVReader()
        var tempList: MutableList<Array<String>>? = null

        try {
            tempList = reader.readAll()
        } catch (e: IOException){
            Toast.makeText(this, "データを取り込めませんでした", Toast.LENGTH_SHORT).show()
            isDataFinished = false

        }finally {
            reader.close()
        }

        writeCSVDataToFirebase(tempList)
    }

    private fun writeCSVDataToFirebase(tempList: MutableList<Array<String>>?) {

        val selectedWordsList: List<String> = select25Words(tempList)

    }

    private fun select25Words(tempList: MutableList<Array<String>>?): List<String> {

        val list: MutableList<String> = mutableListOf()

        for(i in 0 until tempList!!.size-1){

            val a = tempList[i]

            for(element in a){
                list.add(element)
            }
        }
        val notNullList = list.distinct()

        Log.d("CHHhhhhhhhhhhh", "$notNullList")

        val result: MutableList<String> = mutableListOf()
        val remaining: MutableList<String> = notNullList.toMutableList()

        for(i in 0 .. 24){
            val remainingCount = remaining.size
            val index = Random.nextInt(remainingCount)

            val selectedElement = remaining[index]
            result.add(selectedElement)

            val lastIndex = remainingCount - 1
            val lastElement = remaining.removeAt(lastIndex)
            if(index < lastIndex){
                    remaining.set(index, lastElement)
                }
        }

        Log.d("CHHhhhhhhhhhhhHHH", "$result")

        return result
    }

    private fun setCSVReader(): CSVReader {

        val assetManager: AssetManager = resources.assets
        val inputStream = assetManager.open("Words.csv")
        val parser = CSVParserBuilder().withSeparator(',').build()

        return CSVReaderBuilder(InputStreamReader(inputStream)).withCSVParser(parser).build()
    }


    private fun createRoom(status: Status) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.container_type_room_info, SetRoomInfoFragment.newInstance(status)).commit()


    }


}