package com.dwai.idebridge.bridge

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

object VfsBridge {

    fun resolveFile(project: Project, path: String): VirtualFile? {
        val normalizedPath = path.replace('\\', '/')
        var file = LocalFileSystem.getInstance().findFileByPath(normalizedPath)
        if (file != null) return file

        // 刷新 VFS 后重试（新建文件场景）
        file = LocalFileSystem.getInstance().refreshAndFindFileByPath(normalizedPath)
        if (file != null) return file

        val basePath = project.basePath ?: return null
        val absolutePath = basePath.replace('\\', '/') + '/' + normalizedPath.removePrefix("/")
        file = LocalFileSystem.getInstance().findFileByPath(absolutePath)
        if (file != null) return file

        return LocalFileSystem.getInstance().refreshAndFindFileByPath(absolutePath)
    }

    fun resolveFileDirect(path: String): VirtualFile? {
        return LocalFileSystem.getInstance().findFileByPath(path.replace('\\', '/'))
    }
}