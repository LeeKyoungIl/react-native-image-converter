
import { NativeModules, Platform } from 'react-native'

var { RNImageConverter } = NativeModules;

class IImageConverter {
    static convert(param) {
        if (param.hasOwnProperty('grayscale')) {
            param.grayscale = param.grayscale.toString().toLowerCase();
        } else {
            param.grayscale = "false";
        }
        if (param.hasOwnProperty('resizeRatio')) {
            param.resizeRatio = IImageConverter.checkToInputValue(param.resizeRatio);
        } else {
            param.resizeRatio = "1.0";
        }
        if (param.hasOwnProperty('imageQuality')) {
            param.imageQuality = IImageConverter.checkToInputValue(param.imageQuality);
        } else {
            param.imageQuality = "1.0";
        }    

        if (Platform.OS === "ios") {
            return RNImageConverter.imageConvert(param).then(result => (result));
        } else if (Platform.OS === "android") {
            return new Promise((resolve, reject) => {
                RNImageConverter.imageConvert(param, resolve, reject);
              });
        } else {
            return {
                success: false,
                errorMsg: "not yet supported.("+Platform.OS+")"
            }
        }
    }

    static checkToInputValue(inputValue) {
        try {
            var checkDataToString = inputValue.toString();
            var checkData = parseFloat(checkDataToString);
            if (checkData <= 0 || checkData > 1.0) {
                return "1.0";
            } else {
                return checkDataToString;
            }
        } catch(error) {
            return "1.0";
        }
    }
}

export default IImageConverter;