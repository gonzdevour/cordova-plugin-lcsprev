#import <Cordova/CDV.h>
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import "ViewController.h"

@interface lcr : CDVPlugin<AVCaptureVideoDataOutputSampleBufferDelegate, ViewControllerDelegate>

- (void) scan:(CDVInvokedUrlCommand*)command;

@end
