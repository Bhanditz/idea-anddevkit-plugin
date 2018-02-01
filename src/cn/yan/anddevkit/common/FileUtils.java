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
package cn.yan.anddevkit.common;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlDocument;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具类
 */
public class FileUtils {
    public static File psiFile2LocalFile(PsiFile psiFile) {
        if (psiFile == null) {
            return null;
        }
        return new File(psiFile.getVirtualFile().getPath());
    }

    public static File virtualFile2LocalFile(VirtualFile virtualFile) {
        if (virtualFile == null) {
            return null;
        }
        return new File(virtualFile.getPath());
    }

    public static boolean isXmlResourceFile(PsiFile psiFile) {
        if (psiFile == null) {
            return false;
        }
        boolean isRes = false;
        if (psiFile != null && "xml".equalsIgnoreCase(psiFile.getFileType().getDefaultExtension())) {
            XmlDocument xmlDocument = (XmlDocument) psiFile.getFirstChild();
            isRes = ("resources".equals(xmlDocument.getRootTag().getName()));
        }
        return isRes;
    }

    public static List<PsiFile> findResDirLanguageFiles(PsiFile targetFile) {
        List<PsiFile> list = new ArrayList<>();
        if (targetFile == null) {
            return list;
        }

        String targetFileName = targetFile.getName();
        VirtualFile targetFileParent = targetFile.getVirtualFile().getParent().getParent();
        if (targetFileParent == null || !targetFileParent.exists() || !targetFileParent.isDirectory()) {
            return list;
        }

        VirtualFile[] parentContentFiles = targetFileParent.getChildren();
        if (parentContentFiles == null || parentContentFiles.length <= 0) {
            return list;
        }

        for (VirtualFile virtualFile : parentContentFiles) {
            String resChildDirName = virtualFile.getName();
            if (!resChildDirName.matches("values-[a-z][a-z].*")) {
                continue;
            }

            VirtualFile languageFile = virtualFile.findChild(targetFileName);
            if (languageFile == null || !languageFile.exists() || !languageFile.isWritable()) {
                continue;
            }

            PsiFile psiFile = PsiManager.getInstance(targetFile.getProject()).findFile(languageFile);
            if (psiFile != null) {
                list.add(psiFile);
            }
        }
        return list;
    }
}
