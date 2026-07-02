package com.dwai.idebridge.handler

import com.dwai.idebridge.api.ApiRegistry
import com.dwai.idebridge.util.JsonUtil
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange

object ProjectHandler {

    fun handleProject(project: Project, exchange: HttpExchange) {
        val result = ApiRegistry.project.getInfo(project)
        JsonUtil.respondJson(exchange, 200, result)
    }

    fun handleOpenFiles(project: Project, exchange: HttpExchange) {
        val result = ApiRegistry.project.getOpenFiles(project)
        JsonUtil.respondJson(exchange, 200, result)
    }

    fun handleFileStructure(project: Project, exchange: HttpExchange) {
        val params = JsonUtil.parseQueryParams(exchange.requestURI)
        val result = ApiRegistry.project.getFileStructure(project, params["file"])
        JsonUtil.respondJson(exchange, 200, result)
    }
}