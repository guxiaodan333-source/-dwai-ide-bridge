package com.dwai.idebridge.impl

import com.dwai.idebridge.api.ProblemApi
import com.dwai.idebridge.inspector.ProblemInspector
import com.intellij.openapi.project.Project

object ProblemImpl : ProblemApi {
    override fun getProblems(project: Project) = ProblemInspector.getProblems(project)
}