package com.reactlibrary.ScreenRecorderModule.states;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

abstract class State {

    public void  requestPermission(@NonNull StateContext stateContext, Promise promise) {

    }

    public boolean onRequestPermissionsResult(@NonNull StateContext stateContext,  int requestCode, String[] permissions, int[] grantResults) {
        return  false;
    }

    public void  startRecording(@NonNull StateContext stateContext, Promise promise) {

    }

    public void  stopRecording(@NonNull StateContext stateContext, String nativeViewId, Callback callback) {

    }

    public void onRecordingCancelled(@NonNull StateContext stateContext) {

    }

    public void onActivityResult(@NonNull StateContext stateContext, int requestCode, int resultCode, Intent intent) {

    }

    public void onProcessCallback(@NonNull StateContext stateContext, long executionId, int returnCode) {

    }

}
