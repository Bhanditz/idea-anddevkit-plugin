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

import cn.yan.anddevkit.common.isXmlResourceFile
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.UnfairLocalInspectionTool
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import com.intellij.ui.DocumentAdapter
import com.intellij.xml.util.XmlRefCountHolder
import javax.swing.*
import javax.swing.event.DocumentEvent
import java.awt.*

/**
 * Android res Xml string 值重复性静态检查及纠正建议工具
 * strings.xml 中 string tag 的字符串值重复性静态检测及纠正
 * link XmlDuplicatedIdInspection
 */
class AndroidStringXmlValueInspection: XmlSuppressableInspectionTool(), UnfairLocalInspectionTool {
    companion object {
        const val NAME = "AndroidStringXmlValueInspection"
    }

    private val duplications : MutableMap<String, String> = mutableMapOf()

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

    override fun createOptionsPanel(): JComponent? {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        val checkedClasses = JTextField("TODO")
        checkedClasses.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(p0: DocumentEvent?) {
                //TODO("not implemented")
            }
        })
        panel.add(checkedClasses)
        return panel
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!isXmlResourceFile(holder.file)) {
            return super.buildVisitor(holder, isOnTheFly)
        }
        return ValueXmlElementVisitor(holder)
    }

    private inner class ValueXmlElementVisitor: XmlElementVisitor {
        private val holder: ProblemsHolder

        constructor(holder: ProblemsHolder): super() {
            this.holder = holder
            duplications.clear()
        }

        override fun visitXmlText(text: XmlText?) {
            if (text == null || text.textRange.isEmpty) {
                return
            }

            val file: PsiFile = text.containingFile as? XmlFile ?: return

            val baseFile: PsiFile? = PsiUtilCore.getTemplateLanguageFile(file)
            if (baseFile != file && (baseFile !is XmlFile)) {
                return
            }

            XmlRefCountHolder.getRefCountHolder(file as XmlFile) ?: return

            val parent: PsiElement = text.parent as? XmlTag ?: return

            val tag: XmlTag = parent as XmlTag

            if (tag.name != "string" || text.text.trim().startsWith("@string/")) {
                return
            }

            if (duplications.containsKey(text.text)) {
                val linkNameQuickFix = LinkNameQuickFix()
                linkNameQuickFix.setReferenceName(duplications[text.text]!!)
                holder.registerProblem(InspectionManager.getInstance(text.project).createProblemDescriptor(text,
                        "该字符串与 ${duplications[text.text]} 重复啦～", linkNameQuickFix, ProblemHighlightType.GENERIC_ERROR))
                return
            }
            duplications.put(text.text, tag.getAttribute("name")!!.value!!)
        }
    }

    private class LinkNameQuickFix: LocalQuickFix {
        private lateinit var referenceName: String

        fun setReferenceName(referenceName: String) {
            this.referenceName = referenceName
        }

        override fun getFamilyName(): String {
            return name
        }

        override fun getName(): String {
            return "使用 @string/$referenceName 引用已存在的值"
        }

        override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
            if (referenceName == null || referenceName.isEmpty()) {
                return
            }

            val xmlText: XmlText = problemDescriptor.psiElement as XmlText
            xmlText.value = "@string/$referenceName"
        }
    }
}

