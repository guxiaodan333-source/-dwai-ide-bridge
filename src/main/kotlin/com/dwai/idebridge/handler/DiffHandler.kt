package com.dwai.idebridge.handler

import com.dwai.idebridge.api.ApiRegistry
import com.dwai.idebridge.model.DiffApplyRequest
import com.dwai.idebridge.util.JsonUtil
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange

object DiffHandler {

    fun handleApplyDiff(project: Project, exchange: HttpExchange) {
        try {
            val body = String(exchange.requestBody.readAllBytes())
            val request = JsonUtil.gson.fromJson(body, DiffApplyRequest::class.java)
            val results = ApiRegistry.diff.applyDiffs(project, request)
            JsonUtil.respondJson(exchange, 200, mapOf("status" to "ok", "results" to results))
        } catch (e: Exception) {
            JsonUtil.respondJson(exchange, 500, mapOf("error" to (e.message ?: "Invalid request")))
        }
    }
}