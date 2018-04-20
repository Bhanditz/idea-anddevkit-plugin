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

import cn.yan.anddevkit.config.AndroidDevKitSetting;
import cn.yan.anddevkit.inspection.AndroidStringXmlValueInspection;
import cn.yan.anddevkit.inspection.JavaInnerClassOutClassInspection;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Android res Xml string 值重复性静态检查切换开关 Action
 * Java 内部类访问外部 private 属性静态检查切换开关 Action
 * link p3c
 */
public class ToggleConfigInspectionAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = event.getProject();
        if (project == null) {
            return;
        }

        AndroidDevKitSetting setting = ServiceManager.getService(project, AndroidDevKitSetting.class);
        AndroidDevKitSetting.Config config = setting.getConfig();

        InspectionProfileImpl profile = (InspectionProfileImpl) InspectionProjectProfileManager.getInstance(project).getInspectionProfile();

        List<String> items = new ArrayList<>();
        items.add(AndroidStringXmlValueInspection.NAME);
        items.add(JavaInnerClassOutClassInspection.NAME);
        profile.removeScopes(items, AndroidStringXmlValueInspection.NAME, project);
        profile.removeScopes(items, JavaInnerClassOutClassInspection.NAME, project);
        if (config.autoCheckInspectionValues) {
            profile.enableToolsByDefault(items, project);
        } else {
            profile.disableToolByDefault(items, project);
        }
        profile.profileChanged();
        profile.scopesChanged();
        config.autoCheckInspectionValues = !config.autoCheckInspectionValues;
        setting.setConfig(config);
    }

    @Override
    public void update(AnActionEvent event) {
        final Project project = event.getProject();
        if (project == null) {
            return;
        }
        AndroidDevKitSetting setting = ServiceManager.getService(project, AndroidDevKitSetting.class);
        AndroidDevKitSetting.Config config = setting.getConfig();
        if (config.autoCheckInspectionValues) {
            event.getPresentation().setIcon(IconLoader.getIcon("/icons/string_value_check_closed.png"));
            event.getPresentation().setText("打开辅助静态检查");
        } else {
            event.getPresentation().setIcon(IconLoader.getIcon("/icons/string_value_check_opened.png"));
            event.getPresentation().setText("关闭辅助静态检查");
        }
    }
}
