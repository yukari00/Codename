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
import kotlin.collections.HashMap


class GameSettingFragment : Fragment() {

    private var keyword: String = ""
    private var nickname: String = ""

    private val database = FirebaseFirestore.getInstance()

    private var listener: OnFragmentListener? = null
    var adapter: ArrayAdapter<String>? = null

    private var membersList: MutableList<Uid> = mutableListOf()

    var listeningMembers: ListenerRegistration? = null
    var listeningReadySign: ListenerRegistration? = null

    lateinit var mContext: Context

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

        when(status){
            Status.JOIN_ROOM -> {
                btn_team_random.visibility = View.INVISIBLE
                btn_prepared.visibility = View.INVISIBLE
            }
            Status.CREATE_ROOM -> btn_prepared.isEnabled = false
        }

        update()
        btn_prepared.setOnClickListener {
            soundPool?.play2(soundIdButtonClicked)
            database.collection(dbCollection).document(keyword).update("readyForGame", true)
        }

        btn_team_random.setOnClickListener {

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

    private fun membersLessThanFour(membersList: MutableList<Uid>) {
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

    private fun deleteRoom(membersList: MutableList<Uid>){
        listener?.OnRoomDeleted(membersList)
        getFragmentManager()?.beginTransaction()?.remove(this)?.commit()
    }

    private fun deleteMemberInfo() {

        listener?.OnMemberDeleted()
        getFragmentManager()?.beginTransaction()?.remove(this)?.commit()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        if (context is OnFragmentListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        adapter?.clear()
        listeningMembers?.remove()
        listeningReadySign?.remove()
    }

    private fun update() {

        Log.d("update", "update")
        database.collection(dbCollection).document(keyword).collection("members").get().addOnSuccessListener {

            val membersListUpdate: MutableList<Uid> = mutableListOf()

            if(it == null || it.isEmpty) return@addOnSuccessListener
            for (document in it) {
                val name = document.getString("name")?: ""
                val uid = document.id
                Log.d("document.id",uid)
                    membersListUpdate.add(Uid(name, uid))
            }
            membersList = membersListUpdate
            Log.d("status", "$status")

            if (status == Status.CREATE_ROOM) {

                splitMembersToTwoTeam(membersListUpdate)
                getTwoTeamInfoFromFirestore()

            }else getTwoTeamInfoFromFirestore()
        }

}

    private fun getTwoTeamInfoFromFirestore() {

       listeningMembers = database.collection(dbCollection).document(keyword).collection("members")
            .addSnapshotListener { it, e ->
                val membersListUpdate: MutableList<Uid> = mutableListOf()
               val teamRed: MutableList<Uid> = mutableListOf()
                val teamBlue: MutableList<Uid> = mutableListOf()

               Log.d("!!!!!!!!!!!!", "!!!!!!!!!!!!!!")

               if(e != null) return@addSnapshotListener
               if(it == null || it.isEmpty) return@addSnapshotListener
               for (document in it) {
                   val name = document.getString("name")?: ""
                   val uid = document.id
                   val team = document.getString("team")
                   membersListUpdate.add(Uid(name, uid))
                   if(team == "RED") teamRed.add(Uid(name, uid)) else teamBlue.add(Uid(name, uid))
               }

                membersList = membersListUpdate

                if(teamRed.size != 0) {
                    setSpinner(teamRed, teamBlue)
                    individualsInfo(teamRed, teamBlue)
                }
           }
    }

    private fun individualsInfo(
        teamRed: MutableList<Uid>,
        teamBlue: MutableList<Uid>
    ) {

        val teamRedNameList = mutableListOf<String>()
        val teamBlueNameList = mutableListOf<String>()
        teamRed.forEach {
            teamRedNameList.add(it.name)
        }
        teamBlue.forEach {
            teamBlueNameList.add(it.name)
        }
        val myTeam = if (teamRedNameList.contains(nickname)) "RED" else "BLUE"

        //ホストを取得
        showHost(myTeam)

        if (myTeam == "RED") {
            text_tell_which_team.setText("${nickname}さん、あなたは赤チームです")
            text_my_team_members.setText("${teamRedNameList.joinToString()}")
        } else {
            text_tell_which_team.setText("${nickname}さん、あなたは青チームです")
            text_my_team_members.setText("${teamBlueNameList.joinToString()}")
        }

        text_red_mem_num.setText("赤チームの人数は${teamRedNameList.size}人です")
        text_blue_mem_num.setText("青チームの人数は${teamBlueNameList.size}人です")

    }

    private fun showHost(myTeam: String) {

       database.collection(dbCollection).document(keyword).collection("members")
            .whereEqualTo("host", true).get().addOnSuccessListener {

               var numberOfHost = 0
                var host: String? = ""
                if (it == null || it.isEmpty) {
                    text_if_leader.setText("話し合いでスパイマスターを決めてください")

                } else {

                    numberOfHost = it.size()

                    for (document in it) {
                        if (document.getString("team") == myTeam) {

                            host = document.getString("name")
                        }
                    }
                    isHost = when (host) {
                        nickname -> {
                            text_if_leader.setText("あなたはスパイマスターです")
                            true
                        }
                        "" -> {
                            text_if_leader.setText("話し合いでスパイマスターを決めてください")
                            false
                        }
                        else -> {
                            text_if_leader.setText("あなたのチームのスパイマスターは${host}です")
                            false
                        }
                    }
                }

                if(status == Status.CREATE_ROOM){
                    if(numberOfHost == 2){
                        btn_prepared.isEnabled = true
                    }
                }

                listeningReadySign = database.collection(dbCollection).document(keyword).addSnapshotListener { it, e ->

                    if (e != null) return@addSnapshotListener
                    if (it == null || it["readyForGame"] == null) return@addSnapshotListener

                    val ready = it.getBoolean("readyForGame") ?: return@addSnapshotListener
                    if (ready) {
                        listener?.GameStart()
                        getFragmentManager()?.beginTransaction()?.remove(this)?.commit()
                    }
                }
            }
    }


    private fun splitMembersToTwoTeam(membersList: MutableList<Uid>) {

        val teamRed: MutableList<Uid> = mutableListOf()
        val teamBlue: MutableList<Uid> = mutableListOf()

        Collections.shuffle(membersList)

        val memberNum = membersList.size / 2
        for (i in 0 until memberNum) {
            teamRed.add(membersList[i])
        }

        for (j in memberNum until membersList.size) {
            teamBlue.add(membersList[j])
        }

        Log.d("teamRed", "$teamRed")
        Log.d("teamBlue", "$teamBlue")

        val numRedMember = teamRed.size
        val numBlueMember = teamBlue.size

        Log.d("teamRedSize", "$numRedMember")
        Log.d("teamBlueSize", "$numBlueMember")

        teamRed.forEach { it ->
            val memberInfo = Member(it.name, team = Team.RED)
            database.collection(dbCollection).document(keyword).collection("members").document(it.uid).set(memberInfo).addOnSuccessListener {
                teamBlue.forEach {
                    val memberInfo = Member(it.name, team = Team.BLUE)
                    database.collection(dbCollection).document(keyword).collection("members")
                        .document(it.uid).set(memberInfo).addOnSuccessListener {
                        getTwoTeamInfoFromFirestore()
                    }
                }
            }
        }
        //Todo セットできていない！

    }

    private fun setSpinner(teamRed: MutableList<Uid>, teamBlue: MutableList<Uid>) {

        val teamRedNameList = mutableListOf<String>()
        val teamBlueNameList = mutableListOf<String>()
        teamRed.forEach {
            teamRedNameList.add(it.name)
        }
        teamBlue.forEach {
            teamBlueNameList.add(it.name)
        }

        Log.d("teamRedNameList", "$teamRedNameList")
        Log.d("teamBlueNameList", "$teamBlueNameList")

        var host: String = ""
        var listNotSelected = mutableListOf<Uid>()
        isMyTeam = if (teamRedNameList.contains(nickname)) {
            Team.RED
        } else Team.BLUE

        Log.d("activity", "$activity")

        adapter=
            when (isMyTeam) {
                Team.RED -> ArrayAdapter(activity!!.baseContext,
                    R.layout.spiner_item,
                    teamRedNameList)
                Team.BLUE -> ArrayAdapter(activity!!.baseContext,
                    R.layout.spiner_item,
                    teamBlueNameList)
            }
        adapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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

            val newHost = Member(host, isMyTeam, isHost = true)

            listNotSelected.forEach {
                database.collection(dbCollection).document(keyword).collection("members")
                    .document(it.uid)
                    .set(Member(it.name, isMyTeam, isHost= false))
            }

            val hostUid = getHostUid(teamRed, teamBlue, host)
            database.collection(dbCollection).document(keyword).collection("members")
                .document(hostUid)
                .set(newHost)

            val myTeam = if (teamRedNameList.contains(nickname)) "RED" else "BLUE"
            showHost(myTeam)
        }
    }

    private fun getHostUid(teamRed: MutableList<Uid>, teamBlue: MutableList<Uid>, host: String): String {

        val hostInfo = teamRed.filter {
            it.name == host
        }

        if(hostInfo.isNotEmpty()) return hostInfo[0].uid

        val hostInfoB = teamBlue.filter {
            it.name == host
        }
        return hostInfoB[0].uid
    }

    private fun wasNotSelectedPeopleList(selectedMember: String, isMyTeam: Team, teamRed: MutableList<Uid>, teamBlue: MutableList<Uid>): MutableList<Uid> {

        when(isMyTeam){

            Team.RED -> return teamRed.filterNot { it.name == selectedMember }.toMutableList()
            Team.BLUE -> return teamBlue.filterNot { it.name == selectedMember }.toMutableList()
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