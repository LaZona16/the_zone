package com.lazona.domain

import com.lazona.data.local.WallDatasource

class ConnectWallRespositoryImpl: ConnectWallRepository {
    private val listOfWallsConnected: MutableList<WallDatasource> = mutableListOf()

    override fun connectWalls() {

    }
}

