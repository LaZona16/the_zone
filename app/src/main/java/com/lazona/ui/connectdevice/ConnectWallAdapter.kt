package com.lazona.ui.connectdevice

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lazona.core.BaseViewHolder
import com.lazona.databinding.BluetoothScanItemBinding

class ConnectWallAdapter(
    private val wallsList: MutableList<BluetoothDevice>,
    private val bluetoothClickListener: OnBluetoothOnClickListener
) :
    RecyclerView.Adapter<BaseViewHolder<*>>() {
    private var onWallClickListener: OnBluetoothOnClickListener? = null

    init {
        onWallClickListener = bluetoothClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        val itemBinding =
            BluetoothScanItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConnectWallViewHolder(itemBinding, parent.context)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        when (holder) {
            is ConnectWallViewHolder -> holder.bind(wallsList[position])
        }
    }

    override fun getItemCount(): Int = wallsList.size

    private inner class ConnectWallViewHolder(
        val binding: BluetoothScanItemBinding,
        val context: Context
    ) : BaseViewHolder<BluetoothDevice>(binding.root) {
        @SuppressLint("MissingPermission")

        override fun bind(item: BluetoothDevice) {
            binding.tvDeviceName.text = "${item.name}/${item.address}"
            bluetoothDeviceOnClickAction(item)
        }

        fun bluetoothDeviceOnClickAction(item: BluetoothDevice) {
            binding.tvDeviceName.setOnClickListener {
                onWallClickListener?.onClickListener(item)
                if (binding.imConnectedState.visibility == View.INVISIBLE) {
                    binding.imConnectedState.visibility = View.VISIBLE
                } else {
                    binding.imConnectedState.visibility = View.INVISIBLE
                }

            }
        }
    }
}

interface OnBluetoothOnClickListener {
    fun onClickListener(bluetoothDevice: BluetoothDevice)
}