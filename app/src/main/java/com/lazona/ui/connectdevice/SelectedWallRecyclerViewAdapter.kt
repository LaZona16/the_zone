package com.lazona.ui.connectdevice

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lazona.core.BaseViewHolder
import com.lazona.databinding.BluetoothDeviceSelectedItemBinding
import com.lazona.databinding.BluetoothScanItemBinding

class ConnectWallAdapter(
    private val wallsList: MutableList<BluetoothDevice>
):
    RecyclerView.Adapter<BaseViewHolder<*>>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        val itemBinding = BluetoothDeviceSelectedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectedWallViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        when (holder) {
            is SelectedWallViewHolder -> holder.bindWithListNumber(wallsList[position], position)
        }
    }

    override fun getItemCount(): Int = wallsList.size

    private inner class SelectedWallViewHolder(
        val binding: BluetoothDeviceSelectedItemBinding
    ): BaseViewHolder<BluetoothDevice>(binding.root) {

        fun bindWithListNumber(item: BluetoothDevice, position: Int) {
            binding.wallNumberTextView.text = "${position + 1}"
            bind(item)
        }
        @SuppressLint("MissingPermission")
        override fun bind(item: BluetoothDevice) {
            binding.wallNameTextView.text = item.name
            binding.wallSerialNumberTextView.text = item.address.toString()
        }

    }
}