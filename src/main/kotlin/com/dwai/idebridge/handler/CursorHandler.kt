package com.dwai.idebridge.handler

import com.dwai.idebridge.api.ApiRegistry
import com.dwai.idebridge.util.JsonUtil
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange

object CursorHandler {

    fun handleCursor(project: Project, exchange: HttpExchange) {
        try {
            val result = ApiRegistry.editor.getCursor(project)
            JsonUtil.respondJson(exchange, 200, result)
        } catch (e: Exception) {
            JsonUtil.respondJson(exchange, 500, mapOf("status" to "error", "reason" to (e.message ?: "Unknown")))
        }
    }

    fun handleCode(project: Project, exchange: HttpExchange) {
        try {
            val params = JsonUtil.parseQueryParams(exchange.requestURI)
            val radius = params["radius"]?.toIntOrNull() ?: 10
            val result = ApiRegistry.editor.getCode(project, radius)
            JsonUtil.respondJson(exchange, 200, result)
        } catch (e: Exception) {
            JsonUtil.respondJson(exchange, 500, mapOf("status" to "error", "reason" to (e.message ?: "Unknown")))
        }
    }

    fun handleSelection(project: Project, exchange: HttpExchange) {
        try {
            val result = ApiRegistry.editor.getSelection(project)
            JsonUtil.respondJson(exchange, 200, result)
        } catch (e: Exception) {
            JsonUtil.respondJson(exchange, 500, mapOf("status" to "error", "reason" to (e.message ?: "Unknown")))
        }
    }
}