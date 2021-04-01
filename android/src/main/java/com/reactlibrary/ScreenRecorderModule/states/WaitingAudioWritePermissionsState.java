package com.reactlibrary.ScreenRecorderModule.states;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import static com.reactlibrary.ScreenRecorderModule.states.StateContext.REQUEST_AUDIO_WRITE_PERMISSIONS_CODE;

class WaitingAudioWritePermissionsState extends State {

    @Override
    public boolean onRequestPermissionsResult(@NonNull StateContext stateContext, int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_AUDIO_WRITE_PERMISSIONS_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                stateContext.setCurrentState(new RequestScreenCaptureState());
                stateContext.getPermissionsPromise().resolve(null);
            } else {
                stateContext.setCurrentState(new IdleState());
                stateContext.getPermissionsPromise().reject("permissions_denied", "Permissions denied");
            }
        }
        return true;
    }
}
