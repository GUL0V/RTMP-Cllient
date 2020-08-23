package com.esona.webcamcloud.data

import android.os.Parcel
import android.os.Parcelable

data class Settings(var resolution: String="720p", var rate: Int= 15, var login: String= "admin",
                    var password: String= "password", var port: Int= 1935, var lang: Int= 0, var h264: Boolean= false,
                    var camera: Int= 0)
    : Parcelable {

    constructor(parcel: Parcel) : this() {
        resolution= parcel.readString()!!
        login= parcel.readString()!!
        password= parcel.readString()!!
        rate= parcel.readInt()
        port= parcel.readInt()
        lang= parcel.readInt()
        camera= parcel.readInt()
        h264= parcel.readInt()==1
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(resolution)
        parcel.writeString(login)
        parcel.writeString(password)
        parcel.writeInt(rate)
        parcel.writeInt(port)
        parcel.writeInt(lang)
        parcel.writeInt(camera)
        parcel.writeInt(if(h264) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Settings> {
        override fun createFromParcel(parcel: Parcel): Settings {
            return Settings(parcel)
        }

        override fun newArray(size: Int): Array<Settings?> {
            return arrayOfNulls(size)
        }
    }
}