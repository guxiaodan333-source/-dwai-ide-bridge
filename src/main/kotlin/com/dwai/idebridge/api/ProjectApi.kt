package com.dwai.idebridge.api

import com.intellij.openapi.project.Project

interface ProjectApi {
    fun getInfo(project: Project): Map<String, Any?>
    fun getOpenFiles(project: Project): Map<String, Any?>
    fun getFileStructure(project: Project, file: String?): Map<String, Any?>
}