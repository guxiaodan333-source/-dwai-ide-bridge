package com.dwai.idebridge.ui

import com.dwai.idebridge.bridge.DiffBridge
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GraphicsEnvironment
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

class DiffPanel(private val project: Project) : JPanel(), DiffBridge.Listener, Disposable {

    private val listPanel = JPanel()
    private val countLabel = JLabel("").apply {
        horizontalAlignment = JLabel.RIGHT
    }
    private val titleLabel = JLabel("DWAI Changes").apply {
        font = font.deriveFont(java.awt.Font.BOLD, 13f)
    }
    private var dialog: JDialog? = null

    private companion object {
        const val DIALOG_WIDTH = 420
        const val DIALOG_HEIGHT = 320
    }

    init {
        layout = BorderLayout()
        border = JBUI.Borders.empty(0)
        isOpaque = true
        background = JBUI.CurrentTheme.ToolWindow.background()

        val acceptAllBtn = JButton("接受全部").apply {
            addActionListener { DiffBridge.acceptAll() }
        }
        val rejectAllBtn = JButton("拒绝全部").apply {
            addActionListener { DiffBridge.rejectAll() }
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 4)).apply {
            isOpaque = false
        }
        buttonPanel.add(acceptAllBtn)
        buttonPanel.add(rejectAllBtn)

