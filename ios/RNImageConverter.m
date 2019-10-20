
#import "RNImageConverter.h"

@implementation RNImageConverter

static NSString *const ERROR_MESSAGE_EMPTY_URI_KEY = @"URI Path KEY('path') must not be null.";
static NSString *const ERROR_MESSAGE_EMPTY_URI_VALUE = @"URI Path Value must not be null.";

static NSString *const SUCCESS_KEY = @"success";
static NSString *const ERROR_MESSAGE_KEY = @"errorMsg";
static NSString *const IMAGE_URI_KEY = @"imageURI";

static NSString *const PATH_KEY = @"path";
static NSString *const GRAYSCALE_KEY = @"grayscale";
static NSString *const RESIZE_RATIO_KEY = @"resizeRatio";
static NSString *const IMAGE_QUALITY_KEY = @"imageQuality";

static NSString *const SAVE_IMAGE_FILE_NAME_BY_OVERWRITE = @"modifiedImage.jpg";

RCT_EXPORT_MODULE()

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(imageConvert:(NSDictionary *) params
                  resolve:(RCTPromiseResolveBlock) resolve
                  rejecter:(RCTPromiseRejectBlock) reject) {
    
    if ([params objectForKey:PATH_KEY] == nil) {
        return resolve(@{ SUCCESS_KEY: @NO, ERROR_MESSAGE_KEY: ERROR_MESSAGE_EMPTY_URI_KEY });
    }
    
    NSString *const uri = params[PATH_KEY];
    if ([uri length] == 0) {
        return resolve(@{ SUCCESS_KEY: @NO, ERROR_MESSAGE_KEY: ERROR_MESSAGE_EMPTY_URI_VALUE });
    }
    
    CGFloat resizeRatio = 1.0f;
    if ([params objectForKey:RESIZE_RATIO_KEY]) {
        NSString *resizeRatioString = params[RESIZE_RATIO_KEY];
        if ([resizeRatioString length] > 0) {
            resizeRatio = [resizeRatioString floatValue];
        }
    }
    
    CGFloat imageQuality = 1.0f;
    if ([params objectForKey:IMAGE_QUALITY_KEY]) {
        NSString *imageQualityString = params[IMAGE_QUALITY_KEY];
        if ([imageQualityString length] > 0) {
            imageQuality = [imageQualityString floatValue];
        }
    }
    
    BOOL imageToGrayscale = false;
    if ([params objectForKey:GRAYSCALE_KEY]) {
        NSString *imageToGrayscaleString = params[GRAYSCALE_KEY];
        imageToGrayscale = [imageToGrayscaleString boolValue];
    }
    
    NSString *const encodedURI = [uri stringByRemovingPercentEncoding];
    UIImage *const originImage = [self getNSImageByURI: encodedURI];
    UIImage *const resizedImage = [self modifyImage:originImage resizeRatio:resizeRatio];
    
    UIImage *grayscaleImage = nil;
    if (imageToGrayscale) {
        grayscaleImage = [self convertToGray:resizedImage];
    }
    
    return resolve(@{
        SUCCESS_KEY: @YES,
        IMAGE_URI_KEY:
            [self saveImageToLocal:(grayscaleImage != nil ? grayscaleImage : resizedImage) fileName:SAVE_IMAGE_FILE_NAME_BY_OVERWRITE imageQuality:imageQuality]
    });
}

- (UIImage *) getNSImageByURI:(NSString *) uri {
    NSURL *const url = [NSURL fileURLWithPath: uri];
    NSData *const data = [NSData dataWithContentsOfURL: url];
    return [UIImage imageWithData: data];
}

- (NSString *) saveImageToLocal:(UIImage *) image fileName:(NSString *) name imageQuality:(CGFloat) quality {
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths firstObject];

    NSData *data = UIImageJPEGRepresentation(image, quality);
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSString *fullPath = [documentsDirectory stringByAppendingPathComponent:name];

    [fileManager createFileAtPath:fullPath contents:data attributes:nil];
    return fullPath;
}

- (UIImage *) modifyImage:(UIImage *) originImage resizeRatio:(CGFloat) sizeRatio {
    const int colorRed = 1;
    const int colorGreen = 2;
    const int colorBlue = 4;
    
    const int colors = colorRed | colorGreen | colorBlue;
    const int resizeWidth = originImage.size.width * sizeRatio;
    const int resizeHeight = originImage.size.height * sizeRatio;

    uint32_t *rgbImage = (uint32_t *) malloc(resizeWidth * resizeHeight * sizeof(uint32_t));
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(rgbImage, resizeWidth, resizeHeight, 8, (resizeWidth * 4), colorSpace, kCGBitmapByteOrder32Little | kCGImageAlphaNoneSkipLast);
    
    CGContextSetInterpolationQuality(context, kCGInterpolationMedium);
    CGContextSetShouldSmoothFonts (context, NO);
    CGContextSetAllowsFontSmoothing (context, NO);
    CGContextSetShouldAntialias(context, NO);
    
    CGContextDrawImage(context, CGRectMake(0, 0, resizeWidth, resizeHeight), [originImage CGImage]);
    
    CGImageRef image = CGBitmapContextCreateImage(context);
    
    CGContextRelease(context);
    CGColorSpaceRelease(colorSpace);
    
    UIImage *resultUIImage = [UIImage imageWithCGImage:image];
    
    CGImageRelease(image);

    return resultUIImage;
}

- (UIImage *) convertToGray:(UIImage *) originImage {
    const int colorRed = 1;
    const int colorGreen = 2;
    const int colorBlue = 4;
    
    const int colors = colorRed | colorGreen | colorBlue;
    const int originWidth = originImage.size.width;
    const int originHeight = originImage.size.height;
    
    uint32_t *rgbImage = (uint32_t *) malloc(originWidth * originHeight * sizeof(uint32_t));

    // grayscale
    uint8_t *imageData = (uint8_t *) malloc(originWidth * originHeight);
    for(int y = 0; y < originHeight; y++) {
        for(int x = 0; x < originWidth; x++) {
            uint32_t rgbPixel = rgbImage[(y * originWidth) + x];
            uint32_t sum = 0;
            uint32_t count = 0;

            if (colors & colorRed) {
                sum += (rgbPixel >> 24) & 255;
                count++;
            }
            if (colors & colorGreen) {
                sum += (rgbPixel >> 16) & 255;
                count++;
            }
            if (colors & colorBlue) {
                sum += (rgbPixel >> 8) & 255;
                count++;
            }

            imageData[(y * originWidth) + x] = sum / count;
        }
    }

    uint8_t *result = (uint8_t *) calloc(originWidth * originHeight * sizeof(uint32_t), 1);

    for(int i = 0; i < (originHeight * originWidth); i++) {
        result[i * 4] = 0;
        const int val = imageData[i];

        result[i*4+1] = val;
        result[i*4+2] = val;
        result[i*4+3] = val;
    }

    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(result, originWidth, originHeight, 8, originWidth * sizeof(uint32_t), colorSpace, kCGBitmapByteOrder32Little | kCGImageAlphaNoneSkipLast);
    
    CGImageRef image = CGBitmapContextCreateImage(context);
    
    CGContextRelease(context);
    CGColorSpaceRelease(colorSpace);
    
    UIImage *resultUIImage = [UIImage imageWithCGImage:image];
    
    CGImageRelease(image);

    return resultUIImage;
}

@end
  
