package com.example.androidtv

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.*
import java.io.IOException

object ChannelUpdateManager {
    fun updateChannelsFromM3U(url: String, onSuccess: (List<Channel>) -> Unit, onError: (String) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    onError("Ошибка загрузки: ${e.localizedMessage}")
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        onError("Ошибка сервера: ${response.code}")
                    }
                    return
                }
                val body = response.body?.string()
                if (body == null) {
                    Handler(Looper.getMainLooper()).post {
                        onError("Пустой ответ сервера")
                    }
                    return
                }
                try {
                    val lines = body.lines().filter { it.isNotBlank() }
                    val channels = mutableListOf<Channel>()
                    var i = 0
                    while (i < lines.size) {
                        if (lines[i].startsWith("#EXTINF")) {
                            val name = lines[i].substringAfter(",").trim()
                            val urlLine = if (i + 1 < lines.size) lines[i + 1].trim() else null
                            if (urlLine != null && !urlLine.startsWith("#")) {
                                channels.add(Channel(name, R.drawable.placeholder, urlLine))
                                i++
                            }
                        }
                        i++
                    }
                    Handler(Looper.getMainLooper()).post {
                        onSuccess(channels)
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post {
                        onError("Ошибка парсинга: ${e.localizedMessage}")
                    }
                }
            }
        })
    }
} 
 