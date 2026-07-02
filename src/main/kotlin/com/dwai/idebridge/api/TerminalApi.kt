package com.dwai.idebridge.api

import com.intellij.openapi.project.Project

interface TerminalApi {
    fun getContent(project: Project): Map<String, Any?>
    fun exec(project: Project, command: String): Map<String, Any?>
}