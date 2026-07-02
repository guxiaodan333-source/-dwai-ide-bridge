package com.dwai.idebridge.inspector

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project

object ProblemInspector {

    fun getProblems(project: Project): Map<String, Any?> {
        return ReadAction.compute<Map<String, Any?>, Exception> {
            val editor = EditorInspector.getEditor(project) ?: return@compute mapOf("status" to "no_active_editor")

            val document = editor.document
            val problems = mutableListOf<Map<String, Any>>()

            for (highlighter in editor.markupModel.allHighlighters) {
                val tip = highlighter.errorStripeTooltip ?: continue
                val line = document.getLineNumber(highlighter.startOffset)
                val severity = when {
                    highlighter.toString().contains("ERROR") || highlighter.errorStripeTooltip?.toString()?.contains("Error") == true -> "ERROR"
                    highlighter.toString().contains("WARN") -> "WARNING"
                    else -> "INFO"
                }
                problems.add(mapOf(
                    "message" to tip.toString(),
                    "severity" to severity,
                    "line" to line,
                    "start_offset" to highlighter.startOffset,
                    "end_offset" to highlighter.endOffset
                ))
            }

            mapOf(
                "status" to "ok",
                "file" to editor.virtualFile.path,
                "count" to problems.size,
                "problems" to problems
            )
        }
    }
}