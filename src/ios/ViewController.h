//
//  ViewController.h
//  libTest
//
//  Created by 65-0A40338-01 on 2017/3/21.
//  Copyright © 2017年 65-0A40338-01. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>

@protocol ViewControllerDelegate
-(void)scanViewResult:(NSString*)result;
@end


@interface ViewController : UIViewController<AVCaptureVideoDataOutputSampleBufferDelegate>

@property (nonatomic, weak) id <ViewControllerDelegate> delegate;

-(void) setCloseString:(NSString*)strClose;
-(void) setScaningString:(NSString*)strScaning;
-(void) setCameraNum:(NSString*)strCameraNum;

@end

