package com.lazona.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lazona.domain.ConnectWallRepository

class ConnectWallViewModel(private val repository: ConnectWallRepository) : ViewModel() {
    
}

class ConnectWallViewModelFactory(private val repo: ConnectWallRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(ConnectWallRepository::class.java).newInstance(repo)
    }
}
