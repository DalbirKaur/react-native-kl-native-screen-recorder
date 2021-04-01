package com.reactlibrary.ScreenRecorderModule.states;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;

import java.io.File;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;
import static com.reactlibrary.ScreenRecorderModule.states.StateContext.TAG;

class ProcessingVideoState extends State {

    @Override
    public void onProcessCallback(@NonNull StateContext stateContext, long executionId, int returnCode) {
        String originalVideoFilePath = stateContext.getOriginalVideoFilePath();
        if (originalVideoFilePath != null) {
            File fileToDelete = new File(originalVideoFilePath);
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Log.i(TAG, "Original final deleted at path: " + originalVideoFilePath);
                } else {
                    Log.i(TAG, "Original final not deleted at path: " + originalVideoFilePath);
                }
            }
        }
        Callback callback =  stateContext.getStoppingCallback();
        if (callback !=  null) {
            if (returnCode == RETURN_CODE_SUCCESS) {
                Log.i(TAG, "Async command execution completed successfully.");
                callback.invoke(null, stateContext.getCroppedVideoFilePath());
            } else if (returnCode == RETURN_CODE_CANCEL) {
                Log.i("Screen_Recorder_LOG", "Async command execution cancelled by user.");
                callback.invoke("Cropping unknown error", null);
            } else {
                Log.i(TAG, String.format("Async command execution failed with rc=%d.", returnCode));
                callback.invoke("Cropping unknown error", null);
            }
        }
        stateContext.setCurrentState(new IdleState());
    }
}
