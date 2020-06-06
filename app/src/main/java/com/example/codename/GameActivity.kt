package com.example.codename

import android.content.Intent
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import kotlinx.android.synthetic.main.activity_game.*
import java.io.IOException
import java.io.InputStreamReader
import kotlin.random.Random

class GameActivity : AppCompatActivity(), WaitingMembersFragment.OnFragmentWaitingListener,
    GameSettingFragment.OnFragmentGameSettingListener {

    val database = FirebaseFirestore.getInstance()
    lateinit var list: MutableList<String?>

    var keyword: String = ""
    var nickname: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        if (intent.extras == null) {
            //Todo エラー処理
            finish()
        }

        keyword = intent.extras!!.getString(INTENT_KEY_KEYWORD)!!
        nickname = intent.extras!!.getString(INTENT_KEY_NICKNAME)!!

        if (status == Status.CREATE_ROOM) {
            importWordsFromCSV()
        }

        waitMembersFragment()
    }

    private fun waitMembersFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.container_game, WaitingMembersFragment.newInstance(keyword, nickname))
            .commit()

    }

    private fun setCardWords(keyword: String) {

        list = mutableListOf()
        val docRef: DocumentReference =
            database.collection(dbCollection).document(keyword).collection("words")
                .document(keyword)
        docRef.get().addOnSuccessListener {

            list = it["words"] as MutableList<String?>
            showWords()

        }

    }

    private fun showWords() {

        button.setText(list[0])
        button2.setText(list[1])
        button3.setText(list[2])
        button4.setText(list[3])
        button5.setText(list[4])
        button6.setText(list[5])
        button7.setText(list[6])
        button8.setText(list[7])
        button9.setText(list[8])
        button10.setText(list[9])
        button11.setText(list[10])
        button12.setText(list[11])
        button13.setText(list[12])
        button14.setText(list[13])
        button15.setText(list[14])
        button16.setText(list[15])
        button17.setText(list[16])
        button18.setText(list[17])
        button19.setText(list[18])
        button20.setText(list[19])
        button21.setText(list[20])
        button22.setText(list[21])
        button23.setText(list[22])
        button24.setText(list[23])
        button25.setText(list[24])

    }


    companion object {

        const val INTENT_KEY_KEYWORD = "keyword"
        const val INTENT_KEY_NICKNAME = "nickname"

        fun getLaunched(fragment: FragmentActivity?, keyword: String, nickname: String) =
            Intent(fragment, GameActivity::class.java).apply {
                putExtra(INTENT_KEY_KEYWORD, keyword)
                putExtra(INTENT_KEY_NICKNAME, nickname)
            }
    }

    //WaitingMembersFragment.OnFragmentWaitingListener
    override fun OnMembersGathered() {
        //Todo チーム編成（ランダム）
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game, GameSettingFragment.newInstance(keyword, nickname))
            .commit()
    }

    //GameSettingFragment.OnFragmentGameSettingListener
    override fun GameStart() {
        setCardWords(keyword)
    }

    private fun importWordsFromCSV() {

        val reader = setCSVReader()
        var tempList: MutableList<Array<String>>? = null

        try {
            tempList = reader.readAll()
        } catch (e: IOException) {
            Toast.makeText(this, "データを取り込めませんでした", Toast.LENGTH_SHORT).show()
            isDataFinished = false

        } finally {
            reader.close()
        }

        writeCSVDataToFirebase(tempList)
    }

    private fun writeCSVDataToFirebase(tempList: MutableList<Array<String>>?) {

        val selectedWordsList: List<String> = select25Words(tempList)

        val saveList = hashMapOf("words" to selectedWordsList)
        database.collection(dbCollection).document(keyword).collection("words").document(keyword)
            .set(saveList)

    }

    private fun select25Words(tempList: MutableList<Array<String>>?): List<String> {

        val list: MutableList<String> = mutableListOf()

        for (i in 0 until tempList!!.size - 1) {

            val a = tempList[i]

            for (element in a) {
                list.add(element)
            }
        }
        val notNullList = list.distinct()

        Log.d("CHHhhhhhhhhhhh", "$notNullList")

        val result: MutableList<String> = mutableListOf()
        val remaining: MutableList<String> = notNullList.toMutableList()

        for (i in 0..24) {
            val remainingCount = remaining.size
            val index = Random.nextInt(remainingCount)

            val selectedElement = remaining[index]
            result.add(selectedElement)

            val lastIndex = remainingCount - 1
            val lastElement = remaining.removeAt(lastIndex)
            if (index < lastIndex) {
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


}