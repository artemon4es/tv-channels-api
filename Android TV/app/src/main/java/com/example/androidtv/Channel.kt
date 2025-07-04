package com.example.androidtv

import android.os.Parcel
import android.os.Parcelable

data class Channel(
    val name: String,
    val logoResId: Int,
    val streamUrl: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: ""
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(logoResId)
        parcel.writeString(streamUrl)
    }
    override fun describeContents(): Int = 0
    companion object CREATOR : Parcelable.Creator<Channel> {
        override fun createFromParcel(parcel: Parcel): Channel = Channel(parcel)
        override fun newArray(size: Int): Array<Channel?> = arrayOfNulls(size)
    }
} 