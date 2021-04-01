package com.reactlibrary.ScreenRecorderModule.states;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.reactlibrary.ScreenRecorderModule.ScreenRecorderMediaProjectionService;

import java.io.IOException;

import static com.reactlibrary.ScreenRecorderModule.states.StateContext.CAST_PERMISSION_CODE;
import static com.reactlibrary.ScreenRecorderModule.states.StateContext.TAG;

class RequestScreenCaptureState extends State {

    @Override
    public void startRecording(@NonNull StateContext stateContext, Promise promise) {
        stateContext.setStartRecordingPromise(promise);
        Activity activity = stateContext.getActivity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            Intent serviceIntent = new Intent(activity, ScreenRecorderMediaProjectionService.class);
            activity.startForegroundService(serviceIntent);
        }
        stateContext.setMediaProjectionManager((MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE));
        // calculate real size of the device screen
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);
        stateContext.setScreenWidth(point.x);
        stateContext.setScreenHeight(point.y);

        // calculate screen density
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        stateContext.setScreenDensity(displayMetrics.densityDpi);

        if (prepareRecording(stateContext)) {
            startMediaRecorder(stateContext);
        }
    }

    /**
     * @return true if recorder was prepared successfully, false otherwise
     */
    private boolean prepareRecording(@NonNull StateContext stateContext) {
        String filePath = stateContext.generateVideoPath();
        if (filePath == null) {
            return false;
        }
        stateContext.setOriginalVideoFilePath(filePath);
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setVideoFrameRate(30);
        recorder.setVideoSize(stateContext.getScreenWidth(), stateContext.getScreenHeight());
        recorder.setVideoEncodingBitRate(8081049);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(filePath);

        stateContext.setMediaRecorder(recorder);

        try {
            recorder.prepare();
        } catch (IOException error) {
            Log.e(TAG, "Failed to prepare a recorder: " + error.getLocalizedMessage());
            error.printStackTrace();
            return false;
        }
        return true;
    }

    private void startMediaRecorder(@NonNull StateContext stateContext) {
        Activity activity = stateContext.getActivity();
        MediaProjectionManager projectionManager = stateContext.getMediaProjectionManager();
        MediaProjection mediaProjection = stateContext.getMediaProjection();
        if (mediaProjection == null && projectionManager != null && activity != null) {
            // This asks for user permissions to capture the screen
            stateContext.setCurrentState(new WaitingScreenCaptureState());
            activity.startActivityForResult(projectionManager.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
            return;
        }
        MediaRecorder mediaRecorder = stateContext.getMediaRecorder();
        if (mediaRecorder != null) {
            stateContext.setCurrentState(new RecordingState());
            stateContext.prepareVirtualDisplay();
            mediaRecorder.start();
        }
    }
}
