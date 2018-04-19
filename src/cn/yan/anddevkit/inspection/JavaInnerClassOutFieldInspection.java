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
import com.intellij.codeInspection.CleanupLocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.psiutils.ClassUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Java 内部类引用外部类等 access$X() 方法静态检查纠正
 */
public class JavaInnerClassOutFieldInspection extends BaseInspection implements CleanupLocalInspectionTool {
    public static final String NAME = "JavaInnerClassOutFieldInspection";

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

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @NotNull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... objects) {
        return "Field <code>#ref</code> 建议设置成 'protected' 来减少方法数.#loc";
    }

    @Override
    public InspectionGadgetsFix buildFix(Object... infos) {
        return MakeFieldProtectedFix.buildFixUnconditional((PsiField) infos[0]);
    }

    @Override
    public boolean runForWholeFile() {
        return true;
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new InnerClassOutFieldVisitor();
    }

    static class MakeFieldProtectedFix extends InspectionGadgetsFix {

        private final String fieldName;

        private MakeFieldProtectedFix(String fieldName) {
            this.fieldName = fieldName;
        }

        @NotNull
        public static InspectionGadgetsFix buildFixUnconditional(PsiField field) {
            return new MakeFieldProtectedFix(field.getName());
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

    private static class InnerClassOutFieldVisitor extends BaseInspectionVisitor {
        @Override
        public void visitClass(PsiClass aClass) {
            super.visitClass(aClass);
//            boolean isInnerClass = ClassUtils.isInnerClass(aClass);
            for (PsiField psiField : aClass.getAllFields()) {
                if (psiField != null && !psiField.hasModifierProperty(PsiModifier.PRIVATE)) {
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
                                PsiElement refElement = psiReference.getElement();
                                String fieldName = psiField.getName();
                                String refExpersion = refElement.getText();
                                int start = refExpersion.indexOf(fieldName);
                                int end = start + fieldName.length();
                                registerErrorAtOffset(psiReference.getElement(), start, end, psiField);
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
