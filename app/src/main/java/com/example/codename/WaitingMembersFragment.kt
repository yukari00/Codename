package com.example.codename

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_waiting_members.*


class WaitingMembersFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var keyword: String = ""
    private var nickname: String = ""

    var listener: OnFragmentWaitingListener? = null
    val database = FirebaseFirestore.getInstance()

    interface OnFragmentWaitingListener{
        fun OnMembersGathered()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentWaitingListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
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

        database.collection(dbCollection).document(keyword).collection("members").get()
            .addOnSuccessListener {
                val memberList: MutableList<String> = mutableListOf()
                for (document in it) {
                    memberList.add(document.getString("name")!!)
                }
                text_member_join.setText(memberList.joinToString())
                text_member_join_num.setText("現在参加人数は${memberList.size}人です")

                btn_game_start.isEnabled = memberList.size >= 4
            }

        btn_game_start.setOnClickListener {
            listener?.OnMembersGathered()
            getFragmentManager()?.beginTransaction()?.remove(this)?.commit();
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