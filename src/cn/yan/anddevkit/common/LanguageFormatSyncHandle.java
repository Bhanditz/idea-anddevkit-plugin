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

import com.intellij.psi.PsiFile;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
public class LanguageFormatSyncHandle {
    private static final String TMP_FILE = ".tmp.xml";

    private HandleStateListener mHandleStateListener;
    private File mSrcFromFile;
    private List<File> mTargetToFiles;
    private boolean mLineIsComment = false;

    public LanguageFormatSyncHandle(PsiFile srcFromFile, List<PsiFile> languageFileName) {
        this.mSrcFromFile = FileUtils.psiFile2LocalFile(srcFromFile);
        List<File> fileList = new ArrayList<>();
        for (PsiFile psiFile : languageFileName) {
            fileList.add(FileUtils.psiFile2LocalFile(psiFile));
        }
        this.mTargetToFiles = fileList;
    }

    public void setHandleStateListener(HandleStateListener l) {
        mHandleStateListener = l;
    }

    public void start() {
        safeStateStart("start from "+ mSrcFromFile.getAbsolutePath());

        try {
            for (int i = 0; i < this.mTargetToFiles.size(); i++) {
                sleepForUi(50);

                File targetToFile = this.mTargetToFiles.get(i);
                File tmpTargetToFile = new File(targetToFile.getParent() + File.separator + TMP_FILE);

                Map<String, String> targetExistLines = getTargetFileExistStrLineMap(targetToFile);
                syncSrc2TargetTmpFile(this.mSrcFromFile, tmpTargetToFile, targetExistLines);

                safeStateProcess(targetToFile.getAbsolutePath());
            }

            renameTmps2TargetFilesAndDelTmps(this.mTargetToFiles, this.mSrcFromFile.getName());

            safeStateSuccess("success");
        } catch (IOException e) {
            e.printStackTrace();
            deleteTmpFiles(this.mTargetToFiles);
            safeStateErrored("同步失败\n所有资源文件已恢复至同步前状态，请修复后试试咯！\n"+e.getMessage());
        }
    }

    private Map<String, String> getTargetFileExistStrLineMap(File targetToFile) throws IOException {
        BufferedReader targetToReader = new BufferedReader(new InputStreamReader(new FileInputStream(targetToFile), "UTF-8"));
        Map<String, String> existLines = new LinkedHashMap<>();
        String targetToLine;
        while ((targetToLine = targetToReader.readLine()) != null) {
            String targetToTrimLine = targetToLine.trim();
            String[] lineSplits = targetToTrimLine.split("\"");
            if (targetToTrimLine.startsWith("<string")) {
                existLines.put(lineSplits[1], targetToTrimLine);
            }
        }
        targetToReader.close();
        return existLines;
    }

    private void renameTmps2TargetFilesAndDelTmps(List<File> targetToFiles, String targetFileName) {
        for (int i = 0; i < targetToFiles.size(); i++) {
            String dir = targetToFiles.get(i).getParent();
            targetToFiles.get(i).delete();
            File tmpFile = new File(dir + File.separator + TMP_FILE);
            File finalFile = new File(dir + File.separator + targetFileName);
            tmpFile.renameTo(finalFile);
        }
    }

    private void deleteTmpFiles(List<File> targetToFiles) {
        for (int i = 0; i < targetToFiles.size(); i++) {
            String dir = targetToFiles.get(i).getParent();
            targetToFiles.get(i).delete();
            File tmpFile = new File(dir + File.separator + TMP_FILE);
            if (tmpFile != null && tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    private void syncSrc2TargetTmpFile(File srcFile, File targetTmpFile,
                                       Map<String, String> targetExistLines) throws IOException {
        targetTmpFile.setWritable(true);

        BufferedReader srcFromReader = new BufferedReader(new InputStreamReader(new FileInputStream(srcFile), "UTF-8"));
        BufferedWriter tmpTargetToWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetTmpFile.getPath()), "UTF-8"));

        String srcFromLine;
        while ((srcFromLine = srcFromReader.readLine()) != null) {
            String srcFromTrimLine = srcFromLine.trim();
            LineResType type = getResLineType(srcFromTrimLine);
            switch (type) {
                case OTHER:
                case SPACE:
                case STRING_ARRAY_BEGIN:
                case STRING_ARRAY_END:
                case STRING_ARRAY_ITEM:
                case COMMENT:
                    handleOtherElement(tmpTargetToWriter, type, srcFromTrimLine);
                    break;
                case STRING:
                    String[] sourceTrimArrayString = srcFromTrimLine.split("\"");
                    handleStringElement(tmpTargetToWriter, srcFromTrimLine, sourceTrimArrayString[1], targetExistLines);
                    break;
            }
        }

        srcFromReader.close();
        tmpTargetToWriter.close();
    }

