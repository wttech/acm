export const ToastTimeoutQuick = 3000;
export const ToastTimeoutMedium = 5000;
export const ToastTimeoutLong = 10000;

export const intervalToTimeout = (interval: number): number => {
  return Math.round(0.8 * interval);
};
