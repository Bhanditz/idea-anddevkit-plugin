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

import cn.yan.anddevkit.common.FileUtils;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.UnfairLocalInspectionTool;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.ui.DocumentAdapter;
import com.intellij.xml.util.XmlRefCountHolder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Android res Xml string 值重复性静态检查及纠正建议工具
 * strings.xml 中 string tag 的字符串值重复性静态检测及纠正
 * link XmlDuplicatedIdInspection
 */
public class AndroidStringXmlValueInspection extends XmlSuppressableInspectionTool implements UnfairLocalInspectionTool {
    public static final String NAME = "AndroidStringXmlValueInspection";

    private Map<String, String> duplications;

    public AndroidStringXmlValueInspection() {
        super();
        this.duplications = new HashMap<>();
    }

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

    @Nullable
    @Override
    public JComponent createOptionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JTextField checkedClasses = new JTextField("TODO");
        checkedClasses.getDocument().addDocumentListener(new DocumentAdapter() {
            public void textChanged(DocumentEvent event) {
                //TODO
            }
        });
        panel.add(checkedClasses);
        return panel;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!FileUtils.isXmlResourceFile(holder.getFile())) {
            return super.buildVisitor(holder, isOnTheFly);
        }
        return new ValueXmlElementVisitor(holder);
    }

    private class ValueXmlElementVisitor extends XmlElementVisitor {
        private ProblemsHolder holder;



        public ValueXmlElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
            duplications.clear();
        }

        @Override
        public void visitXmlText(XmlText text) {
            if (text.getTextRange().isEmpty()) {
                return;
            }

            final PsiFile file = text.getContainingFile();
            if (!(file instanceof XmlFile)) {
                return;
            }

            PsiFile baseFile = PsiUtilCore.getTemplateLanguageFile(file);
            if (baseFile != file && !(baseFile instanceof XmlFile)) {
                return;
            }

            final XmlRefCountHolder refHolder = XmlRefCountHolder.getRefCountHolder((XmlFile) file);
            if (refHolder == null) {
                return;
            }

            final PsiElement parent = text.getParent();
            if (!(parent instanceof XmlTag)) {
                return;
            }

            final XmlTag tag = (XmlTag) parent;
            if (tag == null) {
                return;
            }

            if (!tag.getName().equals("string") || text.getText().trim().startsWith("@string/")) {
                return;
            }

            if (duplications.containsKey(text.getText())) {
                LinkNameQuickFix linkNameQuickFix = new LinkNameQuickFix();
                linkNameQuickFix.setReferenceName(duplications.get(text.getText()));
                holder.registerProblem(InspectionManager.getInstance(text.getProject()).createProblemDescriptor(text,
                        "该字符串与 "+duplications.get(text.getText())+" 重复啦～", linkNameQuickFix, ProblemHighlightType.ERROR));
                return;
            }
            duplications.put(text.getText(), tag.getAttribute("name").getValue());
        }
    }

    private static class LinkNameQuickFix implements LocalQuickFix {
        private String referenceName;

        public void setReferenceName(String referenceName) {
            this.referenceName = referenceName;
        }

        @Nls
        @NotNull
        @Override
        public String getFamilyName() {
            return getName();
        }

        @Nls
        @NotNull
        @Override
        public String getName() {
            return "使用 @string/"+referenceName+" 引用已存在的值";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
            if (referenceName == null || referenceName.length() == 0) {
                return;
            }

            XmlText xmlText = (XmlText) problemDescriptor.getPsiElement();
            xmlText.setValue("@string/"+referenceName);
        }
    }
}
