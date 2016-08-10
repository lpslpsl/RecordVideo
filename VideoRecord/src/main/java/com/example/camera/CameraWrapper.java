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

package com.example.camera;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.SurfaceHolder;


import java.io.IOException;
import java.util.List;

/**
 * 对Native相机相关的包装
 */
@SuppressWarnings("deprecation")
public class CameraWrapper {

    private final int mDisplayRotation;
    private NativeCamera mNativeCamera = null;
    private Parameters   mParameters   = null;

    public CameraWrapper(NativeCamera nativeCamera, int displayRotation) {
        mNativeCamera = nativeCamera;
        mDisplayRotation = displayRotation;
    }

    /**
     * 获取camera实例
     * @return
     */
    public Camera getCamera() {
        return mNativeCamera.getNativeCamera();
    }

    /**
     * 打开相机
     * @throws OpenCameraException
     */
    public void openCamera() throws OpenCameraException {
        try {
            mNativeCamera.openNativeCamera();
        } catch (final RuntimeException e) {
            e.printStackTrace();
            throw new OpenCameraException(OpenCameraException.OpenType.INUSE);
        }

        if (mNativeCamera.getNativeCamera() == null) throw new OpenCameraException(OpenCameraException.OpenType.NOCAMERA);
    }

    /**
     * 获得相机lock
     * @throws PrepareCameraException
     */
    public void prepareCameraForRecording() throws PrepareCameraException {
        try {
            mNativeCamera.unlockNativeCamera();
        } catch (final RuntimeException e) {
            e.printStackTrace();
            throw new PrepareCameraException();
        }
    }

    /**
     * 释放camera
     */
    public void releaseCamera() {
        if (getCamera() == null) return;
        mNativeCamera.releaseNativeCamera();
    }

    /**
     * 开始预览
     * @param holder
     * @throws IOException
     */
    public void startPreview(final SurfaceHolder holder) throws IOException {
        mNativeCamera.setNativePreviewDisplay(holder);
        mNativeCamera.startNativePreview();
    }

    /**
     * 结束预览
     * @throws Exception
     */
    public void stopPreview() throws Exception {
        mNativeCamera.stopNativePreview();
        mNativeCamera.clearNativePreviewCallback();
    }

    /**
     * 获取录制的宽高信息
     * @param width 宽
     * @param height 高
     * @return
     */
    public RecordingSize getSupportedRecordingSize(int width, int height) {
        CameraSize recordingSize = getOptimalSize(getSupportedVideoSizes(VERSION.SDK_INT), width, height);
        if (recordingSize == null) {
            return new RecordingSize(width, height);
        }
        return new RecordingSize(recordingSize.getWidth(), recordingSize.getHeight());
    }

    /**
     * 获取相机的参数配置
     * @return
     */
    public CamcorderProfile getBaseRecordingProfile() {
        CamcorderProfile returnProfile;
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            returnProfile = getDefaultRecordingProfile();
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
            returnProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
            returnProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        } else {
            returnProfile = getDefaultRecordingProfile();
        }
        return returnProfile;
    }

    /**
     * 获取默认的质量配置
     * @return
     */
    private CamcorderProfile getDefaultRecordingProfile() {
        CamcorderProfile highProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        if (highProfile != null) {
            return highProfile;
        }
        CamcorderProfile lowProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        if (lowProfile != null) {
            return lowProfile;
        }
        throw new RuntimeException("No quality level found");
    }

    public void configureForPreview(int viewWidth, int viewHeight) {
        final Parameters params = mNativeCamera.getNativeCameraParameters();
        final CameraSize previewSize = getOptimalSize(params.getSupportedPreviewSizes(), viewWidth, viewHeight);

        params.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
        params.setPreviewFormat(ImageFormat.NV21);
        mNativeCamera.updateNativeCameraParameters(params);
        mNativeCamera.setDisplayOrientation(getRotationCorrection());
//        CLog.d(CLog.CAMERA, "Preview size: " + previewSize.getWidth() + "x" + previewSize.getHeight());
    }

    /**
     * 自动对焦？
     */
    public void enableAutoFocus() {
        final Parameters params = mNativeCamera.getNativeCameraParameters();
        params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        mNativeCamera.updateNativeCameraParameters(params);
    }

    public int getRotationCorrection() {
        int displayRotation = mDisplayRotation * 90;
        return (mNativeCamera.getCameraOrientation() - displayRotation + 360) % 360;
    }

    /**
     * h获取支持的视频大小
     * @param currentSdkInt
     * @return
     */
    @TargetApi(VERSION_CODES.HONEYCOMB)
    protected List<Size> getSupportedVideoSizes(int currentSdkInt) {
        Parameters params = mNativeCamera.getNativeCameraParameters();

        List<Size> supportedVideoSizes;
        if (currentSdkInt < VERSION_CODES.HONEYCOMB) {
//            CLog.e(CLog.CAMERA, "Using supportedPreviewSizes iso supportedVideoSizes due to API restriction");
            supportedVideoSizes = params.getSupportedPreviewSizes();
        } else if (params.getSupportedVideoSizes() == null) {
//            CLog.e(CLog.CAMERA, "Using supportedPreviewSizes because supportedVideoSizes is null");
            supportedVideoSizes = params.getSupportedPreviewSizes();
        } else {
            supportedVideoSizes = params.getSupportedVideoSizes();
        }

        return supportedVideoSizes;
    }

    /**
     * 设置camera
     * @param sizes
     * @param w
     * @param h
     * @return
     */
    public CameraSize getOptimalSize(List<Size> sizes, int w, int h) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.1;
        final double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;

        // Start with max value and refine as we iterate over available preview sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Target view height
        final int targetHeight = h;

        // Try to find a preview size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (final Size size : sizes) {
            final double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find preview size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (final Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return new CameraSize(optimalSize.width, optimalSize.height);
    }
}
