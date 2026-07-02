package com.dwai.idebridge.executor

import com.dwai.idebridge.util.JsonUtil
import com.google.gson.Gson
import com.intellij.execution.*
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.diagnostic.Logger
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindowManager
import com.sun.net.httpserver.HttpExchange
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

object RunExecutor {

    private val gson = Gson()
    private val LOG = Logger.getInstance(RunExecutor::class.java)
    private val outputCache = mutableMapOf<String, StringBuilder>()

    fun getCachedOutput(name: String): String? = outputCache[name]?.toString()

    fun startRun(project: Project, filePath: String) {
        ApplicationManager.getApplication().invokeAndWait {
            doRunFile(project, filePath)
        }
    }

    fun streamRun(project: Project, filePath: String, exchange: HttpExchange) {
        exchange.responseHeaders.set("Content-Type", "text/event-stream; charset=utf-8")
        exchange.responseHeaders.set("Cache-Control", "no-cache")
        exchange.responseHeaders.set("Connection", "keep-alive")
        exchange.responseHeaders.set("X-Accel-Buffering", "no")
        exchange.sendResponseHeaders(200, 0)

        val writer = OutputStreamWriter(exchange.responseBody, Charsets.UTF_8)
        val connectionClosed = AtomicBoolean(false)

        exchange.responseBody.use { outputStream ->
            val sseWriter = outputStream.writer(Charsets.UTF_8)

            sseWriter.write("event: started\ndata: {\"file\":\"$filePath\"}\n\n")
            sseWriter.flush()

            ApplicationManager.getApplication().invokeAndWait {
                try {
                    val file = LocalFileSystem.getInstance().findFileByPath(filePath)
                    val document = file?.let { FileDocumentManager.getInstance().getDocument(it) }
                    var runDescriptor: RunContentDescriptor? = null
                    val latch = java.util.concurrent.CountDownLatch(1)

                    if (file != null) {
                        val runManager = RunManager.getInstance(project)
                        val selectedConfig = runManager.selectedConfiguration?.configuration
                        if (selectedConfig == null) {
                            sseWriter.write("event: done\ndata: {\"error\":\"No run configuration selected\"}\n\n")
                            sseWriter.flush()
                            return@invokeAndWait
                        }

                        val runner = ProgramRunner.findRunnerById("pythonRunner")
                            ?: ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, selectedConfig)
                            ?: return@invokeAndWait

                        val settings = createPythonRunConfig(project, filePath)
                        val finalSettings = settings
                        if (finalSettings != null) {
                            runManager.setTemporaryConfiguration(finalSettings)
                            runManager.selectedConfiguration = finalSettings

                            val executor = DefaultRunExecutor.getRunExecutorInstance()
                            val env = ExecutionEnvironmentBuilder.create(executor, finalSettings).build()

                            runner.execute(env) { descriptor ->
                                runDescriptor = descriptor
                                latch.countDown()
                                val processHandler = descriptor.processHandler ?: return@execute
                                val buffer = StringBuilder()
                                val name = descriptor.displayName
                                outputCache[name] = buffer

                                processHandler.addProcessListener(object : ProcessListener {
                                    override fun processTerminated(event: ProcessEvent) {
                                        try {
                                            sseWriter.write("event: done\ndata: {\"exit_code\":${event.exitCode}}\n\n")
                                            sseWriter.flush()
                                        } catch (_: Exception) { }
                                    }
                                    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                                        buffer.append(event.text)
                                        try {
                                            val text = event.text
                                            val escaped = JsonUtil.escapeJson(text)
                                            sseWriter.write("event: text\ndata: {\"text\":\"$escaped\"}\n\n")
                                            sseWriter.flush()
                                        } catch (_: Exception) { }
                                    }
                                    override fun startNotified(event: ProcessEvent) {}
                                })
                            }
                        }
                    }

                    if (document != null) {
                        document.addDocumentListener(object : DocumentListener {
                            override fun documentChanged(event: DocumentEvent) {
                                if (connectionClosed.get()) {
                                    document.removeDocumentListener(this)
                                    return
                                }
                                try {
                                    val text = event.newFragment.toString()
                                    if (text.isNotBlank()) {
                                        val escaped = JsonUtil.escapeJson(text)
                                        sseWriter.write("event: text\ndata: {\"text\":\"$escaped\"}\n\n")
                                        sseWriter.flush()
                                    }
                                } catch (_: Exception) { }
                            }
                        })
                    }

                    latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                    if (runDescriptor == null) {
                        sseWriter.write("event: done\ndata: {\"error\":\"Could not start run configuration\"}\n\n")
                        sseWriter.flush()
                    }
                } catch (e: Exception) {
                    runCatching {
                        sseWriter.write("event: done\ndata: {\"error\":\"${e.message}\"}\n\n")
                        sseWriter.flush()
                    }
                }
            }
        }
    }

    private fun createPythonRunConfig(project: Project, filePath: String): RunnerAndConfigurationSettings? {
        try {
            val runManager = RunManager.getInstance(project)
            val configType = com.intellij.execution.configurations.ConfigurationType.CONFIGURATION_TYPE_EP
                .extensionList
                .find { it.id.contains("python", ignoreCase = true) }
                ?: return null

            val factory = configType.configurationFactories.firstOrNull() ?: return null
            val settings = runManager.createConfiguration(
                "DWAI: ${java.io.File(filePath).name}", factory as com.intellij.execution.configurations.ConfigurationFactory
            )
            val config = settings.configuration

            val module = ModuleManager.getInstance(project).modules.firstOrNull() ?: return null
            (config as? com.intellij.execution.configurations.ModuleBasedConfiguration<*, *>)?.setModule(module)

            val configClass = config::class.java
            configClass.methods.find { it.getName() == "setScriptName" }?.invoke(config, filePath)
                ?: configClass.methods.find { it.getName() == "setScriptPath" }?.invoke(config, filePath)

            configClass.methods.find { it.getName() == "setWorkingDirectory" }?.invoke(
                config, java.io.File(filePath).parent
            )

            return settings
        } catch (e: Exception) {
            return null
        }
    }

    private fun doRunFile(project: Project, filePath: String) {
        try {
            if (filePath.endsWith(".py")) {
                val runManager = RunManager.getInstance(project)
                val selectedConfig = runManager.selectedConfiguration?.configuration
                if (selectedConfig == null) {
                    LOG.warn("doRunFile: no run configuration selected")
                    return
                }
                val runner = ProgramRunner.findRunnerById("pythonRunner")
                    ?: ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, selectedConfig)
                if (runner == null) {
                    LOG.warn("doRunFile: no runner found for $filePath")
                    return
                }

                val settings = createPythonRunConfig(project, filePath)
                if (settings == null) return
                val finalSettings = settings
                runManager.setTemporaryConfiguration(finalSettings)
                runManager.selectedConfiguration = finalSettings

                val executor = DefaultRunExecutor.getRunExecutorInstance()
                val env = ExecutionEnvironmentBuilder.create(executor, finalSettings).build()
                runner.execute(env) { descriptor ->
                    val processHandler = descriptor.processHandler ?: return@execute
                    val buffer = StringBuilder()
                    val name = descriptor.displayName
                    outputCache[name] = buffer

                    processHandler.addProcessListener(object : ProcessListener {
                        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                            buffer.append(event.text)
                        }
                        override fun processTerminated(event: ProcessEvent) {}
                        override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {}
                        override fun startNotified(event: ProcessEvent) {}
                    })
                }
            }
        } catch (e: Exception) {
            LOG.warn("doRunFile failed for $filePath", e)
        }
    }
}