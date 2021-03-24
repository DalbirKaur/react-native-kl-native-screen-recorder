//
//  KLScreenRecorderManager.m
//  NativeWrapper
//

#import "KLScreenRecorderManager.h"
#import <ReplayKit/ReplayKit.h>
#import <AVKit/AVKit.h>
#import <Photos/Photos.h>

@interface KLScreenRecorderManager ()

@property (nonatomic, strong) AVAssetWriter *assetWriter;
@property (nonatomic, strong) RPScreenRecorder *recorder;
@property (nonatomic, strong) AVAssetWriterInput *audioInput;
@property (nonatomic, strong) AVAssetWriterInput *videoInput;
@property (nonatomic, strong) NSURL *fullScreenRecordURL;

@end

@implementation KLScreenRecorderManager

+ (instancetype)sharedInstance {
    static KLScreenRecorderManager *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[KLScreenRecorderManager alloc] init];
    });
    return sharedInstance;
}

- (void)startRecordingWithVideoSize:(CGSize)videoSize andRecordingHandler:(void (^)(NSError *error))recordingHandler {
    NSLog(@"ScreenRecorderLog: Start recording");
    NSString *fileUrlString = [KLScreenRecorderManager createFilePathForName:[[NSString alloc] initWithFormat:@"fullScreenRecord_%@", [NSUUID UUID].UUIDString]];
    self.fullScreenRecordURL = [NSURL fileURLWithPath:fileUrlString];
    
    NSError *writerError = nil;
    self.assetWriter = [AVAssetWriter assetWriterWithURL:self.fullScreenRecordURL
                                                fileType:AVFileTypeMPEG4
                                                   error:&writerError];
    if (writerError) {
        NSLog(@"ScreenRecorderLog: Could not create asset writter with error: %@", writerError.localizedDescription);
        recordingHandler(writerError);
        return;
    }
    
    NSDictionary *videoSettings = @{AVVideoCodecKey: AVVideoCodecTypeH264,
                                    AVVideoWidthKey: @(videoSize.width),
                                    AVVideoHeightKey: @(videoSize.height),
                                    AVVideoCompressionPropertiesKey: @{AVVideoAverageBitRateKey: @(8081049),
                                                                       AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel,
                                                                       AVVideoAllowFrameReorderingKey: @(0),
                                                                       AVVideoExpectedSourceFrameRateKey: @(30),
                                                                       AVVideoH264EntropyModeKey: AVVideoH264EntropyModeCABAC,
                                                                       AVVideoMaxKeyFrameIntervalDurationKey: @(1)
                                    }
    };
    
    AudioChannelLayout audioChannelLayout = {
            .mChannelLayoutTag = kAudioChannelLayoutTag_Mono,
            .mChannelBitmap = 0,
            .mNumberChannelDescriptions = 0
        };
    
    NSDictionary *audioSettings = @{AVFormatIDKey: @(kAudioFormatMPEG4AAC),
                                    AVNumberOfChannelsKey: @(1),
                                    AVSampleRateKey: @(44100),
                                    AVChannelLayoutKey: [NSData dataWithBytes:&audioChannelLayout
                                                                       length:sizeof(AudioChannelLayout)]
    };
  
    self.audioInput = [[AVAssetWriterInput alloc] initWithMediaType:AVMediaTypeAudio outputSettings:audioSettings];
    self.videoInput = [[AVAssetWriterInput alloc] initWithMediaType:AVMediaTypeVideo outputSettings:videoSettings];
    
    self.audioInput.expectsMediaDataInRealTime = YES;
    self.videoInput.expectsMediaDataInRealTime = YES;
    
    [self.assetWriter addInput:self.audioInput];
    [self.assetWriter addInput:self.videoInput];
    
    self.recorder = [RPScreenRecorder sharedRecorder];
    self.recorder.microphoneEnabled = YES;
    
    NSLock *lock = [[NSLock alloc] init];
    [self.recorder startCaptureWithHandler:^(CMSampleBufferRef  _Nonnull sampleBuffer, RPSampleBufferType bufferType, NSError * _Nullable error) {
        [lock lock];
        if (self.shouldRecordScreen && CMSampleBufferDataIsReady(sampleBuffer)) {
            if (self.assetWriter.status == AVAssetWriterStatusUnknown) {
                NSLog(@"ScreenRecorderLog: Unknown status for asset writer");
                [self.assetWriter startWriting];
                CMTime time = CMSampleBufferGetPresentationTimeStamp(sampleBuffer);
                [self.assetWriter startSessionAtSourceTime:time];
            }
      
            if (self.assetWriter.status == AVAssetWriterStatusFailed) {
                NSLog(@"ScreenRecorderLog: AVAssetWriterStatusFailed");
                [self.assetWriter cancelWriting];
                [self.assetWriter endSessionAtSourceTime:CMSampleBufferGetPresentationTimeStamp(sampleBuffer)];
                recordingHandler(error);
            }
      
            //-- Video Data
            if (bufferType == RPSampleBufferTypeVideo && self.videoInput.isReadyForMoreMediaData) {
                [self.videoInput appendSampleBuffer:sampleBuffer];
            }
      
            //-- AudioMic Data
            NSLog(@"aaaa buffer: %@", bufferType == RPSampleBufferTypeAudioMic ? @"YES" : @"NO");
            if (bufferType == RPSampleBufferTypeAudioMic && self.audioInput.isReadyForMoreMediaData) {
                [self.audioInput appendSampleBuffer:sampleBuffer];
                NSLog(@"aaaa value: %@", self.audioInput.isReadyForMoreMediaData ? @"YES" : @"NO");
            }
        }
        [lock unlock];
    } completionHandler:^(NSError * _Nullable error) {
        if(error) {
            NSLog(@"ScreenRecorderLog: Could not start recording with error: %@", error.localizedDescription);
        } else {
            NSLog(@"ScreenRecorderLog: Recording has started");
        }
        recordingHandler(error);
    }];
}

