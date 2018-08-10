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
package cn.yan.anddevkit.action

import cn.yan.anddevkit.common.*
import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile

/**
 * Android 多语言同步及格式化 Action
 */
class I18nLanguageFormatSyncAction: AnAction() {

    override fun update(event: AnActionEvent?) {
        super.update(event)
        event?.presentation?.isEnabledAndVisible ?: isXmlResourceFile(event?.getData(CommonDataKeys.PSI_FILE)!!)
    }

    override fun actionPerformed(event: AnActionEvent?) {
        FileDocumentManager.getInstance().saveAllDocuments()

        val project: Project = event?.project ?: return
        val psiFile: PsiFile = event?.getData(CommonDataKeys.PSI_FILE) ?: return

        val targetLanguageFiles: List<PsiFile> = findResDirLanguageFiles(psiFile)

        val state = isValidResFormatFile(psiFile2LocalFile(psiFile))
        if (state == Result.VALID) {
            startSync(project, psiFile, targetLanguageFiles)
        } else {
            Messages.showErrorDialog(String.format(Message.I18N_SYNC_ERROR_CONTENT, psiFile.name, state.toString()), Message.I18N_SYNC_ERROR_TITLE)
        }
    }

    private fun startSync(project: Project, psiFile: PsiFile, targetLanguageFiles: List<PsiFile>) {
        val module: Module = ModuleUtilCore.findModuleForFile(psiFile.virtualFile, project) ?: return

        val sourcePath: String = psiFile.virtualFile.path

        val result: Int = Messages.showOkCancelDialog(
                String.format(Message.I18N_SYNC_CONFIRM, sourcePath, module.name, targetLanguageFiles.size),
                CommonBundle.getWarningTitle(), Messages.getWarningIcon())
        if (result != 0) {
            return
        }

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    val indicator: ProgressIndicator = ProgressManager.getInstance().progressIndicator
                    indicator.fraction = 0.5
                    indicator.isIndeterminate = true

                    val handle = LanguageFormatSyncHandle(psiFile, targetLanguageFiles)
                    handle.setHandleStateListener(object: LanguageFormatSyncHandle.HandleStateListener {
                        override fun onStart(msg: String) {
                            indicator.text = "start"
                            indicator.text2 = msg
                        }

                        override fun onProcess(msg: String) {
                            indicator.text = "process"
                            indicator.text2 = msg
                        }

                        override fun onErrored(msg: String) {
                            indicator.text = "error"
                            indicator.text2 = msg
                            if (msg != null) {
                                showTipsDialogInvokeLater(msg)
                            }
                        }

                        override fun onSuccess(msg: String) {
                            indicator.text = "success"
                            indicator.text2 = msg
//                        FileDocumentManager.getInstance().saveAllDocuments()
                            showTipsDialogInvokeLater(Message.I18N_SYNC_OK_CONFIRM)
                        }
                    })
                    handle.start()
                },
                String.format(Message.I18N_SYNC_ING_TIPS_TITLE, project.name, module.name), false, project)
    }

    private fun showTipsDialogInvokeLater(msg: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showOkCancelDialog(msg, CommonBundle.getWarningTitle(), Messages.getWarningIcon())
        }
    }
}
