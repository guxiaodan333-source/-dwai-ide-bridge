package com.dwai.idebridge.executor

import com.dwai.idebridge.inspector.EditorInspector
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

object EditorExecutor {

    fun insert(project: Project, text: String): Map<String, Any?> {
        try {
            val editor = EditorInspector.getEditor(project) ?: return mapOf("status" to "no_active_editor")

            ApplicationManager.getApplication().invokeAndWait {
                WriteCommandAction.runWriteCommandAction(project) {
                    val document = editor.document
                    val caret = editor.caretModel.currentCaret
                    document.insertString(caret.offset, text)
                    caret.moveToOffset(caret.offset + text.length)
                }
            }

            return mapOf("status" to "ok", "inserted" to text.length)
        } catch (e: Exception) {
            return mapOf("status" to "error", "reason" to (e.message ?: "Unknown error"))
        }
    }

    fun createFile(project: Project, filePath: String, content: String): Map<String, Any?> {
        try {
            ApplicationManager.getApplication().invokeAndWait {
                WriteCommandAction.runWriteCommandAction(project) {
                    val file = java.io.File(filePath)
                    file.parentFile?.mkdirs()
                    file.writeText(content)
                    com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)
                }
            }
            return mapOf("status" to "ok", "file" to filePath)
        } catch (e: Exception) {
            return mapOf("status" to "error", "reason" to (e.message ?: "Unknown error"))
        }
    }
}