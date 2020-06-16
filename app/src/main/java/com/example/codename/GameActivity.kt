package com.example.codename

import android.content.Intent
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.ArrayMap
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.card_words.*
import kotlinx.android.synthetic.main.card_words.view.*
import kotlinx.android.synthetic.main.card_words.view.word
import kotlinx.android.synthetic.main.fragment_game_player.*
import org.apache.commons.lang3.mutable.Mutable
import java.io.IOException
import java.io.InputStreamReader
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashMap
import kotlin.random.Random

class GameActivity : AppCompatActivity(), OnFragmentListener{

    val database = FirebaseFirestore.getInstance()
    private lateinit var listening: ListenerRegistration

    var keyword: String = ""
    var nickname: String = ""

    lateinit var turn: Turn
    var turnCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        btn_explain.visibility = View.INVISIBLE

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

    private fun setCardWords() {

        listening = database.collection(dbCollection).document(keyword).collection("words").document(keyword)
            .addSnapshotListener { it, e ->

                if (e != null) return@addSnapshotListener
                if (it == null || !it.exists()) return@addSnapshotListener


                val list = mutableListOf<WordsData>()
                val hashmap = it["words"] as MutableList<HashMap<String, String>>

                for(i in 0 .. 24){
                    list.add(WordsData(hashmap[i]["word"], hashmap[i]["color"]))
                }

                remaining_red.setText("赤カードの残り枚数:")
                remaining_blue.setText("青カードの残り枚数:")
                red_number_of_remaining.setText("8")
                blue_number_of_remaining.setText("7")

                val onceClicked = mutableListOf<Int>()
                val redCardIndex = mutableListOf<Int>()
                val blueCardIndex = mutableListOf<Int>()

                turn = Turn.RED_TEAM_TURN
                text_which_team_turn.setText("赤チームのターンです")
                newTurn()

                val listItem = mutableListOf<String>()

                val clickedDataList = mutableListOf<ClickedData>()

                val adapter = CardAdapter(list, object : CardAdapter.OnCardAdapterListener {
                    override fun OnClickCard(word: String, wordsData: WordsData, holder: CardAdapter.ViewHolder) {

                        val myTeam = if(isMyTeam == Team.RED) "RED" else "BLUE"
                        val hashmap = it["words"] as List<HashMap<String, String>>
                        val index = hashmap.indexOfFirst { it.containsValue(word) }
                        Log.d("index", "$index")

                        database.collection(dbCollection).document(keyword).collection("words").document(myTeam).addSnapshotListener { it, e ->

                            if (e != null) return@addSnapshotListener
                            if (it == null || !it.exists()) return@addSnapshotListener

                            val numCardPick = it.getLong("number of Cards to pick")?.toInt()
                                ?: return@addSnapshotListener

                            if (listItem.size == numCardPick) {
                                listItem.removeAt(0)
                                listItem.add(word)
                            } else {
                                listItem.add(word)
                            }

                            chosen_cards.setText("${listItem.joinToString()}")

                            btn_vote.setOnClickListener {
                                //Todo 投票ボタン処理
                                val voteData = Member(nickname, isMyTeam, isHost, vote = listItem)
                                database.collection(dbCollection).document(keyword).collection("members").document(nickname).set(voteData)

                                waitUntilAllVote(redCardIndex, blueCardIndex, hashmap,clickedDataList)
                            }
                        }
                    }

                    override fun OnClickedDataSaved(
                        wordsData: WordsData,
                        holder: CardAdapter.ViewHolder
                    ) {
                        val clickedData = ClickedData(wordsData, holder)
                        clickedDataList.add(clickedData)
                    }
                })
                recycler_view.layoutManager = GridLayoutManager(this, 5)
                recycler_view.adapter = adapter

            }


    }

