package com.example.codename

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_type_room_info.*

class SetRoomInfoFragment : Fragment() {

    val database = FirebaseFirestore.getInstance()

    private var status: Status? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            status = it.getSerializable(INTENT_KEY_STATUS) as Status
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        btn_go_next.setOnClickListener {
            typeInfo(status)
        }

    }

    private fun typeInfo(status: Status?) {

        val nickname = input_edit_nickname.text.toString()
        val keyword = input_edit_keyword.text.toString()

        if(createNickname(nickname, keyword)) {

            when(status){
                Status.CREATE_ROOM -> createRoom(nickname, keyword)
                Status.JOIN_ROOM -> joinRoom(nickname, keyword)
            }

            startActivity(GameActivity.getLaunched(activity, keyword))
        }
    }

    private fun joinRoom(nickname: String, keyword: String) {

        val memberList = hashMapOf("name" to nickname)

        if(!ifKeywordAlreadyExist()){
            Toast.makeText(activity, "そのようなキーワードは存在しません。", Toast.LENGTH_SHORT).show()

        } else {
            database.collection(dbCollection).document(keyword).collection("members").document(nickname)
                .set(memberList)
        }


    }

    private fun createRoom(nickname: String, keyword: String) {

        val newRoom = hashMapOf("keyword" to keyword )
        val memberList = hashMapOf("name" to nickname)
        val list = WordsData("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
        "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22",
            "23", "24", "25")

        database.collection(dbCollection).document(keyword).set(newRoom)
        database.collection(dbCollection).document(keyword).collection("members").document(nickname)
            .set(memberList)
        database.collection(dbCollection).document(keyword).collection("words").document(keyword)
            .set(list)
    }

    private fun createNickname(nickname: String, keyword: String): Boolean {

        if(nickname == "") {
            input_nickname.error = "ニックネームを入力してください。"
            return false
        }

        if(keyword == ""){
            input_keyword.error = "キーワードを入力してください。"
            return false
        }

        if(!ifKeywordAlreadyExist()){
            input_keyword.error = "このキーワードは使用できません。"
            return false
        }

        return true

    }

    private fun ifKeywordAlreadyExist(): Boolean {
        //Todo キーワードが既に存在していないチェック
        return true
    }

    private fun checkIfTypedCorrectly(nickname: String): Boolean {

        if(nickname == "") {
            input_nickname.error = "ニックネームを入力してください"
            return false
        }



        return true
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