package com.reactlibrary.ScreenRecorderModule.states;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.PermissionAwareActivity;

import java.util.ArrayList;
import java.util.List;

import static com.reactlibrary.ScreenRecorderModule.states.StateContext.REQUEST_AUDIO_WRITE_PERMISSIONS_CODE;

class RequestingAudioWritePermissionsState extends State {

    @Override
    public void requestPermission(@NonNull StateContext stateContext, Promise promise) {
        PermissionAwareActivity activity = (PermissionAwareActivity) stateContext.getActivity();
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
        if (listOfPermissionsToRequest.isEmpty()) {
            stateContext.setCurrentState(new RequestScreenCaptureState());
            promise.resolve(null);
        } else {
            stateContext.setPermissionsPromise(promise);
            String[] permissionsToRequest = new String[listOfPermissionsToRequest.size()];
            listOfPermissionsToRequest.toArray(permissionsToRequest);
            activity.requestPermissions(permissionsToRequest, REQUEST_AUDIO_WRITE_PERMISSIONS_CODE, stateContext);
            stateContext.setCurrentState(new WaitingAudioWritePermissionsState());
        }
    }



}
