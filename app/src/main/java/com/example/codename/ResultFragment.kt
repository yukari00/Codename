package com.example.codename

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.common.graph.ImmutableNetwork
import kotlinx.android.synthetic.main.fragment_result.*

class ResultFragment : Fragment() {

    private var reason: String = ""
    private var turnCount: Int = 0
    private var team: Team? = null
    var listener: OnFragmentListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            reason = it.getString(INTENT_KEY_REASON)?: return@let
            turnCount = it.getInt(INTENT_KEY_TURN_COUNT)
            team = it.getSerializable(INTENT_KEY_TEAM_GOT_GRAY) as Team?
        }
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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        when(reason){
            "RED" -> {
                when(isMyTeam){
                    Team.RED -> {
                        text_result.setText("赤チームの勝ちです")
                        soundPool?.play2(soundIdWinner)
                    }
                    Team.BLUE -> {
                        text_result.setText("青チームの負けです")
                        soundPool?.play2(soundIdLoser)
                    }
                }

            }
            "BLUE" -> {
                when(isMyTeam){
                    Team.RED -> {
                        text_result.setText("赤チームの負けです")
                        soundPool?.play2(soundIdLoser)
                    }
                    Team.BLUE -> {
                        text_result.setText("青チームの勝ちです")
                        soundPool?.play2(soundIdWinner)
                    }
                }

            }
            "GRAY" -> {
                when(team){
                    Team.RED -> {
                        when(isMyTeam){
                            Team.RED -> {
                                text_result.setText("赤チームの負けです")
                                soundPool?.play2(soundIdShock)
                            }
                            Team.BLUE -> {
                                text_result.setText("青チームの勝ちです")
                                soundPool?.play2(soundIdWinner)
                            }
                        }
                    }
                    Team.BLUE -> {
                        when(isMyTeam){
                            Team.RED -> {
                                text_result.setText("赤チームの勝ちです")
                                soundPool?.play2(soundIdWinner)
                            }
                            Team.BLUE -> {
                                text_result.setText("青チームの負けです")
                                soundPool?.play2(soundIdShock)
                            }
                        }
                    }
                }
            }
        }

        //if(status == Status.CREATE_ROOM){
          //  btn_another_game_start.isEnabled = true
          //  btn_back_game_setting.isEnabled = true
        //} else {
          //  btn_another_game_start.isEnabled = false
          //  btn_back_game_setting.isEnabled = false
        //}

        btn_another_game_start.setOnClickListener {
            listener?.OnStartAnotherGame(turnCount)
            getFragmentManager()?.beginTransaction()?.remove(this)?.commit()
        }

        btn_back_game_setting.setOnClickListener {
            listener?.OnGoBackOnGameSettingFragment()
            getFragmentManager()?.beginTransaction()?.remove(this)?.commit()
        }
    }
    companion object {

        private const val INTENT_KEY_REASON = "INTENT_KEY_REASON"
        private const val INTENT_KEY_TURN_COUNT = "INTENT_KEY_TURN_COUNT"
        private const val INTENT_KEY_TEAM_GOT_GRAY = "INTENT_KEY_TEAM_GOT_GRAY"

        @JvmStatic
        fun newInstance(reason: String, turnCount: Int, teamGotGray: Team?) =
            ResultFragment().apply {
                arguments = Bundle().apply {
                    putString(INTENT_KEY_REASON, reason)
                    putInt(INTENT_KEY_TURN_COUNT, turnCount)
                    putSerializable(INTENT_KEY_TEAM_GOT_GRAY, teamGotGray)
                }
            }
    }
}