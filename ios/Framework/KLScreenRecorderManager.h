//
//  KLScreenRecorderManager.h
//  NativeWrapper
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface KLScreenRecorderManager : NSObject

@property (nonatomic, assign) BOOL shouldRecordScreen;

+ (instancetype)sharedInstance;

- (void)startRecordingWithVideoSize:(CGSize)videoSize andRecordingHandler:(void (^)(NSError *error))recordingHandler;
- (void)stopRecordingAndCropVideoForFrame:(CGRect)frame wthCompletionHandler:(void (^)(NSError *error, NSURL *filePath))completionHandler;

@end

NS_ASSUME_NONNULL_END
