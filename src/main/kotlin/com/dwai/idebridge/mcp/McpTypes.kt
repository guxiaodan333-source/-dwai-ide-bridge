package com.dwai.idebridge.mcp

data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Any? = null,
    val method: String = "",
    val params: Map<String, Any?>? = null
)

data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Any? = null,
    val result: Any? = null,
    val error: JsonRpcError? = null
)

data class JsonRpcError(
    val code: Int,
    val message: String
)

data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any>
)

data class McpToolsListResult(
    val tools: List<McpTool>
)

data class McpToolCallResult(
    val content: List<McpContent>
)

data class McpContent(
    val type: String,
    val text: String
)