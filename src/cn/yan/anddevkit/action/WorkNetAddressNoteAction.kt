/**
 * MIT License
 *
 * Copyright (c) 2018 yanbo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package cn.yan.anddevkit.action

import cn.yan.anddevkit.common.Message
import cn.yan.anddevkit.config.AndroidDevKitSetting
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.awt.Rectangle
import javax.swing.*
/**
 * 常用网站列表 Action
 */
class WorkNetAddressNoteAction: AnAction() {
    companion object {
        private const val DISP_GROUP_ID = "Notice"
    }

    override fun update(event: AnActionEvent?) {
        super.update(event)
        val project: Project? = event?.project
        event?.presentation?.isEnabledAndVisible ?: (project != null)
    }

    override fun actionPerformed(event: AnActionEvent?) {
        if (event == null) {
            return
        }

        val setting: AndroidDevKitSetting = AndroidDevKitSetting.getInstance()
        val map: Map<String, String> = setting.config.workNetUrlMap
        if (map.isEmpty()) {
            return Notifications.Bus.notify(Notification(DISP_GROUP_ID,
                    Message.NOTE_NOTICE_TITLE, Message.NOTE_NOTICE_CONTENT,
                    NotificationType.WARNING))
        }

        var dlm = DefaultListModel<String>()
        map.forEach { key, value ->
            dlm.addElement(key)
        }

        val jList: JList<String> = JList()
        jList.apply {
            visibleRowCount = 10
            fixedCellHeight = 30
            fixedCellWidth = 300
            dragEnabled = false
            bounds = Rectangle(10, 0, 10, 0)
            model = dlm
            selectionMode = ListSelectionModel.SINGLE_INTERVAL_SELECTION
        }

        event.project?.let {
            JBPopupFactory.getInstance()
                .createListPopupBuilder(jList)
                .setTitle(Message.NOTE_LIST_TITLE)
                .setResizable(true)
                .setItemChoosenCallback{
                    val url: String? = map[jList.selectedValue]
                    url?.let { BrowserUtil.open(it) }
                }
                .createPopup()
                .showCenteredInCurrentWindow(it)
        }
    }
}