        val headerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(6, 12, 6, 12)
        }
        headerPanel.add(titleLabel, BorderLayout.WEST)
        headerPanel.add(buttonPanel, BorderLayout.EAST)
        headerPanel.add(countLabel, BorderLayout.CENTER).apply {
            countLabel.horizontalAlignment = JLabel.CENTER
        }

        add(headerPanel, BorderLayout.NORTH)

        listPanel.layout = BoxLayout(listPanel, BoxLayout.Y_AXIS)
        listPanel.isOpaque = true
        listPanel.background = JBUI.CurrentTheme.ToolWindow.background()

        val centerPanel = JPanel(BorderLayout()).apply {
            isOpaque = true
            background = JBUI.CurrentTheme.ToolWindow.background()
            add(JSeparator(SwingConstants.HORIZONTAL), BorderLayout.NORTH)
            add(JBScrollPane(listPanel).apply {
                border = BorderFactory.createEmptyBorder()
                preferredSize = Dimension(DIALOG_WIDTH - 10, DIALOG_HEIGHT - 80)
            }, BorderLayout.CENTER)
        }
        add(centerPanel, BorderLayout.CENTER)

        DiffBridge.addListener(this)
        refresh()
    }

    override fun onPendingDiffsChanged() {
        refresh()
    }

    private fun refresh() {
        val list = DiffBridge.getPendingList()
        listPanel.removeAll()

        if (list.isEmpty()) {
            countLabel.text = ""
            titleLabel.text = "DWAI Changes"
            listPanel.add(JBLabel("  暂无待审批的改动").apply {
                border = JBUI.Borders.empty(20, 12)
                foreground = JBColor.GRAY
            })
            ApplicationManager.getApplication().invokeLater {
                hideDialog()
            }
        } else {
            titleLabel.text = "DWAI Changes"
            countLabel.text = "${list.size} 个待审批"
            for (info in list) {
                listPanel.add(createItemRow(info))
            }
            ApplicationManager.getApplication().invokeLater {
                showDialog()
            }
        }

        listPanel.revalidate()
        listPanel.repaint()
    }

    private fun showDialog() {
        if (dialog == null || !dialog!!.isShowing) {
            val parent = try {
                com.intellij.openapi.wm.WindowManager.getInstance().suggestParentWindow(project)
            } catch (_: Exception) { null }
            if (dialog != null) dialog?.dispose()
            dialog = JDialog(parent, "DWAI Changes").apply {
                defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
                isResizable = true
                contentPane.add(this@DiffPanel, BorderLayout.CENTER)
                pack()
                setSize(DIALOG_WIDTH, DIALOG_HEIGHT)
                minimumSize = Dimension(300, 200)
            }
        }

        if (dialog?.isShowing == true) return

        val dlg = dialog ?: return
        dlg.setSize(DIALOG_WIDTH, DIALOG_HEIGHT)

        ApplicationManager.getApplication().invokeLater {
            positionAtBottomRight(dlg)
            dlg.isVisible = true
            dlg.toFront()
        }
    }

    private fun hideDialog() {
        dialog?.isVisible = false
    }

    private fun positionAtBottomRight(dialog: JDialog) {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screenDevices = ge.screenDevices
        var targetBounds: Rectangle? = null

        for (device in screenDevices) {
            for (window in java.awt.Window.getWindows()) {
                if (window is JFrame && window.isShowing && window.title.contains("PyCharm")) {
                    val config = device.defaultConfiguration
                    targetBounds = config.bounds
                    break
                }
            }
            if (targetBounds != null) break
        }

        if (targetBounds == null) {
            targetBounds = ge.defaultScreenDevice.defaultConfiguration.bounds
        }

        val bounds = targetBounds ?: return
        val x = bounds.x + bounds.width - dialog.width - 40
        val y = bounds.y + bounds.height - dialog.height - 60
        dialog.location = Point(x, y)
    }

    override fun dispose() {
        DiffBridge.removeListener(this)
        dialog?.dispose()
        dialog = null
    }

    private fun createItemRow(info: DiffBridge.PendingInfo): JPanel {
        val row = JPanel(GridBagLayout()).apply {
            isOpaque = true
            background = JBUI.CurrentTheme.ToolWindow.background()
            border = JBUI.Borders.compound(
                JBUI.Borders.empty(4, 12),
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)
            )
        }
        val gc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
        }

        val nameLabel = JLabel(info.fileName).apply {
            putClientProperty("html.disable", null)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = DiffBridge.openDiff(info.diffId)
                override fun mouseEntered(e: MouseEvent) { text = "<html><u>${info.fileName}</u></html>" }
                override fun mouseExited(e: MouseEvent) { text = info.fileName }
            })
        }

        val green = "#2DA44E"
        val red = "#CF222E"
        val gray = "#888888"
        val changeText = "<html>" +
            "<font color='${if (info.insertCount > 0) green else gray}'>+${info.insertCount}</font>" +
            " <font color='${if (info.deleteCount > 0) red else gray}'>-${info.deleteCount}</font>" +
            "</html>"
        val changeLabel = JLabel(changeText)

        val acceptBtn = JButton("\u2713").apply {
            toolTipText = "接受"
            foreground = JBColor(0x2DA44E, 0x57AB5A)
            isContentAreaFilled = false
            border = BorderFactory.createEmptyBorder(2, 6, 2, 6)
            margin = Insets(0, 0, 0, 0)
            addActionListener { DiffBridge.acceptDiff(info.diffId) }
        }

        val rejectBtn = JButton("\u2715").apply {
            toolTipText = "拒绝"
            foreground = JBColor(0xCF222E, 0xF85149)
            isContentAreaFilled = false
            border = BorderFactory.createEmptyBorder(2, 6, 2, 6)
            margin = Insets(0, 0, 0, 0)
            addActionListener { DiffBridge.rejectDiff(info.diffId) }
        }

        gc.gridx = 0; gc.weightx = 1.0; gc.gridy = 0
        row.add(nameLabel, gc)

        gc.gridx = 1; gc.weightx = 0.0; gc.anchor = GridBagConstraints.CENTER
        gc.insets = JBUI.insets(0, 16, 0, 16)
        row.add(changeLabel, gc)

        gc.gridx = 2; gc.insets = JBUI.insets(0, 2, 0, 2)
        row.add(acceptBtn, gc)

        gc.gridx = 3; gc.insets = JBUI.insets(0, 2, 0, 0)
        row.add(rejectBtn, gc)

        row.maximumSize = java.awt.Dimension(Int.MAX_VALUE, row.preferredSize.height)
        return row
    }
}