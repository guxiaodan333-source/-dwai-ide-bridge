package com.dwai.idebridge.handler

import com.dwai.idebridge.api.ApiRegistry
import com.dwai.idebridge.util.JsonUtil
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange

object EditorHandler {

    fun handleInsert(project: Project, exchange: HttpExchange) {
        val body = String(exchange.requestBody.readAllBytes())
        val json = JsonUtil.gson.fromJson(body, Map::class.java) as Map<String, Any>
        val text = json["text"] as? String ?: run {
            JsonUtil.respondJson(exchange, 400, mapOf("error" to "Missing text"))
            return
        }
        val result = ApiRegistry.editor.insert(project, text)
        JsonUtil.respondJson(exchange, 200, result)
    }

    fun handleCreateFile(project: Project, exchange: HttpExchange) {
        val body = String(exchange.requestBody.readAllBytes())
        val json = JsonUtil.gson.fromJson(body, Map::class.java) as Map<String, Any>
        val filePath = json["file_path"] as? String ?: run {
            JsonUtil.respondJson(exchange, 400, mapOf("error" to "Missing file_path"))
            return
        }
        val content = json["content"] as? String ?: ""
        val result = ApiRegistry.editor.createFile(project, filePath, content)
        JsonUtil.respondJson(exchange, 200, result)
    }
}