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

class SetRoomInfoFragment : Fragment() {

    val database = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        btn_go_next.setOnClickListener {
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

        val memberList = Member(nickname)

        database.collection(dbCollection).document(keyword).collection("members").document(nickname)
            .set(memberList)


    }

    private fun createRoom(nickname: String, keyword: String) {

        val newRoom = hashMapOf("keyword" to keyword)
        val memberList = Member(nickname)

        database.collection(dbCollection).document(keyword).set(newRoom)
        database.collection(dbCollection).document(keyword).collection("members").document(nickname)
            .set(memberList)
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

        const val INTENT_KEY_STATUS = "INTENT_KEY_STATUS"

        @JvmStatic
        fun newInstance(status: Status) =
            SetRoomInfoFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(INTENT_KEY_STATUS, status)
                }
            }
    }

}