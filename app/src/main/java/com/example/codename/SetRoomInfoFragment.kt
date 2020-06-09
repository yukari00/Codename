package com.example.codename

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_type_room_info.*
import kotlin.math.log

class SetRoomInfoFragment : Fragment() {

    val database = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        when(status){
            Status.CREATE_ROOM -> btn_go_next.text = "作成する"
            Status.JOIN_ROOM -> btn_go_next.text = "参加する"
        }

        btn_go_back.setOnClickListener {
            soundPool?.play2(soundIdButtonClicked)
            getFragmentManager()?.beginTransaction()?.remove(this)?.commit()
        }

        btn_go_next.setOnClickListener {
            soundPool?.play2(soundIdButtonClicked)
            typeInfo()
        }

    }

    private fun typeInfo() {

        val nickname = input_edit_nickname.text.toString()
        val keyword = input_edit_keyword.text.toString()


        if (createNickname(nickname, keyword)) {
            ifKeywordAlreadyExist(keyword, nickname)
        }
    }

    private fun joinRoom(nickname: String, keyword: String) {

        doNotUpdateIfYouJoinedBefore(nickname, keyword)
    }

    private fun  doNotUpdateIfYouJoinedBefore(nickname: String, keyword: String) {

        database.collection(dbCollection).document(keyword).collection("members").whereEqualTo("name", nickname).get().addOnSuccessListener {
            if(it.isEmpty){
                Log.d("IF this is Empty", "${it.isEmpty}")
                val member = hashMapOf("member" to nickname)
                database.collection(dbCollection).document(keyword).collection("members").document(nickname)
                    .set(member)
            }
        }
    }

    private fun createRoom(nickname: String, keyword: String) {

        val newRoom = hashMapOf("keyword" to keyword)
        val member = hashMapOf("member" to nickname)

        database.collection(dbCollection).document(keyword).set(newRoom)
        database.collection(dbCollection).document(keyword).collection("members").document(nickname)
            .set(member)
    }

    private fun createNickname(nickname: String, keyword: String): Boolean {

        if (nickname == "") {
            input_nickname.error = "ニックネームを入力してください。"
            return false
        }

        if (keyword == "") {
            input_keyword.error = "キーワードを入力してください。"
            return false
        }
        return true

    }

    private fun ifKeywordAlreadyExist(keyword: String, nickname: String) {
        //Todo キーワードが既に存在していないチェック
        database.collection(dbCollection).whereEqualTo("keyword", keyword).get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    when (status) {
                        Status.CREATE_ROOM -> {
                            createRoom(nickname, keyword)
                            startActivity(GameActivity.getLaunched(activity, keyword, nickname))
                        }
                        Status.JOIN_ROOM -> {
                            input_keyword.error = "そのようなキーワードは存在しません"
                        }
                    }
                } else {
                    when (status) {
                        Status.CREATE_ROOM -> {
                            input_keyword.error = "このキーワードは使用できません。"

                        }
                        Status.JOIN_ROOM -> {
                            joinRoom(nickname, keyword)
                            if (nickname == "GM") status = Status.CREATE_ROOM
                            startActivity(GameActivity.getLaunched(activity, keyword, nickname))
                        }
                    }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_type_room_info, container, false)
    }

    companion object {

        @JvmStatic
        fun newInstance(status: Status) =
            SetRoomInfoFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

}