- (void)stopRecordingAndCropVideoForFrame:(CGRect)frame
                     wthCompletionHandler:(void (^)(NSError *error, NSURL *filePath))completionHandler {
    self.shouldRecordScreen = NO;
    [self.recorder stopCaptureWithHandler:^(NSError * _Nullable error) {
        if (error) {
            NSLog(@"ScreenRecorderLog: Recording stopped with error %@", error.localizedDescription);
            completionHandler(error, nil);
            return;
        }
        if (self.assetWriter.status == AVAssetWriterStatusFailed
            || self.assetWriter.status == AVAssetWriterStatusCancelled
            || self.assetWriter.status == AVAssetWriterStatusUnknown
            || self.assetWriter.status == AVAssetWriterStatusCompleted) {
            NSLog(@"ScreenRecorderLog: Recording stopped with assetWriters unknown error");
            completionHandler(nil, nil);
            return;
        } else {
            NSLog(@"ScreenRecorderLog: Recording stopped successfully");
            [self.audioInput markAsFinished];
            [self.videoInput markAsFinished];
            [self.assetWriter finishWritingWithCompletionHandler:^{
                [self cropAndSaveVideoFromFileURL:self.fullScreenRecordURL
                                         forFrame:frame
                            withCompletionHandler:^(NSError *error, NSURL *fileURL) {
                    NSLog(@"ScreenRecorderLog: Cropped video successfully");
                    [KLScreenRecorderManager removeFileAtURL:self.fullScreenRecordURL];
                    self.fullScreenRecordURL = nil;
                    completionHandler(error, fileURL);
                }];
            }];
        }
    }];
}

