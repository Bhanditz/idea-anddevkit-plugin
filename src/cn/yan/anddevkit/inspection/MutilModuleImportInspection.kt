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
package cn.yan.anddevkit.inspection

import cn.yan.anddevkit.service.MutilModuleImportParse
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtilCore
import com.siyeh.ig.BaseInspectionVisitor

/**
 * 多 module 互斥引入静态检查
 * @Deprecated
 */
class MutilModuleImportInspection : AbstractBaseJavaLocalInspectionTool(), CleanupLocalInspectionTool {
    companion object {
        const val NAME = "MutilModuleImportInspection"
    }

    override fun getDisplayName(): String {
        return NAME
    }

    override fun getShortName(): String {
        return displayName
    }

    override fun getGroupDisplayName(): String {
        return displayName
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.ERROR
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return ImportVisitor(holder)
    }

    /**
     * Link as ImportsAreUsedVisitor
     */
    private class ImportVisitor: BaseInspectionVisitor {
        private val holder: ProblemsHolder
        private val dexMap: Map<String, List<String>>

        constructor(holder: ProblemsHolder): super() {
            this.holder = holder
            this.dexMap = ServiceManager.getService(MutilModuleImportParse::class.java).scriptMap
        }

        override fun visitImportList(list: PsiImportList) {
            super.visitImportList(list)
            val module = ModuleUtil.findModuleForFile(list.containingFile.virtualFile, list.project) ?: return

            list.importStatements.forEach { element->
                val curFilePath = list.containingFile.virtualFile.path
                val curModulePath = module.moduleFilePath.substring (0,
                        module.moduleFilePath.lastIndexOf(module.name+".iml"))
                val importSourcePath = PsiUtilCore.getVirtualFile(element.resolve())?.path
                if (importSourcePath == null) {
                    return@forEach
                }
                checkModuleImport(curModulePath, curFilePath, importSourcePath, element)
            }
        }

        fun checkModuleImport(curModulePath: String, curFilePath: String, importSourcePath: String, psiImportStatement: PsiImportStatement) {
            //TODO ...
        }
    }
}
