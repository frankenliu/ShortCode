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

package org.duoji.shortcode.android.record.util;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class SaveVoiceUtil {
    private static final String TAG = "SaveVoiceUtil";
    private static SaveVoiceUtil mInstance;

    private HandlerThread mSaveThread;
    private Handler mSaveHandle;

    private static String mRecordDir = "duoji/record";
    private ByteArrayOutputStream mByteBuffer;
    private FileOutputStream mOutputStream;

    private SaveVoiceUtil() {
        mByteBuffer = new ByteArrayOutputStream(5 * 1024);
        mSaveThread = new HandlerThread("save_voice");
        mSaveThread.start();
        mSaveHandle = new Handler(mSaveThread.getLooper(),new SaveVoiceCallback());
    }

    private boolean checkAndCreateDir(String strFolder) {
        File file = new File(strFolder);
        if (!file.exists()) {
            if (file.mkdirs()) {
                return true;
            } else
                return false;
        }
        return true;
    }

    private String getCurrentTime() {
        Calendar c = Calendar.getInstance();//可以对每个时间域单独修改
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int date = c.get(Calendar.DATE);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);
        StringBuffer sb = new StringBuffer();
        sb.append(year);
        sb.append("-");
        sb.append(month);
        sb.append("-");
        sb.append(date);
        sb.append("-");
        sb.append(hour);
        sb.append("-");
        sb.append(minute);
        sb.append("-");
        sb.append(second);
        return sb.toString();
    }

    public static SaveVoiceUtil getInstance() {
        if(mInstance == null) {
            synchronized (SaveVoiceUtil.class) {
                if(mInstance == null) {
                    mInstance = new SaveVoiceUtil();
                }
            }
        }
        return mInstance;
    }

    /**
     * 设置存储目录,强制存储在sd卡下面.具体位置为sd卡根目录+dir.
     * 默认为:duoji/record
     * example: dir->duoji/record; 存储目录为/sdcard/duoji/record
     * @param dir 目录名
     */
    public void setSaveDir(String dir) {
        if(!TextUtils.isEmpty(dir)) {
            mRecordDir = dir;
        }
    }

    /**
     * 设置文件名前缀, 文件默认保存路径为/sdcard/${RECORD_DIR}.
     * @param prefix 文件名前缀 eg: keywords -> 2018-7-6-11-27-53_keywords.pcm
     */
    public void createFile(String prefix) {
        Message msg = mSaveHandle.obtainMessage(0x01);
        msg.obj = prefix;
        mSaveHandle.sendMessage(msg);
    }

    public void writeFile(byte[] data) {
        if(data == null || data.length <= 0) {
            return;
        }
        Message msg = mSaveHandle.obtainMessage(0x02);
        Bundle bundle = new Bundle();
        bundle.putByteArray("data", data);
        msg.setData(bundle);
        mSaveHandle.sendMessage(msg);
    }

    public void closeFile() {
        Message msg = mSaveHandle.obtainMessage(0x03);
        mSaveHandle.sendMessageDelayed(msg, 500);
    }

    class SaveVoiceCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0x01:
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + mRecordDir;
                    if(checkAndCreateDir(path)) {
                        try {
                            mOutputStream = new FileOutputStream(path + "/" + getCurrentTime() + "_" + (String)msg.obj + ".pcm");
                            mByteBuffer.reset();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 0x02:
                    Bundle bundle = msg.getData();
                    byte[] data = bundle.getByteArray("data");
                    try {
                        mByteBuffer.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;
                case 0x03:
                    if(mOutputStream != null) {
                        try {
                            mOutputStream.write(mByteBuffer.toByteArray());
                            mByteBuffer.reset();
                            mOutputStream.close();
                            mOutputStream = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
            return false;
        }
    }
}
