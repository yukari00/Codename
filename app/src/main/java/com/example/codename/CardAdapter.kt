package com.example.codename


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.card_words.view.*

class CardAdapter(private val wordList: List<WordsData>, private val selectedCardList: List<WordsData>, private val listener: OnCardAdapterListener): RecyclerView.Adapter<CardAdapter.ViewHolder>() {

    interface OnCardAdapterListener{
        fun OnClickCard(word: String, wordsData: WordsData, holder: ViewHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
            .inflate(R.layout.card_words, parent, false))
    }

    override fun getItemCount(): Int { return wordList.size }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.word.text = wordList[position].word

        if(selectedCardList.isNotEmpty()){
            for (i in selectedCardList.indices){
                if(wordList[position] == selectedCardList[i]){
                    when(wordList[position].color){
                        "RED" -> {
                            holder.itemView.card_view.setBackgroundResource(R.color.RED)
                            holder.color.setBackgroundResource(R.color.RED)
                        }
                        "BLUE" -> {
                            holder.itemView.card_view.setBackgroundResource(R.color.BLUE)
                            holder.color.setBackgroundResource(R.color.BLUE)
                        }
                        "GRAY" -> {
                            holder.itemView.card_view.setBackgroundResource(R.color.GRAY)
                            holder.color.setBackgroundResource(R.color.GRAY)
                        }
                        else -> {
                            holder.itemView.card_view.setBackgroundResource(R.color.LIGHT_GRAY)
                            holder.color.setBackgroundResource(R.color.LIGHT_GRAY)
                        }
                    }
                }
            }
        }


        if(isHost){
            if(wordList[position].color == "RED"){
                holder.color.setBackgroundResource(R.color.RED)
            } else if (wordList[position].color == "BLUE"){
                holder.color.setBackgroundResource(R.color.BLUE)
            } else if (wordList[position].color == "GRAY"){
                holder.color.setBackgroundResource(R.color.GRAY)
            }
        }


        holder.itemView.setOnClickListener {

            val selectedCard= wordList[position]

            val word = holder.word.text as String

            if(!isHost && !isWaiter){
                listener.OnClickCard(word, selectedCard, holder)
            }

        }


    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val word = itemView.word
        val color = itemView.color

    }

}


