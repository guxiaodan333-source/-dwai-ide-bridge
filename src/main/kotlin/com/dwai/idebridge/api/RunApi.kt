package com.dwai.idebridge.api

import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange

interface RunApi {
    fun getOutput(project: Project): Map<String, Any?>
    fun getOutputSummary(project: Project): Map<String, Any?>
    fun startRun(project: Project, filePath: String)
    fun streamRun(project: Project, filePath: String, exchange: HttpExchange)
}