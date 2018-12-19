/*
 * Copyright (C) 2018 DUOJI Android Short Code
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.duoji.shortcode.android.view;

import java.util.HashMap;
import java.util.Map;

public class CheckDoubleClick {
    private static Map<String, Long> mViewLastClickTimes = new HashMap<String, Long>();

    private static int mMinClickDelayTime = 1000;

    public static void setmMinClickDelayTime(int time) {
        mMinClickDelayTime = time;
    }

    /**
     * check if the double click,support multi view. each view's key is this func be transfer in which class and which line.
     * @return true:fast double click; false: not fast double click.
     */
    public static boolean isFastDoubleClick() {
        StackTraceElement stack = new Throwable().getStackTrace()[1];
        String key = stack.getFileName() + stack.getLineNumber();

        Long lastClickTime = mViewLastClickTimes.get(key);
        if(lastClickTime == null) {
            lastClickTime = 0L;
        }
        long currentTime = System.currentTimeMillis();
        mViewLastClickTimes.put(key, currentTime);
        if((currentTime - lastClickTime) > mMinClickDelayTime) {
            return false;
        }
        return true;
    }
}
