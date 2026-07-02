package com.dwai.idebridge.api

import com.intellij.openapi.project.Project

interface EditorApi {
    fun getCursor(project: Project): Map<String, Any?>
    fun getCode(project: Project, radius: Int): Map<String, Any?>
    fun getSelection(project: Project): Map<String, Any?>
    fun insert(project: Project, text: String): Map<String, Any?>
    fun createFile(project: Project, filePath: String, content: String): Map<String, Any?>
}