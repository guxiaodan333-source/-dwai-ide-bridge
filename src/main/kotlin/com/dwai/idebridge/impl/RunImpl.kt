package com.dwai.idebridge.impl

import com.dwai.idebridge.api.RunApi
import com.dwai.idebridge.executor.RunExecutor
import com.dwai.idebridge.inspector.RunInspector
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange

object RunImpl : RunApi {
    override fun getOutput(project: Project) = RunInspector.getOutput(project)
    override fun getOutputSummary(project: Project) = RunInspector.getOutputSummary(project)
    override fun startRun(project: Project, filePath: String) = RunExecutor.startRun(project, filePath)
    override fun streamRun(project: Project, filePath: String, exchange: HttpExchange) = RunExecutor.streamRun(project, filePath, exchange)
}