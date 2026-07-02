package com.dwai.idebridge.ui

import com.dwai.idebridge.bridge.DiffBridge
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import java.awt.event.MouseEvent

class DiffStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "DWAI_DiffStatus"

    override fun getDisplayName(): String = "DWAI Changes"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return DiffStatusBarWidget(project)
    }
}

class DiffStatusBarWidget(project: Project) : EditorBasedWidget(project), DiffBridge.Listener {

    override fun ID(): String = "DWAI_DiffStatus"

    override fun install(statusBar: StatusBar) {
        super.install(statusBar)
        DiffBridge.addListener(this)
        statusBar.updateWidget(ID())
    }

    override fun dispose() {
        DiffBridge.removeListener(this)
        super.dispose()
    }

    override fun onPendingDiffsChanged() {
        if (myStatusBar != null) {
            myStatusBar!!.updateWidget(ID())
        }
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation {
        return object : StatusBarWidget.TextPresentation {
            override fun getText(): String {
                val count = DiffBridge.getPendingList().size
                return if (count > 0) "DWAI: $count \u4e2a\u5f85\u5ba1\u6279" else ""
            }

            override fun getTooltipText(): String? {
                val count = DiffBridge.getPendingList().size
                return if (count > 0) "$count \u4e2a\u6587\u4ef6\u5f85\u5ba1\u6279" else null
            }

            override fun getClickConsumer(): Consumer<MouseEvent>? {
                return Consumer { _ ->
                    val tw = ToolWindowManager.getInstance(project).getToolWindow("DWAI Changes")
                    tw?.activate(null)
                }
            }

            override fun getAlignment(): Float = 0.5f
        }
    }
}