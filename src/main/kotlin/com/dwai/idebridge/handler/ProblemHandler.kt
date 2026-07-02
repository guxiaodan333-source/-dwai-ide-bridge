package com.dwai.idebridge.handler

import com.dwai.idebridge.api.ApiRegistry
import com.dwai.idebridge.util.JsonUtil
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange

object ProblemHandler {

    fun handleProblems(project: Project, exchange: HttpExchange) {
        val result = ApiRegistry.problem.getProblems(project)
        JsonUtil.respondJson(exchange, 200, result)
    }
}