- (void)cropAndSaveVideoFromFileURL:(NSURL *)fileURL
                           forFrame:(CGRect)frame
              withCompletionHandler:(void (^)(NSError *error, NSURL *fileURL))completionHandler {
    AVAsset *asset = [AVAsset assetWithURL:fileURL];
    AVMutableComposition *mutableComposition = [AVMutableComposition composition];
  
    AVAssetTrack *sourceVideoAssetTrack = [[asset tracksWithMediaType:AVMediaTypeVideo] firstObject];
    AVAssetTrack *sourceAudioAssetTrack = [[asset tracksWithMediaType:AVMediaTypeAudio] firstObject];
    
    NSLog(@"ScreenRecorderLog: Cropping: video asset exist %@", sourceVideoAssetTrack != nil ? @"true" : @"false");
    NSLog(@"ScreenRecorderLog: Cropping: audio asset exist %@", sourceAudioAssetTrack != nil ? @"true" : @"false");
  
    AVMutableCompositionTrack *videoCompositionTrack = [mutableComposition addMutableTrackWithMediaType:AVMediaTypeVideo
                                                                                       preferredTrackID:kCMPersistentTrackID_Invalid];
  
    CMTimeRange range = CMTimeRangeMake(kCMTimeZero, sourceVideoAssetTrack.asset.duration);
    NSError *error;
    [videoCompositionTrack insertTimeRange:range ofTrack:sourceVideoAssetTrack atTime:kCMTimeZero error:&error];
    if(error) {
        NSLog(@"ScreenRecorderLog: Cropping: insert video time range error %@", error.localizedDescription);
        completionHandler(error, nil);
        return;
    }
  
    CGAffineTransform firstTransform = CGAffineTransformMake(videoCompositionTrack.preferredTransform.a,
                                                             videoCompositionTrack.preferredTransform.b,
                                                             videoCompositionTrack.preferredTransform.c,
                                                             videoCompositionTrack.preferredTransform.d,
                                                             videoCompositionTrack.preferredTransform.tx - frame.origin.x,
                                                             videoCompositionTrack.preferredTransform.ty - frame.origin.y);
  
    AVMutableVideoCompositionLayerInstruction *fromLayer = [AVMutableVideoCompositionLayerInstruction videoCompositionLayerInstructionWithAssetTrack:videoCompositionTrack];
    [fromLayer setTransform:firstTransform atTime:kCMTimeZero];
    [fromLayer setCropRectangle:frame atTime:kCMTimeZero];
  
    AVMutableVideoCompositionInstruction *instruction = [AVMutableVideoCompositionInstruction videoCompositionInstruction];
    [instruction setLayerInstructions:@[fromLayer]];
    instruction.timeRange = CMTimeRangeMake(kCMTimeZero, sourceVideoAssetTrack.asset.duration);
  
    AVMutableVideoComposition *videoComposition = [AVMutableVideoComposition videoComposition];
    [videoComposition setInstructions:@[instruction]];
    videoComposition.renderSize = frame.size;
    videoComposition.frameDuration = CMTimeMake(1, 30);
  
    NSString *exportPath = [KLScreenRecorderManager createFilePathForName:[[NSString alloc] initWithFormat:@"screen_record_%@", [NSUUID UUID].UUIDString]];
    NSURL *exportURL = [NSURL fileURLWithPath:exportPath];
  
    AVAssetExportSession *exporter = [AVAssetExportSession exportSessionWithAsset:mutableComposition
                                                                       presetName:AVAssetExportPresetHighestQuality];
    exporter.outputURL = exportURL;
    exporter.videoComposition = videoComposition;
    exporter.outputFileType = AVFileTypeMPEG4;
    exporter.shouldOptimizeForNetworkUse = YES;
    exporter.canPerformMultiplePassesOverSourceMediaData = YES;
  
    if (sourceAudioAssetTrack) {
        AVMutableCompositionTrack *compositionAudioVideo = [mutableComposition addMutableTrackWithMediaType:AVMediaTypeAudio
                                                                                           preferredTrackID:kCMPersistentTrackID_Invalid];
        AVMutableAudioMix *audioMix = [AVMutableAudioMix audioMix];
        NSMutableArray *audioMixParam = [[NSMutableArray alloc] init];
        AVAssetTrack *assetVideoTrack = sourceAudioAssetTrack;
        AVMutableAudioMixInputParameters *videoParam = [AVMutableAudioMixInputParameters audioMixInputParametersWithTrack:assetVideoTrack];
        videoParam.trackID = compositionAudioVideo.trackID;
        [audioMixParam addObject:videoParam];
      
        NSError *compositionAudioVideoError;
        [compositionAudioVideo insertTimeRange:CMTimeRangeMake(kCMTimeZero, sourceAudioAssetTrack.asset.duration)
                                       ofTrack:assetVideoTrack
                                        atTime:kCMTimeZero error:&compositionAudioVideoError];
        if(compositionAudioVideoError) {
            completionHandler(compositionAudioVideoError, nil);
            return;
        }
        [audioMix setInputParameters:audioMixParam];
        exporter.audioMix = audioMix;
    }
  
    [exporter exportAsynchronouslyWithCompletionHandler:^{
        completionHandler(nil, exportURL);
    }];
}

+ (void)createReplaysFolder {
    NSString *documentDirectoryPath = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    if (documentDirectoryPath) {
        NSString *replayDirectoryPath = [documentDirectoryPath stringByAppendingString:@"/Replays"];
        NSFileManager *fileManager = [NSFileManager defaultManager];
        if (![fileManager fileExistsAtPath:replayDirectoryPath]) {
            NSError *error;
            [fileManager createDirectoryAtPath:replayDirectoryPath withIntermediateDirectories:NO attributes:nil error:&error];
            if (error) {
                NSLog(@"Exception: %@", error);
            }
        }
    }
}

+ (NSString *)createFilePathForName:(NSString *)fileName {
    [KLScreenRecorderManager createReplaysFolder];
    NSString *directory = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    return [[NSString alloc] initWithFormat:@"%@/Replays/%@.mp4", directory, fileName];
}

+ (void)removeFileAtURL:(NSURL *)fileURL {
    NSFileManager *fileManager = [NSFileManager defaultManager];
    [fileManager removeItemAtURL:fileURL error:nil];
}

@end
