package com.dwai.idebridge.api

import com.dwai.idebridge.model.DiffApplyRequest
import com.dwai.idebridge.model.DiffResult
import com.intellij.openapi.project.Project

interface DiffApi {
    fun applyDiffs(project: Project, request: DiffApplyRequest): List<DiffResult>
    fun getPendingResults(): List<DiffResult>
}