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
import java.io.*

/**
 * Android res Xml string 多语言同步实现
 * 一个完整的 <string>xxxxx<string/> 中必须不准换行，同步原理为按行同步，方便送翻对比。
 *
 * 格式必须如下(标准 XML TAG)：
 * <resources>
 *     <string name="app_name">My Application</string>
 *     <string name="app_name1">@string/app_name</string>
 *
 *     <!-- xxxxxx -->
 *
 *     <array name="test">
 *         <item>xxix</item>
 *         <item>@string/app_name</item>
 *     </array>
 * </resources>
 */
class LanguageFormatSyncHandle {
    companion object {
        private const val TMP_FILE = ".tmp.xml"
    }

    private var mHandleStateListener: HandleStateListener? = null
    private val mSrcFromFile: File
    private val mTargetToFiles: MutableList<File>
    private var mLineIsComment: Boolean

    constructor(srcFromFile: PsiFile, languageFileName: List<PsiFile>) {
        this.mLineIsComment = false
        this.mSrcFromFile = psiFile2LocalFile(srcFromFile)
        this.mTargetToFiles = mutableListOf()
        languageFileName.forEach {
            this.mTargetToFiles.add(psiFile2LocalFile(it))
        }
    }

    fun setHandleStateListener(l: HandleStateListener) {
        this.mHandleStateListener = l
    }

    fun start() {
        safeStateStart("start from ${mSrcFromFile.absolutePath}")

        try {
            mTargetToFiles.forEach {
                sleepForUi(50)

                val targetToFile: File = it
                val tmpTargetToFile = File(targetToFile.parent + File.separator + TMP_FILE)
                val targetExistLines = getTargetFileExistStrLineMap(targetToFile)

                syncSrc2TargetTmpFile(this.mSrcFromFile, tmpTargetToFile, targetExistLines)
                safeStateProcess(targetToFile.absolutePath)
            }
            renameTmps2TargetFilesAndDelTmps(this.mTargetToFiles, this.mSrcFromFile.name)

            safeStateSuccess("success")
        } catch (e: IOException) {
            e.printStackTrace();
            deleteTmpFiles(this.mTargetToFiles);
            safeStateErrored("同步失败\n所有资源文件已恢复至同步前状态，请修复后试试咯！\n${e.message}")
        }
    }

    private fun getTargetFileExistStrLineMap(targetToFile: File): Map<String, String> {
        val targetToReader = BufferedReader(InputStreamReader(FileInputStream(targetToFile), "UTF-8"))
        val existLines = mutableMapOf<String, String>()
        var targetToLine: String? = targetToReader.readLine()
        while (targetToLine != null) {
            val targetToTrimLine: String = targetToLine.trim()
            if (targetToTrimLine.startsWith("<string")) {
                existLines.put(targetToTrimLine.split("\"")[1], targetToTrimLine)
            }

            targetToLine = targetToReader.readLine()
        }
        targetToReader.close()
        return existLines
    }

    private fun renameTmps2TargetFilesAndDelTmps(targetToFiles: List<File>, targetFileName: String) {
        targetToFiles.forEach {
            val dir: String = it.parent
            it.delete()
            val tmpFile = File(dir + File.separator + TMP_FILE)
            val finalFile = File(dir + File.separator + targetFileName)
            tmpFile.renameTo(finalFile)
        }
    }

    private fun deleteTmpFiles(targetToFiles: List<File>) {
        targetToFiles.forEach {
            val dir: String = it.parent
            it.delete()
            val tmpFile = File(dir + File.separator + TMP_FILE)
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
        }
    }

