package com.dwai.idebridge.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sun.net.httpserver.HttpExchange
import java.net.URI

object JsonUtil {

    val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun respondJson(exchange: HttpExchange, statusCode: Int, data: Any) {
        val json = gson.toJson(data)
        val bytes = json.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
        exchange.responseBody.write(bytes)
        exchange.responseBody.close()
    }

    fun parseQueryParams(uri: URI): Map<String, String> {
        val query = uri.query ?: return emptyMap()
        val result = LinkedHashMap<String, String>()
        for (param in query.split("&")) {
            val parts = param.split("=", limit = 2)
            result[parts[0]] = if (parts.size > 1) parts[1] else ""
        }
        return result
    }

    fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}