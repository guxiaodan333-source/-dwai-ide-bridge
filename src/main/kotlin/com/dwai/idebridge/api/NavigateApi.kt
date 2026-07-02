package com.dwai.idebridge.api

import com.intellij.openapi.project.Project

interface NavigateApi {
    fun openFile(project: Project, filePath: String, line: Int): Map<String, Any?>
    fun findSymbol(project: Project, query: String): Map<String, Any?>
}