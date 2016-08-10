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

package com.example.recorder;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.view.Surface;
import android.view.SurfaceHolder;


import java.io.IOException;

import com.example.CLog;
import com.example.VideoFile;
import com.example.camera.CameraWrapper;
import com.example.camera.OpenCameraException;
import com.example.camera.PrepareCameraException;
import com.example.camera.RecordingSize;
import com.example.configuration.CaptureConfiguration;
import com.example.preview.CapturePreview;
import com.example.preview.CapturePreviewInterface;

public class VideoRecorder implements OnInfoListener, CapturePreviewInterface {

    private CameraWrapper mCameraWrapper;//相机包装类
    private final Surface mPreviewSurface;
    private CapturePreview mVideoCapturePreview;//自定义的view

    private final CaptureConfiguration mCaptureConfiguration;//对相机的配置
    private VideoFile mVideoFile;//录制的文件

    private MediaRecorder mRecorder;//录制的类
    private boolean mRecording = false;//是否正在录制
    private final VideoRecorderInterface mRecorderInterface;//视频录制状态变更接口
    private SurfaceHolder mSurfaceHolder;

    public VideoRecorder(VideoRecorderInterface recorderInterface, CaptureConfiguration captureConfiguration, VideoFile videoFile,
                         CameraWrapper cameraWrapper, SurfaceHolder previewHolder) {
        mCaptureConfiguration = captureConfiguration;
        mRecorderInterface = recorderInterface;
        mVideoFile = videoFile;
        mCameraWrapper = cameraWrapper;
        mPreviewSurface = previewHolder.getSurface();
        mSurfaceHolder = previewHolder;
        initializeCameraAndPreview(previewHolder);
    }

    //初始化相机并开启预览
    protected void initializeCameraAndPreview(SurfaceHolder previewHolder) {
        try {
            mCameraWrapper.openCamera();
        } catch (final OpenCameraException e) {
            e.printStackTrace();
            mRecorderInterface.onRecordingFailed(e.getMessage());
            return;
        }
//创建自定义view
        mVideoCapturePreview = new CapturePreview(this, mCameraWrapper, previewHolder);
    }

    /**
     * 切换状态，开始录制和停止录制
     *
     * @throws AlreadyUsedException
     */
    public void toggleRecording() throws AlreadyUsedException {
        if (mCameraWrapper == null) {
            throw new AlreadyUsedException();
        }

        if (isRecording()) {
            stopRecording(null);
        } else {
            startRecording();
        }
    }

    /**
     * 准备开始录制视频
     */
    public void startRecording() {
        mRecording = false;

        if (!initRecorder()) return;
        if (!prepareRecorder()) return;
        if (!startRecorder()) return;

        mRecording = true;
        mRecorderInterface.onRecordingStarted();
        CLog.d(CLog.RECORDER, "Successfully started recording - outputfile: " + mVideoFile.getFullPath());
    }

    /**
     * 再次准备开始录制视频
     *
     * @param mVideoFilex
     * @param mCameraWrapper
     */
    public void restartRecording(VideoFile mVideoFilex, CameraWrapper mCameraWrapper) {
        mRecording = false;
        this.mCameraWrapper = mCameraWrapper;
        try {
            mCameraWrapper.openCamera();
        } catch (final OpenCameraException e) {
            e.printStackTrace();
            mRecorderInterface.onRecordingFailed(e.getMessage());
            return;
        }
        setVideoFile(mVideoFilex);
        if (!initRecorder()) return;
        if (!prepareRecorder()) return;
        if (!startRecorder()) return;

        mRecording = true;
        mRecorderInterface.onRecordingStarted();
        CLog.d(CLog.RECORDER, "Successfully started recording - outputfile: " + mVideoFile.getFullPath());
    }

    /**
     * 停止录制视频
     *
     * @param message
     */
    public void stopRecording(String message) {
        if (!isRecording()) return;

        try {
            getMediaRecorder().stop();
            mRecorderInterface.onRecordingSuccess();
            CLog.d(CLog.RECORDER, "Successfully stopped recording - outputfile: " + mVideoFile.getFullPath());
        } catch (final RuntimeException e) {
            CLog.d(CLog.RECORDER, "Failed to stop recording");
        }

        mRecording = false;
        mRecorderInterface.onRecordingStopped(message);
    }

    /**
     * 开始录制前的初始化
     *
     * @return 失败则结束
     */
    private boolean initRecorder() {
        try {
            mCameraWrapper.prepareCameraForRecording();
        } catch (final PrepareCameraException e) {
            e.printStackTrace();
            mRecorderInterface.onRecordingFailed("Unable to record video");
            CLog.e(CLog.RECORDER, "Failed to initialize recorder - " + e.toString());
            return false;
        }

        setMediaRecorder(new MediaRecorder());
        configureMediaRecorder(getMediaRecorder(), mCameraWrapper.getCamera());

        CLog.d(CLog.RECORDER, "MediaRecorder successfully initialized");
        return true;
    }

