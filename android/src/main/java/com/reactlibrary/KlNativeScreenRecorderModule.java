package com.reactlibrary;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.PermissionListener;
import com.reactlibrary.ScreenRecorderModule.states.StateContext;

public class KlNativeScreenRecorderModule extends ReactContextBaseJavaModule implements PermissionListener {

    private StateContext stateContext = null;

    @NonNull
    private StateContext getStateContext() {
        if (stateContext == null) {
            stateContext = new StateContext(getCurrentActivity());
        }
        return stateContext;
    }

    public KlNativeScreenRecorderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
            @Override
            public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
                getStateContext().onActivityResult(requestCode, resultCode, intent);
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
        getStateContext().requestPermission(promise);
    }

    @ReactMethod
    public void nativeStartRecording(Promise promise) {
        getStateContext().startRecording(promise);
    }

    /**
     * @param nativeViewId a nativeId of view which should be cropped
     */
    @ReactMethod
    public void stopRecording(final String nativeViewId, final Callback callback) {
        getStateContext().stopRecording(nativeViewId, callback);
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        return getStateContext().onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
