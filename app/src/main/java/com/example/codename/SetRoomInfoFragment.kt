package com.example.codename

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_type_room_info.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SetRoomInfoFragment : Fragment() {

    val database = FirebaseFirestore.getInstance()

    private var param1: String? = null
    private var param2: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        if(createNickname(nickname, keyword)) {

            val newRoom = hashMapOf("keyword" to keyword )
            val memberList = hashMapOf("name" to nickname)
            val list = hashMapOf("word1" to "apple", "word2" to "orange", "word3" to "grape")

            database.collection(dbCollection).document(keyword).set(newRoom)
            database.collection(dbCollection).document(keyword).collection("members").document(nickname)
                .set(memberList)
            database.collection(dbCollection).document(keyword).collection("words").document(keyword)
                .set(list)
        }


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

        if(!ifAlreadyExist()){
            input_keyword.error = "このキーワードは使用できません。"
            return false
        }

        return true

    }

    private fun ifAlreadyExist(): Boolean {
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

        const val dbCollection = "COLLECTION"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SetRoomInfoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

}