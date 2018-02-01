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
package cn.yan.anddevkit.action;

import cn.yan.anddevkit.common.FileUtils;
import cn.yan.anddevkit.common.LanguageFormatSyncHandle;
import cn.yan.anddevkit.common.Message;
import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import java.util.List;

/**
 * Android 多语言同步及格式化 Action
 */
public class I18nLanguageFormatSyncAction extends AnAction {
    @Override
    public void update(AnActionEvent event) {
        super.update(event);
        event.getPresentation().setEnabledAndVisible(FileUtils.isXmlResourceFile(event.getData(CommonDataKeys.PSI_FILE)));
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        if (event == null) {
            return;
        }
        FileDocumentManager.getInstance().saveAllDocuments();

        final Project project = event.getProject();
        final PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }

        final Module module = ModuleUtilCore.findModuleForFile(psiFile.getVirtualFile(), project);

        String sourcePath = psiFile.getVirtualFile().getPath();
        List<PsiFile> targetLanguageFiles = FileUtils.findResDirLanguageFiles(psiFile);

        final int result = Messages.showOkCancelDialog(
                String.format(Message.I18N_SYNC_CONFIRM, sourcePath, module.getName(), targetLanguageFiles.size()),
                CommonBundle.getWarningTitle(), Messages.getWarningIcon());
        if (result != 0) {
            return;
        }

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
                new Runnable() {
                    public void run() {
                        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                        indicator.setFraction(0.5);
                        indicator.setIndeterminate(true);
                        LanguageFormatSyncHandle handle = new LanguageFormatSyncHandle(psiFile, targetLanguageFiles);
                        handle.setHandleStateListener(new LanguageFormatSyncHandle.HandleStateListener() {
                            @Override
                            public void onStart(String msg) {
                                indicator.setText("start");
                                indicator.setText2(msg);
                            }

                            @Override
                            public void onProcess(String msg) {
                                indicator.setText("process");
                                indicator.setText2(msg);
                            }

                            @Override
                            public void onErrored(String msg) {
                                indicator.setText("error");
                                indicator.setText2(msg);
                                showTipsDialogInvokeLater(msg);
                            }

                            @Override
                            public void onSuccess(String msg) {
                                indicator.setText("success");
                                indicator.setText2(msg);
//                                FileDocumentManager.getInstance().saveAllDocuments();
                                showTipsDialogInvokeLater(Message.I18N_SYNC_OK_CONFIRM);
                            }
                        });
                        handle.start();
                    }
                },
                String.format(Message.I18N_SYNC_ING_TIPS_TITLE, project.getName(), module.getName()), false, project);
    }

    private void showTipsDialogInvokeLater(String msg) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Messages.showOkCancelDialog(msg, CommonBundle.getWarningTitle(), Messages.getWarningIcon());
            }
        });
    }
}
