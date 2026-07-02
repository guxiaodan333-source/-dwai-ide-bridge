package com.dwai.idebridge.handler

import com.dwai.idebridge.api.ApiRegistry
import com.dwai.idebridge.util.JsonUtil
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange

object RunHandler {

    fun handleRunOutput(project: Project, exchange: HttpExchange) {
        val result = ApiRegistry.run.getOutput(project)
        JsonUtil.respondJson(exchange, 200, result)
    }

    fun handleRun(project: Project, exchange: HttpExchange) {
        try {
            val body = String(exchange.requestBody.readAllBytes())
            val json = JsonUtil.gson.fromJson(body, Map::class.java) as Map<String, Any>
            val filePath = json["file_path"] as? String ?: run {
                JsonUtil.respondJson(exchange, 400, mapOf("error" to "Missing file_path"))
                return
            }
            val stream = json["stream"] as? Boolean ?: false

            if (!stream) {
                ApiRegistry.run.startRun(project, filePath)
                JsonUtil.respondJson(exchange, 200, mapOf("status" to "ok", "file" to filePath))
                return
            }

            ApiRegistry.run.streamRun(project, filePath, exchange)
        } catch (e: Exception) {
            JsonUtil.respondJson(exchange, 500, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }
}