#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface RNImageConverter : NSObject <RCTBridgeModule>

@end
  
