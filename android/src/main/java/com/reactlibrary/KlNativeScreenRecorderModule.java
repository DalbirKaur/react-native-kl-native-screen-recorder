package com.reactlibrary;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;

import androidx.annotation.NonNull;

import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;
import com.facebook.react.uimanager.util.ReactFindViewUtil;
import com.reactlibrary.ScreenRecorderModule.ScreenRecorderMediaProjectionService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class KlNativeScreenRecorderModule extends ReactContextBaseJavaModule implements PermissionListener {
    private static final int REQUEST_PERMISSIONS_CODE = 231444;
    private static final int CAST_PERMISSION_CODE = 22;

    private static final String TAG = "Screen_Recorder_LOG";

    private Promise permissionsPromise;
    private Promise startRecordingPromise;
    private MediaProjection mMediaProjection = null;
    private VirtualDisplay mVirtualDisplay = null;
    private MediaRecorder mediaRecorder;
    private MediaProjectionManager mProjectionManager;
    private String originalVideoFilePath;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    public KlNativeScreenRecorderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
            @Override
            public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
                if (requestCode != CAST_PERMISSION_CODE) {
                    return;
                }
                if (resultCode != Activity.RESULT_OK) {
                    startRecordingPromise.reject("no_permission", "User denied recording");
                    return;
                }
                if (intent != null) {
                    mMediaProjection = mProjectionManager.getMediaProjection(resultCode, intent);
                }
                mVirtualDisplay = getVirtualDisplay();
                mediaRecorder.start();
                startRecordingPromise.resolve(null);
            }
        };
        reactContext.addActivityEventListener(mActivityEventListener);
    }

    @NonNull
    @Override
    public String getName() {
        return "KLScreenRecorderNativeWrapper";
    }

    @ReactMethod
    public void nativeRequestPermissions(Promise promise) {
        PermissionAwareActivity activity = (PermissionAwareActivity) getCurrentActivity();
        if (activity == null) {
            promise.reject("no_current_activity", "No current activity");
            return;
        }

        int audioPermission = activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO);
        int readStoragePermission = activity.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeStoragePermission = activity.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        List<String> listOfPermissionsToRequest = new ArrayList<>();
        if (audioPermission != PackageManager.PERMISSION_GRANTED) {
            listOfPermissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO);
        }
        if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listOfPermissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (writeStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listOfPermissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listOfPermissionsToRequest.isEmpty()) {
            permissionsPromise = promise;
            String[] permissionsToRequest = new String[listOfPermissionsToRequest.size()];
            listOfPermissionsToRequest.toArray(permissionsToRequest);
            activity.requestPermissions(permissionsToRequest, REQUEST_PERMISSIONS_CODE, this);
        } else {
            promise.resolve(null);
        }
    }

    @ReactMethod
    public void nativeStartRecording(Promise promise) {
        startRecordingPromise = promise;
        Activity activity = getCurrentActivity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            Intent serviceIntent = new Intent(activity, ScreenRecorderMediaProjectionService.class);
            activity.startForegroundService(serviceIntent);
        }
        mProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // calculate real size of the device screen
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);
        screenWidth = point.x;
        screenHeight = point.y;

        // calculate screen density
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        screenDensity = displayMetrics.densityDpi;

        if (prepareRecording()) {
            startMediaRecorder();
        }
    }

    /**
     * @param nativeViewId a nativeId of view which should be cropped
     */
    @ReactMethod
    public void stopRecording(final String nativeViewId, final Callback callback) {
        Activity activity = getCurrentActivity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            Intent serviceIntent = new Intent(activity, ScreenRecorderMediaProjectionService.class);
            activity.stopService(serviceIntent);
        }
        stopMediaRecorder();

        final String croppedVideoFilePath = generateVideoPath();
        try {
            String command = generateFFmpegCommand(nativeViewId, originalVideoFilePath, croppedVideoFilePath);
            FFmpeg.executeAsync(command, new ExecuteCallback() {
                @Override
                public void apply(final long executionId, final int returnCode) {
                    File fileToDelete = new File(originalVideoFilePath);
                    if (fileToDelete.exists()) {
                        if (fileToDelete.delete()) {
                            Log.i(TAG, "Original final deleted at path: " + originalVideoFilePath);
                        } else {
                            Log.i(TAG, "Original final not deleted at path: " + originalVideoFilePath);
                        }
                    }
                    if (returnCode == RETURN_CODE_SUCCESS) {
                        Log.i(TAG, "Async command execution completed successfully.");
                        callback.invoke(null, croppedVideoFilePath);
                    } else if (returnCode == RETURN_CODE_CANCEL) {
                        Log.i("Screen_Recorder_LOG", "Async command execution cancelled by user.");
                        callback.invoke("Cropping unknown error", null);
                    } else {
                        Log.i(TAG, String.format("Async command execution failed with rc=%d.", returnCode));
                        callback.invoke("Cropping unknown error", null);
                    }
                }
            });
        } catch (NullPointerException e) {
            callback.invoke("View with nativeID hasn't been found or current activity is null: " + e.getMessage(), null);
        }
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionsPromise.resolve(null);
            } else {
                permissionsPromise.reject("permissions_denied", "Permissions denied");
            }
        }
        return true;
    }

    /**
     * @return true if recorder was prepared successfully, false otherwise
     */
    private boolean prepareRecording() {
        String filePath = generateVideoPath();
        if (filePath == null) {
            return false;
        }
        originalVideoFilePath = filePath;

        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setVideoFrameRate(30);
        recorder.setVideoSize(screenWidth, screenHeight);
        recorder.setVideoEncodingBitRate(8081049);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(filePath);

        mediaRecorder = recorder;

        try {
            mediaRecorder.prepare();
        } catch (IOException error) {
            Log.e(TAG, "Failed to prepare a recorder: " + error.getLocalizedMessage());
            error.printStackTrace();
            return false;
        }
        return true;
    }

    private void startMediaRecorder() {
        Activity activity = getCurrentActivity();
        if (mMediaProjection == null && mProjectionManager != null && activity != null) {
            // This asks for user permissions to capture the screen
            activity.startActivityForResult(mProjectionManager.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
            return;
        }
        mVirtualDisplay = getVirtualDisplay();
        mediaRecorder.start();
    }

    private void stopMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.reset();
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
        mMediaProjection = null;
        mProjectionManager = null;
        mediaRecorder = null;
    }

    private VirtualDisplay getVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay(
                this.getClass().getName(),
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(),
                null,
                null);
    }


    private String generateVideoPath() {
        String directory = getReactApplicationContext().getExternalFilesDir(null).getAbsolutePath() + "/Recordings/";
        File folder = new File(directory);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        String filePath;
        if (success) {
            String videoName = "capture_" + UUID.randomUUID().toString() + ".mp4";
            filePath = directory + videoName;
        } else {
            Log.e(TAG, "Cann't create folder");
            return null;
        }
        return filePath;
    }

    /**
     * @param nativeViewId    a nativeId of view which should be cropped
     * @param inputVideoPath  path to the video which should be cropped
     * @param outputVideoPath path to the file where cropped video should be written
     * @return FFmpeg command for video cropping
     * @throws NullPointerException in case of current activity is null or view with nativeID hasn't been found
     */
    private String generateFFmpegCommand(String nativeViewId, String inputVideoPath, String outputVideoPath)
            throws NullPointerException {

        View rootView = getCurrentActivity().getWindow().getDecorView().getRootView();
        View reactView = ReactFindViewUtil.findView(rootView, nativeViewId);

        // We should get absolute Y position on the screen
        // It needs for handling status bar height
        int[] rect = new int[2];
        reactView.getLocationInWindow(rect);

        int x = rect[0];
        int y = rect[1];
        int width = reactView.getWidth();
        int height = reactView.getHeight();
        return "-i "
                + inputVideoPath
                + " -filter:v crop=" + width + ":" + height + ":" + x + ":" + y
                + " -crf 18 -vb 8M -c:a copy "
                + outputVideoPath;
    }
}
