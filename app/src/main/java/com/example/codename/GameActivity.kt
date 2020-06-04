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

class GameActivity : AppCompatActivity() {

    val database = FirebaseFirestore.getInstance()
    lateinit var list: MutableList<String?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val keyword = intent.extras?.getString(INTENT_KEY_KEYWORD)

        setCardWords(keyword!!)
    }

    private fun setCardWords(keyword: String) {

        list = mutableListOf()
        val docRef: DocumentReference = database.collection(dbCollection).document(keyword).collection("words").document(keyword)
        docRef.get().addOnSuccessListener {

            for(i in 1 .. 25 ){
                list.add(it.getString("word$i"))
                showWords()
            }

        }

    }

    private fun showWords() {

        button.setText(list[0])

    }

    companion object{

        const val INTENT_KEY_KEYWORD = "keyword"

        fun getLaunched(fragment: FragmentActivity?, keyword: String) = Intent(fragment, GameActivity::class.java).apply {
            putExtra(INTENT_KEY_KEYWORD, keyword )
        }
    }

}