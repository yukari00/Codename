package com.example.codename

import android.content.Context
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
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.protobuf.Empty
import kotlinx.android.synthetic.main.fragment_game_setting.*
import java.util.*


class GameSettingFragment : Fragment() {

    private var keyword: String = ""
    private var nickname: String = ""

    var isPrepared: Boolean? = false

    val database = FirebaseFirestore.getInstance()

    var listener: OnFragmentGameSettingListener? = null
    lateinit var adapter: ArrayAdapter<String>

    interface OnFragmentGameSettingListener {

        fun GameStart()
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
        return inflater.inflate(R.layout.fragment_game_setting, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        update()

        btn_prepared.setOnClickListener {
            //Todo 準備完了ボタン処
            when (isPrepared) {
                false -> {
                    btn_prepared.setText("待機中")
                    isPrepared = true
                }
                true -> {
                    btn_prepared.setText("準備完了")
                    isPrepared = false
                }
            }

            listener?.GameStart()
            getFragmentManager()?.beginTransaction()?.remove(this)?.commit()
        }

        btn_team_random.setOnClickListener {
            //Todo ランダムにチーム再編成
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentGameSettingListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        adapter.clear()

    }


    private fun update() {

        val membersList: MutableList<String> = mutableListOf()

        database.collection(dbCollection).document(keyword).collection("members").get()
            .addOnSuccessListener {
                for (document in it) {
                    if(document.getString("member").isNullOrEmpty()){
                        membersList.add(document.getString("name")!!)
                    }else{
                        membersList.add(document.getString("member")!!)
                    }
                }

                if (status == Status.CREATE_ROOM) {
                    splitMembersToTwoTeam(membersList, nickname)
                }

                //Creatorがチームを作っている間にロード中ができたらいい（チーム編成が終わったらメンバーの画面が表示される）
                val teamRed: MutableList<String> = mutableListOf()
                val teamBlue: MutableList<String> = mutableListOf()
                database.collection(dbCollection).document(keyword).collection("members")
                    .whereEqualTo("team", "RED").get().addOnSuccessListener {
                        for (document in it) {
                            teamRed.add(document.getString("name")!!)
                        }

                        database.collection(dbCollection).document(keyword).collection("members")
                            .whereEqualTo("team", "BLUE").get().addOnSuccessListener {
                                for (document in it) {
                                    teamBlue.add(document.getString("name")!!)
                                }

                                Log.d("チーム赤", "$teamRed")
                                Log.d("チーム青", "$teamBlue")
                                setSpinner(teamRed, teamBlue)
                                individualsInfo(teamRed, teamBlue)
                            }
                    }
            }
    }

    private fun individualsInfo(
        teamRed: MutableList<String>,
        teamBlue: MutableList<String>
    ) {

        var isYourHost: Boolean? = false

        val isMyTeam = if (teamRed.contains(nickname)) {
            Team.RED
        } else Team.BLUE

        val docRef: DocumentReference =
            database.collection(dbCollection).document(keyword).collection("members")
                .document(nickname)
        docRef.get().addOnSuccessListener {
            isYourHost = it.getBoolean("host")
            isPrepared = it.getBoolean("prepared")

            //ホストを取得
            showHost(isYourHost)
        }
        if (isMyTeam == Team.RED) text_tell_which_team.setText("あなたは赤チームです") else text_tell_which_team.setText(
            "あなたは青チームです"
        )

    }

    private fun showHost(ifYourHost: Boolean?) {
        var host: String? = ""
        database.collection(dbCollection).document(keyword).collection("members")
            .whereEqualTo("host", true).get().addOnSuccessListener {
                Log.d("CHHHHHHHHHHHCHECKTHIS", "${it}")
               if(it.isEmpty){
                    text_if_leader.setText("話し合いでチームリーダを決めてください")
                }
                else {
                    host = it.documents.first().getString("name")
                    if (ifYourHost!!) text_if_leader.setText("あなたはリーダーです") else text_if_leader.setText(
                        "あなたのチームのリーダーは${host}です"
                    )
                }
            }
    }

    private fun splitMembersToTwoTeam(membersList: MutableList<String>, nickname: String) {

        Collections.shuffle(membersList)

        val teamRed: MutableList<String> = mutableListOf()
        val teamBlue: MutableList<String> = mutableListOf()

        val memberNum = membersList.size / 2
        for (i in 0..memberNum - 1) {
            teamRed.add(membersList[i])
        }
        for (j in memberNum..membersList.size - 1) {
            teamBlue.add(membersList[j])
        }

        teamRed.forEach {
            val memberInfo = Member(it, team = Team.RED)
            database.collection(dbCollection).document(keyword).collection("members").document(it)
                .set(memberInfo)
        }

        teamBlue.forEach {
            val memberInfo = Member(it, team = Team.BLUE)
            database.collection(dbCollection).document(keyword).collection("members").document(it)
                .set(memberInfo)
        }

        setSpinner(teamRed, teamBlue)
        individualsInfo(teamRed, teamBlue)

        text_red_mem_num.setText("赤チームの人数は${teamRed.size}人です")
        text_blue_mem_num.setText("青チームの人数は${teamBlue.size}人です")

    }

    private fun setSpinner(teamRed: MutableList<String>, teamBlue: MutableList<String>) {

        var host: String = ""

        val isMyTeam = if (teamRed.contains(nickname)) {
            Team.RED
        } else Team.BLUE

        adapter=
            when (isMyTeam) {
                Team.RED -> ArrayAdapter(activity!!, android.R.layout.simple_spinner_item, teamRed)
                Team.BLUE -> ArrayAdapter(
                    activity!!,
                    android.R.layout.simple_spinner_item,
                    teamBlue
                )
            }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                //何もクリックしないときの処理
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val spinnerParent = parent as Spinner
                val selectedMember = spinnerParent.selectedItem as String
                host = selectedMember

            }
        }

        btn_change_leader.setOnClickListener {

            val newHost = Member(host, isMyTeam, isHost = true)

            database.collection(dbCollection).document(keyword).collection("members")
                .document(nickname)
                .set(newHost)

            text_if_leader.setText("あなたのチームのリーダーは${host}です")

            database.collection(dbCollection).document(keyword).collection("members")
                .whereEqualTo("host", true).get().addOnSuccessListener {
                    val host: MutableList<QueryDocumentSnapshot> = mutableListOf()
                    for (document in it) {
                        host.add(document)
                    }

                    btn_prepared.isEnabled = host.size == 2
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