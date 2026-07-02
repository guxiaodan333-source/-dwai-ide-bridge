package com.dwai.idebridge.inspector

import com.intellij.openapi.project.Project

object TerminalInspector {

    fun getContent(project: Project): Map<String, Any?> {
        return try {
            val terminalViewClass = Class.forName("com.intellij.terminal.TerminalView")
            val getInstanceMethod = terminalViewClass.getMethod("getInstance", Project::class.java)
            val terminalView = getInstanceMethod.invoke(null, project)

            val getWidgetsMethod = terminalViewClass.getMethod("getWidgets")
            val widgets = getWidgetsMethod.invoke(terminalView) as? List<*> ?: emptyList<Any>()

            val sessions = mutableListOf<Map<String, Any>>()
            for (widget in widgets) {
                if (widget == null) continue
                val title = try {
                    widget::class.java.getMethod("getTitle").invoke(widget)?.toString() ?: ""
                } catch (_: Exception) { "" }

                val content = try {
                    val contentMethod = widget::class.java.getMethod("getContent")
                    val contentWidget = contentMethod.invoke(widget)
                    val textMethod = contentWidget::class.java.getMethod("getText")
                    textMethod.invoke(contentWidget)?.toString() ?: ""
                } catch (_: Exception) { "" }

                sessions.add(mapOf("title" to title, "content" to content.take(2000)))
            }

            mapOf("status" to "ok", "count" to sessions.size, "sessions" to sessions)
        } catch (e: Exception) {
            mapOf("status" to "ok", "count" to 0, "sessions" to emptyList<Map<String, Any>>(), "note" to "Terminal not available or not open")
        }
    }
}