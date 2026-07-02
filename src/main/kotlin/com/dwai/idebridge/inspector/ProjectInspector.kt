package com.dwai.idebridge.inspector

import com.dwai.idebridge.bridge.PsiBridge
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager

object ProjectInspector {

    fun getInfo(project: Project): Map<String, Any?> {
        val appInfo = ApplicationInfo.getInstance()
        val sdk = try { ProjectRootManager.getInstance(project).projectSdk } catch (e: Exception) { null }

        return ReadAction.compute<Map<String, Any?>, Exception> {
            val activeEditor = EditorInspector.getEditor(project)
            val currentFile = activeEditor?.virtualFile

            mapOf(
                "status" to "ok",
                "name" to project.name,
                "base_path" to (project.basePath ?: ""),
                "is_open" to project.isOpen,
                "editor" to mapOf(
                    "name" to appInfo.fullApplicationName,
                    "version" to appInfo.fullVersion
                ),
                "language" to (currentFile?.fileType?.name ?: "unknown"),
                "runtime" to mapOf(
                    "sdk_name" to (sdk?.name ?: "unknown"),
                    "sdk_version" to (sdk?.versionString ?: "unknown"),
                    "sdk_home" to (sdk?.homePath ?: "unknown")
                )
            )
        }
    }

    fun getOpenFiles(project: Project): Map<String, Any?> {
        return ReadAction.compute<Map<String, Any?>, Exception> {
            val editorManager = FileEditorManager.getInstance(project)
            val files = editorManager.openFiles.map { file ->
                mapOf(
                    "name" to file.name,
                    "path" to file.path,
                    "type" to (file.fileType?.name ?: "unknown"),
                    "is_directory" to file.isDirectory
                )
            }
            mapOf("status" to "ok", "count" to files.size, "files" to files)
        }
    }

    fun getFileStructure(project: Project, requestedFile: String?): Map<String, Any?> {
        return ReadAction.compute<Map<String, Any?>, Exception> {
            val targetFile = if (requestedFile != null) {
                LocalFileSystem.getInstance().findFileByPath(requestedFile)
            } else {
                EditorInspector.getEditor(project)?.virtualFile
            }

            if (targetFile == null) {
                return@compute mapOf<String, Any?>(
                    "status" to (if (requestedFile != null) "file_not_found" else "no_active_editor")
                )
            }

            val psiFile = PsiManager.getInstance(project).findFile(targetFile)
                ?: return@compute mapOf<String, Any?>("status" to "no_psi_file")

            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            val structure = mutableListOf<Map<String, Any>>()
            PsiBridge.walkStructure(psiFile, document, structure, 0)

            mapOf<String, Any?>(
                "status" to "ok",
                "file" to targetFile.path,
                "language" to (targetFile.fileType?.name ?: "unknown"),
                "structure" to structure
            )
        }
    }
}