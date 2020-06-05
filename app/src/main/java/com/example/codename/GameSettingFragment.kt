package com.example.codename

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_game_setting.*
import java.util.*


class GameSettingFragment : Fragment() {

    private var keyword: String = ""
    private var nickname: String = ""

    var isPrepared: Boolean? = false

    val database = FirebaseFirestore.getInstance()

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
        return inflater.inflate(R.layout.fragment_game_setting, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        update()

        btn_prepared.setOnClickListener {
            //Todo 準備完了ボタン処
            when(isPrepared){
                false -> {
                    btn_prepared.setText("待機中")
                    isPrepared = true
                }
                true -> {
                    btn_prepared.setText("準備完了")
                    isPrepared = false
                }
            }
        }

        btn_change_leader.setOnClickListener {
            //Todo リーダー変更ボタンクリック処理
        }

        btn_team_random.setOnClickListener {
            //Todo ランダムにチーム再編成
        }

    }

    private fun update() {
        var yourTeam: String? = "Team.RED"
        var ifYourHost: Boolean? = false
        val membersList: MutableList<String> = mutableListOf()


        //メンバー情報取得
        val docRef: DocumentReference =
            database.collection(dbCollection).document(keyword).collection("members")
                .document(nickname)
        docRef.get().addOnSuccessListener {
            yourTeam = it.getString("team")
            ifYourHost = it.getBoolean("host")
            isPrepared = it.getBoolean("prepared")

            //Todo ホストを取得
            val host = "host"
            if (yourTeam == "RED") text_tell_which_team.setText("あなたは赤チームです") else text_tell_which_team.setText(
                "あなたは青チームです"
            )
            if (ifYourHost!!) text_if_leader.setText("あなたはリーダーです") else text_if_leader.setText("あなたのチームのリーダーは${host}です")

            //Todo メンバーリスt取得

            database.collection(dbCollection).document(keyword).collection("members").get()
                .addOnSuccessListener {
                    for (document in it) {
                        membersList.add(document.getString("name")!!)
                    }
                    text_my_team_members.setText(membersList[0])
                    //スピナー設定
                    setSpinner(membersList)
                    SplitMembersToTwoTeam(membersList)

                }
        }

    }

    private fun SplitMembersToTwoTeam(membersList: MutableList<String>) {

        Collections.shuffle(membersList)

        val teamRed: MutableList<String> = mutableListOf()
        val teamBlue: MutableList<String> = mutableListOf()

        val memberNum = membersList.size / 2
        for (i in 0..memberNum - 1) {
            teamRed.add(membersList[i])
        }
        for (j in memberNum ..membersList.size - 1) {
            teamBlue.add(membersList[j])
        }

        text_red_mem_num.setText("赤チームの人数は${teamRed.size}人です")
        text_blue_mem_num.setText("青チームの人数は${teamBlue.size}人です")

    }


    private fun setSpinner(membersList: MutableList<String>) {
        val adapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_item, membersList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val spinnerParent = parent as Spinner
                val selectedMember = spinnerParent.selectedItem as String
                text_if_leader.setText("あなたのチームのリーダーは${selectedMember}です")
            }

        }
    }

    companion object {

        const val INTENT_KEY_KEYWORD = "keyword"
        const val INTENT_KEY_NICKNAME = "nickname"

        @JvmStatic
        fun newInstance(keyword: String, nickname: String) =
            GameSettingFragment().apply {
                arguments = Bundle().apply {
                    putString(INTENT_KEY_KEYWORD, keyword)
                    putString(INTENT_KEY_NICKNAME, nickname)
                }
            }
    }
}