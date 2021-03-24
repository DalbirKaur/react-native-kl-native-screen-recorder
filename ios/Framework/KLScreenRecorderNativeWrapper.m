//
//  ScreenRecorderNativeWrapper.m
//  NativeWrapper
//

#import "KLScreenRecorderNativeWrapper.h"
#import "KLScreenRecorderManager.h"
#import <React/RCTLog.h>
#import <React/RCTConvert.h>
#import <UIKit/UIKit.h>
#import "UIView+React.h"
#import <Photos/Photos.h>

@implementation KLScreenRecorderNativeWrapper

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE(KLScreenRecorderNativeWrapper);

RCT_EXPORT_METHOD(nativeRequestPermissions:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    CGFloat height = screenRect.size.height;
    CGFloat width = screenRect.size.width;
    [[KLScreenRecorderManager sharedInstance] startRecordingWithVideoSize:CGSizeMake(width, height)
                                                      andRecordingHandler:^(NSError * _Nonnull error) {
        if(error) {
          reject(@"request_premissions_error", error.localizedDescription, error);
          return;
        }
        resolve(@[[NSNull null]]);
    }];
}

RCT_EXPORT_METHOD(nativeStartRecording:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  [KLScreenRecorderManager sharedInstance].shouldRecordScreen = YES;
    resolve([NSNull null]);
}

RCT_EXPORT_METHOD(stopRecording:(NSString *)nativeViewID
                  callback:(RCTResponseSenderBlock)callback) {
    UIWindow *window = [[UIApplication sharedApplication] keyWindow];
    UIView *topView = window.rootViewController.view;
    UIView *viewToCrop = [self findSubviewInView:topView
                                    withNativeID:nativeViewID];
    CGPoint originOnScreen = [viewToCrop convertPoint:CGPointZero
                                               toView:UIApplication.sharedApplication.delegate.window];
    CGFloat x = originOnScreen.x;
    CGFloat y = originOnScreen.y;
    CGFloat width = CGRectGetWidth(viewToCrop.bounds);
    CGFloat height = CGRectGetHeight(viewToCrop.bounds);
    
    CGRect recordFrame = CGRectMake(x, y, width, height);
    
    [[KLScreenRecorderManager sharedInstance] stopRecordingAndCropVideoForFrame:recordFrame
                                                         wthCompletionHandler:^(NSError * _Nonnull error, NSURL * _Nonnull filePath) {
      if(error) {
        NSDictionary *rctError = RCTMakeError(error.localizedDescription, nil, nil);
        callback(@[rctError, [NSNull null]]);
        return;
      }
        
        [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
            [PHAssetChangeRequest creationRequestForAssetFromVideoAtFileURL:filePath];
        } completionHandler:^(BOOL success, NSError *error) {
            if (success) {
                 NSLog(@"Success");
            }
            else {
                NSLog(@"write error : %@",error);
            }
        }];
        
      callback(@[[NSNull null], filePath.absoluteString]);
  }];
}

// recursevily find a view
- (UIView *)findSubviewInView:(UIView *)view
                 withNativeID:(NSString *)nativeID {
    if ([view.nativeID isEqualToString: nativeID]) {
        return view;
    }
    
    UIView *targetView = nil;
    for (UIView *subview in view.subviews) {
        targetView = [self findSubviewInView:subview
                                withNativeID:nativeID];
        if (targetView != nil) {
            break;
        }
    }
    
    return targetView;
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end
