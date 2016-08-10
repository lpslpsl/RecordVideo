package com.example.camera;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * Wrapper around the native camera class so all camera access
 * can easily be mocked.
 * <p/>
 * Created by Jeroen Mols on 06/12/15. 对Camera的包装
 */
public class
NativeCamera {

    private Camera     camera = null;
    private Parameters params = null;

    public Camera getNativeCamera() {
        return camera;
    }

    /**
     * 打开后置相机
     * @throws RuntimeException
     */
    public void openNativeCamera() throws RuntimeException {
        camera = Camera.open(CameraInfo.CAMERA_FACING_BACK);
    }

    /**
     * 打开相机
     */
    public void unlockNativeCamera() {
        camera.unlock();
    }

    /**
     * 断开与相机的连接并且释放资源
     */
    public void releaseNativeCamera() {
        camera.release();
    }

    /**
     * 设置预览
     * @param holder
     * @throws IOException
     */
    public void setNativePreviewDisplay(SurfaceHolder holder) throws IOException {
        camera.setPreviewDisplay(holder);
    }

    /**
     * 开始预览
     */
    public void startNativePreview() {
        camera.startPreview();
    }

    /**
     * 停止预览
     */
    public void stopNativePreview() {
        camera.stopPreview();
    }

    /**
     * 关闭预览的回调接口
     */
    public void clearNativePreviewCallback() {
        camera.setPreviewCallback(null);
    }

    /**
     * 获取 相机的参数
     * @return 相机参数
     */
    public Parameters getNativeCameraParameters() {
        if (params == null) {
            params = camera.getParameters();
        }
        return params;
    }

    /**
     * 重新设置相机的参数
     * @param params 自定义的相机参数
     */
    public void updateNativeCameraParameters(Parameters params) {
        this.params = params;
        camera.setParameters(params);
    }

    /**
     * 设置旋转角度（顺时针）
     * @param degrees 角度
     */
    public void setDisplayOrientation(int degrees) {
        camera.setDisplayOrientation(degrees);
    }

    /**
     * 获取相机方向
     * @return
     */
    public int getCameraOrientation() {
        CameraInfo camInfo = new CameraInfo();
        Camera.getCameraInfo(getBackFacingCameraId(), camInfo);
        return camInfo.orientation;
    }

    private int getBackFacingCameraId() {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }
}
