package com.dwai.idebridge.inspector

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager

object SymbolInspector {

    private val IGNORE_DIRS = setOf(
        ".git", "__pycache__", "node_modules", ".idea", ".gradle",
        "build", "dist", ".venv", "venv", "env", ".env",
        "target", "bin", "obj", ".next", ".nuxt"
    )

    private const val MAX_RESULTS = 50
    private const val MAX_FILES_SCANNED = 500

    fun findSymbols(project: Project, query: String): Map<String, Any?> {
        val results = mutableListOf<Map<String, Any>>()
        val fileCount = intArrayOf(0)
        try {
            val moduleManager = ModuleManager.getInstance(project)
            for (module in moduleManager.modules) {
                if (results.size >= MAX_RESULTS || fileCount[0] >= MAX_FILES_SCANNED) break
                val rootManager = ProjectRootManager.getInstance(project)
                val vFile = rootManager.contentRoots.firstOrNull() ?: continue
                searchInDirectory(project, query, vFile, results, fileCount)
            }
        } catch (_: Exception) { }

        return mapOf(
            "status" to "ok",
            "query" to query,
            "count" to results.size,
            "truncated" to (results.size >= MAX_RESULTS || fileCount[0] >= MAX_FILES_SCANNED),
            "results" to results.take(MAX_RESULTS)
        )
    }

    private fun searchInDirectory(
        project: Project,
        query: String,
        vFile: com.intellij.openapi.vfs.VirtualFile,
        results: MutableList<Map<String, Any>>,
        fileCount: IntArray
    ) {
        if (results.size >= MAX_RESULTS || fileCount[0] >= MAX_FILES_SCANNED) return
        for (child in vFile.children) {
            if (results.size >= MAX_RESULTS || fileCount[0] >= MAX_FILES_SCANNED) break
            if (child.isDirectory) {
                if (child.name !in IGNORE_DIRS) {
                    searchInDirectory(project, query, child, results, fileCount)
                }
            } else {
                fileCount[0]++
                if (child.name.contains(query, ignoreCase = true)) {
                    results.add(mapOf(
                        "name" to child.name,
                        "path" to child.path,
                        "type" to "file"
                    ))
                }
                try {
                    val psiFile = PsiManager.getInstance(project).findFile(child) ?: continue
                    val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: continue
                    for (line in 0 until document.lineCount) {
                        val text = document.getText(
                            com.intellij.openapi.util.TextRange(
                                document.getLineStartOffset(line),
                                document.getLineEndOffset(line)
                            )
                        )
                        if (text.contains(query)) {
                            results.add(mapOf(
                                "name" to query,
                                "path" to "${child.path}:${line + 1}",
                                "type" to "match",
                                "line" to (line + 1),
                                "text" to text.trim().take(200)
                            ))
                            if (results.size >= MAX_RESULTS) break
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }
}