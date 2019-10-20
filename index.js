
import { NativeModules, Platform } from 'react-native'

var { RNImageConverter } = NativeModules;

class IImageConverter {
    static convert(param) {
        if (param.hasOwnProperty('grayscale')) {
            param.grayscale = param.grayscale.toString();
        }
        if (param.hasOwnProperty('resizeRatio')) {
            param.resizeRatio = param.resizeRatio.toString();
        }
        if (param.hasOwnProperty('imageQuality')) {
            param.imageQuality = param.imageQuality.toString();
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
}

export default IImageConverter;