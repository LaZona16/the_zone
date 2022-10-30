package com.lazona.ui.connectdevice

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lazona.core.BaseViewHolder
import com.lazona.databinding.BluetoothDeviceViewBinding

class ConnectWallAdapter(private val wallsList: List<String>) :
    RecyclerView.Adapter<BaseViewHolder<*>>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        val itemBinding =
            BluetoothDeviceViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConnectWallViewHolder(itemBinding, parent.context)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        when (holder) {
            is ConnectWallViewHolder -> holder.bind(wallsList[position])
        }
    }

    override fun getItemCount(): Int = wallsList.size

    private inner class ConnectWallViewHolder(
        val binding: BluetoothDeviceViewBinding,
        val context: Context
    ) : BaseViewHolder<String>(binding.root) {
        override fun bind(deviceName: String) {
            binding.textViewWallName.text = deviceName
        }

    }
}