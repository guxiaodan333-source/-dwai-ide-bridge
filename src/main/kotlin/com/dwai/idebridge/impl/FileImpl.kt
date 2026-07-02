package com.dwai.idebridge.impl

import com.dwai.idebridge.api.FileApi
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

object FileImpl : FileApi {
    override fun saveClose(project: Project, paths: List<String>?): Map<String, Any?> {
        ApplicationManager.getApplication().invokeLater {
            FileDocumentManager.getInstance().saveAllDocuments()
            val editorManager = FileEditorManager.getInstance(project)
            val filesToClose = if (paths != null && paths.isNotEmpty()) {
                val pathsToClose = paths.toSet()
                editorManager.openFiles.filter { it.path in pathsToClose }
            } else {
                editorManager.openFiles.toList()
            }
            filesToClose.forEach { editorManager.closeFile(it) }
        }
        return mapOf("status" to "ok", "message" to "保存并关闭操作已提交")
    }

    override fun listOpen(project: Project): Map<String, Any?> {
        var result: Map<String, Any?> = mapOf("status" to "error", "message" to "Timeout")
        ApplicationManager.getApplication().invokeAndWait {
            val editorManager = FileEditorManager.getInstance(project)
            val files = editorManager.openFiles.map { it.path }
            result = mapOf("status" to "ok", "files" to files, "count" to files.size)
        }
        return result
    }
}