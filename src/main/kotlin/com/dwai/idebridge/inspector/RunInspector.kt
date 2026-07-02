package com.dwai.idebridge.inspector

import com.dwai.idebridge.executor.RunExecutor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

object RunInspector {

    fun getOutput(project: Project): Map<String, Any?> = getOutputInternal(project, full = true)

    fun getOutputSummary(project: Project): Map<String, Any?> = getOutputInternal(project, full = false)

    private fun getOutputInternal(project: Project, full: Boolean): Map<String, Any?> {
        try {
            val contentManager = RunContentManager.getInstance(project)
            val allDescriptors = contentManager.allDescriptors
            if (allDescriptors.isEmpty()) {
                return mapOf("status" to "ok", "has_running_process" to false, "messages" to emptyList<Map<String, Any>>())
            }

            val messages = mutableListOf<Map<String, Any>>()
            for (descriptor in allDescriptors) {
                val name = descriptor.displayName
                val processHandler = descriptor.processHandler
                val isRunning = processHandler != null && !processHandler.isProcessTerminated

                val content = getContent(name, descriptor)

                val lines = content.split("\n").filter { it.isNotBlank() }
                val errorLines = lines.filter {
                    it.contains("Error", ignoreCase = true) ||
                    it.contains("Traceback", ignoreCase = true) ||
                    it.contains("Exception", ignoreCase = true)
                }
                val warningLines = lines.filter { it.contains("Warning", ignoreCase = true) }

                val msg = mutableMapOf<String, Any>(
                    "name" to name,
                    "is_running" to isRunning,
                    "errors" to errorLines,
                    "warnings" to warningLines,
                    "total_lines" to lines.size
                )
                if (full) {
                    msg["output"] = content
                }
                messages.add(msg)
            }

            return mapOf(
                "status" to "ok",
                "has_running_process" to messages.any { it["is_running"] == true },
                "count" to messages.size,
                "messages" to messages
            )
        } catch (e: Exception) {
            return mapOf("status" to "error", "reason" to (e.message ?: "Unknown error"))
        }
    }

    private fun getContent(name: String, descriptor: com.intellij.execution.ui.RunContentDescriptor): String {
        // 1. 优先从 RunExecutor 缓存读取（进程结束后仍然可用）
        val cached = RunExecutor.getCachedOutput(name)
        if (cached != null && cached.isNotBlank()) {
            return cached
        }

        // 2. 从 ConsoleView 读文档（进程结束后仍然可用）
        val consoleText = tryReadConsoleView(descriptor)
        if (consoleText != null && consoleText.isNotBlank()) {
            return consoleText
        }

        // 3. 兜底：反射读 myOutput（仅进程运行时可用）
        val fallback = tryReadFallback(descriptor)
        if (fallback != "N/A" && fallback.isNotBlank()) {
            return fallback
        }

        return cached ?: "N/A"
    }

    private fun tryReadConsoleView(descriptor: com.intellij.execution.ui.RunContentDescriptor): String? {
        return try {
            val console = descriptor.executionConsole ?: return null
            val clazz = console.javaClass

            // 1. getText()
            try {
                val m = clazz.getDeclaredMethod("getText")
                m.isAccessible = true
                val text = m.invoke(console)?.toString()
                if (text != null && text.isNotBlank()) return text
            } catch (_: Exception) {}

            // 2. getEditor().document.text
            try {
                val m = clazz.getDeclaredMethod("getEditor")
                m.isAccessible = true
                val editor = m.invoke(console) as? Editor
                editor?.document?.text?.let { if (it.isNotBlank()) return it }
            } catch (_: Exception) {}

            // 3. getConsoleEditor().document.text
            try {
                val m = clazz.getDeclaredMethod("getConsoleEditor")
                m.isAccessible = true
                val editor = m.invoke(console) as? Editor
                editor?.document?.text?.let { if (it.isNotBlank()) return it }
            } catch (_: Exception) {}

            // 4. getComponent() -> find JTextComponent (TerminalExecutionConsole 走这条)
            try {
                val m = clazz.getDeclaredMethod("getComponent")
                m.isAccessible = true
                val comp = m.invoke(console)
                val text = findTextInComponent(comp)
                if (text != null && text.isNotBlank()) return text
            } catch (_: Exception) {}

            null
        } catch (_: Exception) {
            null
        }
    }

    private fun findTextInComponent(comp: Any?): String? {
        if (comp == null) return null
        try {
            val textMethod = comp.javaClass.getMethod("getText")
            return textMethod.invoke(comp)?.toString()
        } catch (_: Exception) {}
        try {
            val docMethod = comp.javaClass.getMethod("getDocument")
            val doc = docMethod.invoke(comp)
            val textMethod = doc?.javaClass?.getMethod("getText", Int::class.java, Int::class.java)
            val lengthMethod = doc?.javaClass?.getMethod("getLength")
            val length = lengthMethod?.invoke(doc) as? Int ?: 0
            return textMethod?.invoke(doc, 0, length)?.toString()
        } catch (_: Exception) {}
        return null
    }

    private fun tryReadFallback(descriptor: com.intellij.execution.ui.RunContentDescriptor): String {
        return try {
            val process = descriptor.processHandler
            val outputField = process?.javaClass?.getDeclaredField("myOutput")?.apply { isAccessible = true }
            val output = outputField?.get(process)
            val textMethod = output?.javaClass?.getMethod("getText")
            textMethod?.invoke(output)?.toString() ?: "N/A"
        } catch (_: Exception) {
            "N/A"
        }
    }
}