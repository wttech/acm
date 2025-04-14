import { ToastQueue } from '@react-spectrum/toast';
import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
import { ToastTimeoutMedium } from './spectrum.ts';

export type ApiResponse<T> = {
  status: number;
  message: string;
  data: T;
};

type ApiRequestConfig = AxiosRequestConfig & {
  operation: string;
  quiet?: boolean;
};

/* eslint-disable @typescript-eslint/no-explicit-any */
export async function apiRequest<D>(config: ApiRequestConfig): Promise<AxiosResponse<ApiResponse<D>>> {
  try {
    const response = await axios<ApiResponse<D>>(config);
    if (response.status >= 200 && response.status < 300) {
      return response;
    } else {
      throw new Error(`${config.operation} failed!`);
    }
  } catch (error: any) {
    if (config.quiet !== true) {
      console.error(`${config.operation} error!`, error);
    }
    if (error.response && error.response.data && error.response.data.message) {
      throw new Error(error.response.data.message);
    } else {
      throw new Error(`${config.operation} failed!`);
    }
  }
}

type ToastRequestConfig = ApiRequestConfig & {
  positive?: boolean;
  hideAfter?: number;
};

export async function toastRequest<D>(config: ToastRequestConfig): Promise<AxiosResponse<ApiResponse<D>>> {
  const toastTimeout = config.hideAfter || ToastTimeoutMedium;
  try {
    const response = await axios<ApiResponse<D>>(config);
    if (response.status >= 200 && response.status < 300) {
      if (config.positive !== false) {
        if (response.data && response.data.message) {
          ToastQueue.positive(response.data.message, { timeout: toastTimeout });
        } else {
          ToastQueue.positive(`${config.operation} succeeded!`, {
            timeout: toastTimeout,
          });
        }
      }
      return response;
    } else {
      ToastQueue.negative(`${config.operation} failed!`, {
        timeout: toastTimeout,
      });
      throw new Error(`${config.operation} failed!`);
    }
  } catch (error: any) {
    console.error(`${config.operation} error!`, error);
    if (error.response && error.response.data && error.response.data.message) {
      ToastQueue.negative(error.response.data.message, {
        timeout: toastTimeout,
      });
    } else {
      ToastQueue.negative(`${config.operation} failed!`, {
        timeout: toastTimeout,
      });
    }
    throw error;
  }
}
