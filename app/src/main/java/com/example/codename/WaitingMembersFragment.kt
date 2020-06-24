package com.example.codename

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.fragment_waiting_members.*


class WaitingMembersFragment : Fragment() {

    private var keyword: String = ""
    private var nickname: String = ""

    var listener: OnFragmentListener? = null
    val database = FirebaseFirestore.getInstance()

    var listeningMembers: ListenerRegistration? = null
    var listeningReadySign: ListenerRegistration? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        listeningMembers?.remove()
        listeningReadySign?.remove()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            keyword = it.getString(INTENT_KEY_KEYWORD)!!
            nickname = it.getString(INTENT_KEY_NICKNAME)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_waiting_members, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        listeningMembers = database.collection(dbCollection).document(keyword).collection("members").addSnapshotListener { it, e ->
            val memberList: MutableList<String> = mutableListOf()
            val memberNameUidList = mutableListOf<Uid>()

            if (e != null) return@addSnapshotListener

            if(it == null || it.isEmpty) return@addSnapshotListener
            for (document in it) {
                val name = document.getString("name")?:""
                val uid = document.id
                memberList.add(name)
                memberNameUidList.add(Uid(name, uid))
            }
            Log.d("memberList", "$memberList")
            text_member_join.setText(memberList.joinToString())
            text_member_join_num.setText("現在参加人数は${memberList.size}人です")

            when(status){
                Status.JOIN_ROOM -> btn_game_start.visibility = View.INVISIBLE
                Status.CREATE_ROOM -> btn_game_start.isEnabled = memberList.size >= 4
            }

            btn_game_start.setOnClickListener {
                soundPool?.play2(soundIdButtonClicked)
                database.collection(dbCollection).document(keyword).update("readyForGameSetting", true)
            }

            btn_go_back.setOnClickListener {
                soundPool?.play2(soundIdButtonClicked)
                when(status){
                    Status.CREATE_ROOM -> deleteRoom(memberNameUidList)
                    Status.JOIN_ROOM -> deleteMemberInfo()
                }

            }

            onListeningReadySign()

        }
    }

    private fun onListeningReadySign() {
        listeningReadySign =
            database.collection(dbCollection).document(keyword).addSnapshotListener { it, e ->

                if (e != null) return@addSnapshotListener
                if (it == null || it["readyForGameSetting"] == null) return@addSnapshotListener

                val ready = it.getBoolean("readyForGameSetting") ?: return@addSnapshotListener
                if (ready) {
                    listener?.OnMembersGathered()
                    getFragmentManager()?.beginTransaction()?.remove(this)?.commit()
                }
            }
    }

    private fun deleteMemberInfo() {
        AlertDialog.Builder(activity).apply {
            setTitle("退出")
            setMessage("退出しますか？")
            setPositiveButton("退出する"){dialog, which ->
                listener?.OnMemberDeleted()
                getFragmentManager()?.beginTransaction()?.remove(this@WaitingMembersFragment)?.commit()
            }
            setNegativeButton("キャンセル"){dialog, which ->  }
            show()
        }
    }

    private fun deleteRoom(memberList: MutableList<Uid>) {
        AlertDialog.Builder(activity).apply {
            setTitle("注意")
            setMessage("ルーム作成者である${nickname}さんが退出した場合、ルームは削除され現在ルーム内に居る全プレイヤーが強制的に退出させられることになりますが、よろしいですか？")
            setPositiveButton("はい"){dialog, which ->

                listener?.OnRoomDeleted(memberList)
                getFragmentManager()?.beginTransaction()?.remove(this@WaitingMembersFragment)?.commit()
            }
            setNegativeButton("キャンセル"){dialog, which ->  }
            show()
        }

    }

    companion object {

        const val INTENT_KEY_KEYWORD = "keyword"
        const val INTENT_KEY_NICKNAME = "nickname"

        @JvmStatic
        fun newInstance(keyword: String, nickname: String) =
            WaitingMembersFragment().apply {
                arguments = Bundle().apply {
                    putString(INTENT_KEY_KEYWORD, keyword)
                    putString(INTENT_KEY_NICKNAME, nickname)
                }
            }
    }
}