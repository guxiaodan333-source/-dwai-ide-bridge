package com.dwai.idebridge.impl

import com.dwai.idebridge.api.EditorApi
import com.dwai.idebridge.executor.EditorExecutor
import com.dwai.idebridge.inspector.EditorInspector
import com.intellij.openapi.project.Project

object EditorImpl : EditorApi {
    override fun getCursor(project: Project) = EditorInspector.getCursor(project)
    override fun getCode(project: Project, radius: Int) = EditorInspector.getCode(project, radius)
    override fun getSelection(project: Project) = EditorInspector.getSelection(project)
    override fun insert(project: Project, text: String) = EditorExecutor.insert(project, text)
    override fun createFile(project: Project, filePath: String, content: String) = EditorExecutor.createFile(project, filePath, content)
}