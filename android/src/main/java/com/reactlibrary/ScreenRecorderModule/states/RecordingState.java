package com.reactlibrary.ScreenRecorderModule.states;

import android.app.Activity;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.facebook.react.bridge.Callback;
import com.facebook.react.uimanager.util.ReactFindViewUtil;
import com.reactlibrary.ScreenRecorderModule.ScreenRecorderMediaProjectionService;

class RecordingState extends State {

    @Override
    public void stopRecording(@NonNull final StateContext stateContext, String nativeViewId,  final Callback stoppingCallback) {
        stateContext.setStoppingCallback(stoppingCallback);
        Activity activity = stateContext.getActivity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            Intent serviceIntent = new Intent(activity, ScreenRecorderMediaProjectionService.class);
            activity.stopService(serviceIntent);
        }
        stopMediaRecorder(stateContext);

        final String croppedVideoFilePath = stateContext.generateVideoPath();
        try {
            String command = generateFFmpegCommand(stateContext, nativeViewId, stateContext.getOriginalVideoFilePath(), croppedVideoFilePath);
            stateContext.setCroppedVideoFilePath(croppedVideoFilePath);
            stateContext.setCurrentState(new ProcessingVideoState());
            FFmpeg.executeAsync(command, stateContext);
        } catch (NullPointerException e) {
            //TODO: remove original  video
            stoppingCallback.invoke("View with nativeID hasn't been found or current activity is null: " + e.getMessage(), null);
            stateContext.setCurrentState(new IdleState());
        }
    }

    private void stopMediaRecorder(@NonNull StateContext stateContext) {
        MediaRecorder mediaRecorder = stateContext.getMediaRecorder();
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.reset();
        }
        VirtualDisplay virtualDisplay = stateContext.getVirtualDisplay();
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        MediaProjection mediaProjection = stateContext.getMediaProjection();
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        stateContext.setMediaProjection(null);
        stateContext.setMediaProjectionManager(null);
        stateContext.setMediaRecorder(null);
    }

    /**
     * @param nativeViewId    a nativeId of view which should be cropped
     * @param inputVideoPath  path to the video which should be cropped
     * @param outputVideoPath path to the file where cropped video should be written
     * @return FFmpeg command for video cropping
     * @throws NullPointerException in case of current activity is null or view with nativeID hasn't been found
     */
    private String generateFFmpegCommand(StateContext stateContext, String nativeViewId, String inputVideoPath, String outputVideoPath)
            throws NullPointerException {

        View rootView = stateContext.getActivity().getWindow().getDecorView().getRootView();
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
