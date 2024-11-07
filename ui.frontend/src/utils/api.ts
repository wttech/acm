import axios, {AxiosRequestConfig} from "axios";
import {ToastQueue} from "@react-spectrum/toast";

type ToastRequestConfig = AxiosRequestConfig & {
    operation: string,
    timeout?: number
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
            return response;
        } else {
            ToastQueue.negative(`${config.operation} failed!`, {timeout: toastTimeout});
            throw new Error(`${config.operation} failed!`);
        }
    } catch (error: any) {
        console.error(`${config.operation} error!`, error);
        if (error.response && error.response.data && error.response.data.message) {
            ToastQueue.negative(error.response.data.message, {timeout: toastTimeout});
        } else {
            ToastQueue.negative(`${config.operation} failed!`, {timeout: toastTimeout});
        }
        throw error;
    }
}

export async function apiRequest(config: ToastRequestConfig) {
    try {
        const response = await axios(config);
        if (response.status >= 200 && response.status < 300) {
            return response;
        } else {
            throw new Error(`${config.operation} failed!`);
        }
    } catch (error: any) {
        console.error(`${config.operation} error!`, error);
        if (error.response && error.response.data && error.response.data.message) {
            throw new Error(error.response.data.message);
        } else {
            throw new Error(`${config.operation} failed!`);
        }
    }
}
