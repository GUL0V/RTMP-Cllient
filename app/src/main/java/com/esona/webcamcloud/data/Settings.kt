package com.esona.webcamcloud.data

data class Settings(var resolution: String="720p", var rate: Int= 15, var login: String= "admin",
    var password: String= "password", val port: Int= 1935, var lang: Int= 0, var h264: Boolean)
