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
package cn.yan.anddevkit.contributor

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiFormatUtil
import com.intellij.util.ProcessingContext

/**
 * 增强型 Java Class Reference Element popwindow 提示，附带路径
 * JavaPsiClassReferenceElement
 */
class PathInfoCompletionContributor: CompletionContributor {
    constructor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(JavaTokenType.IDENTIFIER), PathInfoProvider())
    }

    class PathInfoProvider : CompletionProvider<CompletionParameters>() {

        public override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val unfilteredCompletions: LinkedHashSet<CompletionResult> = result.runRemainingContributors(parameters, false)
            unfilteredCompletions.forEach { completionResult->
                if (completionResult.lookupElement is JavaPsiClassReferenceElement) {
                    val look = completionResult.lookupElement as JavaPsiClassReferenceElement
                    val element = CsJavaPsiClassReferenceElement(look.`object`)
                    result.addElement(element)
                } else {
                    result.addElement(completionResult.lookupElement)
                }
            }
            result.stopHere()
        }
    }

    class CsJavaPsiClassReferenceElement : JavaPsiClassReferenceElement {
        val psiClass: PsiClass

        constructor(psiClass: PsiClass) : super(psiClass) {
            this.psiClass = psiClass
        }

        override fun renderElement(presentation: LookupElementPresentation) {
            val packageName = PsiFormatUtil.getPackageDisplayName(psiClass)
            val pathName = psiClass?.containingFile?.virtualFile?.path ?: ""
            renderClassItem(presentation, this, `object`, false, " ($packageName) $pathName", substitutor)
        }
    }
}