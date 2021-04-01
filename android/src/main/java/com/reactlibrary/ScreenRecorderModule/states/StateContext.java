package com.reactlibrary.ScreenRecorderModule.states;

import android.app.Activity;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.PermissionListener;

import java.io.File;
import java.util.UUID;


public class StateContext implements PermissionListener, ExecuteCallback {

    static final int REQUEST_AUDIO_WRITE_PERMISSIONS_CODE = 231444;
    static final String TAG = "Screen_Recorder_LOG";
    static final int CAST_PERMISSION_CODE = 22;

    private final Activity activity;
    @NonNull
    private State currentState = new IdleState();
    @Nullable
    private Promise permissionsPromise = null;
    @Nullable
    private Promise startRecordingPromise = null;
    @Nullable
    private MediaProjectionManager mediaProjectionManager = null;
    @Nullable
    private MediaRecorder mediaRecorder = null;
    @Nullable
    private MediaProjection mediaProjection = null;
    @Nullable
    private String originalVideoFilePath = null;
    @Nullable
    private VirtualDisplay virtualDisplay = null;
    @Nullable
    private Callback stoppingCallback = null;
    @Nullable
    private String croppedVideoFilePath = null;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    public StateContext(Activity activity) {
        this.activity = activity;
    }

    public void requestPermission(Promise promise) {
        currentState.requestPermission(this, promise);
    }

    public void startRecording(Promise promise) {
        currentState.startRecording(this, promise);
    }

    public void stopRecording(String nativeViewId, Callback callback) {
        currentState.stopRecording(this, nativeViewId, callback);
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        return currentState.onRequestPermissionsResult(this, requestCode,  permissions, grantResults);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        currentState.onActivityResult(this, requestCode, resultCode,  intent);
    }


    void prepareVirtualDisplay() {
        if (mediaProjection == null || mediaRecorder == null || mediaRecorder.getSurface() == null)
            return;
        virtualDisplay = mediaProjection.createVirtualDisplay(
                this.getClass().getName(),
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(),
                null,
                null);
    }

    String generateVideoPath() {
        String directory = getActivity().getExternalFilesDir(null).getAbsolutePath() + "/Recordings/";
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

    @Override
    public void apply(long executionId, int returnCode) {
        currentState.onProcessCallback(this, executionId, returnCode);
    }

    //region Getters & Setters

    public void setCurrentState(@NonNull State currentState) {
        Log.d(TAG, "New  State: " + currentState.getClass().getSimpleName());
        this.currentState = currentState;
    }

    @Nullable
    public Promise getPermissionsPromise() {
        return permissionsPromise;
    }

    public void setPermissionsPromise(@Nullable Promise permissionsPromise) {
        this.permissionsPromise = permissionsPromise;
    }

    @Nullable
    public Promise getStartRecordingPromise() {
        return startRecordingPromise;
    }

    public void setStartRecordingPromise(@Nullable Promise startRecordingPromise) {
        this.startRecordingPromise = startRecordingPromise;
    }

    public Activity getActivity() {
        return activity;
    }

    @Nullable
    public MediaProjectionManager getMediaProjectionManager() {
        return mediaProjectionManager;
    }

    public void setMediaProjectionManager(@Nullable MediaProjectionManager mediaProjectionManager) {
        this.mediaProjectionManager = mediaProjectionManager;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public int getScreenDensity() {
        return screenDensity;
    }

    public void setScreenDensity(int screenDensity) {
        this.screenDensity = screenDensity;
    }

    @Nullable
    public MediaRecorder getMediaRecorder() {
        return mediaRecorder;
    }

    public void setMediaRecorder(@Nullable MediaRecorder mediaRecorder) {
        this.mediaRecorder = mediaRecorder;
    }

    @Nullable
    public MediaProjection getMediaProjection() {
        return mediaProjection;
    }

    public void setMediaProjection(@Nullable MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    @Nullable
    public VirtualDisplay getVirtualDisplay() {
        return virtualDisplay;
    }

    public void setVirtualDisplay(@Nullable VirtualDisplay virtualDisplay) {
        this.virtualDisplay = virtualDisplay;
    }

    @Nullable
    public String getOriginalVideoFilePath() {
        return originalVideoFilePath;
    }

    public void setOriginalVideoFilePath(@Nullable String originalVideoFilePath) {
        this.originalVideoFilePath = originalVideoFilePath;
    }

    @Nullable
    public Callback getStoppingCallback() {
        return stoppingCallback;
    }

    public void setStoppingCallback(@Nullable Callback stoppingCallback) {
        this.stoppingCallback = stoppingCallback;
    }

    @Nullable
    public String getCroppedVideoFilePath() {
        return croppedVideoFilePath;
    }

    public void setCroppedVideoFilePath(@Nullable String croppedVideoFilePath) {
        this.croppedVideoFilePath = croppedVideoFilePath;
    }

    //endregion
}
