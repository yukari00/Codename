package com.example.codename

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_game_host.*

class GameHostFragment : Fragment() {

    var listener: OnFragmentListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        var numOfCardToPick = 1
        text_num_pick.setText("$numOfCardToPick")

        btn_increase.setOnClickListener {
            if(numOfCardToPick == 10) return@setOnClickListener
            numOfCardToPick ++
            text_num_pick.setText("$numOfCardToPick")
        }

        btn_decrease.setOnClickListener {
            if(numOfCardToPick == 1) return@setOnClickListener
            numOfCardToPick--
            text_num_pick.setText("$numOfCardToPick")
        }

        btn_confirm.setOnClickListener {
            val hint: String = input_edit_text_hint.text.toString()
            if(checkIfFilled(hint)){
                listener?.OnHost(hint, numOfCardToPick)
                getFragmentManager()?.beginTransaction()?.remove(this)?.commit();
            }

        }

    }

    private fun checkIfFilled(hint: String): Boolean {

        if(hint == "") return false

        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game_host, container, false)
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            GameHostFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}