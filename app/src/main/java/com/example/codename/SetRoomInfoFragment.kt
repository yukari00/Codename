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
            typeNickname()
        }
    }

    private fun typeNickname() {

        val nickname: String? = createNickname()
        if(nickname != null) {
            val newRoom = hashMapOf("keyword" to "a")
            val memberList = hashMapOf("member1" to nickname)
            val list = hashMapOf("word1" to "apple", "word2" to "orange", "word3" to "grape")

            database.collection("keyword").document("keyword").set(newRoom)
            database.collection("keyword").document("keyword").collection("members")
                .add(memberList)
            database.collection("keyword").document("keyword").collection("words")
                .add(list)
        }


    }

    private fun createNickname(): String? {

        val nickname = input_edit_nickname.text.toString()

        if(checkIfTypedCorrectly(nickname)) return nickname

        return null

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
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BlankFragment.
         */
        // TODO: Rename and change types and number of parameters
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