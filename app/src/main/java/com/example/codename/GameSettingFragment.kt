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
import com.google.firebase.firestore.*
import com.google.protobuf.Empty
import kotlinx.android.synthetic.main.fragment_game_setting.*
import java.util.*


class GameSettingFragment : Fragment() {

    private var keyword: String = ""
    private var nickname: String = ""

    private var isPrepared: Boolean? = false

    private val database = FirebaseFirestore.getInstance()

    private var listener: OnFragmentListener? = null
    private lateinit var adapter: ArrayAdapter<String>

    private var membersList: MutableList<String> = mutableListOf()

    private lateinit var listeningUpdate: ListenerRegistration

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

            listener?.GameStart()
            getFragmentManager()?.beginTransaction()?.remove(this)?.commit()
        }

        btn_team_random.setOnClickListener {
            //ランダムにチーム再編成
            splitMembersToTwoTeam(membersList)
        }

        btn_leave_room.setOnClickListener {

            leaveRoom()
        }

    }

    private fun leaveRoom() {
        AlertDialog.Builder(activity).apply {
            setTitle("退出")
            setMessage("退出しますか？")
            setPositiveButton("退出する") { dialog, which ->
                if (membersList.size <= 4) {
                    membersLessThanFour(membersList)
                } else{
                    deleteMemberInfo()
                }
            }
            setNegativeButton("キャンセル"){dialog, which ->  }
            show()
        }
    }

    private fun membersLessThanFour(membersList: MutableList<String>) {
        AlertDialog.Builder(activity).apply {
            setTitle("注意")
            setMessage("${nickname}さんが退出すると参加メンバーの人数が４人未満になるのでルームが強制的に削除されますが、良いですか？")
            setPositiveButton("はい") { dialog, which ->
                deleteRoom(membersList)
            }
            setNegativeButton("キャンセル") { dialog, which -> }
            show()
        }
    }

    private fun deleteRoom(membersList: MutableList<String>){
        listener?.OnRoomDeleted(membersList)
        getFragmentManager()?.beginTransaction()?.remove(this)?.commit()
    }

    private fun deleteMemberInfo() {

        listener?.OnMemberDeleted()
        getFragmentManager()?.beginTransaction()?.remove(this)?.commit()

    }

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
        adapter.clear()
        listeningUpdate.remove()
    }

    private fun update() {

        listeningUpdate = database.collection(dbCollection).document(keyword).collection("members").addSnapshotListener { it, e->

            val membersListUpdate: MutableList<String> = mutableListOf()

            if(e != null) return@addSnapshotListener

            if(it == null || it.isEmpty) return@addSnapshotListener
            for (document in it) {
                if(document.getString("member").isNullOrEmpty()){
                    membersListUpdate.add(document.getString("name")!!)
                }else{
                    membersListUpdate.add(document.getString("member")!!)
                }
            }

            membersList = membersListUpdate

            if (status == Status.CREATE_ROOM) {

                splitMembersToTwoTeam(membersListUpdate)

            }else getTwoTeamInfoFromFirestore()
        }


}

    private fun getTwoTeamInfoFromFirestore() {

       database.collection(dbCollection).document(keyword).collection("members")
            .whereEqualTo("team", "RED").get().addOnSuccessListener {
                val teamRed: MutableList<String> = mutableListOf()

                if(it == null || it.isEmpty) return@addOnSuccessListener
                for (document in it) {
                    teamRed.add(document.getString("name")!!)
                }

                pickBlueteam(teamRed)
                //Creatorがチームを作っている間にロード中ができたらいい（チーム編成が終わったらメンバーの画面が表示される）

            }
    }

    private fun pickBlueteam(teamRed: MutableList<String>) {
        database.collection(dbCollection).document(keyword).collection("members")
            .whereEqualTo("team", "BLUE").get().addOnSuccessListener {

                if(it == null || it.isEmpty) return@addOnSuccessListener

                val teamBlue: MutableList<String> = mutableListOf()
                for (document in it) {
                    teamBlue.add(document.getString("name")!!)
                }

                Log.d("チーム赤", "$teamRed")
                Log.d("チーム青", "$teamBlue")

                Log.d("チーム赤", "${teamRed.size}")
                text_red_mem_num.setText("赤チームの人数は${teamRed.size}人です")
                text_blue_mem_num.setText("青チームの人数は${teamBlue.size}人です")

                setSpinner(teamRed, teamBlue)
                individualsInfo(teamRed, teamBlue)
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

        database.collection(dbCollection).document(keyword).collection("members")
            .whereEqualTo("host", true).get().addOnSuccessListener {

                if (it == null) return@addOnSuccessListener

                var host: String? = ""
                if (it.isEmpty) {
                    text_if_leader.setText("話し合いでチームリーダを決めてください")
                    btn_prepared?.isEnabled = false
                } else {
                    //if(btn_prepared == null) return@addSnapshotListener //Todo なぜかbtn_preparedがnullになる
                    btn_prepared?.isEnabled = it.size() == 2

                    for (document in it) {
                        if (document.getString("team") == myTeam) {

                            host = document.getString("name")
                        }
                    }
                    isHost = when (host) {
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
            Log.d("nickname", nickname)
            Log.d("HOST", host)

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