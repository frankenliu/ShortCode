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

package org.duoji.shortcode.android.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

public class VoiceRecord {
    private static final String TAG = "VoiceRecord";
    private AudioRecord mAudioRecord = null;
    private RecordingRunnable mRecordThread;

    // 录音相关参数设置
    public static final int PARAM_KEY_SET_CHANNEL = 0;
    public static final int PARAM_KEY_SET_AUDIO_SOURCE = 1;
    public static final int PARAM_KEY_SET_RECORD_BUFFER_SIZE = 2;
    public static final int PARAM_KEY_SET_SAMPLE_RATE = 3;
    public static final int PARAM_KEY_SET_ENCODING_BITS = 4;
    // 录音相关参数默认设置
    /**
     * 通道数
     */
    private int mChannels = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 声源
     */
    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    /**
     * 录音线程buffer
     */
    private int mRecordBufferSize = 1536;
    /**
     * 采样率
     */
    private int mSampleRate = 16000;
    /**
     * 采样位数
     */
    private int mAudioEncodingBits = AudioFormat.ENCODING_PCM_16BIT;
    /**
     * 读取音频数据的同步对象
     */
    protected Object syncObj = new Object();
    private RecordListener mListener;

    public VoiceRecord(RecordListener listener) {
        mListener = listener;
    }

    /**
     * 设置录音参数
     * @param key
     * @param value
     */
    public void setParam(int key, int value){
        switch (key) {
            case PARAM_KEY_SET_AUDIO_SOURCE:
                mAudioSource = value;
                break;
            case PARAM_KEY_SET_CHANNEL:
                mChannels = value;
                break;
            case PARAM_KEY_SET_RECORD_BUFFER_SIZE:
                mRecordBufferSize = value;
                break;
            case PARAM_KEY_SET_SAMPLE_RATE:
                mSampleRate = value;
                break;
            case PARAM_KEY_SET_ENCODING_BITS:
                mAudioEncodingBits = value;
                break;
        }
    }

    /**
     * 获取录音参数
     * @param key
     * @return
     */
    public int getParamValue(int key) {
        switch (key) {
            case PARAM_KEY_SET_AUDIO_SOURCE:
                return mAudioSource;
            case PARAM_KEY_SET_CHANNEL:
                return mChannels;
            case PARAM_KEY_SET_RECORD_BUFFER_SIZE:
                return mRecordBufferSize;
            case PARAM_KEY_SET_SAMPLE_RATE:
                return mSampleRate;
            case PARAM_KEY_SET_ENCODING_BITS:
                return mAudioEncodingBits;
        }
        return -1;
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        try {
            if (mRecordThread != null) {
                new Thread(mRecordThread).start();
            } else {
                mRecordThread = new RecordingRunnable();
                new Thread(mRecordThread).start();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            if(mListener != null) {
                mListener.onRecordingFailed();
            }
            return;
        }
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        if(mRecordThread != null) {
            Log.d(TAG, "stopRecord");
            mRecordThread.stop();
        }
    }

    public interface RecordListener {

        void onRecordingStart();
        /**
         * 录音过程中调用，可能会调用多次
         * @param data 录音数据
         * @param sizeInBytes 成功返回数据大小，失败返回错误码。
         * @see {@link AudioRecord#read(byte[], int, int)}.
         */
        void onRecording(byte[] data, int sizeInBytes);

        /**
         * AudioRecord 创建失败
         */
        void onRecordCreateError();

        /**
         * 录音过程中的异常处理
         */
        void onRecordingFailed();

        void onRecordingEnd();
    }

    /**
     * 录音线程
     */
    private class RecordingRunnable implements Runnable {
        /** 是否结束识别 */
        private boolean isEnd = false;
        /** 是否退出录音线程 */
        private boolean isExit = false;

        private boolean init() {
            try {
                int mixRecordBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannels, mAudioEncodingBits);
                if (mixRecordBufferSize < 0) {
                    if(mListener != null) {
                        mListener.onRecordCreateError();
                    }
                    return false;
                } else if (mAudioRecord == null) {
                    if(mRecordBufferSize < mixRecordBufferSize) { // 当外部设置的录音buffer小于底层最小反馈buffer, 强制修改为底层最小反馈buffer
                        mRecordBufferSize = mixRecordBufferSize;
                    }
                    mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannels, mAudioEncodingBits, mRecordBufferSize);
                    Log.d(TAG, "mAudioSource:" + mAudioSource + ", mSampleRate:" + mSampleRate + ", mChannels:" + mChannels + ", mAudioEncodingBits:" + mAudioEncodingBits + ", mRecordBufferSize:" + mRecordBufferSize);
                }

                if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    if(mListener != null) {
                        mListener.onRecordCreateError();
                    }
                    return false;
                }
            } catch (Exception e) {
                if(mListener != null) {
                    mListener.onRecordCreateError();
                }
                return false;
            }
            Log.d(TAG, "init Recording");
            return true;
        }

        private boolean startup() {
            isEnd = false;
            isExit = false;
            if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                try {
                    Log.d(TAG, "start Recording");
                    mAudioRecord.startRecording();
                } catch (IllegalStateException e) {
                    Log.d(TAG, "start Recording failed");
                    if(mListener != null) {
                        mListener.onRecordCreateError();
                    }
                    return false;
                }
            } else {
                if(mListener != null) {
                    mListener.onRecordCreateError();
                }
                return false;
            }
            return true;
        }

        public void stop() {
            synchronized (syncObj) {
                isEnd = true;
                Log.d(TAG, "stop");
            }
        }

        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            if (init() != true) {
                return;
            }

            try {
                // 每次读取音频数据大小
                byte[] pcmBuffer = new byte[mRecordBufferSize];
                // 实际读取音频数据大小
                int pcmBufferSize;
                if (startup()) {
                    if(mListener != null) {
                        mListener.onRecordingStart();
                    }
                    while (!isExit) {
                        pcmBufferSize = mAudioRecord.read(pcmBuffer, 0, mRecordBufferSize);
                        if (pcmBufferSize == AudioRecord.ERROR_INVALID_OPERATION) {
                            throw new IllegalStateException("read() return AudioRecord.ERROR_INVALID_OPERATION");
                        } else if (pcmBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                            throw new IllegalStateException("read() return AudioRecord.ERROR_BAD_VALUE");
                        }
                        synchronized (syncObj) {
                            if(mListener != null) {
                                mListener.onRecording(pcmBuffer, pcmBufferSize);
                            }

                            if (isEnd == true) {
                                isExit = true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            if(mListener != null) {
                mListener.onRecordingEnd();
            }
            if (mAudioRecord != null) {
                synchronized (syncObj) {
                    try {
                        if (AudioRecord.STATE_INITIALIZED == mAudioRecord.getState()) {
                            mAudioRecord.stop();
                            mAudioRecord.release();
                            mAudioRecord = null;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
            Log.d(TAG, "RecordingRunnable is exit");
        }
    }
}
