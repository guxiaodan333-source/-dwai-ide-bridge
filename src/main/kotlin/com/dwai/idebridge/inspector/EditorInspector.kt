package com.dwai.idebridge.inspector

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange

object EditorInspector {

    fun getEditor(project: Project): Editor? {
        return FileEditorManager.getInstance(project).selectedTextEditor
    }

    fun getCursor(project: Project): Map<String, Any?> {
        var result: Map<String, Any?> = emptyMap<String, Any?>()
        ApplicationManager.getApplication().invokeAndWait {
            val editor = getEditor(project)
            if (editor == null) {
                result = mapOf<String, Any?>("status" to "no_active_editor")
                return@invokeAndWait
            }

            val caret = editor.caretModel.currentCaret
            val file = editor.virtualFile
            val pos = caret.logicalPosition

            result = mapOf<String, Any?>(
                "status" to "ok",
                "file" to (file?.path ?: ""),
                "project" to project.name,
                "line" to pos.line,
                "column" to pos.column,
                "offset" to caret.offset
            )
        }
        return result
    }

    fun getCode(project: Project, radius: Int): Map<String, Any?> {
        var result: Map<String, Any?> = emptyMap<String, Any?>()
        ApplicationManager.getApplication().invokeAndWait {
            val editor = getEditor(project)
            if (editor == null) {
                result = mapOf("status" to "no_active_editor")
                return@invokeAndWait
            }

            val caret = editor.caretModel.currentCaret
            val file = editor.virtualFile
            val document = editor.document

            val currentLine = caret.logicalPosition.line
            val totalLines = document.lineCount
            val startLine = maxOf(0, currentLine - radius)
            val endLine = minOf(totalLines - 1, currentLine + radius)

            val lines = (startLine..endLine).map { i ->
                val text = document.getText(TextRange(document.getLineStartOffset(i), document.getLineEndOffset(i)))
                mapOf("line" to i, "text" to text, "is_current" to (i == currentLine))
            }

            result = mapOf(
                "status" to "ok",
                "file" to (file?.path ?: ""),
                "project" to project.name,
                "language" to (file?.fileType?.name ?: "unknown"),
                "current_line" to currentLine,
                "cursor_column" to caret.logicalPosition.column,
                "lines" to lines
            )
        }
        return result
    }

    fun getSelection(project: Project): Map<String, Any?> {
        var result: Map<String, Any?> = emptyMap<String, Any?>()
        ApplicationManager.getApplication().invokeAndWait {
            val editor = getEditor(project)
            if (editor == null) {
                result = mapOf<String, Any?>("status" to "no_active_editor")
                return@invokeAndWait
            }

            val selection = editor.selectionModel
            if (!selection.hasSelection()) {
                result = mapOf<String, Any?>("status" to "ok", "has_selection" to false)
                return@invokeAndWait
            }

            result = mapOf<String, Any?>(
                "status" to "ok",
                "has_selection" to true,
                "selected_text" to (selection.selectedText ?: ""),
                "start" to mapOf("line" to selection.selectionStartPosition?.line, "column" to selection.selectionStartPosition?.column),
                "end" to mapOf("line" to selection.selectionEndPosition?.line, "column" to selection.selectionEndPosition?.column)
            )
        }
        return result
    }
}