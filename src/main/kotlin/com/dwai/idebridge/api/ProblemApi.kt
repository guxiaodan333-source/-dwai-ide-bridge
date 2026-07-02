package com.dwai.idebridge.api

import com.intellij.openapi.project.Project

interface ProblemApi {
    fun getProblems(project: Project): Map<String, Any?>
}