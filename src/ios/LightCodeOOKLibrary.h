//
//  LightCodeOOKLibrary.h
//  LightCodeOOKLibrary
//
//  Created by 65-0A40338-01 on 2018/5/7.
//  Copyright © 2018年 65-0A40338-01. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>

@interface LightCodeOOKLibrary : NSObject
- (AVCaptureSession*)LCcameraControl:(AVCaptureSession*) mycaptureSession:(int)cameraLocation;
-(void)LCcameraClean;
-(NSString *)readLC:(CMSampleBufferRef)sampleBuffer;
@end
