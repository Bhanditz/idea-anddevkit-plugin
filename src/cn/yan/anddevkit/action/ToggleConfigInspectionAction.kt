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

import cn.yan.anddevkit.config.AndroidDevKitSetting
import cn.yan.anddevkit.inspection.AndroidStringXmlValueInspection
import cn.yan.anddevkit.inspection.JavaInnerClassOutClassInspection
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.profile.codeInspection.InspectionProjectProfileManager

/**
 * Android res Xml string 值重复性静态检查切换开关 Action
 * Java 内部类访问外部 private 属性静态检查切换开关 Action
 * link p3c
 */
class ToggleConfigInspectionAction: AnAction() {
    override fun actionPerformed(event: AnActionEvent?) {
        val project: Project? = event?.project
        val setting: AndroidDevKitSetting? = project?.let { ServiceManager.getService(it, AndroidDevKitSetting::class.java) }
        val config: AndroidDevKitSetting.Config = setting?.config ?: return

        val profile: InspectionProfileImpl = InspectionProjectProfileManager.getInstance(project).inspectionProfile as InspectionProfileImpl
        profile.apply {
            removeScope(AndroidStringXmlValueInspection.NAME, AndroidStringXmlValueInspection.NAME, project)
            removeScope(JavaInnerClassOutClassInspection.NAME, JavaInnerClassOutClassInspection.NAME, project)
            if (config.autoCheckInspectionValues) {
                enableToolsByDefault(listOf(AndroidStringXmlValueInspection.NAME), project)
                enableToolsByDefault(listOf(JavaInnerClassOutClassInspection.NAME), project)
            } else {
                disableToolByDefault(listOf(AndroidStringXmlValueInspection.NAME), project)
                disableToolByDefault(listOf(JavaInnerClassOutClassInspection.NAME), project)
            }
            profileChanged()
            scopesChanged()
        }
        config.autoCheckInspectionValues = !config.autoCheckInspectionValues
        setting.config = config
    }

    override fun update(event: AnActionEvent?) {
        super.update(event)
        val project: Project = event?.project ?: return
        val setting: AndroidDevKitSetting = ServiceManager.getService(project, AndroidDevKitSetting::class.java)
        val config: AndroidDevKitSetting.Config = setting.config

        event.presentation.apply {
            if (config.autoCheckInspectionValues) {
                icon = IconLoader.getIcon("/icons/string_value_check_closed.png")
                text = "打开辅助静态检查"
            } else {
                icon = IconLoader.getIcon("/icons/string_value_check_opened.png")
                text = "关闭辅助静态检查"
            }
        }
    }
}
