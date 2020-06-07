package com.example.codename

import android.content.Intent
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.ArrayMap
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import kotlinx.android.synthetic.main.activity_game.*
import org.apache.commons.lang3.mutable.Mutable
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.HashMap
import kotlin.random.Random

class GameActivity : AppCompatActivity(), WaitingMembersFragment.OnFragmentWaitingListener,
    GameSettingFragment.OnFragmentGameSettingListener{

    val database = FirebaseFirestore.getInstance()
    lateinit var list: MutableList<WordsData>

    var keyword: String = ""
    var nickname: String = ""

    var wordDataSavedToFirestore: MutableList<WordsData> = mutableListOf()

    lateinit var red : MutableList<Int>
    lateinit var blue: MutableList<Int>

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

    private fun checkScore(){
        database.collection(dbCollection).document(keyword).collection("words")
            .document(keyword).get().addOnSuccessListener {

                var numRedCard = 0
                var numBlueCard = 0


            }
    }

    private fun setCardWords(keyword: String) {

        list = mutableListOf()
        val docRef: DocumentReference =
            database.collection(dbCollection).document(keyword).collection("words")
                .document(keyword)
        docRef.get().addOnSuccessListener {

            val hashmap = it["words"] as List<HashMap<String, String>>

            for(i in 0 .. 24){
               list.add(WordsData(hashmap[i]["word"], hashmap[i]["color"]))
           }

            showWords(list)
        }
    }

    private fun showWords(list: List<WordsData>) {

        var numRedCard = red.size
        var numBlueCard = blue.size

        val adapter = CardAdapter(list, object : CardAdapter.OnCardAdapterListener{
            override fun OnClickCard(word: String) {
                val grayCard = WordsData(word, "OVER")

                database.collection(dbCollection).document(keyword).collection("words").document(keyword)
                    .get().addOnSuccessListener {
                        val hashmap = it["words"] as List<HashMap<String, String>>
                        val index = hashmap.indexOfFirst { it.containsValue(word) }

                        //赤のカードのindexと青のカードのindex
                        
                        for (i in 0 until red.size){
                            if(index == red[i]){
                                numRedCard -= 1
                                if(numRedCard == 0) Toast.makeText(this@GameActivity, "赤チームの勝利です", Toast.LENGTH_LONG).show()
                            }
                        }

                        for(i in 0 until blue.size){
                            if(index == blue[i]){
                                numBlueCard--
                                if(numBlueCard == 0)Toast.makeText(this@GameActivity, "青チームの勝利です", Toast.LENGTH_LONG).show()
                            }
                        }
                        wordDataSavedToFirestore.set(index, grayCard)
                        val saveList = hashMapOf("words" to wordDataSavedToFirestore)
                        database.collection(dbCollection).document(keyword).collection("words").document(keyword).set(saveList)

            }
        }})

       recycler_view.layoutManager = GridLayoutManager(this, 5)
       recycler_view.adapter = adapter

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

        val selectedWordsList: MutableList<WordsData> = select25Words(tempList).toMutableList()

        //赤が８
        //青が７

      val list = mutableListOf<Int>()

      for(i in 0..24){
           list.add(i)
       }

        Collections.shuffle(list)

       red = mutableListOf<Int>()
       blue = mutableListOf<Int>()

        //RED
        for (i in 0 ..7) {
            red.add(list[i])
        }
        for (i in 0 until red.size){
            selectedWordsList.set(red[i], WordsData(selectedWordsList[red[i]].word, "RED"))
        }

        //BLUE
        for (i in 8 .. 14) {
            blue.add(list[i])
        }
        for (i in 0 until blue.size) {
            selectedWordsList.set(blue[i], WordsData(selectedWordsList[blue[i]].word, "BLUE"))
        }
        //GRAY
        val gray = list[24]
        selectedWordsList.set(gray, WordsData(selectedWordsList[gray].word, "GRAY"))

        wordDataSavedToFirestore = selectedWordsList
        val saveList = hashMapOf("words" to wordDataSavedToFirestore)
        database.collection(dbCollection).document(keyword).collection("words").document(keyword)
            .set(saveList)

    }

    private fun select25Words(tempList: MutableList<Array<String>>?): List<WordsData> {

        val list: MutableList<WordsData> = mutableListOf()

        for (i in 0 until tempList!!.size - 1) {

            val a = tempList[i]

            for (element in a) {
                list.add(WordsData(element))
            }
        }
        val notNullList = list.distinct()

        Log.d("CHHhhhhhhhhhhh", "$notNullList")

        val result: MutableList<WordsData> = mutableListOf()
        val remaining: MutableList<WordsData> = notNullList.toMutableList()

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