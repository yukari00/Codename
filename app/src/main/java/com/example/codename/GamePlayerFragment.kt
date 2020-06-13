package com.example.codename

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_game_player.*


class GamePlayerFragment : Fragment() {

    private var hint: String? = ""
    private var numCardPick: Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            hint = it.getString(INTENT_KEY_HINT) ?: return
            numCardPick = it.getInt(INTENT_KEY_NUMBER_CARD) ?: return

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game_player, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        hint_word.setText(hint)
        number_of_cards_to_choose.setText("$numCardPick")

    }

    companion object {

        private const val INTENT_KEY_HINT = "INTENT_KEY_HINT"
        private const val INTENT_KEY_NUMBER_CARD = "INTENT_KEY_NUMBER_CARD"
        @JvmStatic
        fun newInstance(hint: String, numCardPick: Int) =
            GamePlayerFragment().apply {
                arguments = Bundle().apply {
                    putString(INTENT_KEY_HINT, hint)
                    putInt(INTENT_KEY_NUMBER_CARD, numCardPick)

                }
            }
    }
}