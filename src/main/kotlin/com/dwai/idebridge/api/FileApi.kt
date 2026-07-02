package com.dwai.idebridge.api

import com.intellij.openapi.project.Project

interface FileApi {
    fun saveClose(project: Project, paths: List<String>?): Map<String, Any?>
    fun listOpen(project: Project): Map<String, Any?>
}