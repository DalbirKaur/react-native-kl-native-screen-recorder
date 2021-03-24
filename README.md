# Native Screen Recorder

## Getting started

`$ npm install <relative_path_to_module_dir> --save`

##### Automatically link the library

`$ react-native link`

###### _extra steps for the iOS platform_

`$ cd ./ios`

`$ pod install`

###### _extra steps for the Android platform_

Add service to AndroidManifest of the main app in <application> section.

`<service
    android:name="com.reactlibrary.ScreenRecorderModule.ScreenRecorderMediaProjectionService"
    android:enabled="true"
    android:exported="true"
    android:foregroundServiceType="mediaProjection" />`

If you will have problems with Gradle sync try to add the following function to build.gradle file in 'android' section:

`packagingOptions {
pickFirst 'lib/x86/libc++_shared.so'
pickFirst 'lib/x86_64/libc++_shared.so'
pickFirst 'lib/armeabi-v7a/libc++_shared.so'
pickFirst 'lib/arm64-v8a/libc++_shared.so'
}`

## Usage

Use `requestPermissions(/*dictionary*/details,/*success callback*/,/*error callback*/)` function to request permissions. You should provide size of a video in pixels:
*      WIDTH - video width,
*      HEIGHT - video height,

`onError` callback is being called if the permissions weren't granted or due to unknown error

Use `startRecording(/*success callback*/,/*error callback*/)` function to start recording.

Use `stopRecording(/*dictionary*/details,/*callback*/handler)` function to request permissions.

`details` should be provided for the cropping action. You should provide coordinates and size of captured view in pixels:
*      X_POSITION - X coordinate of a view,
*      Y_POSITION - Y coordinate of a view,
*      WIDTH - captured view width,
*      HEIGHT - captured view height,

`handler` callback is being called after cropping. It contains two arguments error and filepath to a local storage with final video

## Usage example
```javascript

const screenRecorder = require('react-native-kl-native-screen-recorder');


const WIDTH = screenRecorder.WIDTH;
    const HEIGHT = screenRecorder.HEIGHT;
    var details = {
      WIDTH: 1080,
      HEIGHT: 1920
    }; 
    acreenRecorder.requestPermissions(details, () => {
        screenRecorder.startRecording(() => {
            console.log("Started successful");
        }, (error) => {
            console.error(error);
        });
    }, (error) => {
        console.error(error);
    });
});
    

const X_POSITION = screenRecorder.X_POSITION;
const Y_POSITION = screenRecorder.Y_POSITION;
const WIDTH = screenRecorder.WIDTH;
const HEIGHT = screenRecorder.HEIGHT;
var details = {
    X_POSITION: 0,
    Y_POSITION: 100,
    WIDTH: 400,
    HEIGHT: 400
}; 
screenRecorder.stopRecording(details, (error, filePath) => {
    if (error) {
        console.error(error);
    } else { 
        console.log(filePath);
    }
});

```
