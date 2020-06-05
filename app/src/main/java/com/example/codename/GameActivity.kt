package com.example.codename

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_game.*
import java.util.*

class GameActivity : AppCompatActivity(), WaitingMembersFragment.OnFragmentWaitingListener {

    val database = FirebaseFirestore.getInstance()
    lateinit var list: MutableList<String?>

    var keyword: String = ""
    var nickname: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        if (intent.extras == null){
            //Todo エラー処理
            finish()
        }

        keyword = intent.extras!!.getString(INTENT_KEY_KEYWORD)!!
        nickname = intent.extras!!.getString(INTENT_KEY_NICKNAME)!!

        setCardWords(keyword)
        waitMembersFragment()
    }

    private fun waitMembersFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.container_game, WaitingMembersFragment.newInstance(keyword, nickname)).commit()

    }

    private fun setCardWords(keyword: String) {

        list = mutableListOf()
        val docRef: DocumentReference = database.collection(dbCollection).document(keyword).collection("words").document(keyword)
        docRef.get().addOnSuccessListener {

            for(i in 1 .. 25 ){
                list.add(it.getString("word$i"))

            }
            Collections.shuffle(list)
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

    companion object{

        const val INTENT_KEY_KEYWORD = "keyword"
        const val INTENT_KEY_NICKNAME = "nickname"

        fun getLaunched(fragment: FragmentActivity?, keyword: String, nickname: String) = Intent(fragment, GameActivity::class.java).apply {
            putExtra(INTENT_KEY_KEYWORD, keyword )
            putExtra(INTENT_KEY_NICKNAME, nickname)
        }
    }

    //WaitingMembersFragment.OnFragmentWaitingListener
    override fun OnMembersGathered() {
        //Todo チーム編成（ランダム）
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_game, GameSettingFragment.newInstance(keyword, nickname)).commit()
    }

}