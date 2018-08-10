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
package cn.yan.anddevkit.service

import cn.yan.anddevkit.service.impl.MutilModuleImportParaser
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File

/**
 * 多 module 互斥引入静态检查扫描触发器
 * @Deprecated
 */
class MutilModuleImportParseProject : ProjectComponent {
    val scriptFileListener = ScriptFileListener()
    val project: Project

    constructor(project: Project) {
        this.project = project
    }

    override fun getComponentName(): String {
        return "cn.yan.android.devkit.MutilModuleImportParseProject"
    }

    override fun projectOpened() {
        println("projectOpened")
        val parse = ServiceManager.getService(MutilModuleImportParse::class.java)
        val projectManager = ProjectManager.getInstance()
        val allProjects = projectManager.openProjects
        allProjects.forEach { project ->
            val scriptPath = project.basePath + File.separator + MutilModuleImportParaser.TARGET_MODULE_NAME +
                                        File.separator + MutilModuleImportParaser.TARGET_FILE
            val scriptFile = File(scriptPath)
            if (scriptFile.exists()) {
                parse.startUpdateScriptMap(scriptFile)
            }
            VirtualFileManager.getInstance().addVirtualFileListener(scriptFileListener)
        }
    }

    override fun projectClosed() {
        println("projectClosed")
        val parse = ServiceManager.getService(MutilModuleImportParse::class.java)
        parse.clearScriptMap()
        VirtualFileManager.getInstance().removeVirtualFileListener(scriptFileListener)
    }

    override fun initComponent() {
    }

    override fun disposeComponent() {
    }

    class ScriptFileListener : VirtualFileListener {
        override fun contentsChanged(event: VirtualFileEvent) {
            if (event.fileName == MutilModuleImportParaser.TARGET_FILE) {
                val parse = ServiceManager.getService(MutilModuleImportParse::class.java)
                parse.startUpdateScriptMap(File(event.file.path))
            }
        }
    }
}