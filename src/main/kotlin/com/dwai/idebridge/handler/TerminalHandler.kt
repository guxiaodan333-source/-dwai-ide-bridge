package com.dwai.idebridge.handler

import com.dwai.idebridge.api.ApiRegistry
import com.dwai.idebridge.util.JsonUtil
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange

object TerminalHandler {

    fun handleTerminal(project: Project, exchange: HttpExchange) {
        val result = ApiRegistry.terminal.getContent(project)
        JsonUtil.respondJson(exchange, 200, result)
    }

    fun handleTerminalExec(project: Project, exchange: HttpExchange) {
        val body = String(exchange.requestBody.readAllBytes())
        val json = JsonUtil.gson.fromJson(body, Map::class.java) as Map<String, Any>
        val command = json["command"] as? String ?: run {
            JsonUtil.respondJson(exchange, 400, mapOf("error" to "Missing command"))
            return
        }
        val result = ApiRegistry.terminal.exec(project, command)
        JsonUtil.respondJson(exchange, 200, result)
    }
}