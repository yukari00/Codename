package com.example.codename

interface OnFragmentListener {
    //WaitingFragment
    fun OnMembersGathered()

    //GameSettingFragmwent
    fun GameStart()

    //both
    fun OnRoomDeleted(membersList: MutableList<String>)
    fun OnMemberDeleted()

    //GameHostFragment
    fun OnHost(hint: String, numCardPick: Int)
}