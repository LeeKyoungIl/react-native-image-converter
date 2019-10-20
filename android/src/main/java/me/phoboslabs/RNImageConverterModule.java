
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

public class RNImageConverterModule extends ReactContextBaseJavaModule {

  private final Context reactContext;

  private static final String SUCCESS_KEY = "success";
  private static final String ERROR_MESSAGE_KEY = "errorMsg";
  private static final String IMAGE_URI_KEY = "imageURI";

  private static final String ERROR_MESSAGE_EMPTY_URI_KEY = "URI Path KEY('path') must not be null.";
  private static final String ERROR_MESSAGE_EMPTY_URI_VALUE = "URI Path Value must not be null.";

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
  private static final String RESIZE_RATIO_KEY = "resizeRatio";
  private static final String IMAGE_QUALITY_KEY = "imageQuality";

  private static final String ANDROID_URI_FILE_SCHEME = "file://";

  private static Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.valueOf("JPEG");

  @ReactMethod
  public void imageConvert(ReadableMap data, final Callback successCb, final Callback failureCb) {
    String errorMessage = null;
    if (data.hasKey(PATH_KEY) == false) {
      errorMessage = ERROR_MESSAGE_EMPTY_URI_KEY;
    } else if (StringUtils.isBlank(data.getString(PATH_KEY))) {
      errorMessage = ERROR_MESSAGE_EMPTY_URI_VALUE;
    }

    String savedImageURI = null;
    Uri imageURI = Uri.parse(data.getString(PATH_KEY));

    try {
      Bitmap resizeImage = null, grayscaleImage = null;
      Bitmap sourceImage = ImageConverterUtil.getSourceImageByPath(this.reactContext, imageURI);

      if (data.hasKey(RESIZE_RATIO_KEY) == true) {
        final float resizeRatio = Float.parseFloat(data.getString(RESIZE_RATIO_KEY));
        if (resizeRatio > 0.0 && resizeRatio < 1.0) {
          resizeImage = ImageConverterUtil.getImageByResize(sourceImage, resizeRatio);
        }
      }

      if (data.hasKey(GRAYSCALE_KEY) == true) {
        final boolean grayscale = Boolean.parseBoolean(data.getString(GRAYSCALE_KEY));
        if (grayscale) {
          grayscaleImage = ImageConverterUtil.imageToGrayscale(resizeImage != null ? resizeImage : sourceImage);
        }
      }

      float imageQuality = 1.0f;
      if (data.hasKey(IMAGE_QUALITY_KEY) == true) {
        imageQuality = Float.parseFloat(data.getString(IMAGE_QUALITY_KEY));
      }

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

      final String fileName = Long.toString(new Date().getTime()).concat(".").concat(COMPRESS_FORMAT.name());
      File saveTargetFile = new File(this.reactContext.getCacheDir(), fileName);

      ImageConverterUtil.saveImageFile(targetImage, saveTargetFile, COMPRESS_FORMAT, imageQuality);
      if (saveTargetFile.exists() && saveTargetFile.canRead()) {
        savedImageURI = ANDROID_URI_FILE_SCHEME.concat(saveTargetFile.getAbsolutePath());
      }
    } catch (Exception e) {
      errorMessage = e.getMessage();
    }

    WritableMap response = Arguments.createMap();

    boolean result = false;
    if (StringUtils.isNotBlank(savedImageURI)) {
      result = true;
      response.putString(IMAGE_URI_KEY, savedImageURI);
    } else {
      response.putString(ERROR_MESSAGE_KEY, errorMessage);
    }
    response.putBoolean(SUCCESS_KEY, result);

    successCb.invoke(response);
  }
}