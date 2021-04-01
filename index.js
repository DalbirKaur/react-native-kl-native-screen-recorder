import { NativeModules } from 'react-native';
import React, { Component } from 'react';

let screenRecorder = NativeModules.KLScreenRecorderNativeWrapper

module.exports = NativeModules.KLScreenRecorderNativeWrapper;

module.exports.requestPermissions = function asyncRequestPermissions(onPickSuccess, onPickError) {
    invokeNativeRequestPermissions(onPickSuccess, onPickError)
}

async function invokeNativeRequestPermissions(onPickSuccess, onPickError) {
    try {
        await screenRecorder.nativeRequestPermissions();
        onPickSuccess()
    } catch (e) {
        onPickError(e)
    }
}

module.exports.startRecording = function asyncStartRecording(onPickSuccess, onPickError) {
    invokeNativeStartRecording(onPickSuccess, onPickError)
}

async function invokeNativeStartRecording(onPickSuccess, onPickError) {
    try {
        await screenRecorder.nativeStartRecording();
        onPickSuccess()
    } catch (e) {
        onPickError(e)
    }
}
