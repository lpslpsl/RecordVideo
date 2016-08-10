/**
 * Copyright 2014 Jeroen Mols
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.inputmethodservice.Keyboard;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.example.rec.R;


public class VideoCaptureView extends FrameLayout implements OnClickListener {

    private static final int RECORDING = 111;//录制中。更新ui
    private ImageView mDeclineBtnIv;
    private ImageView mAcceptBtnIv;
    private ImageView mRecordBtnIv;
    private ImageView mContinueBtnIv;//暂停
    private ImageView mPauseBtnIv;//暂停
    private ImageView mLocalBtnIv;//本地选择
    private SurfaceView mSurfaceView;
    private ImageView mThumbnailIv;
    private TextView mTimerTv;
    private int timedurtion = 0;
    private Handler timeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECORDING:
                    int seconds =time;
                    int minutes = seconds / 60;
                    seconds = seconds % 60;
                    updateRecordingTime(seconds, minutes);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                                if (!pause) {
                                    Log.e("tag", "run: "+time+"\n"+timedurtion );
                                    if (time < timedurtion || timedurtion == 0) {
                                        time++;
                                        timeHandler.sendEmptyMessage(RECORDING);
                                    }
//                                    else if (time==timedurtion){//达到限定时间则结束录制
//                                      mRecordingInterface.onRecordButtonClicked();
//                                    }
                                }
                            } catch (InterruptedException mE) {
                                mE.printStackTrace();
                            }
                        }
                    }).start();
            }
        }
    };
    private RecordingButtonInterface mRecordingInterface;
    private boolean mShowTimer;

    public VideoCaptureView(Context context) {
        super(context);
        initialize(context);
    }

    public VideoCaptureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public VideoCaptureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    //初始化各个组件
    private void initialize(Context context) {
        final View videoCapture = View.inflate(context, R.layout.view_videocapture, this);
        mRecordBtnIv = (ImageView) videoCapture.findViewById(R.id.videocapture_recordbtn_iv);
        mAcceptBtnIv = (ImageView) videoCapture.findViewById(R.id.videocapture_acceptbtn_iv);
        mDeclineBtnIv = (ImageView) videoCapture.findViewById(R.id.videocapture_declinebtn_iv);
        mPauseBtnIv = (ImageView) videoCapture.findViewById(R.id.videocapture_pause_iv);
        mLocalBtnIv = (ImageView) videoCapture.findViewById(R.id.videocapture_local_iv);
        mContinueBtnIv = (ImageView) videoCapture.findViewById(R.id.videocapture_continue_iv);
        mRecordBtnIv.setOnClickListener(this);
        mAcceptBtnIv.setOnClickListener(this);
        mDeclineBtnIv.setOnClickListener(this);
        mLocalBtnIv.setOnClickListener(this);
        mPauseBtnIv.setOnClickListener(this);
        mContinueBtnIv.setOnClickListener(this);
        mThumbnailIv = (ImageView) videoCapture.findViewById(R.id.videocapture_preview_iv);
        mSurfaceView = (SurfaceView) videoCapture.findViewById(R.id.videocapture_preview_sv);
        mTimerTv = (TextView) videoCapture.findViewById(R.id.videocapture_timer_tv);
    }

    public void setRecordingButtonInterface(RecordingButtonInterface mBtnInterface) {
        this.mRecordingInterface = mBtnInterface;
    }

    public SurfaceHolder getPreviewSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    //更新录制的UI
    public void updateUINotRecording() {
        mRecordBtnIv.setSelected(false);
        mRecordBtnIv.setVisibility(View.VISIBLE);
        mAcceptBtnIv.setVisibility(View.GONE);
        mDeclineBtnIv.setVisibility(View.GONE);
        mThumbnailIv.setVisibility(View.GONE);
        mPauseBtnIv.setVisibility(GONE);
        mSurfaceView.setVisibility(View.VISIBLE);
    }

    private boolean pause;
    int time = 0;

    //正在录制的时候更新UI
    public void updateUIRecordingOngoing() {
        mRecordBtnIv.setSelected(true);
        mRecordBtnIv.setVisibility(View.VISIBLE);
        mAcceptBtnIv.setVisibility(View.GONE);
        mDeclineBtnIv.setVisibility(View.GONE);
        mThumbnailIv.setVisibility(View.GONE);
        mSurfaceView.setVisibility(View.VISIBLE);
        mPauseBtnIv.setVisibility(VISIBLE);
        mLocalBtnIv.setVisibility(GONE);
        if (mShowTimer) {
            mTimerTv.setVisibility(View.VISIBLE);
            updateRecordingTime(0, 0);
            timeHandler.sendEmptyMessage(RECORDING);
        }
    }

    //录制完成更新UI
    public void updateUIRecordingFinished(Bitmap videoThumbnail) {
        mRecordBtnIv.setVisibility(View.INVISIBLE);
        mAcceptBtnIv.setVisibility(View.VISIBLE);
        mDeclineBtnIv.setVisibility(View.VISIBLE);
        mThumbnailIv.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.GONE);
        mPauseBtnIv.setVisibility(GONE);
        mLocalBtnIv.setVisibility(GONE);
        if (videoThumbnail != null) {
            mThumbnailIv.setScaleType(ScaleType.CENTER_CROP);
            mThumbnailIv.setImageBitmap(videoThumbnail);
        }
        timeHandler.removeMessages(RECORDING);
    }

    //录制暂停
    public void updateUIRecordingPause(Bitmap videoThumbnail) {
        pause = true;
        mPauseBtnIv.setVisibility(GONE);
        mContinueBtnIv.setVisibility(VISIBLE);
        mThumbnailIv.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.GONE);
        mAcceptBtnIv.setVisibility(View.GONE);
        mDeclineBtnIv.setVisibility(View.GONE);
        mLocalBtnIv.setVisibility(GONE);
        mRecordBtnIv.setVisibility(View.VISIBLE);
        if (videoThumbnail != null) {
            mThumbnailIv.setScaleType(ScaleType.CENTER_CROP);
            mThumbnailIv.setImageBitmap(videoThumbnail);
        }
        if (mShowTimer) {
            mTimerTv.setVisibility(View.VISIBLE);}
    }

    //录制继续
    public void updateUIRecordingContinue() {
        pause = false;
        mPauseBtnIv.setVisibility(VISIBLE);
        mContinueBtnIv.setVisibility(GONE);
        mThumbnailIv.setVisibility(View.GONE);
        mSurfaceView.setVisibility(View.VISIBLE);
        mRecordBtnIv.setVisibility(VISIBLE);
        mDeclineBtnIv.setVisibility(GONE);
        mAcceptBtnIv.setVisibility(GONE);
        timeHandler.sendEmptyMessage(RECORDING);
    }

    @Override
    public void onClick(View v) {
        if (mRecordingInterface == null) return;
        if (v.getId() == mRecordBtnIv.getId()) {
            mRecordingInterface.onRecordButtonClicked();//录制
        } else if (v.getId() == mAcceptBtnIv.getId()) {
            mRecordingInterface.onAcceptButtonClicked();//结果勾选
        } else if (v.getId() == mDeclineBtnIv.getId()) {
            mRecordingInterface.onDeclineButtonClicked();//不要
        } else if (v.getId() == mLocalBtnIv.getId()) {
            mRecordingInterface.chooseLocal();//从本地选择
        } else if (v.getId() == mPauseBtnIv.getId()) {
            mRecordingInterface.onRecordPause();//暂停录制

        } else if (v.getId() == mContinueBtnIv.getId()) {
            mRecordingInterface.continueRecord();//继续录制视频

        }

    }

    public void showTimer(boolean showTimer) {
        this.mShowTimer = showTimer;
    }

    private void updateRecordingTime(int seconds, int minutes) {
        mTimerTv.setText(String.format("%02d", minutes) + ":" + String.format("%02d", seconds));
    }

    public int getTimedurtion() {
        return timedurtion;
    }

    /**
     * 设置录制限定的时长
     *
     * @param mTimedurtion 限定时间长度
     */
    public void setTimedurtion(int mTimedurtion) {
        timedurtion = mTimedurtion/1000;
    }

}
