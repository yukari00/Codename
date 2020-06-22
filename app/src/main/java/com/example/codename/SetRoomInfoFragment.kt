package com.example.codename

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_type_room_info.*

class SetRoomInfoFragment : Fragment() {

    val database = FirebaseFirestore.getInstance()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        when(status){
            Status.CREATE_ROOM -> btn_go_next.text = getString(R.string.create)
            Status.JOIN_ROOM -> btn_go_next.text = getString(R.string.join)
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

        database.collection(dbCollection).document(keyword).collection(collectionMembers).whereEqualTo(
            nameFieldPath, nickname).get().addOnSuccessListener {
            if(it.isEmpty){
                val member = hashMapOf(memberFieldPath to nickname)
                database.collection(dbCollection).document(keyword).collection(collectionMembers).document(nickname)
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
            input_nickname.error = getString(R.string.error_please_type_nickname)
            return false
        }

        if (keyword == "") {
            input_keyword.error = getString(R.string.error_please_type_keyword)
            return false
        }
        return true

    }

    private fun ifKeywordAlreadyExist(keyword: String, nickname: String) {

        database.collection(dbCollection).document(keyword).get()
            .addOnSuccessListener {
                if (!it.exists()) {
                    when (status) {
                        Status.CREATE_ROOM -> {
                            createRoom(nickname, keyword)
                            startActivity(GameActivity.getLaunched(activity, keyword, nickname))
                        }
                        Status.JOIN_ROOM -> {
                            input_keyword.error = getString(R.string.error_keyword_does_not_exist)
                        }
                    }
                } else {
                    when (status) {
                        Status.CREATE_ROOM -> {
                            input_keyword.error = getString(R.string.error_keyword_cannot_be_used)

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

    companion object{
        const val collectionMembers = "members"

        const val memberFieldPath = "member"
        const val nameFieldPath = "name"
    }
}