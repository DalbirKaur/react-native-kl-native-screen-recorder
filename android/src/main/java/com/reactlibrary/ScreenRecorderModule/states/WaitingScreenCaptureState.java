package com.reactlibrary.ScreenRecorderModule.states;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.media.projection.MediaProjectionManager;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;

import static com.reactlibrary.ScreenRecorderModule.states.StateContext.CAST_PERMISSION_CODE;

class WaitingScreenCaptureState extends State {

    @Override
    public void onActivityResult(@NonNull StateContext stateContext, int requestCode, int resultCode, Intent intent) {
        Promise startRecordingPromise = stateContext.getStartRecordingPromise();
        if (requestCode != CAST_PERMISSION_CODE) {
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            if (startRecordingPromise !=  null) {
                startRecordingPromise.reject("no_permission", "User denied recording");
                stateContext.setCurrentState(new IdleState());
            }
            return;
        }
        if (intent != null) {
            MediaProjectionManager mediaProjectionManager = stateContext.getMediaProjectionManager();
            if(mediaProjectionManager == null) return;
            stateContext.setMediaProjection(mediaProjectionManager.getMediaProjection(resultCode, intent));
        }
        MediaRecorder mediaRecorder = stateContext.getMediaRecorder();
        if (mediaRecorder != null) {
            stateContext.setCurrentState(new RecordingState());
            stateContext.prepareVirtualDisplay();
            mediaRecorder.start();
        }
        startRecordingPromise.resolve(null);
    }
}
