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
package cn.yan.anddevkit.config;

import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 插件设置配置相关面板
 */
public class AndroidDevKitConfigurable implements SearchableConfigurable {
    private static final String ID = "Android DevKit";

    private JPanel parentContent;
    private JLabel labelWorkAddrName;
    private JLabel labelWorkAddrUrl;

    private JList<String> listWorkAddr;
    private JTextField inputWorkAddrName;
    private JTextField inputWorkAddrUrl;
    private JButton btnWorkAddrAdd;
    private JButton btnWorkAddrDel;

    private boolean changed = false;

    private AndroidDevKitSetting setting = AndroidDevKitSetting.Companion.getInstance();

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return this.getId();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        AndroidDevKitSetting.Config config = setting.getConfig();
        Map<String, String> configMap = config.getWorkNetUrlMap();

        DefaultListModel dlm = new DefaultListModel();
        refreshList(dlm, configMap);

        btnWorkAddrAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                changed = true;

                String name = inputWorkAddrName.getText();
                String url = inputWorkAddrUrl.getText();
                if (name == null || name.length() == 0) {
                    inputWorkAddrName.requestFocus();
                    return;
                }

                if (url == null || url.length() == 0) {
                    inputWorkAddrUrl.requestFocus();
                    return;
                }

                configMap.put(name, url);
                refreshList(dlm, configMap);
                setting.setConfig(config);
            }
        });
        btnWorkAddrDel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                changed = true;

                List<String> selects = listWorkAddr.getSelectedValuesList();
                if (selects != null && selects.size() > 0) {
                    for (String key : selects) {
                        configMap.remove(key);
                    }
                }
                refreshList(dlm, configMap);

                setting.setConfig(config);
            }
        });
        return parentContent;
    }

    private void refreshList(DefaultListModel dlm, Map<String, String> configMap) {
        dlm.clear();
        Iterator<Map.Entry<String, String>> iterator = configMap.entrySet().iterator();
        while (iterator.hasNext()) {
            dlm.addElement(iterator.next().getKey());
        }
        listWorkAddr.setModel(dlm);
    }

    @Override
    public boolean isModified() {
        return changed;
    }



    @Override
    public void apply() {
        //empty
    }

    @Override
    public void reset() {
        setting.setConfig(setting.getConfig());
    }
}
