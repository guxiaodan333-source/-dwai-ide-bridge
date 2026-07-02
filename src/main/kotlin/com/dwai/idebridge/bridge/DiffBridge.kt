package com.dwai.idebridge.bridge

import com.dwai.idebridge.model.DiffApplyRequest
import com.dwai.idebridge.model.DiffResult
import com.dwai.idebridge.model.FileDiff
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.WriteIntentReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Frame
import java.awt.Dialog
import java.awt.Window
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object DiffBridge {

    private val LOG = Logger.getInstance(DiffBridge::class.java)
    private val diffCounter = AtomicInteger(0)

    interface Listener {
        fun onPendingDiffsChanged()
    }

    data class PendingInfo(
        val diffId: String,
        val fileName: String,
        val filePath: String,
        val insertCount: Int,
        val deleteCount: Int
    )

    private val pendingDiffs = ConcurrentHashMap<String, PendingDiff>()
    private val listeners = CopyOnWriteArrayList<Listener>()

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        ApplicationManager.getApplication().invokeLater {
            listeners.forEach { it.onPendingDiffsChanged() }
        }
    }

    fun getPendingList(): List<PendingInfo> {
        return pendingDiffs.map { (id, pending) ->
            PendingInfo(
                diffId = id,
                fileName = pending.file.name,
                filePath = pending.filePath,
                insertCount = pending.insertCount,
                deleteCount = pending.deleteCount
            )
        }
    }

    fun acceptDiff(diffId: String) {
        val pending = pendingDiffs[diffId] ?: return
        val project = pending.project
        val title = pending.title
        pending.approved.set(true)
        pending.latch.countDown()
        pendingDiffs.remove(diffId)
        closeDiffDialog(title, project)
        openFile(project, pending.file)
        notifyListeners()
    }

    fun rejectDiff(diffId: String) {
        val pending = pendingDiffs[diffId] ?: return
        val project = pending.project
        val title = pending.title
        WriteAction.run<Throwable> { pending.document.setText(pending.originalContent) }
        pending.approved.set(false)
        pending.latch.countDown()
        pendingDiffs.remove(diffId)
        closeDiffDialog(title, project)
        openFile(project, pending.file)
        notifyListeners()
    }

    fun acceptAll() {
        val ids = pendingDiffs.keys().toList()
        var project: Project? = null
        for (id in ids) {
            pendingDiffs[id]?.let { pending ->
                if (project == null) project = pending.project
                pending.approved.set(true)
                pending.latch.countDown()
            }
            pendingDiffs.remove(id)
        }
        closeAllDialogs(project)
        notifyListeners()
    }

    fun rejectAll() {
        val ids = pendingDiffs.keys().toList()
        var project: Project? = null
        for (id in ids) {
            pendingDiffs[id]?.let { pending ->
                if (project == null) project = pending.project
                WriteAction.run<Throwable> { pending.document.setText(pending.originalContent) }
                pending.approved.set(false)
                pending.latch.countDown()
            }
            pendingDiffs.remove(id)
        }
        closeAllDialogs(project)
        notifyListeners()
    }

    fun openDiff(diffId: String) {
        val pending = pendingDiffs[diffId] ?: return
        ApplicationManager.getApplication().invokeLater {
            try {
                WriteIntentReadAction.run<Exception> {
                    val factory = DiffContentFactory.getInstance()
                    val left = factory.create(pending.project, pending.originalContent, pending.file.fileType)
                    val right = factory.create(pending.project, pending.document.text, pending.file.fileType)
                    val request = SimpleDiffRequest(
                        pending.title, left, right,
                        "原始 (${pending.file.name})", "修改后 (${pending.file.name})"
                    )
                    DiffManager.getInstance().showDiff(pending.project, request)
                }
            } catch (e: Exception) {
                LOG.error("DiffBridge: openDiff failed", e)
            }
        }
    }

    private fun getWindowTitle(window: Window): String? = when (window) {
            is Frame -> window.title
            is Dialog -> window.title
            else -> null
        }

        private fun closeDiffDialog(title: String, project: Project? = null) {
            ApplicationManager.getApplication().invokeLater {
                try {
                    for (window in Window.getWindows()) {
                        if (window.isShowing && getWindowTitle(window) == title) {
                            window.dispose()
                        }
                    }
                    if (project != null) {
                        closeDiffEditorTab(project, title)
                    }
                } catch (_: Exception) {}
            }
        }

        private fun closeAllDialogs(project: Project? = null) {
            ApplicationManager.getApplication().invokeLater {
                try {
                    for (window in Window.getWindows()) {
                        val t = getWindowTitle(window)
                        if (window.isShowing && t != null && t.startsWith("DWAI")) {
                            window.dispose()
                        }
                    }
                    if (project != null) {
                        closeAllDiffEditorTabs(project)
                    }
                } catch (_: Exception) {}
            }
        }

        private fun closeDiffEditorTab(project: Project, title: String) {
            try {
                val fem = FileEditorManager.getInstance(project)
                for (editor in fem.allEditors) {
                    if (editor.name == title) {
                        val vf = editor.file ?: continue
                        fem.closeFile(vf)
                        break
                    }
                }
            } catch (_: Exception) {}
        }

        private fun closeAllDiffEditorTabs(project: Project) {
            try {
                val fem = FileEditorManager.getInstance(project)
                val toClose = fem.openFiles.filter { vf ->
                    vf::class.java.name.contains("DiffVirtualFile", ignoreCase = true)
                }
                toClose.forEach { fem.closeFile(it) }
            } catch (_: Exception) {}
        }

        private fun openFile(project: Project, file: VirtualFile) {
            ApplicationManager.getApplication().invokeLater {
                try {
                    FileEditorManager.getInstance(project).openFile(file, true)
                } catch (_: Exception) {}
            }
        }

    fun getPendingResults(): List<DiffResult> {
        return pendingDiffs.values.map { pending ->
            if (pending.latch.count == 0L) {
                if (pending.approved.get()) {
                    DiffResult(path = pending.filePath, status = "approved", reason = pending.description ?: "User approved")
                } else {
                    DiffResult(path = pending.filePath, status = "rejected", reason = "User rejected")
                }
            } else {
                DiffResult(path = pending.filePath, status = "pending", reason = "Waiting for user decision")
            }
        }
    }

    fun applyDiffs(project: Project, request: DiffApplyRequest): List<DiffResult> {
        val results = mutableListOf<DiffResult>()
        for (fileDiff in request.files ?: emptyList()) {
            results.add(processSingleDiff(project, fileDiff))
        }
        notifyListeners()
        return results
    }

    private fun processSingleDiff(project: Project, fileDiff: FileDiff): DiffResult {
        try {
            val file = VfsBridge.resolveFile(project, fileDiff.path)
                ?: return DiffResult(path = fileDiff.path, status = "error", reason = "File not found: ${fileDiff.path}")

            val document = ReadAction.compute<Document?, Throwable> {
                FileDocumentManager.getInstance().getDocument(file)
            } ?: return DiffResult(path = fileDiff.path, status = "error", reason = "Cannot read document: ${fileDiff.path}")

            val originalContent = ReadAction.compute<String, Throwable> { document.text }

            val newContent = when {
                fileDiff.modified != null -> fileDiff.modified
                fileDiff.diff != null -> applyUnifiedDiff(originalContent, fileDiff.diff)
                else -> return DiffResult(path = fileDiff.path, status = "error", reason = "Neither diff nor modified content provided")
            }

            if (originalContent == newContent) {
                return DiffResult(path = fileDiff.path, status = "approved", reason = "No changes")
            }

            if (fileDiff.skip_review) {
                writeDocument(document, newContent)
                return DiffResult(path = fileDiff.path, status = "applied", reason = "skip_review")
            }

            writeDocument(document, newContent)

            val title = fileDiff.description ?: "DWAI: ${file.name}"
            val diffId = "${System.currentTimeMillis()}_${diffCounter.incrementAndGet()}_${file.name.hashCode()}"
            val lines = computeLineDiff(originalContent, newContent)
            pendingDiffs[diffId] = PendingDiff(
                project = project,
                file = file,
                document = document,
                originalContent = originalContent,
                filePath = fileDiff.path,
                description = fileDiff.description,
                insertCount = lines.first,
                deleteCount = lines.second,
                title = title
            )

            ApplicationManager.getApplication().invokeLater {
                try {
                    WriteIntentReadAction.run<Exception> {
                        val factory = DiffContentFactory.getInstance()
                        val left = factory.create(project, originalContent, file.fileType)
                        val right = factory.create(project, newContent, file.fileType)
                        val request = SimpleDiffRequest(title, left, right, "原始 (${file.name})", "修改后 (${file.name})")
                        DiffManager.getInstance().showDiff(project, request)
                    }
                } catch (e: Exception) {
                    LOG.error("DiffBridge: showDiff failed", e)
                    pendingDiffs[diffId]?.let { p ->
                        p.approved.set(false)
                        p.latch.countDown()
                    }
                    pendingDiffs.remove(diffId)
                    notifyListeners()
                }
            }

            return DiffResult(
                path = fileDiff.path,
                status = "pending",
                reason = "Changes written. Review in DWAI Changes panel."
            )
        } catch (e: Exception) {
            return DiffResult(path = fileDiff.path, status = "error", reason = e.message ?: "Unknown error")
        }
    }

    private fun computeLineDiff(original: String, modified: String): Pair<Int, Int> {
        val origLines = original.lines()
        val modLines = modified.lines()
        var insertCount = 0
        var deleteCount = 0
        var i = 0
        var j = 0
        while (i < origLines.size || j < modLines.size) {
            when {
                i >= origLines.size -> { insertCount++; j++ }
                j >= modLines.size -> { deleteCount++; i++ }
                origLines[i] == modLines[j] -> { i++; j++ }
                i + 1 < origLines.size && origLines[i + 1] == modLines[j] -> { deleteCount++; i++ }
                j + 1 < modLines.size && origLines[i] == modLines[j + 1] -> { insertCount++; j++ }
                else -> { deleteCount++; insertCount++; i++; j++ }
            }
        }
        return Pair(insertCount, deleteCount)
    }

    private fun writeDocument(document: Document, content: String) {
        ApplicationManager.getApplication().invokeAndWait {
            WriteAction.run<Exception> {
                document.setText(content)
            }
        }
    }

    private fun applyUnifiedDiff(original: String, diff: String): String {
        val lines = original.split("\n").toMutableList()
        val diffLines = diff.split("\n")
        var currentLine = 0
        var i = 0

        while (i < diffLines.size) {
            when {
                diffLines[i].startsWith("---") || diffLines[i].startsWith("+++") -> i++
                diffLines[i].startsWith("@@") -> {
                    val match = Regex("@@ -(\\d+),?(\\d*) \\+(\\d+),?(\\d*) @@").find(diffLines[i])
                    if (match != null) {
                        currentLine = match.groupValues[1].toInt() - 1
                    }
                    i++
                }
                diffLines[i].startsWith(" ") -> {
                    if (currentLine < lines.size) currentLine++
                    i++
                }
                diffLines[i].startsWith("-") -> {
                    if (currentLine < lines.size) {
                        lines.removeAt(currentLine)
                    }
                    i++
                }
                diffLines[i].startsWith("+") -> {
                    val newLine = diffLines[i].substring(1)
                    if (currentLine <= lines.size) {
                        lines.add(currentLine, newLine)
                        currentLine++
                    } else {
                        lines.add(newLine)
                    }
                    i++
                }
                else -> i++
            }
        }
        return lines.joinToString("\n")
    }

    private data class PendingDiff(
        val project: Project,
        val file: VirtualFile,
        val document: Document,
        val originalContent: String,
        val filePath: String,
        val description: String? = null,
        val insertCount: Int = 0,
        val deleteCount: Int = 0,
        val title: String,
        val approved: AtomicBoolean = AtomicBoolean(false),
        val latch: CountDownLatch = CountDownLatch(1)
    )
}