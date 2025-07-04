package com.example.androidtv

object ChannelList {
    var channels: List<Channel> = listOf()
    fun update(newChannels: List<Channel>) {
        channels = newChannels
    }
} 