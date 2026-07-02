package com.dwai.idebridge.mcp

import com.dwai.idebridge.api.ApiRegistry
import com.dwai.idebridge.model.DiffApplyRequest
import com.dwai.idebridge.util.JsonUtil
import com.intellij.openapi.project.Project

object McpToolRegistry {

    fun listTools(): List<McpTool> = listOf(
        // 以下与 JetBrains 官方 MCP Server 重叠，已注释，请使用官方版本：
        // McpTool("editor.get_cursor", ...)
        // McpTool("editor.get_code", ...)
        // McpTool("editor.get_selection", ...)
        // McpTool("editor.create_file", ...)
        // McpTool("project.get_open_files", ...)
        // McpTool("project.get_file_structure", ...)
        // McpTool("navigate.open_file", ...)
        // McpTool("navigate.find_symbol", ...)

        // ═══════════════════════════════════════════════════════════
        // 组合工具（一次调用获取完整上下文，推荐优先使用）
        // ═══════════════════════════════════════════════════════════

        McpTool("editor.get_context",
            "获取用户正在编辑的代码：项目信息、光标位置（文件、行号、列号）、选中代码、光标附近代码上下文。radius 参数控制上下文行数，默认 20 行。",
            mapOf("radius" to mapOf("type" to "integer", "default" to 20, "description" to "光标附近代码的行数"))
        ),
        McpTool("diagnostics.get_all",
            "获取用户正在编辑的代码错误信息，编辑器静态错误（当前文件，含 severity 分级）和 Run 控制台筛选后的错误/警告行" +
            "如果摘要信息不足以定位问题，请调用 diagnostics.get_full_output 获取完整输出。",
            emptyMap()
        ),
        McpTool("diagnostics.get_full_output",
            "当 diagnostics.get_all 返回的摘要信息不足以定位报错问题时，调用此工具获取 Run/Debug 控制台的完整输出文本。" +
            "不要直接调用此工具——先调用 diagnostics.get_all 获取摘要，确认需要更多信息后再调用此工具。",
            emptyMap()
        ),

        // ═══════════════════════════════════════════════════════════
        // DWAI 独有工具（官方 MCP Server 不具备的能力）
        // ═══════════════════════════════════════════════════════════

        McpTool("editor.insert",
            "在用户正在编辑的代码光标处插入代码。",
            mapOf("text" to mapOf("type" to "string", "description" to "要插入的代码文本"))
        ),
        McpTool("run.start",
            "在 IDE 的 Run 窗口执行脚本，让用户看到运行过程和报错。建议所有关键执行都通过此工具运行。非阻塞模式（立即返回）。",
            mapOf("file_path" to mapOf("type" to "string", "description" to "要运行的脚本文件路径"))
        ),
        McpTool("diff.apply",
            "推送代码修改到 IDE 供开发者审批确认。项目文件修改必须推送 diff，不影响 agent 运行。测试文件、调试脚本等非核心源码可跳过审批。",
            mapOf("files" to mapOf("type" to "array",
                "description" to "文件列表: [{path: 文件路径, modified: 完整新内容, description: 修改说明, skip_review: 是否跳过审批}]"))
        ),
        McpTool("diff.get_pending",
            "查询所有已推送但未被用户审批的 diff 状态。用户审批后调用此工具查看结果。",
            emptyMap()
        ),
        McpTool("file.save_close",
            "保存所有打开的文件并关闭所有编辑器标签页。当用户说「保存并关闭所有文件」「保存并关闭所有编辑器」时调用。" +
            "可选传入 paths 参数指定要关闭的文件路径列表，未传入时默认关闭所有。",
            mapOf("paths" to mapOf("type" to "array",
                "description" to "可选，要关闭的文件路径列表，不传则关闭所有"))
        ),
        McpTool("file.list_open",
            "查询当前 IDE 中打开的文件列表，返回文件路径。当用户说「打开了哪些文件」「当前打开了什么文件」时调用。",
            emptyMap()
        ),
        McpTool("file.open",
            "在 IDE 编辑器中打开指定文件。当用户说「打开某个文件」时调用。",
            mapOf(
                "file_path" to mapOf("type" to "string", "description" to "要打开的文件路径"),
                "line" to mapOf("type" to "integer", "default" to 0, "description" to "跳转到指定行号，0 表示不跳转")
            )
        )
    )

    fun callTool(project: Project, name: String, args: Map<String, Any?>): McpToolCallResult {
        val gson = JsonUtil.gson
        val result = gson.toJson(
            when (name) {
                // 组合工具：
                "editor.get_context" -> {
                    val radius = (args["radius"] as? Number)?.toInt() ?: 20
                    mapOf(
                        "cursor" to ApiRegistry.editor.getCursor(project),
                        "selection" to ApiRegistry.editor.getSelection(project),
                        "code" to ApiRegistry.editor.getCode(project, radius),
                        "project" to ApiRegistry.project.getInfo(project)
                    )
                }
                "diagnostics.get_all" -> mapOf(
                    "project" to ApiRegistry.project.getInfo(project),
                    "static_errors" to ApiRegistry.problem.getProblems(project),
                    "run_output" to ApiRegistry.run.getOutputSummary(project)
                )
                "diagnostics.get_full_output" -> ApiRegistry.run.getOutput(project)

                // DWAI 独有工具：
                "editor.insert" -> ApiRegistry.editor.insert(project, args["text"] as? String ?: "")
                "run.start" -> {
                    ApiRegistry.run.startRun(project, args["file_path"] as? String ?: "")
                    mapOf("status" to "ok", "message" to "Run started, check PyCharm Run window")
                }
                "diff.apply" -> {
                    val request = gson.fromJson(gson.toJson(args), DiffApplyRequest::class.java)
                    val results = ApiRegistry.diff.applyDiffs(project, request)
                    mapOf("status" to "ok", "results" to results)
                }
                "diff.get_pending" -> {
                    val results = ApiRegistry.diff.getPendingResults()
                    mapOf("status" to "ok", "results" to results)
                }
                "file.save_close" -> {
                    val pathsArg = args["paths"] as? List<*>
                    ApiRegistry.file.saveClose(project, pathsArg?.mapNotNull { it?.toString() })
                }
                "file.list_open" -> ApiRegistry.file.listOpen(project)
                "file.open" -> {
                    val filePath = args["file_path"] as? String ?: ""
                    val line = (args["line"] as? Number)?.toInt() ?: 0
                    ApiRegistry.navigate.openFile(project, filePath, line)
                }
                else -> mapOf("error" to "Unknown tool: $name")
            }
        )
        return McpToolCallResult(content = listOf(McpContent(type = "text", text = result)))
    }
}