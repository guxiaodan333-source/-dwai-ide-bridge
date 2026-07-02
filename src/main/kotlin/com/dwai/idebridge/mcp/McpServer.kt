package com.dwai.idebridge.mcp

import com.dwai.idebridge.util.JsonUtil
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange

object McpServer {

    private val gson = JsonUtil.gson

    fun handle(project: Project, exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "POST") {
                JsonUtil.respondJson(exchange, 405, mapOf("error" to "Method not allowed"))
                return
            }

            val body = String(exchange.requestBody.readAllBytes())
            val request = gson.fromJson(body, JsonRpcRequest::class.java)

            when (request.method) {
                "tools/list" -> handleToolsList(exchange, request.id)
                "tools/call" -> handleToolsCall(project, exchange, request)
                "initialize" -> handleInitialize(exchange, request.id)
                else -> respondError(exchange, request.id, -32601, "Method not found: ${request.method}")
            }
        } catch (e: JsonSyntaxException) {
            respondError(exchange, null, -32700, "Parse error: ${e.message}")
        } catch (e: Exception) {
            respondError(exchange, null, -32603, "Internal error: ${e.message}")
        }
    }

    private fun handleInitialize(exchange: HttpExchange, id: Any?) {
        val result = mapOf(
            "protocolVersion" to "2024-11-05",
            "serverInfo" to mapOf(
                "name" to "DWAI IDE Bridge",
                "version" to "1.0.0",
                "description" to "AI Agent 与 JetBrains IDE 的桥接工具：让 AI 助手获取光标代码上下文、诊断运行报错、执行脚本、推送 Diff 修改供开发者审批。帮助开发者快速定位问题、审查和确认代码修改。"
            ),
            "capabilities" to mapOf(
                "tools" to mapOf<String, Any>()
            )
        )
        respondJson(exchange, id, result)
    }

    private fun handleToolsList(exchange: HttpExchange, id: Any?) {
        val result = McpToolsListResult(tools = McpToolRegistry.listTools())
        respondJson(exchange, id, result)
    }

    private fun handleToolsCall(project: Project, exchange: HttpExchange, request: JsonRpcRequest) {
        val params = request.params ?: run {
            respondError(exchange, request.id, -32602, "Invalid params")
            return
        }
        val toolName = params["name"] as? String ?: run {
            respondError(exchange, request.id, -32602, "Missing tool name")
            return
        }
        @Suppress("UNCHECKED_CAST")
        val toolArgs = (params["arguments"] as? Map<String, Any?>) ?: emptyMap()

        val result = McpToolRegistry.callTool(project, toolName, toolArgs)
        respondJson(exchange, request.id, result)
    }

    private fun respondJson(exchange: HttpExchange, id: Any?, result: Any) {
        val response = JsonRpcResponse(id = id, result = result)
        val json = gson.toJson(response)
        val bytes = json.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.write(bytes)
        exchange.responseBody.close()
    }

    private fun respondError(exchange: HttpExchange, id: Any?, code: Int, message: String) {
        val response = JsonRpcResponse(id = id, error = JsonRpcError(code, message))
        val json = gson.toJson(response)
        val bytes = json.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.write(bytes)
        exchange.responseBody.close()
    }
}