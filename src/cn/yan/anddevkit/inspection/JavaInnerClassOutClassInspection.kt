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

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Query
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix

/**
 * Java 内部类引用外部类等 access$X() 方法静态检查纠正
 * 可纠正 Field 和 Method
 * link BaseInspection & FieldMayBeFinalInspection
 */
class JavaInnerClassOutClassInspection: AbstractBaseJavaLocalInspectionTool(), CleanupLocalInspectionTool {
    companion object {
        const val NAME = "JavaInnerClassOutClassInspection"
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

//    override fun runForWholeFile(): Boolean {
//        return true
//    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return InnerClassOutFieldVisitor(holder)
    }

    private class MakeFieldProtectedFix: InspectionGadgetsFix {
        private val fieldName: String

        constructor(fieldName: String): super() {
            this.fieldName = fieldName
        }

        override fun getName(): String {
            return "Make '$fieldName' 'protected'"
        }

        override fun getFamilyName(): String {
            return "Make protected"
        }

        override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
            if (project == null || descriptor == null) {
                return
            }

            val element: PsiElement = descriptor.psiElement
            var field: PsiField
            if (element is PsiReferenceExpression) {
                val referenceExpression: PsiReferenceExpression = element
                val target: PsiElement = referenceExpression.resolve() as? PsiField ?: return
                field = target as PsiField
            } else {
                val parent: PsiElement = element.parent as? PsiField ?: return
                field = parent as PsiField
            }
            field.normalizeDeclaration()
            val modifierList: PsiModifierList = field.modifierList ?: return
            modifierList.setModifierProperty(PsiModifier.PROTECTED, true)
        }
    }

    private class MakeMethodProtectedFix: InspectionGadgetsFix {
        private val methodName: String

        constructor(methodName: String): super() {
            this.methodName = methodName
        }

        override fun getName(): String {
            return "Make '$methodName' 'protected'"
        }

        override fun getFamilyName(): String {
            return "Make protected"
        }

        override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
            if (project == null || descriptor == null) {
                return
            }

            val element: PsiElement = descriptor.psiElement
            val method: PsiMethod
            if (element is PsiReferenceExpression) {
                val referenceExpression: PsiReferenceExpression = element
                val target: PsiElement = referenceExpression.resolve() as? PsiMethod ?: return
                method = target as PsiMethod
            } else {
                val parent: PsiElement = element.parent as? PsiMethod ?: return
                method = parent as PsiMethod
            }
            val modifierList: PsiModifierList = method.modifierList
            modifierList.setModifierProperty(PsiModifier.PROTECTED, true)
        }
    }

    private class InnerClassOutFieldVisitor: BaseInspectionVisitor {
        private val holder: ProblemsHolder

        constructor(holder: ProblemsHolder): super() {
            this.holder = holder
        }

        override fun visitClass(aClass: PsiClass?) {
            super.visitClass(aClass)
            if (aClass == null) {
                return
            }

            handleField(holder, aClass)
            handleMethod(holder, aClass)
        }

        private fun handleField(holder: ProblemsHolder, aClass: PsiClass) {
            aClass.fields.forEach { psiField: PsiField ->
                if (!psiField.hasModifierProperty(PsiModifier.PRIVATE) ||
                        (psiField.hasModifierProperty(PsiModifier.FINAL) && psiField.hasModifierProperty(PsiModifier.STATIC))) {
                    return@forEach
                }

                val references: Query<PsiReference> = ReferencesSearch.search(psiField, aClass.useScope)
                references.forEach { psiReference: PsiReference ->
                    val referenceElement: PsiElement = psiReference.element
                    var referenceElementParent: PsiElement? = referenceElement.parent
                    while (referenceElementParent != null) {
                        if (referenceElementParent is PsiClass) {
                            val refClassName: String? = referenceElementParent.name
                            if (!aClass.name.equals(refClassName)) {
                                holder.registerProblem(InspectionManager.getInstance(aClass.project).createProblemDescriptor(psiReference.element,
                                        "Field <code>${psiField.name}</code> 建议设置成 'protected' 来减少方法数.", MakeFieldProtectedFix(psiField.name), ProblemHighlightType.GENERIC_ERROR))
                            }
                            break
                        }
                        referenceElementParent = referenceElementParent.parent
                    }
                }
            }
        }

        private fun handleMethod(holder: ProblemsHolder, aClass: PsiClass) {
            aClass.methods.forEach { psiMethod: PsiMethod ->
                if (!psiMethod.hasModifierProperty(PsiModifier.PRIVATE)) {
                    return@forEach
                }

                val references: Query<PsiReference> = ReferencesSearch.search(psiMethod, aClass.useScope)
                references.forEach { psiReference: PsiReference ->
                    val referenceElement: PsiElement = psiReference.element
                    var referenceElementParent: PsiElement? = referenceElement.parent
                    while (referenceElementParent != null) {
                        if (referenceElementParent is PsiClass) {
                            val refClassName: String? = referenceElementParent.name
                            if (!aClass.name.equals(refClassName)) {
                                holder.registerProblem(InspectionManager.getInstance(aClass.project).createProblemDescriptor(psiReference.element,
                                        "Method <code>${psiMethod.name}</code> 建议设置成 'protected' 来减少方法数.", MakeMethodProtectedFix(psiMethod.name), ProblemHighlightType.GENERIC_ERROR))
                            }
                            break
                        }
                        referenceElementParent = referenceElementParent.parent
                    }
                }
            }
        }
    }
}
