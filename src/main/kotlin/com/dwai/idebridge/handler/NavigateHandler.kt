package com.dwai.idebridge.handler

import com.dwai.idebridge.api.ApiRegistry
import com.dwai.idebridge.util.JsonUtil
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange

object NavigateHandler {

    fun handleNavigateOpen(project: Project, exchange: HttpExchange) {
        val params = JsonUtil.parseQueryParams(exchange.requestURI)
        val filePath = params["file"] ?: run {
            JsonUtil.respondJson(exchange, 400, mapOf("error" to "Missing file parameter"))
            return
        }
        val line = params["line"]?.toIntOrNull() ?: 0
        val result = ApiRegistry.navigate.openFile(project, filePath, line)
        JsonUtil.respondJson(exchange, 200, result)
    }

    fun handleSymbol(project: Project, exchange: HttpExchange) {
        val params = JsonUtil.parseQueryParams(exchange.requestURI)
        val query = params["q"] ?: run {
            JsonUtil.respondJson(exchange, 400, mapOf("error" to "Missing q parameter"))
            return
        }
        val result = ApiRegistry.navigate.findSymbol(project, query)
        JsonUtil.respondJson(exchange, 200, result)
    }
}