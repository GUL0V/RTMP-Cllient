package com.esona.webcamcloud.data

import android.os.Bundle

data class BaseEvent(val type: EventEnum){
    lateinit var bundle: Bundle
    constructor(type: EventEnum, bundle: Bundle) : this(type){
        this.bundle= bundle
    }
}