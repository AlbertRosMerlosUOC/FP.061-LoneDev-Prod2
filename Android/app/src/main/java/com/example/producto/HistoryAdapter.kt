package com.example.producto2

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.producto2.databinding.ItemHistoryBinding
import com.example.producto2.model.GameResult

class HistoryAdapter(private val partidas: List<GameResult>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val partida = partidas[position]

        holder.binding.fechaTextView.text = partida.date

        val lootText = if (partida.loot >= 0) "+${partida.loot}" else "${partida.loot}"
        holder.binding.lootTextView.text = lootText

        val resultImageMap = mapOf(
            "s0" to R.drawable.ic_reels_0,
            "s2" to R.drawable.ic_reels_2,
            "s3" to R.drawable.ic_reels_3,
            "s4" to R.drawable.ic_reels_4,
            "s5" to R.drawable.ic_reels_5,
            "s6" to R.drawable.ic_reels_6
        )

        when {
            partida.loot > 0 -> holder.binding.lootTextView.setTextColor(Color.parseColor("#39FF14"))
            partida.loot < 0 -> holder.binding.lootTextView.setTextColor(Color.parseColor("#FF5252"))
        }

        Glide.with(holder.itemView.context)
            .load(resultImageMap[partida.result1])
            .into(holder.binding.result1ImageView)

        Glide.with(holder.itemView.context)
            .load(resultImageMap[partida.result2])
            .into(holder.binding.result2ImageView)

        Glide.with(holder.itemView.context)
            .load(resultImageMap[partida.result3])
            .into(holder.binding.result3ImageView)
    }

    override fun getItemCount(): Int = partidas.size
}
