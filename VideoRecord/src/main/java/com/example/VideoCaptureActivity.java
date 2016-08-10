package com.example; /**
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Thumbnails;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.example.rec.R;

import com.example.camera.CameraWrapper;
import com.example.camera.NativeCamera;
import com.example.configuration.CaptureConfiguration;
import com.example.recorder.AlreadyUsedException;
import com.example.recorder.VideoRecorder;
import com.example.recorder.VideoRecorderInterface;
import com.example.view.RecordingButtonInterface;
import com.example.view.VideoCaptureView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VideoCaptureActivity extends Activity implements RecordingButtonInterface, VideoRecorderInterface {

    public static final int RESULT_ERROR = 753245;

    public static final String EXTRA_OUTPUT_FILENAME = "com.jmolsmobile.extraoutputfilename";//文件名
    //配置
    public static final String EXTRA_CAPTURE_CONFIGURATION = "com.jmolsmobile.extracaptureconfiguration";
    //    错误信息
    public static final String EXTRA_ERROR_MESSAGE = "com.jmolsmobile.extraerrormessage";
    //    是否显示时间
    public static final String EXTRA_SHOW_TIMER = "com.jmolsmobile.extrashowtimer";
    //是否保存
    private static final String SAVED_RECORDED_BOOLEAN = "com.jmolsmobile.savedrecordedboolean";
    //    保存文件路径
    protected static final String SAVED_OUTPUT_FILENAME = "com.jmolsmobile.savedoutputfilename";

    private boolean mVideoRecorded = false;
    private VideoFile mVideoFile = null;
    private CaptureConfiguration mCaptureConfiguration;//录制的配置
    private VideoCaptureView mVideoCaptureView;//view
    private VideoRecorder mVideoRecorder;//录制工具

    List<String> mVideoFiles = new ArrayList<>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CLog.toggleLogging(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        设置窗体全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_videocapture);

        initializeCaptureConfiguration(savedInstanceState);

        mVideoCaptureView = (VideoCaptureView) findViewById(R.id.videocapture_videocaptureview_vcv);
        if (mVideoCaptureView == null) return; // Wrong orientation

        initializeRecordingUI();
    }

    //加载配置
    private void initializeCaptureConfiguration(final Bundle savedInstanceState) {
        mCaptureConfiguration = generateCaptureConfiguration();
        mVideoRecorded = generateVideoRecorded(savedInstanceState);
        mVideoFile = generateOutputFile(savedInstanceState);
    }

    //加载界面
    private void initializeRecordingUI() {
        VideoFile mVideoFile1 = new VideoFile(mVideoFile.getFullPath().replace(".mp4", "x.mp4"));
        mVideoFiles.add(mVideoFile1.getFullPath());
        mVideoCaptureView.setRecordingButtonInterface(this);
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        mVideoRecorder = new VideoRecorder(this, mCaptureConfiguration, mVideoFile1, new CameraWrapper(new NativeCamera(), display.getRotation()),
                mVideoCaptureView.getPreviewSurfaceHolder());
        boolean showTimer = this.getIntent().getBooleanExtra(EXTRA_SHOW_TIMER, false);
        mVideoCaptureView.showTimer(showTimer);
        if (mVideoRecorded) {
            mVideoCaptureView.updateUIRecordingFinished(getVideoThumbnail());
        } else {
            mVideoCaptureView.updateUINotRecording();
        }
        mVideoCaptureView.setTimedurtion(mCaptureConfiguration.getMaxCaptureDuration());
        mVideoCaptureView.showTimer(mCaptureConfiguration.getShowTimer());
    }

    @Override
    protected void onPause() {
        if (mVideoRecorder != null) {
            mVideoRecorder.stopRecording(null);
        }
        releaseAllResources();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        finishCancelled();
    }

    //录制按钮的点击效果
    @Override
    public void onRecordButtonClicked() {
        try {
            mVideoRecorder.toggleRecording();
        } catch (AlreadyUsedException e) {
            CLog.d(CLog.ACTIVITY, "Cannot toggle recording after cleaning up all resources");
        }
    }

    //完成
    @Override
    public void onAcceptButtonClicked() {
        finishCompleted();
    }

    //不要该视频
    @Override
    public void onDeclineButtonClicked() {
        finishCancelled();
    }

    //录制视频暂停的处理

    @Override
    public void onRecordPause() {
        mVideoRecorder.stopRecording(null);
        mVideoCaptureView.updateUIRecordingPause(getVideoThumbnail());
        releaseAllResources();
    }

    //录制继续
    int cout = 0;

    @Override
    public void continueRecord() {
        cout++;
        VideoFile mVideoFilex = new VideoFile(mVideoFile.getFullPath().replace(".mp4", cout + ".mp4"));
        mVideoFiles.add(mVideoFilex.getFullPath());
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        mVideoRecorder = new VideoRecorder(this, mCaptureConfiguration, mVideoFilex, new CameraWrapper(new NativeCamera(), display.getRotation()),
                mVideoCaptureView.getPreviewSurfaceHolder());
        mVideoRecorder.setRecording(false);
        boolean showTimer = this.getIntent().getBooleanExtra(EXTRA_SHOW_TIMER, false);
        mVideoCaptureView.showTimer(showTimer);
        if (mVideoRecorded) {
            mVideoCaptureView.updateUIRecordingFinished(getVideoThumbnail());
        } else {
            mVideoCaptureView.updateUINotRecording();
        }
        mVideoCaptureView.updateUIRecordingContinue();
        onRecordButtonClicked();

    }

    //从本地选择
    public static final int CHOOSEVIDEOFROMLOCAL = 111;

    @Override
    public void chooseLocal() {
        Intent chooseVideo = new Intent();
        chooseVideo.setType("video/*");
        chooseVideo.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(chooseVideo, CHOOSEVIDEOFROMLOCAL);
    }

    //录制开始
    @Override
    public void onRecordingStarted() {
        mVideoCaptureView.updateUIRecordingOngoing();
    }

    //停止
    @Override
    public void onRecordingStopped(String message) {
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

        mVideoCaptureView.updateUIRecordingFinished(getVideoThumbnail());
        releaseAllResources();
    }

    //成功
    @Override
    public void onRecordingSuccess() {
        mVideoRecorded = true;
    }

    //失败
    @Override
    public void onRecordingFailed(String message) {
//        finishError(message);
    }

    //完成录制
    private void finishCompleted() {
        try {
            VideoUtils.appendVideo(this, mVideoFile.getFullPath(), mVideoFiles);
            for (String mFile : mVideoFiles) {
                File mFile1 = new File(mFile);
                if (mFile1.exists()) {
                    mFile1.delete();
                }
            }
        } catch (IOException mE) {
            mE.printStackTrace();
        }
        final Intent result = new Intent();
        result.putExtra(EXTRA_OUTPUT_FILENAME, mVideoFile.getFullPath());
        this.setResult(RESULT_OK, result);
        finish();
    }

    //取消录制
    private void finishCancelled() {
        this.setResult(RESULT_CANCELED);
        finish();
    }

    private void finishError(final String message) {
        Toast.makeText(getApplicationContext(), "Can't capture video: " + message, Toast.LENGTH_LONG).show();

        final Intent result = new Intent();
        result.putExtra(EXTRA_ERROR_MESSAGE, message);
        this.setResult(RESULT_ERROR, result);
        finish();
    }

    //释放资源
    private void releaseAllResources() {
        if (mVideoRecorder != null) {
            mVideoRecorder.releaseAllResources();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(SAVED_RECORDED_BOOLEAN, mVideoRecorded);
        savedInstanceState.putString(SAVED_OUTPUT_FILENAME, mVideoFile.getFullPath());
        super.onSaveInstanceState(savedInstanceState);
    }

    //设置配置信息
    protected CaptureConfiguration generateCaptureConfiguration() {
        CaptureConfiguration returnConfiguration = this.getIntent().getParcelableExtra(EXTRA_CAPTURE_CONFIGURATION);
        if (returnConfiguration == null) {
            returnConfiguration = new CaptureConfiguration();
            CLog.d(CLog.ACTIVITY, "No captureconfiguration passed - using default configuration");
        }
        return returnConfiguration;
    }

    //设置是否保存
    private boolean generateVideoRecorded(final Bundle savedInstanceState) {
        if (savedInstanceState == null) return false;
        return savedInstanceState.getBoolean(SAVED_RECORDED_BOOLEAN, false);
    }

    //配置保存文件信息
    protected VideoFile generateOutputFile(Bundle savedInstanceState) {
        VideoFile returnFile;
        if (savedInstanceState != null) {
            returnFile = new VideoFile(savedInstanceState.getString(SAVED_OUTPUT_FILENAME));
        } else {
            returnFile = new VideoFile(this.getIntent().getStringExtra(EXTRA_OUTPUT_FILENAME));
        }
        // TODO: add checks to see if outputfile is writeable
        return returnFile;
    }

    //获取录制结束的预览图片
    public Bitmap getVideoThumbnail() {
        final Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(mVideoFiles.get(mVideoFiles.size() - 1),
                Thumbnails.FULL_SCREEN_KIND);
        if (thumbnail == null) {
            CLog.d(CLog.ACTIVITY, "Failed to generate video preview");
        }
        return thumbnail;
    }

    //选择本地视频的返回
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (RESULT_OK == resultCode && requestCode == CHOOSEVIDEOFROMLOCAL) {
            String videopath = getvideopath(data);
            final Intent result = new Intent();
            result.putExtra(EXTRA_OUTPUT_FILENAME, videopath);
            this.setResult(RESULT_OK, result);
            finish();
        }
    }

    @Nullable
    private String getvideopath(Intent data) {
        if (data == null) {
            return null;
        }
        Uri mUri = data.getData();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(this, mUri)) {
                if (isMediaDocument(mUri)) {
                    final String docId = DocumentsContract.getDocumentId(mUri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };
                    return getDataColumn(this, contentUri, selection, selectionArgs);
                } else if (isExternalStorageDocument(mUri)) {
                    final String docId = DocumentsContract.getDocumentId(mUri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(mUri.getScheme())) {

                return getDataColumn(this, mUri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(mUri.getScheme())) {
                return mUri.getPath();
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(mUri.getScheme())) {

            return getDataColumn(this, mUri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(mUri.getScheme())) {
            return mUri.getPath();
        }
        return null;
    }

    private String getDataColumn(Context mContext, Uri mContentUri, String mSelection, String[] mSelectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = mContext.getContentResolver().query(mContentUri, projection, mSelection, mSelectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private boolean isMediaDocument(Uri mUri) {
        return "com.android.providers.media.documents".equals(mUri.getAuthority());
    }

    private boolean isExternalStorageDocument(Uri mUri) {
        return "com.android.externalstorage.documents".equals(mUri.getAuthority());
    }
}
