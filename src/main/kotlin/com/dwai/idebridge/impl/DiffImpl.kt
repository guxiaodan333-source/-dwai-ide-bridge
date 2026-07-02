package com.dwai.idebridge.impl

import com.dwai.idebridge.api.DiffApi
import com.dwai.idebridge.bridge.DiffBridge
import com.dwai.idebridge.model.DiffApplyRequest
import com.dwai.idebridge.model.DiffResult
import com.intellij.openapi.project.Project

object DiffImpl : DiffApi {
    override fun applyDiffs(project: Project, request: DiffApplyRequest): List<DiffResult> = DiffBridge.applyDiffs(project, request)
    override fun getPendingResults(): List<DiffResult> = DiffBridge.getPendingResults()
}