package com.example.codename

import android.app.AlertDialog
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

    val membersList: MutableList<String> = mutableListOf()

    interface OnFragmentGameSettingListener {

        fun GameStart()
        fun OnDeleted()
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
            soundPool?.play2(soundIdButtonClicked)
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
            //ランダムにチーム再編成
            splitMembersToTwoTeam(membersList)
        }

        btn_leave_room.setOnClickListener {
            AlertDialog.Builder(activity).apply {
                setTitle("退出")
                setMessage("退出しますか？")
                setPositiveButton("退出する"){dialog, which ->
                    deleteMemberInfo()
                }
                setNegativeButton("キャンセル"){dialog, which ->  }
                show()
            }
        }

    }

    private fun deleteMemberInfo() {

        listener?.OnDeleted()
        getFragmentManager()?.beginTransaction()?.remove(this)?.commit()

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

                    splitMembersToTwoTeam(membersList)

                } else {
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

                                    text_red_mem_num.setText("赤チームの人数は${teamRed.size}人です")
                                    text_blue_mem_num.setText("青チームの人数は${teamBlue.size}人です")

                                    setSpinner(teamRed, teamBlue)
                                    individualsInfo(teamRed, teamBlue)
                }

                //Creatorがチームを作っている間にロード中ができたらいい（チーム編成が終わったらメンバーの画面が表示される）

                            }
                    }
            }
    }

    private fun individualsInfo(
        teamRed: MutableList<String>,
        teamBlue: MutableList<String>
    ) {

        val myTeam = if (teamRed.contains(nickname)) "RED" else "BLUE"

        //ホストを取得
        showHost(myTeam)

        if (myTeam == "RED") {
            text_tell_which_team.setText("${nickname}さん、あなたは赤チームです")
            text_my_team_members.setText("${teamRed.joinToString()}")
        } else {
            text_tell_which_team.setText("${nickname}さん、あなたは青チームです")
            text_my_team_members.setText("${teamBlue.joinToString()}")
        }

    }

    private fun showHost(myTeam: String) {
        var host: String? = ""

        database.collection(dbCollection).document(keyword).collection("members")
            .whereEqualTo("host", true).get().addOnSuccessListener {
                if (it.isEmpty) {
                    text_if_leader.setText("話し合いでチームリーダを決めてください")
                    btn_prepared.isEnabled = false
                }
                else {
                    btn_prepared.isEnabled = it.size() == 2

                   for(document in it){
                       if(document.getString("team") == myTeam){

                           host = document.getString("name")
                       }
                   }
                    isHost = when(host){
                        nickname -> {
                            text_if_leader.setText("あなたはリーダーです")
                            true
                        }
                        "" -> {
                            text_if_leader.setText("話し合いでチームリーダを決めてください")
                            false
                        }
                        else -> {
                            text_if_leader.setText("あなたのチームのリーダーは${host}です")
                            false
                        }
                    }
                }
            }
    }

    private fun splitMembersToTwoTeam(membersList: MutableList<String>) {

        Collections.shuffle(membersList)

        val teamRed: MutableList<String> = mutableListOf()
        val teamBlue: MutableList<String> = mutableListOf()

        val memberNum = membersList.size / 2
        for (i in 0 until memberNum) {
            teamRed.add(membersList[i])
        }
        for (j in memberNum until membersList.size) {
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

        text_red_mem_num.setText("赤チームの人数は${teamRed.size}人です")
        text_blue_mem_num.setText("青チームの人数は${teamBlue.size}人です")

        setSpinner(teamRed, teamBlue)
        individualsInfo(teamRed, teamBlue)

    }

    private fun setSpinner(teamRed: MutableList<String>, teamBlue: MutableList<String>) {

        var host: String = ""
        var listNotSelected = mutableListOf<String>()
        isMyTeam = if (teamRed.contains(nickname)) {
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

                listNotSelected = wasNotSelectedPeopleList(selectedMember, isMyTeam ,teamRed, teamBlue)

                }
            }

        btn_change_leader.setOnClickListener {
            soundPool?.play2(soundIdButtonClicked)

            if(host == nickname) isHost = true
            Log.d("MEMBER", "$isHost")
            Log.d("nickname", "$nickname")
            Log.d("HOST", "$host")

            val newHost = Member(host, isMyTeam, isHost = true)

            listNotSelected.forEach {
                database.collection(dbCollection).document(keyword).collection("members")
                    .document(it)
                    .set(Member(it, isMyTeam, isHost= false))
            }

            database.collection(dbCollection).document(keyword).collection("members")
                .document(host)
                .set(newHost)

            val myTeamString = if (isMyTeam == Team.RED) "RED" else "BLUE"
            showHost(myTeamString)

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

    private fun wasNotSelectedPeopleList(selectedMember: String, isMyTeam: Team, teamRed: MutableList<String>, teamBlue: MutableList<String>): MutableList<String> {

        when(isMyTeam){

            Team.RED -> return teamRed.filterNot { it == selectedMember }.toMutableList()
            Team.BLUE -> return teamBlue.filterNot { it == selectedMember }.toMutableList()
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