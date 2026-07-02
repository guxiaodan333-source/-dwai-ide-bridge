package com.dwai.idebridge.handler

import com.dwai.idebridge.util.JsonUtil
import com.sun.net.httpserver.HttpExchange

object HealthHandler {
    fun handle(exchange: HttpExchange) {
        JsonUtil.respondJson(exchange, 200, mapOf(
            "status" to "ok",
            "service" to "dwai-ide-bridge",
            "version" to "1.0.0"
        ))
    }
}