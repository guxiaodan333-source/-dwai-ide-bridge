package com.dwai.idebridge.impl

import com.dwai.idebridge.api.ProjectApi
import com.dwai.idebridge.inspector.ProjectInspector
import com.intellij.openapi.project.Project

object ProjectImpl : ProjectApi {
    override fun getInfo(project: Project) = ProjectInspector.getInfo(project)
    override fun getOpenFiles(project: Project) = ProjectInspector.getOpenFiles(project)
    override fun getFileStructure(project: Project, file: String?) = ProjectInspector.getFileStructure(project, file)
}