    private fun waitUntilAllVote(redCardIndex: MutableList<Int>, blueCardIndex: MutableList<Int>, hashmap: List<HashMap<String, String>>,
                                 clickedDataList: MutableList<ClickedData>) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game_detail, GameWaitingFragment()).commit()

        val myTeam = if(isMyTeam == Team.RED) "RED" else "BLUE"
        database.collection(dbCollection).document(keyword).collection("members")
            .whereEqualTo("team", myTeam).whereEqualTo("host", false).addSnapshotListener { it, e ->
                val players: MutableList<String> = mutableListOf()
                val voteItemList = mutableListOf<String>()
                if (e != null) return@addSnapshotListener
                if (it == null || it.isEmpty) return@addSnapshotListener

                for (document in it) {
                    val voteItems= document.get("vote") ?: return@addSnapshotListener
                    voteItems as List<String>
                    if(voteItems.isEmpty()) return@addSnapshotListener
                    voteItems.forEach {
                        voteItemList.add(it)
                    }
                    players.add(document.getString("name")!!)
                }

                Log.d("players", "$players")
                Log.d("voteItemList", "$voteItemList")

                resultOfVote(voteItemList,redCardIndex, blueCardIndex, hashmap,clickedDataList)
            }



    }

    private fun resultOfVote(voteItemList: MutableList<String>, redCardIndex: MutableList<Int>,
                             blueCardIndex: MutableList<Int>, hashmap: List<HashMap<String, String>>, clickedDataList: MutableList<ClickedData>) {

        val myTeam = if(isMyTeam == Team.RED) "RED" else "BLUE"
        database.collection(dbCollection).document(keyword).collection("words").document(myTeam).get().addOnSuccessListener {
            val numCardPick = it.getLong("number of Cards to pick")?.toInt() ?: return@addOnSuccessListener

            val countWords = voteItemList.groupingBy { it }.eachCount()
            Log.d("countWords", "$countWords")

            val collectionNum = countWords.values
            Log.d("collection", "$collectionNum")
            val collectionAscending = collectionNum.sorted().distinct()
            val collectionDescending = collectionAscending.reversed()
            Log.d("collectionAscending", "$collectionAscending")
            Log.d("collectionDescending", "$collectionDescending")

            val selectedWordMapList = mutableListOf<Map.Entry<String, Int>>()

            for (element in collectionDescending){
                val chosenWordMap = countWords.filterValues {
                    it == element
                }

                for( it in chosenWordMap){
                    selectedWordMapList.add(it)
                }
            }

            Log.d("selectedWordMapList", "$selectedWordMapList")

            val selectedWordList = mutableListOf<String>()
            for( i in 0 until numCardPick){
                val chosenWord = selectedWordMapList[i].key
                selectedWordList.add(chosenWord)
            }
            Log.d("selectedWordList", "$selectedWordList")

            if (selectedWordList.size == numCardPick){

                for (i in 0 until numCardPick){
                    val word = selectedWordList[i]
                    val index = hashmap.indexOfFirst {
                        it.containsValue(word)
                    }

                    val data = clickedDataList.filter {
                        it.wordsData.word == word
                    }

                    val wordsData = data.toList()[0].wordsData
                    val holder = data.toList()[0].holder

                    Log.d("word", "$word")
                    Log.d("index", "$index")
                    Log.d("hashmap[index]", "${hashmap[index]}")
                    Log.d("wordsData", "$wordsData")
                    Log.d("holder", "$holder")
                    Log.d("data", "$data")

                    when (wordsData.color) {
                        "RED" -> {
                            holder.itemView.card_view.setBackgroundResource(R.color.RED)
                            holder.color.setBackgroundResource(R.color.RED)
                        }
                        "BLUE" -> {
                            holder.itemView.card_view.setBackgroundResource(R.color.BLUE)
                            holder.color.setBackgroundResource(R.color.BLUE)
                        }
                        "GRAY" -> {
                            holder.itemView.card_view.setBackgroundResource(R.color.GRAY)
                            holder.color.setBackgroundResource(R.color.GRAY)
                        }
                        else -> {
                            holder.itemView.card_view.setBackgroundResource(R.color.LIGHT_GRAY)
                            holder.color.setBackgroundResource(R.color.LIGHT_GRAY)
                        }
                    }

                    when (hashmap[index]["color"]) {
                        "RED" -> redCardIndex.add(index)
                        "BLUE" -> blueCardIndex.add(index)
                        "GRAY" -> {
                            soundPool?.play2(soundIdIncorrect)
                            Toast.makeText(
                                this@GameActivity,
                                "ゲームオーバーです",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                }


                if (redCardIndex.size == 8) Toast.makeText(
                    this@GameActivity,
                    "赤チームの勝利です",
                    Toast.LENGTH_LONG
                ).show()
                if (blueCardIndex.size == 7) Toast.makeText(
                    this@GameActivity,
                    "青チームの勝利です",
                    Toast.LENGTH_LONG
                ).show()

                turnCount++

                Log.d("redCardIndex", "$redCardIndex")
                Log.d("blueCardIndex", "$blueCardIndex")

                red_number_of_remaining.setText("${8 - redCardIndex.size}")
                blue_number_of_remaining.setText("${7 - blueCardIndex.size}")

                when (turnCount % 2) {
                    0 -> {
                        turn = Turn.RED_TEAM_TURN
                        text_which_team_turn.setText("赤チームのターンです")
                    }
                    1 -> {
                        turn = Turn.BLUE_TEAM_TURN
                        text_which_team_turn.setText("青チームのターンです")
                    }
                }

                newTurn()

            }
        }


    }

    private fun newTurn() {

        when(turn){
            Turn.RED_TEAM_TURN -> redTurn()
            Turn.BLUE_TEAM_TURN -> blueTurn()
        }

    }

    private fun blueTurn() {
        if(isMyTeam == Team.BLUE && isHost) host()
        else if(isMyTeam == Team.BLUE) player()
        else waitUntilYourTurn()
    }

    private fun redTurn() {
        if(isMyTeam == Team.RED && isHost) host() else if (isMyTeam == Team.RED) player() else waitUntilYourTurn()
        Log.d("isMyTeam", "$isMyTeam")
        Log.d("isHost", "$isHost")


    }

    private fun waitUntilYourTurn() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game_detail, GameWaitingFragment()).commit()
    }

    private fun player() {
        val myTeam = if(isMyTeam == Team.RED) "RED" else "BLUE"
        database.collection(dbCollection).document(keyword).collection("words").document(myTeam).addSnapshotListener { it, e ->

            if(e != null) return@addSnapshotListener

            if (it == null || !it.exists()) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_game_detail, GameWaitingFragment())
                    .commit()
            }

            val hint = it?.getString("hint") ?: return@addSnapshotListener
            val numCardPick = it.getLong("number of Cards to pick")?.toInt() ?: return@addSnapshotListener
            Log.d("hint", hint)
            Log.d("number of Cards to pick", "$numCardPick")
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_game_detail, GamePlayerFragment.newInstance(hint, numCardPick)).commit()
        }

    }

    private fun host() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game_detail, GameHostFragment()).commit()
    }

    override fun onPause() {
        super.onPause()

        listening.remove()
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

    override fun OnMembersGathered() {
        //Todo チーム編成（ランダム）
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game, GameSettingFragment.newInstance(keyword, nickname))
            .commit()
    }

    override fun GameStart() {
        setCardWords()
        btn_explain.visibility = View.VISIBLE
    }

    override fun OnMemberDeleted() {
        database.collection(dbCollection).document(keyword).collection("members").document(nickname).delete()
        finish()
    }

    override fun OnRoomDeleted(membersList: MutableList<String>) {
        membersList.forEach {
            database.collection(dbCollection).document(keyword).collection("members").document(it).delete()
        }
        database.collection(dbCollection).document(keyword).collection("words").document(keyword).delete()
        database.collection(dbCollection).document(keyword).delete()
        finish()
    }

    override fun OnHost(hint: String, numCardPick: Int) {
        val myTeam = if(isMyTeam == Team.RED) "RED" else "BLUE"
        val hashmap = hashMapOf("hint" to hint, "number of Cards to pick" to numCardPick)
        database.collection(dbCollection).document(keyword).collection("words").document(myTeam).set(hashmap)

        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game_detail, GameWaitingFragment())
            .commit()
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

        val red = mutableListOf<Int>()
        val blue = mutableListOf<Int>()

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

        val saveList = hashMapOf("words" to selectedWordsList)
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