package com.example.videocallingapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class RecyclerViewAdapter(private val list: ArrayList<User>?, val context: Context) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val txt : TextView = itemView.findViewById(R.id.history_name_textview)
        val btn : ImageView = itemView.findViewById(R.id.history_call_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_calls, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list!![position]
        holder.txt.text = item.name
        holder.btn.setOnClickListener { v ->
            (v.context as CallActivity).onClickCalled(item.name.toString())
        }
    }

    override fun getItemCount(): Int {
        return list?.size!!
    }

}
