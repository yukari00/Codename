package com.example.codename

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.card_words.view.*

class CardAdapter(val wordList: List<WordsData>): RecyclerView.Adapter<CardAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.card_words, parent, false))
    }

    override fun getItemCount(): Int { return wordList.size }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.word.text = wordList[position].word
        //holder.color.setBackgroundResource(wordList[position].color!!)
        Log.d("CHHHHHHHHHHHHHhh", "${wordList[position].word}")
        Log.d("CHHHHHHHHHHHHHhh", "${wordList[position].color}")


    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val word = itemView.word
        val color = itemView.color

    }

}


