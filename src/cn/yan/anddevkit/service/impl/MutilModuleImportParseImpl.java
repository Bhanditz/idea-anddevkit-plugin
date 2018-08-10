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
package cn.yan.anddevkit.service.impl;

import cn.yan.anddevkit.service.MutilModuleImportParse;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多 module 互斥引入静态检查扫描全局service实现
 * @Deprecated
 */
public class MutilModuleImportParseImpl implements MutilModuleImportParse {
    private static final Object LOCK = new Object();

    private Map<String, List<String>> mScriptMap = new ConcurrentHashMap<String, List<String>>();


    @Override
    public Map<String, List<String>> getScriptMap() {
        synchronized (LOCK) {
            return mScriptMap;
        }
    }

    @Override
    public void startUpdateScriptMap(File file) {
        synchronized (LOCK) {
            MutilModuleImportParaser paraser = new MutilModuleImportParaser();
            try {
                Map<String, List<String>> map = paraser.s2m(file);
                if (map != null) {
                    mScriptMap.clear();
                    mScriptMap.putAll(map);
                }
            } catch (Exception e) {
                e.printStackTrace();
                mScriptMap.clear();
            }
        }
    }

    @Override
    public void clearScriptMap() {
        synchronized (LOCK) {
            mScriptMap.clear();
        }
    }
}
