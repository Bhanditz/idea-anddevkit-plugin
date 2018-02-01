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
package cn.yan.anddevkit.action;

import cn.yan.anddevkit.common.Message;
import cn.yan.anddevkit.config.AndroidDevKitSetting;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import javax.swing.*;
import java.util.Iterator;
import java.util.Map;
/**
 * 常用网站列表 Action
 */
public class WorkNetAddressNoteAction extends AnAction {
    private static final String DISP_GROUP_ID = "Notice";

    @Override
    public void update(AnActionEvent event) {
        final Project project = event.getProject();
        event.getPresentation().setEnabledAndVisible(project != null);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        AndroidDevKitSetting setting = AndroidDevKitSetting.getInstance();
        Map<String, String> map = setting.getConfig().workNetUrlMap;
        if (map.isEmpty()) {
            Notifications.Bus.notify(new Notification(DISP_GROUP_ID,
                    Message.NOTE_NOTICE_TITLE, Message.NOTE_NOTICE_CONTENT,
                    NotificationType.WARNING));
            return;
        }

        DefaultListModel dlm = new DefaultListModel();
        Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            dlm.addElement(iterator.next().getKey());
        }

        JList<String> jList = new JList<>();
        jList.setVisibleRowCount(10);
        jList.setFixedCellHeight(30);
        jList.setFixedCellWidth(300);
        jList.setDragEnabled(false);
        jList.setBounds(10, 0, 10, 0);
        jList.setModel(dlm);
        jList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        JBPopupFactory.getInstance()
                .createListPopupBuilder(jList)
                .setTitle(Message.NOTE_LIST_TITLE)
                .setResizable(true)
                .setItemChoosenCallback(new Runnable() {
                    @Override
                    public void run() {
                        String url = map.get(jList.getSelectedValue());
                        BrowserUtil.open(url);
                    }
                })
                .createPopup()
                .showCenteredInCurrentWindow(event.getProject());
    }
}
