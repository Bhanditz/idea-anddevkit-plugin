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

/**
 * 消息常量
 */
class Message {
    companion object {
        const val I18N_SYNC_CONFIRM = "确定要将 %s 语言文件\n同步到 %s 的其他 %d 个国家吗？\n注意：被同步的资源文件<string>标签必须只能以行为单位，不允许换行！"
        const val I18N_SYNC_OK_CONFIRM = "恭喜你送翻前多语言同步接锅成功！"
        const val I18N_SYNC_ING_TIPS_TITLE = "%s 中 %s 模块多语言同步"

        const val NOTE_LIST_TITLE = "我的常用工作网址"
        const val NOTE_NOTICE_TITLE = "Android DevKit 常用工作网站不可用"
        const val NOTE_NOTICE_CONTENT = "当前无可用配置项！\n请先在设置中配置后再使用！"
    }
}
