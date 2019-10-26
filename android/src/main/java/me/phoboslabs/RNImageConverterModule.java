
package me.phoboslabs;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Date;

/**
 * @author kellin.me (Lee Kyoungil) [mailto:leekyoungil@gmail.com]
 */
public class RNImageConverterModule extends ReactContextBaseJavaModule {

  private final Context reactContext;

  private static final String SUCCESS_KEY = "success";
  private static final String ERROR_MESSAGE_KEY = "errorMsg";
  private static final String IMAGE_URI_KEY = "imageURI";
  private static final String BASE64_STRING_KEY = "base64String";

  private static final String ERROR_MESSAGE_EMPTY_URI_KEY = "URI Path KEY('path') must not be null.";
  private static final String ERROR_MESSAGE_EMPTY_URI_VALUE = "URI Path Value must not be null.";
  private static final String ERROR_MESSAGE_FILE_SAVE_FAILED = "File save failed.";

  public RNImageConverterModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNImageConverter";
  }

  private static final String PATH_KEY = "path";
  private static final String GRAYSCALE_KEY = "grayscale";
  private static final String BASE64_KEY = "base64";
  private static final String RESIZE_RATIO_KEY = "resizeRatio";
  private static final String IMAGE_QUALITY_KEY = "imageQuality";

  private static final String ANDROID_URI_FILE_SCHEME = "file://";

  private static Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.valueOf("JPEG");

  @ReactMethod
  public void imageConvert(ReadableMap data, final Callback responseCb) {
    String errorMessage = null;
    if (data.hasKey(PATH_KEY) == false) {
      errorMessage = ERROR_MESSAGE_EMPTY_URI_KEY;
    } else if (StringUtils.isBlank(data.getString(PATH_KEY))) {
      errorMessage = ERROR_MESSAGE_EMPTY_URI_VALUE;
    }

    if (StringUtils.isBlank(errorMessage)) {
      Uri imageURI = Uri.parse(data.getString(PATH_KEY));

      try {
        Bitmap sourceImage = ImageConverterUtil.getSourceImageByPath(this.reactContext, imageURI);

        Bitmap resizeImage = null;
        if (data.hasKey(RESIZE_RATIO_KEY) == true) {
          final float resizeRatio = Float.parseFloat(data.getString(RESIZE_RATIO_KEY));
          if (resizeRatio > 0.0 && resizeRatio < 1.0) {
            resizeImage = ImageConverterUtil.getImageByResize(sourceImage, resizeRatio, false);
          }
        }

        Bitmap grayscaleImage = null;
        if (data.hasKey(GRAYSCALE_KEY) && Boolean.parseBoolean(data.getString(GRAYSCALE_KEY))) {
          grayscaleImage = ImageConverterUtil.imageToGrayscale(resizeImage != null ? resizeImage : sourceImage);
        }

        float imageQuality = 1.0f;
        try {
          if (data.hasKey(IMAGE_QUALITY_KEY) == true) {
            imageQuality = Float.parseFloat(data.getString(IMAGE_QUALITY_KEY));
          }
        } catch (NumberFormatException ignore) {}

        Bitmap targetImage;
        if (grayscaleImage != null) {
          targetImage = Bitmap.createBitmap(grayscaleImage);
          grayscaleImage.recycle();

          if (resizeImage != null) {
            resizeImage.recycle();
          }
        } else if (resizeImage != null) {
          targetImage = Bitmap.createBitmap(resizeImage);
          resizeImage.recycle();
        } else {
          targetImage = Bitmap.createBitmap(sourceImage);
        }
        sourceImage.recycle();

        try {
          WritableMap response = this.getReturnMessage(true);

          if (data.hasKey(BASE64_KEY) && Boolean.parseBoolean(data.getString(BASE64_KEY))) {
            response.putString(BASE64_STRING_KEY, ImageConverterUtil.getBase64FromBitmap(targetImage, COMPRESS_FORMAT));
          } else {
            response.putString(IMAGE_URI_KEY, this.saveToLocalStorage(targetImage, imageQuality));
          }

          responseCb.invoke(response);
        } catch (Exception ex) {
          responseCb.invoke(this.getReturnMessage(false, ex.getMessage()));
        }
      } catch (Exception ex) {
        responseCb.invoke(this.getReturnMessage(false, ex.getMessage()));
      }
    } else {
      responseCb.invoke(this.getReturnMessage(false, errorMessage));
    }
  }

  private String saveToLocalStorage(Bitmap targetImage, final float imageQuality) throws Exception {
    try {
      final String fileName = Long.toString(new Date().getTime()).concat(".").concat(COMPRESS_FORMAT.name());
      File saveTargetFile = new File(this.reactContext.getCacheDir(), fileName);

      ImageConverterUtil.saveImageFile(targetImage, saveTargetFile, COMPRESS_FORMAT, imageQuality);
      if (saveTargetFile.exists() && saveTargetFile.canRead()) {
        return ANDROID_URI_FILE_SCHEME.concat(saveTargetFile.getAbsolutePath());
      }
    } catch (Exception ex) {
      throw ex;
    }

    throw new Exception(ERROR_MESSAGE_FILE_SAVE_FAILED);
  }

  private WritableMap getReturnMessage(final boolean isSuccess, final String errorMessage) {
    WritableMap response = this.getReturnMessage(isSuccess);
    if (StringUtils.isNotBlank(errorMessage)) {
      response.putString(ERROR_MESSAGE_KEY, errorMessage);
    }

    return response;
  }

  private WritableMap getReturnMessage(final boolean isSuccess) {
    WritableMap response = Arguments.createMap();
    response.putBoolean(SUCCESS_KEY, isSuccess);

    return response;
  }
}