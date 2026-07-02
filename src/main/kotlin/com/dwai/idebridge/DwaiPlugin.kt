package com.dwai.idebridge

import com.dwai.idebridge.handler.*
import com.dwai.idebridge.mcp.McpServer
import com.dwai.idebridge.ui.DiffPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

class DwaiPlugin : ProjectActivity {

    private var httpServer: HttpServer? = null
    private val lock = ReentrantLock()

    override suspend fun execute(project: Project) {
        startHttpServer(project)
        ApplicationManager.getApplication().invokeLater {
            DiffPanel(project)
        }
        Disposer.register(project, Disposable {
            stopHttpServer()
        })
    }

    private fun startHttpServer(project: Project) {
        lock.lock()
        try {
            if (httpServer != null) return

            val port = System.getenv("DWAI_PORT")?.toIntOrNull() ?: 8765
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
            server.executor = Executors.newCachedThreadPool()

            server.createContext("/api/cursor") { CursorHandler.handleCursor(project, it) }
            server.createContext("/api/code") { CursorHandler.handleCode(project, it) }
            server.createContext("/api/selection") { CursorHandler.handleSelection(project, it) }
            server.createContext("/api/project") { ProjectHandler.handleProject(project, it) }

            server.createContext("/api/open-files") { ProjectHandler.handleOpenFiles(project, it) }
            server.createContext("/api/file-structure") { ProjectHandler.handleFileStructure(project, it) }
            server.createContext("/api/problems") { ProblemHandler.handleProblems(project, it) }
            server.createContext("/api/terminal") { TerminalHandler.handleTerminal(project, it) }
            server.createContext("/api/terminal/exec") { TerminalHandler.handleTerminalExec(project, it) }

            server.createContext("/api/editor/insert") { EditorHandler.handleInsert(project, it) }
            server.createContext("/api/editor/create-file") { EditorHandler.handleCreateFile(project, it) }

            server.createContext("/api/navigate/open") { NavigateHandler.handleNavigateOpen(project, it) }
            server.createContext("/api/navigate/symbol") { NavigateHandler.handleSymbol(project, it) }

            server.createContext("/api/run-output") { RunHandler.handleRunOutput(project, it) }
            server.createContext("/api/run") { RunHandler.handleRun(project, it) }

            server.createContext("/api/apply-diff") { DiffHandler.handleApplyDiff(project, it) }

            server.createContext("/api/health") { HealthHandler.handle(it) }

            server.createContext("/mcp") { McpServer.handle(project, it) }

            server.start()
            httpServer = server

            println("[DWAI] HTTP server started on http://127.0.0.1:$port")
        } finally {
            lock.unlock()
        }
    }

    private fun stopHttpServer() {
        lock.lock()
        try {
            httpServer?.stop(1)
            httpServer = null
            println("[DWAI] HTTP server stopped")
        } finally {
            lock.unlock()
        }
    }
}