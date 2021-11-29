package com.esona.webcamcloud.data

import android.hardware.Camera
import android.os.Parcel
import android.os.Parcelable

data class Settings(var resolutionW: Int= 0,
                    var resolutionH: Int= 0,
                    var rate: Int= 15,
                    var auto: Int= 60,
                    var login: String= "admin",
                    var password: String= "password",
                    var port: Int= 1935,
                    var lang: Int= 0,
                    var camera: Int= 0,
                    var resolutions: List<Camera.Size>?= null)
    : Parcelable {

    constructor(parcel: Parcel) : this() {
        resolutionW= parcel.readInt()
        resolutionH= parcel.readInt()
        login= parcel.readString()!!
        password= parcel.readString()!!
        rate= parcel.readInt()
        auto= parcel.readInt()
        port= parcel.readInt()
        lang= parcel.readInt()
        camera= parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(resolutionW)
        parcel.writeInt(resolutionH)
        parcel.writeString(login)
        parcel.writeString(password)
        parcel.writeInt(rate)
        parcel.writeInt(auto)
        parcel.writeInt(port)
        parcel.writeInt(lang)
        parcel.writeInt(camera)
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