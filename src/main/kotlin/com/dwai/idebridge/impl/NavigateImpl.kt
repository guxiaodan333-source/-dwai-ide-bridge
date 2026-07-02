package com.dwai.idebridge.impl

import com.dwai.idebridge.api.NavigateApi
import com.dwai.idebridge.executor.NavigateExecutor
import com.dwai.idebridge.inspector.SymbolInspector
import com.intellij.openapi.project.Project

object NavigateImpl : NavigateApi {
    override fun openFile(project: Project, filePath: String, line: Int) = NavigateExecutor.openFile(project, filePath, line)
    override fun findSymbol(project: Project, query: String) = SymbolInspector.findSymbols(project, query)
}