package com.dwai.idebridge.impl

import com.dwai.idebridge.api.TerminalApi
import com.dwai.idebridge.executor.TerminalExecutor
import com.dwai.idebridge.inspector.TerminalInspector
import com.intellij.openapi.project.Project

object TerminalImpl : TerminalApi {
    override fun getContent(project: Project) = TerminalInspector.getContent(project)
    override fun exec(project: Project, command: String) = TerminalExecutor.exec(project, command)
}