    //加载录制的配置
    @SuppressWarnings("deprecation")
    public void configureMediaRecorder(final MediaRecorder recorder, android.hardware.Camera camera) throws IllegalStateException, IllegalArgumentException {
        recorder.setCamera(camera);
        recorder.setAudioSource(mCaptureConfiguration.getAudioSource());
        recorder.setVideoSource(mCaptureConfiguration.getVideoSource());

        CamcorderProfile baseProfile = mCameraWrapper.getBaseRecordingProfile();
        baseProfile.fileFormat = mCaptureConfiguration.getOutputFormat();

        RecordingSize size = mCameraWrapper.getSupportedRecordingSize(mCaptureConfiguration.getVideoWidth(), mCaptureConfiguration.getVideoHeight());
        baseProfile.videoFrameWidth = size.width;
        baseProfile.videoFrameHeight = size.height;
        baseProfile.videoBitRate = mCaptureConfiguration.getVideoBitrate();

        baseProfile.audioCodec = mCaptureConfiguration.getAudioEncoder();
        baseProfile.videoCodec = mCaptureConfiguration.getVideoEncoder();

        recorder.setProfile(baseProfile);
        recorder.setMaxDuration(mCaptureConfiguration.getMaxCaptureDuration());
        recorder.setOutputFile(mVideoFile.getFullPath());
        recorder.setOrientationHint(mCameraWrapper.getRotationCorrection());

        try {
            recorder.setMaxFileSize(mCaptureConfiguration.getMaxCaptureFileSize());
        } catch (IllegalArgumentException e) {
            CLog.e(CLog.RECORDER, "Failed to set max filesize - illegal argument: " + mCaptureConfiguration.getMaxCaptureFileSize());
        } catch (RuntimeException e2) {
            CLog.e(CLog.RECORDER, "Failed to set max filesize - runtime exception");
        }
        recorder.setOnInfoListener(this);
    }

    public VideoFile getVideoFile() {
        return mVideoFile;
    }

    public void setVideoFile(VideoFile mVideoFile) {
        this.mVideoFile = mVideoFile;
    }

    /**
     * 预览
     *
     * @return
     */
    private boolean prepareRecorder() {
        try {
            getMediaRecorder().prepare();
            CLog.d(CLog.RECORDER, "MediaRecorder successfully prepared");
            return true;
        } catch (final IllegalStateException e) {
            e.printStackTrace();
            CLog.e(CLog.RECORDER, "MediaRecorder preparation failed - " + e.toString());
            return false;
        } catch (final IOException e) {
            e.printStackTrace();
            CLog.e(CLog.RECORDER, "MediaRecorder preparation failed - " + e.toString());
            return false;
        }
    }

    /**
     * 开始录制
     *
     * @return
     */
    public boolean startRecorder() {
        try {
            getMediaRecorder().start();
            CLog.d(CLog.RECORDER, "MediaRecorder successfully started");
            return true;
        } catch (final IllegalStateException e) {
            e.printStackTrace();
            CLog.e(CLog.RECORDER, "MediaRecorder start failed - " + e.toString());
            return false;
        } catch (final RuntimeException e2) {
            e2.printStackTrace();
            CLog.e(CLog.RECORDER, "MediaRecorder start failed - " + e2.toString());
            mRecorderInterface.onRecordingFailed("Unable to record video with given settings");
            return false;
        }
    }

    /**
     * 返回是否是在录制
     *
     * @return 录制的状态
     */
    protected boolean isRecording() {
        return mRecording;
    }

    protected void setMediaRecorder(MediaRecorder recorder) {
        mRecorder = recorder;
    }

    protected MediaRecorder getMediaRecorder() {
        return mRecorder;
    }

    /**
     * 释放recorder
     */
    private void releaseRecorderResources() {
        MediaRecorder recorder = getMediaRecorder();
        if (recorder != null) {
            recorder.release();
            setMediaRecorder(null);
        }
    }

    /**
     * 释放相机和预览资源
     */
    public void releaseAllResources() {
        if (mVideoCapturePreview != null) {
            mVideoCapturePreview.releasePreviewResources();
        }
        if (mCameraWrapper != null) {
            mCameraWrapper.releaseCamera();
            mCameraWrapper = null;
        }
        releaseRecorderResources();
        CLog.d(CLog.RECORDER, "Released all resources");
    }

    /**
     * 开启预览失败
     */
    @Override
    public void onCapturePreviewFailed() {
        mRecorderInterface.onRecordingFailed("Unable to show camera preview");
    }

    /**
     * mediareorder 的回调
     *
     * @param mr
     * @param what
     * @param extra
     */
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                // NOP
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                CLog.d(CLog.RECORDER, "MediaRecorder max duration reached");
                stopRecording("Capture stopped - Max duration reached");
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                CLog.d(CLog.RECORDER, "MediaRecorder max filesize reached");
                stopRecording("Capture stopped - Max file size reached");
                break;
            default:
                break;
        }
    }

    public void setRecording(boolean mRecording) {
        this.mRecording = mRecording;
    }
}