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
    fun OnHostCallBack(hint: String, numCardPick: Int)

    //ResultFragment
    fun OnStartAnotherGame(turnCount: Int)
    fun OnGoBackOnGameSettingFragment()
}