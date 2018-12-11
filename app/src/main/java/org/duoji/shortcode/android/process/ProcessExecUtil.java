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

package org.duoji.shortcode.android.process;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessExecUtil {
    private static final String TAG = "ProcessExecUtil";
    private static ProcessExecUtil mInstance;

    private String mInputInfo;
    private String mErrorInfo;

    private ProcessExecUtil() {

    }

    public static ProcessExecUtil getInstance() {
        if(mInstance == null) {
            synchronized (ProcessExecUtil.class) {
                if(mInstance == null) {
                    mInstance = new ProcessExecUtil();
                }
            }
        }
        return mInstance;
    }

    private String getProcessErrorInfo(Process process) {
        try {
            String data = "";
            BufferedReader ie = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String error;
            while ((error = ie.readLine()) != null
                    && !error.equals("null")) {
                data += error + "\n";
            }
            Log.d(TAG, data);
            return data;
        }catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return "";
    }

    private String getProcessInputInfo(Process process) {
        try {
            String data = "";
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = in.readLine()) != null && !line.equals("null")) {
                data += line + "\n";
            }
            Log.d(TAG, data);
            return data;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return "";
    }

    /**
     * get process exec cmd return info.
     * @return input info
     */
    public String getInputInfo() {
        return mInputInfo;
    }

    /**
     * get process exec cmd error info.
     * @return error info
     */
    public String getErrorInfo() {
        return mErrorInfo;
    }

    /**
     * process exec cmd in app, eg:
     * single cmd:"setenforce 0"
     * multi cmd:"setenforce 0\n chmod 777 /data/data/"
     * @param cmd exec cmd.
     * @param isNeedSu whether need su cmd.
     * @return true:process success;false:process fail.
     */
    public boolean processExec(String cmd, boolean isNeedSu) {
        try {
            Log.d(TAG, "ProcessExec cmd:" + cmd + ", isNeedSu:" + isNeedSu);
            Process proc;
            if(isNeedSu) {
                proc = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(proc.getOutputStream());
                if(os != null) {
                    os.writeBytes(cmd + "\nexit\n");
                    os.flush();
                }
            } else {
                proc = Runtime.getRuntime().exec(cmd);
            }
            String processErrorInfo = getProcessErrorInfo(proc);
            if(TextUtils.isEmpty(processErrorInfo)) {
                mErrorInfo = "";
                mInputInfo = getProcessInputInfo(proc);
                return true;
            } else {
                mErrorInfo = processErrorInfo;
                mInputInfo = "";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
