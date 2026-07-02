package com.dwai.idebridge.executor

import com.intellij.openapi.project.Project

object TerminalExecutor {

    fun exec(project: Project, command: String): Map<String, Any?> {
        return try {
            val terminalViewClass = Class.forName("com.intellij.terminal.TerminalView")
            val getInstanceMethod = terminalViewClass.getMethod("getInstance", Project::class.java)
            val terminalView = getInstanceMethod.invoke(null, project)

            val getWidgetsMethod = terminalViewClass.getMethod("getWidgets")
            val widgets = getWidgetsMethod.invoke(terminalView) as? List<*> ?: emptyList<Any>()

            if (widgets.isEmpty()) {
                return mapOf("status" to "error", "reason" to "No terminal tab open")
            }

            val widget = widgets.first()!!
            val contentMethod = widget::class.java.getMethod("getContent")
            val contentWidget = contentMethod.invoke(widget)

            val termMethod = try {
                contentWidget::class.java.getMethod("getTerminal")
            } catch (_: Exception) { null }

            if (termMethod != null) {
                val terminal = termMethod.invoke(contentWidget)
                val writeMethod = terminal::class.java.getMethod("write", String::class.java)
                writeMethod.invoke(terminal, command + "\n")
                mapOf("status" to "ok", "command" to command, "note" to "Command sent to terminal")
            } else {
                val sendMethod = contentWidget::class.java.getMethod("sendCommand", String::class.java)
                sendMethod.invoke(contentWidget, command)
                mapOf("status" to "ok", "command" to command, "note" to "Command sent to terminal")
            }
        } catch (e: Exception) {
            mapOf("status" to "error", "reason" to (e.message ?: "Terminal not available"))
        }
    }
}