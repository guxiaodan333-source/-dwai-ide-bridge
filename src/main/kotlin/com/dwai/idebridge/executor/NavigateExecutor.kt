package com.dwai.idebridge.executor

import com.dwai.idebridge.bridge.VfsBridge
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project

object NavigateExecutor {

    fun openFile(project: Project, filePath: String, line: Int): Map<String, Any?> {
        return try {
            val file = VfsBridge.resolveFile(project, filePath)
                ?: return mapOf("status" to "error", "reason" to "File not found: $filePath")

            ApplicationManager.getApplication().invokeAndWait {
                val descriptor = OpenFileDescriptor(project, file, line, 0)
                FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            }

            mapOf("status" to "ok", "file" to filePath, "line" to line)
        } catch (e: Exception) {
            mapOf("status" to "error", "reason" to (e.message ?: "Unknown error"))
        }
    }
}