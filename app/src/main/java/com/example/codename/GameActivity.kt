package com.example.codename

import android.content.Intent
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.*
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.fragment_game_player.*
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.HashMap
import kotlin.random.Random

class GameActivity : AppCompatActivity(), OnFragmentListener{

    val database = FirebaseFirestore.getInstance()
    var listeningWords: ListenerRegistration? = null
    var listeningSelectedCards: ListenerRegistration? = null
    var listeningMembers: ListenerRegistration? = null
    var listeningCleared: ListenerRegistration? = null

    var keyword: String = ""
    var nickname: String = ""

    lateinit var turn: Turn
    var turnCount = 0

    var teamToCollectAllCards = ""
    var teamGotGray: Team? = null
    var ifGameIsOver = false

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

        waitMembersFragment()
    }

    private fun waitMembersFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.container_game, WaitingMembersFragment.newInstance(keyword, nickname))
            .commit()

    }

    private fun locateEachCard() {

        btn_explain.visibility = View.VISIBLE
        btn_explain.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_game, ExplanationFragment())
                .commit()
        }

        listeningWords = database.collection(dbCollection).document(keyword).collection(
            collectionWords).document(keyword)
            .addSnapshotListener { it, e ->

                if (e != null) return@addSnapshotListener
                if (it == null || !it.exists()) return@addSnapshotListener


                val wordsDataList = mutableListOf<WordsData>()
                val hashmap = it[wordsFieldPath] as MutableList<HashMap<String, String>>

                for(i in 0 .. 24){
                    wordsDataList.add(WordsData(hashmap[i][wordFieldPath], hashmap[i][colorFieldPath]))
                }

                updateSelectedCards(wordsDataList)

            }
    }

    private fun updateSelectedCards(wordsDataList: MutableList<WordsData>) {
        listeningSelectedCards = database.collection(dbCollection).document(keyword).collection(
            collectionSelectedCards).addSnapshotListener { query, e ->

            if(e != null) return@addSnapshotListener
            val selectedCardList = mutableListOf<WordsData>()

            supportFragmentManager.beginTransaction().remove(ResultFragment()).commit();

            if(query == null || query.isEmpty){

                remaining_red.setText("赤カードの残り枚数:")
                remaining_blue.setText("青カードの残り枚数:")
                red_number_of_remaining.setText("8")
                blue_number_of_remaining.setText("7")

                turn = Turn.RED_TEAM_TURN
                text_which_team_turn.setText("赤チームのターンです")

            } else{

                willUpdate(query, selectedCardList)
            }

            newTurn()

            val listItem = mutableListOf<String>()

            val adapter = CardAdapter(wordsDataList, selectedCardList, object : CardAdapter.OnCardAdapterListener {
                override fun OnClickCard(word: String, wordsData: WordsData, holder: CardAdapter.ViewHolder) {

                    showWhatYouClicked(listItem, word, wordsDataList)
                }
            })
            recycler_view.layoutManager = GridLayoutManager(this, 5)
            recycler_view.adapter = adapter

            if(ifGameIsOver) showResultFragment()
        }
    }

    private fun showResultFragment() {
        deletePreviousReadySign()
        supportFragmentManager.beginTransaction()
           .replace(R.id.container_game, ResultFragment.newInstance(keyword, teamToCollectAllCards, turnCount,teamGotGray)).commit()
    }

    private fun showWhatYouClicked(listItem: MutableList<String>, word: String, wordsDataList: MutableList<WordsData>) {

        val myTeam = if(isMyTeam == Team.RED) "RED" else "BLUE"
        database.collection(dbCollection).document(keyword).collection(collectionWords).document(myTeam).get().addOnSuccessListener{

            if (it == null || !it.exists()) return@addOnSuccessListener

            val numCardPick = it.getLong(numberOfCardsToPickFieldPath)?.toInt()
                ?: return@addOnSuccessListener

            if (!listItem.contains(word)){
                if (listItem.size == numCardPick) {
                    listItem.removeAt(0)
                    listItem.add(word)
                } else {
                    listItem.add(word)
                }
            }

            chosen_cards.setText(listItem.joinToString())

            btn_vote.setOnClickListener {
                //投票ボタン処理
                val voteData = Member(nickname, isMyTeam, isHost, vote = listItem)
                database.collection(dbCollection).document(keyword).collection(collectionMembers).document(nickname).set(voteData)

                waitUntilAllVote(wordsDataList)
            }
        }
    }

    private fun willUpdate(query: QuerySnapshot, selectedCardList: MutableList<WordsData>) {

        val turnCountList = mutableListOf<Int>()
        for (i in query.documents.indices){
            val tCount = query.documents[i].getLong(turnCountFieldPath)?.toInt() ?: return
            turnCountList.add(tCount)
        }
        turnCount = turnCountList.max()?: return

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

        var NumRedCard = 0
        var NumBlueCard = 0

        query.documents.forEach {
            val wordList = it[clickedWordsFieldPath] as List<HashMap<String, String>>
            for (j in wordList.indices){
                val color = wordList[j][colorFieldPath]
                when(color){
                    "RED" -> NumRedCard++
                    "BLUE" -> NumBlueCard++
                    "GRAY" -> {
                        teamGotGray = if(turnCount % 2 == 0) Team.BLUE else Team.RED
                        teamToCollectAllCards = "GRAY"
                        ifGameIsOver = true
                    }
                }
            }
        }
        remaining_red.setText("赤カードの残り枚数:")
        remaining_blue.setText("青カードの残り枚数:")
        red_number_of_remaining.setText("${8 - NumRedCard}")
        blue_number_of_remaining.setText("${7 - NumBlueCard}")

        if (NumRedCard == 8) {
            teamToCollectAllCards = "RED"
            ifGameIsOver = true
        }

        if (NumBlueCard == 7) {
            when(teamToCollectAllCards){
                "RED" -> {
                    if(turnCount % 2 == 1) teamToCollectAllCards = "BLUE"
                }
                "" -> teamToCollectAllCards = "BLUE"
            }
            ifGameIsOver = true
        }

        for(i in 0 until query.documents.size){
            val cardsHash = query.documents[i][clickedWordsFieldPath] as MutableList<HashMap<String, String>>? ?: return

            for (j in 0 until cardsHash.size ){
                selectedCardList.add(WordsData(cardsHash[j][wordFieldPath], cardsHash[j][colorFieldPath]))
            }
        }
    }

    private fun waitUntilAllVote(workDataList: MutableList<WordsData>) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game_detail, GameWaitingFragment()).commit()

        val myTeam = if(isMyTeam == Team.RED) "RED" else "BLUE"
        listeningMembers = database.collection(dbCollection).document(keyword).collection(
            collectionMembers)
            .whereEqualTo(teamFieldPath, myTeam).whereEqualTo(hostFieldPath, false).addSnapshotListener { it, e ->
                val players: MutableList<String> = mutableListOf()
                val voteItemList = mutableListOf<String>()
                if (e != null) return@addSnapshotListener
                if (it == null || it.isEmpty) return@addSnapshotListener

                for (document in it) {
                    val voteItems= document.get(voteFieldPath) ?: return@addSnapshotListener
                    voteItems as List<String>
                    if(voteItems.isEmpty()) return@addSnapshotListener
                    voteItems.forEach {
                        voteItemList.add(it)
                    }
                    players.add(document.getString(nameFieldPath)!!)
                }
                resultOfVote(voteItemList, workDataList)
            }
    }

    private fun resultOfVote(voteItemList: MutableList<String>, workDataList: MutableList<WordsData>) {

        val myTeam = if(isMyTeam == Team.RED) "RED" else "BLUE"
        database.collection(dbCollection).document(keyword).collection(collectionWords).document(myTeam).get().addOnSuccessListener {
            val numCardPick = it.getLong(numberOfCardsToPickFieldPath)?.toInt() ?: return@addOnSuccessListener

            val countWords = voteItemList.groupingBy { it }.eachCount()

            val collectionNum = countWords.values
            val collectionAscending = collectionNum.sorted().distinct()
            val collectionDescending = collectionAscending.reversed()

            val selectedWordMapList = mutableListOf<Map.Entry<String, Int>>()

            for (element in collectionDescending){
                val chosenWordMap = countWords.filterValues {
                    it == element
                }

                for( it in chosenWordMap){
                    selectedWordMapList.add(it)
                }
            }

            val selectedWordList = mutableListOf<String>()
            for( i in 0 until numCardPick){
                val chosenWord = selectedWordMapList[i].key
                selectedWordList.add(chosenWord)
            }

            if (selectedWordList.size == numCardPick){

                val clickedData = mutableListOf<WordsData>()
                for (word in selectedWordList){

                    val data = workDataList.filter {
                        it.word == word
                    }

                    if(data.isEmpty()) return@addOnSuccessListener
                    val wordsData = data[0]

                    clickedData.add(wordsData)
                }

                turnCount++

                val selectedCardsInfo = SelectedCardsInfo(clickedData, turnCount)
                deleteHostHintInfo()
                database.collection(dbCollection).document(keyword).collection(
                    collectionSelectedCards).add(selectedCardsInfo)

            }
        }
    }

    private fun deleteHostHintInfo() {

        val updatesHint = hashMapOf<String, Any>(
            hintFieldPath to FieldValue.delete()
        )

        val updatesNumber = hashMapOf<String, Any>(
            numberOfCardsToPickFieldPath to FieldValue.delete()
        )

        val myTeam = if(isMyTeam == Team.RED) "RED" else "BLUE"
        database.collection(dbCollection).document(keyword).collection(collectionWords).document(myTeam).update(updatesHint)
        database.collection(dbCollection).document(keyword).collection(collectionWords).document(myTeam).update(updatesNumber)
    }

    private fun newTurn() {

        when(turn){
            Turn.RED_TEAM_TURN -> redTurn()
            Turn.BLUE_TEAM_TURN -> blueTurn()
        }
    }

    private fun blueTurn() {
        if(isMyTeam == Team.BLUE && isHost) host() else if(isMyTeam == Team.BLUE) player() else waitUntilYourTurn()
    }

    private fun redTurn() {
        if(isMyTeam == Team.RED && isHost) host() else if (isMyTeam == Team.RED) player() else waitUntilYourTurn()

    }

    private fun waitUntilYourTurn() {
        isWaiter = true
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game_detail, GameWaitingFragment()).commit()

    }

    private fun player() {
        isWaiter = false
        val myTeam = if(isMyTeam == Team.RED) "RED" else "BLUE"
        database.collection(dbCollection).document(keyword).collection(collectionWords).document(myTeam).get().addOnSuccessListener {

            if (it == null || !it.exists()) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_game_detail, GameWaitingFragment())
                    .commit()
            }

            val hint = it?.getString(hintFieldPath) ?: return@addOnSuccessListener
            val numCardPick = it.getLong(numberOfCardsToPickFieldPath)?.toInt() ?: return@addOnSuccessListener
            Log.d(hintFieldPath, hint)
            Log.d(numberOfCardsToPickFieldPath, "$numCardPick")
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_game_detail, GamePlayerFragment.newInstance(hint, numCardPick)).commit()
        }

    }

    private fun host() {
        isWaiter = false
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game_detail, GameHostFragment()).commit()

        deleteAllVoteInfo()
    }

    private fun deleteAllVoteInfo() {
        database.collection(dbCollection).document(keyword).collection(collectionMembers).get().addOnSuccessListener {
            val membersList = mutableListOf<String>()
            for (i in it.documents.indices){
                val name = it.documents[i].getString(nameFieldPath)?:return@addOnSuccessListener
                membersList.add(name)
            }

            val updates = hashMapOf<String, Any>(
                voteFieldPath to FieldValue.delete()
            )
            for (member in membersList){
                database.collection(dbCollection).document(keyword).collection(collectionMembers).document(member).update(updates)
            }
        }
    }

    companion object {

        const val INTENT_KEY_KEYWORD = "keyword"
        const val INTENT_KEY_NICKNAME = "nickname"

        const val collectionMembers = "members"
        const val collectionWords = "words"
        const val collectionSelectedCards = "selectedCards"

        const val numberOfCardsToPickFieldPath = "number of Cards to pick"
        const val hintFieldPath = "hint"

        const val wordsFieldPath = "words"

        const val clickedWordsFieldPath = "clickedWords"
        const val turnCountFieldPath = "turnCount"

        const val nameFieldPath = "name"
        const val hostFieldPath = "host"
        const val teamFieldPath = "team"
        const val voteFieldPath = "vote"

        const val colorFieldPath = "color"
        const val wordFieldPath = "word"



        fun getLaunched(fragment: FragmentActivity?, keyword: String, nickname: String) =
            Intent(fragment, GameActivity::class.java).apply {
                putExtra(INTENT_KEY_KEYWORD, keyword)
                putExtra(INTENT_KEY_NICKNAME, nickname)
            }
    }

    override fun OnMembersGathered() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game, GameSettingFragment.newInstance(keyword, nickname))
            .commit()
    }

    override fun GameStart() {

        if (status == Status.CREATE_ROOM) {
            database.collection(dbCollection).document(keyword).collection(collectionSelectedCards)
                .get().addOnSuccessListener {
                    for (i in 0 until it.documents.size) {
                        val documentId = it.documents[i].id
                        database.collection(dbCollection).document(keyword).collection(
                            collectionSelectedCards
                        ).document(documentId).delete()
                    }
                    importWordsFromCSV()
                    database.collection(dbCollection).document(keyword)
                        .update("clearedPreviousData", true)
                    locateEachCard()
                }
        } else {
            listeningCleared =
                database.collection(dbCollection).document(keyword).addSnapshotListener { it, e ->
                    if (e != null) return@addSnapshotListener
                    if (it == null) return@addSnapshotListener

                    val clearedPreviousData = it.getBoolean("clearedPreviousData") ?: false
                    if (clearedPreviousData) locateEachCard()
                }
        }
    }

    override fun OnMemberDeleted() {
        database.collection(dbCollection).document(keyword).collection(collectionMembers).document(nickname).delete()
        finish()
    }

    override fun OnRoomDeleted(membersList: MutableList<String>) {
        membersList.forEach {
            database.collection(dbCollection).document(keyword).collection(collectionMembers).document(it).delete()
        }
        database.collection(dbCollection).document(keyword).collection(collectionWords).document("RED").delete()
        database.collection(dbCollection).document(keyword).collection(collectionWords).document("BLUE").delete()
        database.collection(dbCollection).document(keyword).collection(collectionWords).document(keyword).delete()
        database.collection(dbCollection).document(keyword).delete()
        finish()
    }

    override fun OnHostCallBack(hint: String, numCardPick: Int) {
        val myTeam = if(isMyTeam == Team.RED) "RED" else "BLUE"
        val hashmap = hashMapOf( hintFieldPath to hint, numberOfCardsToPickFieldPath to numCardPick)
        database.collection(dbCollection).document(keyword).collection(collectionWords).document(myTeam).set(hashmap)

        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game_detail, GameWaitingFragment())
            .commit()
    }

    override fun OnStartAnotherGame(turnCount: Int) {

        teamToCollectAllCards = ""
        teamGotGray = null
        ifGameIsOver = false

        if (status == Status.CREATE_ROOM){
            database.collection(dbCollection).document(keyword).collection(collectionSelectedCards).get()
                .addOnSuccessListener {
                    for (i in 0 until it.documents.size) {
                        val documentId = it.documents[i].id
                        database.collection(dbCollection).document(keyword).collection(
                            collectionSelectedCards)
                            .document(documentId).delete()
                    }
                    importWordsFromCSV()
                }
        }

    }

    override fun OnGoBackOnGameSettingFragment() {

        listeningWords?.remove()
        listeningSelectedCards?.remove()
        listeningMembers?.remove()
        listeningCleared?.remove()

        teamToCollectAllCards = ""
        teamGotGray = null
        ifGameIsOver = false

        recycler_view.layoutManager = null
        recycler_view.adapter = null
        btn_explain.visibility = View.INVISIBLE
        text_which_team_turn.setText("")
        remaining_red.setText("")
        remaining_blue.setText("")
        blue_number_of_remaining.setText("")
        red_number_of_remaining.setText("")

        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game, GameSettingFragment.newInstance(keyword, nickname))
            .commit()

    }

    private fun deletePreviousReadySign() {

        val delete1 = hashMapOf<String, Any>(
            "readyForGame" to FieldValue.delete()
        )
        val delete2 = hashMapOf<String, Any>(
            "readyForGameSetting" to FieldValue.delete()
        )
        val delete3 = hashMapOf<String, Any>(
            "readyToEndGame" to FieldValue.delete()
        )
        val delete4 = hashMapOf<String, Any>(
            "readyForAnotherGame" to FieldValue.delete()
        )
        val delete5 = hashMapOf<String, Any>(
            "clearedPreviousData" to FieldValue.delete()
        )
        database.collection(dbCollection).document(keyword).update(delete1)
        database.collection(dbCollection).document(keyword).update(delete2)
        database.collection(dbCollection).document(keyword).update(delete3)
        database.collection(dbCollection).document(keyword).update(delete4)
        database.collection(dbCollection).document(keyword).update(delete5)
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

        val saveList = hashMapOf(wordsFieldPath to selectedWordsList)
        database.collection(dbCollection).document(keyword).collection(collectionWords).document(keyword)
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

        return result
    }

    private fun setCSVReader(): CSVReader {

        val assetManager: AssetManager = resources.assets
        val inputStream = assetManager.open("Words.csv")
        val parser = CSVParserBuilder().withSeparator(',').build()

        return CSVReaderBuilder(InputStreamReader(inputStream)).withCSVParser(parser).build()
    }

}