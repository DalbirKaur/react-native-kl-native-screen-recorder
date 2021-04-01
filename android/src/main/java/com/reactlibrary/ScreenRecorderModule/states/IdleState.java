package com.reactlibrary.ScreenRecorderModule.states;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;

class IdleState extends State {

    @Override
    public void requestPermission(@NonNull StateContext stateContext, Promise promise) {
        RequestingAudioWritePermissionsState nextState = new RequestingAudioWritePermissionsState();
        stateContext.setCurrentState(nextState);
        nextState.requestPermission(stateContext, promise);
    }

}
