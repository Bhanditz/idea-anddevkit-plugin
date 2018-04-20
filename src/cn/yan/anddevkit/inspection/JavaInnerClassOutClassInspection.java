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
package cn.yan.anddevkit.inspection;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Java 内部类引用外部类等 access$X() 方法静态检查纠正
 * 可纠正 Field 和 Method
 * link BaseInspection & FieldMayBeFinalInspection
 */
public class JavaInnerClassOutClassInspection extends AbstractBaseJavaLocalInspectionTool implements CleanupLocalInspectionTool {
    public static final String NAME = "JavaInnerClassOutClassInspection";

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return NAME;
    }

    @NotNull
    @Override
    public String getShortName() {
        return getDisplayName();
    }

    @Nls
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return getDisplayName();
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @NotNull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public boolean runForWholeFile() {
        return true;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new InnerClassOutFieldVisitor(holder);
    }

    static class MakeFieldProtectedFix extends InspectionGadgetsFix {

        private final String fieldName;

        private MakeFieldProtectedFix(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        @NotNull
        public String getName() {
            return "Make '" + fieldName + "' 'protected'";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "Make protected";
        }

        @Override
        protected void doFix(Project project, ProblemDescriptor descriptor) {
            final PsiElement element = descriptor.getPsiElement();
            final PsiField field;
            if (element instanceof PsiReferenceExpression) {
                final PsiReferenceExpression referenceExpression =
                        (PsiReferenceExpression)element;
                final PsiElement target = referenceExpression.resolve();
                if (!(target instanceof PsiField)) {
                    return;
                }
                field = (PsiField)target;
            }
            else {
                final PsiElement parent = element.getParent();
                if (!(parent instanceof PsiField)) {
                    return;
                }
                field = (PsiField)parent;
            }
            field.normalizeDeclaration();
            final PsiModifierList modifierList = field.getModifierList();
            if (modifierList == null) {
                return;
            }
            modifierList.setModifierProperty(PsiModifier.PROTECTED, true);
        }
    }

    static class MakeMethodProtectedFix extends InspectionGadgetsFix {

        private final String methodName;

        private MakeMethodProtectedFix(String methodName) {
            this.methodName = methodName;
        }

        @Override
        @NotNull
        public String getName() {
            return "Make '" + methodName + "' 'protected'";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "Make protected";
        }

        @Override
        protected void doFix(Project project, ProblemDescriptor descriptor) {
            final PsiElement element = descriptor.getPsiElement();
            final PsiMethod method;
            if (element instanceof PsiReferenceExpression) {
                final PsiReferenceExpression referenceExpression =
                        (PsiReferenceExpression)element;
                final PsiElement target = referenceExpression.resolve();
                if (!(target instanceof PsiMethod)) {
                    return;
                }
                method = (PsiMethod) target;
            }
            else {
                final PsiElement parent = element.getParent();
                if (!(parent instanceof PsiMethod)) {
                    return;
                }
                method = (PsiMethod) parent;
            }
            final PsiModifierList modifierList = method.getModifierList();
            if (modifierList == null) {
                return;
            }
            modifierList.setModifierProperty(PsiModifier.PROTECTED, true);
        }
    }

    private static class InnerClassOutFieldVisitor extends BaseInspectionVisitor {
        private ProblemsHolder holder;

        public InnerClassOutFieldVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitClass(PsiClass aClass) {
            super.visitClass(aClass);
            handleField(holder, aClass);
            handleMethod(holder, aClass);
        }

        private void handleField(ProblemsHolder holder, PsiClass aClass) {
            for (PsiField psiField : aClass.getFields()) {
                if (psiField != null && (!psiField.hasModifierProperty(PsiModifier.PRIVATE) ||
                        (psiField.hasModifierProperty(PsiModifier.FINAL) && psiField.hasModifierProperty(PsiModifier.STATIC)))) {
                    continue;
                }

                Query<PsiReference> references = ReferencesSearch.search(psiField, aClass.getUseScope());
                for (PsiReference psiReference: references) {
                    if (psiReference == null) {
                        continue;
                    }

                    PsiElement referenceElement = psiReference.getElement();
                    PsiElement referenceElementParent = referenceElement.getParent();
                    while (referenceElementParent != null) {
                        if (referenceElementParent instanceof PsiClass) {
                            String refClassName = ((PsiClass) referenceElementParent).getName();
                            if (!aClass.getName().equals(refClassName)) {
                                holder.registerProblem(InspectionManager.getInstance(aClass.getProject()).createProblemDescriptor(psiReference.getElement(),
                                        "Field <code>"+psiField.getName()+"</code> 建议设置成 'protected' 来减少方法数.", new MakeFieldProtectedFix(psiField.getName()), ProblemHighlightType.GENERIC_ERROR));
                            }
                            break;
                        }
                        referenceElementParent = referenceElementParent.getParent();
                    }
                }
            }
        }

        private void handleMethod(ProblemsHolder holder, PsiClass aClass) {
            for (PsiMethod psiMethod : aClass.getMethods()) {
                if (psiMethod != null && !psiMethod.hasModifierProperty(PsiModifier.PRIVATE)) {
                    continue;
                }

                Query<PsiReference> references = ReferencesSearch.search(psiMethod, aClass.getUseScope());
                for (PsiReference psiReference: references) {
                    if (psiReference == null) {
                        continue;
                    }

                    PsiElement referenceElement = psiReference.getElement();
                    PsiElement referenceElementParent = referenceElement.getParent();
                    while (referenceElementParent != null) {
                        if (referenceElementParent instanceof PsiClass) {
                            String refClassName = ((PsiClass) referenceElementParent).getName();
                            if (!aClass.getName().equals(refClassName)) {
                                holder.registerProblem(InspectionManager.getInstance(aClass.getProject()).createProblemDescriptor(psiReference.getElement(),
                                        "Field <code>"+psiMethod.getName()+"</code> 建议设置成 'protected' 来减少方法数.", new MakeMethodProtectedFix(psiMethod.getName()), ProblemHighlightType.GENERIC_ERROR));
                            }
                            break;
                        }
                        referenceElementParent = referenceElementParent.getParent();
                    }
                }
            }
        }
    }
}
