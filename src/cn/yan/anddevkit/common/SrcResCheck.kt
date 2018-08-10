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

import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlDocument
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
/**
 * 资源源文件校验
 */

fun isXmlResourceFile(psiFile: PsiFile): Boolean {
    if (psiFile == null) {
        return false
    }
    var isRes = false
    if ("xml" == psiFile.fileType.defaultExtension.toLowerCase()) {
        val xmlDocument: XmlDocument = psiFile.firstChild as XmlDocument
        isRes = ("resources" == xmlDocument.rootTag!!.name)
    }
    return isRes
}

fun isValidResFormatFile(src: File): Result {
    val targetToReader = BufferedReader(InputStreamReader(FileInputStream(src), "UTF-8"))
    var targetToLine: String? = targetToReader.readLine()

    /**
     * 注释中可以包含 <string-array> 元素，正式内容不可以
     */
    var isComment = false
    while (targetToLine != null) {
        val targetToTrimLine: String = targetToLine.trim()
        if (targetToTrimLine.startsWith("<!--") && !targetToTrimLine.endsWith("-->")) {
            isComment = true
        } else if (targetToTrimLine.endsWith("-->")) {
            isComment = false
        }

        if (!isComment && targetToTrimLine.startsWith("<string-array")) {
            return Result.CONTAINS_STRING_ARRAY
        }

        targetToLine = targetToReader.readLine()
    }
    targetToReader.close()
    return Result.VALID
}

enum class Result(private val msg: String) {
    VALID(Message.MSG_VALID),
    CONTAINS_STRING_ARRAY(Message.MSG_CONTAINS_STRING_ARRAY),
    UNKNOW(Message.MSG_UNKNOW);

    override fun toString() = msg
}