    private LineResType getResLineType(String lineTrimedStr) {
        if (lineTrimedStr == null) {
            return LineResType.OTHER;
        }

        if (this.mLineIsComment) {
            if (lineTrimedStr.endsWith("-->")) {
                this.mLineIsComment = false;
            }
            return LineResType.COMMENT;
        }

        if (lineTrimedStr.startsWith("<!--")) {
            if (!lineTrimedStr.endsWith("-->")) {
                this.mLineIsComment = true;
            }
            return LineResType.COMMENT;
        }

        if (lineTrimedStr.startsWith("<string-array")) {
            return LineResType.STRING_ARRAY_BEGIN;
        } else if (lineTrimedStr.endsWith("</string-array>")) {
            return LineResType.STRING_ARRAY_END;
        } else if ((lineTrimedStr.startsWith("<item")) && (lineTrimedStr.endsWith("</item>"))) {
            return LineResType.STRING_ARRAY_ITEM;
        } else if ((lineTrimedStr.startsWith("<string")) && (lineTrimedStr.endsWith("</string>"))) {
            return LineResType.STRING;
        } else if (lineTrimedStr.equals("")) {
            return LineResType.SPACE;
        }
        return LineResType.OTHER;
    }

    private void handleStringElement(BufferedWriter tmpTargetToWriter, String srcFromTrimLine,
                                     String srcStrLineName, Map<String, String> targetExistLines) throws IOException {
        tmpTargetToWriter.write("\t");
        if (targetExistLines.containsKey(srcStrLineName)) {
            tmpTargetToWriter.write(targetExistLines.get(srcStrLineName));
        } else {
            tmpTargetToWriter.write(srcFromTrimLine);
        }
        tmpTargetToWriter.write("\r\n");
    }

    private void handleOtherElement(BufferedWriter tmpTargetToWriter, LineResType value,
                                    String srcFromTrimLine) throws IOException {
        switch (value) {
            case STRING_ARRAY_END:
            case COMMENT:
                tmpTargetToWriter.write("\t");
                break;
            case STRING_ARRAY_ITEM:
                tmpTargetToWriter.write("\t\t");
                break;
        }
        tmpTargetToWriter.write(srcFromTrimLine);
        tmpTargetToWriter.write("\r\n");
    }

    private void sleepForUi(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void safeStateStart(String msg) {
        if (mHandleStateListener != null) {
            mHandleStateListener.onStart(msg);
        }
    }

    private void safeStateErrored(String msg) {
        if (mHandleStateListener != null) {
            mHandleStateListener.onErrored(msg);
        }
    }

    private void safeStateProcess(String msg) {
        if (mHandleStateListener != null) {
            mHandleStateListener.onProcess(msg);
        }
    }

    private void safeStateSuccess(String msg) {
        if (mHandleStateListener != null) {
            mHandleStateListener.onSuccess(msg);
        }
    }

    public interface HandleStateListener {
        public void onStart(String msg);
        public void onProcess(String msg);
        public void onErrored(String msg);
        public void onSuccess(String msg);
    }

    private enum LineResType {
        STRING,
        STRING_ARRAY_BEGIN,
        STRING_ARRAY_END,
        STRING_ARRAY_ITEM,
        OTHER,
        COMMENT,
        SPACE
    }
}
