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
package cn.yan.anddevkit.common

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlDocument
import java.io.File
/**
 * 工具
 */

fun psiFile2LocalFile(psiFile: PsiFile): File {
    return File(psiFile.virtualFile.path)
}

fun virtualFile2LocalFile(virtualFile: VirtualFile): File {
    return File(virtualFile.path)
}

fun isXmlResourceFile(psiFile: PsiFile): Boolean {
    var isRes = false
    if ("xml" == psiFile.fileType.defaultExtension.toLowerCase()) {
        val xmlDocument: XmlDocument = psiFile.firstChild as XmlDocument
        isRes = ("resources" == xmlDocument.rootTag!!.name)
    }
    return isRes
}

fun findResDirLanguageFiles(targetFile: PsiFile): List<PsiFile> {
    val list: MutableList<PsiFile> = mutableListOf()
    val targetFileName = targetFile.name
    val targetFileParent: VirtualFile = targetFile.virtualFile.parent.parent
    if (!targetFileParent.exists() || !targetFileParent.isDirectory) {
        return list
    }

    val parentContentFiles: Array<VirtualFile>  = targetFileParent.children
    if (parentContentFiles.isEmpty()) {
        return list
    }

    parentContentFiles.forEach {
        val resChildDirName: String  = it.name
        if (!resChildDirName.matches(Regex("values-[a-z][a-z].*"))) {
            return@forEach
        }

        val languageFile: VirtualFile? = it.findChild(targetFileName)
        if (languageFile == null || !languageFile.exists() || !languageFile.isWritable) {
            return@forEach
        }

        val psiFile: PsiFile? = PsiManager.getInstance(targetFile.project).findFile(languageFile)
        if (psiFile != null) {
            list.add(psiFile)
        }
    }
    return list
}