    private fun syncSrc2TargetTmpFile(srcFile: File, targetTmpFile: File,
                                      targetExistLines: Map<String, String>) {
        targetTmpFile.setWritable(true)

        val srcFromReader = BufferedReader(InputStreamReader(FileInputStream(srcFile), "UTF-8"))
        val tmpTargetToWriter = BufferedWriter(OutputStreamWriter(FileOutputStream(targetTmpFile.path), "UTF-8"))

        var srcFromLine: String? = srcFromReader.readLine()
        while (srcFromLine != null) {
            val srcFromTrimLine: String = srcFromLine.trim()
            val type: LineResType = getResLineType(srcFromTrimLine)
            when(type) {
                LineResType.OTHER,
                LineResType.SPACE,
                LineResType.STRING_ARRAY_BEGIN,
                LineResType.STRING_ARRAY_END,
                LineResType.STRING_ARRAY_ITEM,
                LineResType.COMMENT -> handleOtherElement(tmpTargetToWriter, type, srcFromTrimLine)
                LineResType.STRING -> handleStringElement(tmpTargetToWriter, srcFromTrimLine, srcFromTrimLine.split("\"")[1], targetExistLines)
            }

            srcFromLine = srcFromReader.readLine()
        }
        srcFromReader.close()
        tmpTargetToWriter.close()
    }

    private fun getResLineType(lineTrimedStr: String?): LineResType {
        if (lineTrimedStr == null) {
            return LineResType.OTHER
        }

        if (this.mLineIsComment) {
            if (lineTrimedStr.endsWith("-->")) {
                this.mLineIsComment = false
            }
            return LineResType.COMMENT
        }

        if (lineTrimedStr.startsWith("<!--")) {
            if (!lineTrimedStr.endsWith("-->")) {
                this.mLineIsComment = true
            }
            return LineResType.COMMENT
        }

        if (lineTrimedStr.startsWith("<string-array")) {
            return LineResType.STRING_ARRAY_BEGIN
        } else if (lineTrimedStr.endsWith("</string-array>")) {
            return LineResType.STRING_ARRAY_END
        } else if ((lineTrimedStr.startsWith("<item")) && (lineTrimedStr.endsWith("</item>"))) {
            return LineResType.STRING_ARRAY_ITEM
        } else if ((lineTrimedStr.startsWith("<string")) && (lineTrimedStr.endsWith("</string>"))) {
            return LineResType.STRING
        } else if (lineTrimedStr.equals("")) {
            return LineResType.SPACE
        }
        return LineResType.OTHER
    }

    private fun handleStringElement(tmpTargetToWriter: BufferedWriter, srcFromTrimLine: String,
                                    srcStrLineName: String, targetExistLines: Map<String, String>) {
        tmpTargetToWriter.write("\t")
        if (targetExistLines.containsKey(srcStrLineName)) {
            tmpTargetToWriter.write(targetExistLines[srcStrLineName])
        } else {
            tmpTargetToWriter.write(srcFromTrimLine)
        }
        tmpTargetToWriter.write("\r\n")
    }

    private fun handleOtherElement(tmpTargetToWriter: BufferedWriter, value: LineResType,
                                   srcFromTrimLine: String) {
        when(value) {
            LineResType.STRING_ARRAY_END -> tmpTargetToWriter.write("\t")
            LineResType.COMMENT -> tmpTargetToWriter.write("\t")
            LineResType.STRING_ARRAY_ITEM -> tmpTargetToWriter.write("\t\t")
        }
        tmpTargetToWriter.write(srcFromTrimLine)
        tmpTargetToWriter.write("\r\n")
    }

    private fun sleepForUi(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun safeStateStart(msg: String) {
        mHandleStateListener?.onStart(msg)
    }

    private fun safeStateErrored(msg: String) {
        mHandleStateListener?.onErrored(msg)
    }

    private fun safeStateProcess(msg: String) {
        mHandleStateListener?.onProcess(msg)
    }

    private fun safeStateSuccess(msg: String) {
        mHandleStateListener?.onSuccess(msg)
    }

    interface HandleStateListener {
        fun onStart(msg: String)
        fun onProcess(msg: String)
        fun onErrored(msg: String)
        fun onSuccess(msg: String)
    }

    enum class LineResType {
        STRING,
        STRING_ARRAY_BEGIN,
        STRING_ARRAY_END,
        STRING_ARRAY_ITEM,
        OTHER,
        COMMENT,
        SPACE
    }
}
