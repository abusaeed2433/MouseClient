package com.unknownn.mouseclient.main_activity.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unknownn.mouseclient.databinding.EachIpLayoutBinding

class IpAdapter(val listener: ClickListener):ListAdapter<String, IpAdapter.ViewModel>(diffUtilCallback) {

    companion object {
        val diffUtilCallback = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewModel {
        val binding = EachIpLayoutBinding.inflate(LayoutInflater.from(parent.context),parent, false)
        return ViewModel(binding)
    }

    override fun onBindViewHolder(holder: ViewModel, position: Int) {
        holder.bind( getItem(position) )
    }

    inner class ViewModel(val binding: EachIpLayoutBinding): RecyclerView.ViewHolder(binding.root){

        init {
            binding.root.setOnClickListener{
                listener.onIpClicked(binding.tvIp.text.toString())
            }
        }
        fun bind(ip:String){
            binding.tvIp.text = ip
        }
    }

    interface ClickListener{
        fun onIpClicked(ip: String)
    }

}
