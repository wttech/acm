import axios, {AxiosRequestConfig} from "axios";
import {ToastQueue} from "@react-spectrum/toast";

type ToastRequestConfig = AxiosRequestConfig & {
    operation: string,
    timeout?: number,
    onSuccess?: (response: any) => void,
    onFailure?: (error: any) => void,
}

export async function toastRequest(config: ToastRequestConfig) {
    const toastTimeout = config.timeout || 5000;
    try {
        const response = await axios(config);
        if (response.status >= 200 && response.status < 300) {
            if (response.data && response.data.message) {
                ToastQueue.positive(response.data.message, {timeout: toastTimeout});
            } else {
                ToastQueue.positive(`${config.operation} succeeded!`, {timeout: toastTimeout});
            }
        } else {
            ToastQueue.negative(`${config.operation} failed!`, {timeout: toastTimeout});
        }
        if (config.onSuccess) {
            config.onSuccess(response);
        }
    } catch (error: any) {
        console.error(`${config.operation} error!`, error);
        if (error.response && error.response.data && error.response.data.message) {
            ToastQueue.negative(error.response.data.message, {timeout: toastTimeout});
        } else {
            ToastQueue.negative(`${config.operation} failed!`, {timeout: toastTimeout});
        }
        if (config.onFailure) {
            config.onFailure(error);
        }
    }
}
