package com.pedro.rtspserver

import android.util.Base64
import android.util.Log
import com.pedro.rtsp.rtsp.Body
import com.pedro.rtsp.rtsp.CommandsManager
import com.pedro.rtsp.rtsp.Protocol
import java.io.BufferedReader
import java.io.IOException
import java.net.SocketException
import java.nio.charset.Charset
import java.util.regex.Pattern
import kotlin.collections.ArrayList

/**
 *
 * Created by pedro on 23/10/19.
 *
 */
class ServerCommandManager(
    private val serverIp: String, private val serverPort: Int,
    val clientIp: String?
) : CommandsManager() {

    private val TAG = "ServerCommandManager"
    var audioPorts = ArrayList<Int>()
    var videoPorts = ArrayList<Int>()
    private var track: Int? = null
    private var authenticated = false

    fun createResponse(action: String, request: String, cSeq: Int): String {
        return when {
            action.contains("options", true) -> {
                createOptions(cSeq)
            }
            action.contains("describe", true) -> {
                if (!request.contains("Authorization"))
                    createAuthErrorResponse(cSeq)
                else {
                    val regex = Regex("Authorization:.*")
                    regex.find(request)?.let {
                        val str = it.groups[0]?.value
                        str?.let { it1 ->
                            val creds = it1.substringAfter("Basic ")
                            Log.i(TAG, "credentials encoded: $creds")
                            val strCred = String(
                                Base64.decode(creds, Base64.DEFAULT),
                                Charset.forName("UTF-8")
                            )
                            Log.i(TAG, "credentials decoded: $strCred")
                            val login = strCred.substringBefore(":")
                            val pass = strCred.substringAfter(":")
                            if (user == login && password == pass)
                                authenticated = true
                        }
                    }
                    if (authenticated)
                        createDescribe(cSeq)
                    else
                        createAuthErrorResponse(cSeq)
                }

            }
            action.contains("setup", true) -> {
                if (authenticated) {
                    protocol = getProtocol(request)
                    return when (protocol) {
                        Protocol.TCP -> {
                            getTrack(request)
                            if (track != null) createSetup(cSeq) else createError(500, cSeq)
                        }
                        Protocol.UDP -> {
                            if (loadPorts(request)) createSetup(cSeq) else createError(500, cSeq)
                        }
                        else -> {
                            createError(500, cSeq)
                        }
                    }
                } else
                    createAuthErrorResponse(cSeq)
            }
            action.contains("play", true) -> {
                if (authenticated)
                    createPlay(cSeq)
                else
                    createError(401, cSeq)
            }
            action.contains("pause", true) -> createPause(cSeq)
            action.contains("teardown", true) -> createTeardown(cSeq)
            else -> createError(400, cSeq)
        }
    }

    private fun getProtocol(request: String): Protocol? {
        return if (request.contains("UDP", true) || loadPorts(request)) {
            Protocol.UDP
        } else {
            Protocol.TCP
        }
    }

    private fun loadPorts(request: String): Boolean {
        val ports = ArrayList<Int>()
        val portsMatcher =
            Pattern.compile("client_port=(\\d+)(?:-(\\d+))?", Pattern.CASE_INSENSITIVE)
                .matcher(request)
        if (portsMatcher.find()) {
            portsMatcher.group(1)?.toInt()?.let { ports.add(it) }
            portsMatcher.group(2)?.toInt()?.let { ports.add(it) }
        } else {
            Log.e(TAG, "UDP ports not found")
            return false
        }
        getTrack(request)
        if (track != null) {
            if (track == 0) { //audio ports
                audioPorts.clear()
                audioPorts.add(ports[0])
                audioPorts.add(ports[1])
            } else { //video ports
                videoPorts.clear()
                videoPorts.add(ports[0])
                videoPorts.add(ports[1])
            }
        } else {
            Log.e(TAG, "Track id not found")
            return false
        }
        Log.i(TAG, "Video ports: $videoPorts")
        Log.i(TAG, "Audio ports: $audioPorts")
        return true
    }

    private fun getTrack(request: String) {
        val trackMatcher =
            Pattern.compile("trackID=(\\w+)", Pattern.CASE_INSENSITIVE).matcher(request)
        return if (trackMatcher.find()) {
            track = trackMatcher.group(1)?.toInt()
        } else {
            track = null
        }
    }

    fun getCSeq(request: String): Int {
        val cSeqMatcher =
            Pattern.compile("CSeq\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(request)
        return if (cSeqMatcher.find()) {
            cSeqMatcher.group(1)?.toInt() ?: -1
        } else {
            Log.e(TAG, "cSeq not found")
            return -1
        }
    }

    @Throws(IOException::class, IllegalStateException::class, SocketException::class)
    fun getRequest(
        input: BufferedReader
    ): String {
        var request = ""
        var line: String? = input.readLine()
        while (line != null && line.length > 3) {
            request += "$line\n"
            line = input.readLine()
        }
        return request
    }

    private fun createStatus(code: Int): String {
        return when (code) {
            200 -> "200 OK"
            400 -> "400 Bad Request"
            401 -> "401 Unauthorized"
            403 -> "403 Forbidden"
            404 -> "404 Not Found"
            500 -> "500 Internal Server Error"
            else -> "500 Internal Server Error"
        }
    }

    fun createError(code: Int, cSeq: Int): String {
        return "RTSP/1.0 ${createStatus(
            code
        )}\r\n" + "Server: pedroSG94 Server\r\n" + "Cseq: $cSeq\r\n\r\n"
    }

    private fun createHeader(cSeq: Int): String {
        return "RTSP/1.0 ${createStatus(200)}\r\n" + "Server: pedroSG94 Server\r\n" + "Cseq: $cSeq\r\n"
    }

    private fun createOptions(cSeq: Int): String {
        return createHeader(cSeq) + "Public: DESCRIBE,SETUP,TEARDOWN,PLAY,PAUSE\r\n\r\n"
    }

    private fun createDescribe(cSeq: Int): String {
        val body = createBody()
        return createHeader(
            cSeq
        ) + "Content-Length: ${body.length}\r\n" + "Content-Base: $serverIp:$serverPort/\r\n" + "Content-Type: application/sdp\r\n\r\n" + body
    }

    private fun createBody(): String {
        val audioBody = Body.createAacBody(trackAudio, sampleRate, isStereo)
        val videoBody = if (vps == null) Body.createH264Body(
            trackVideo,
            encodeToString(sps),
            encodeToString(pps)
        ) else Body.createH265Body(
            trackVideo,
            encodeToString(sps),
            encodeToString(pps),
            encodeToString(vps)
        )
        return "v=0\r\n" + "o=- 0 0 IN IP4 $serverIp\r\n" + "s=Unnamed\r\n" + "i=N/A\r\n" + "c=IN IP4 $clientIp\r\n" + "t=0 0\r\n" + "a=recvonly\r\n" + audioBody + videoBody + "\r\n"
    }

    override fun createSetup(cSeq: Int): String {
        val protocolSetup = if (protocol == Protocol.UDP) {
            "UDP;unicast;destination=$clientIp;client_port=8000-8001;server_port=39000-35968"
        } else {
            "TCP;unicast;interleaved=" + (2 * track!!) + "-" + (2 * track!! + 1)
        }
        return createHeader(
            cSeq
        ) + "Content-Length: 0\r\n" +
                "Transport: RTP/AVP/$protocolSetup;mode=play\r\n" + "Session: 1185d20035702ca\r\n" + "Cache-Control: no-cache\r\n\r\n"
    }

    private fun createPlay(cSeq: Int): String {
        return createHeader(
            cSeq
        ) + "Content-Length: 0\r\n" + "RTP-Info: url=rtsp://$serverIp:$serverPort/\r\n" + "Session: 1185d20035702ca\r\n\r\n"
    }

    private fun createPause(cSeq: Int): String {
        return createHeader(cSeq) + "Content-Length: 0\r\n\r\n"
    }

    private fun createAuthErrorResponse(cSeq: Int): String {
        val res =
            "RTSP/1.0 ${createStatus(401)}\r\n" + "WWW-Authenticate: Basic realm=\"Samsung\"\r\n" + "Server: pedroSG94 Server\r\n" + "Cseq: $cSeq\r\n"
        return res + "\r\n"
    }

    private fun createTeardown(cSeq: Int): String {
        return createHeader(cSeq) + "\r\n"
    }

    private fun encodeToString(bytes: ByteArray): String? {
        return Base64.encodeToString(bytes, 0, bytes.size, Base64.NO_WRAP)
    }
}

/*
        if(!request.contains("Authorization"))
          return createAuthErrorResponse(cSeq)
        else{

        }
/*
        if(user!= null && password!= null){
          val regex= Regex("\\S*rtsp:\\S*")
          regex.find(request)?.let{
            val str= it.groups[0]?.value
            str?.let{it1->
              val parts= it1.split("/")
              if(parts.size> 2){
                val pass= parts[parts.size- 1]
                val uss= parts[parts.size- 2]
                if(pass!= password || uss!= user)
                  reject= true
              }
              else
                reject= true
            }
          }
        }
        Log.i(TAG, "----------- reject= $reject -------------")
*/
        if(authenticated)

 */