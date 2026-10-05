/** Standard ACM API envelope: { status, message, data } */
export interface ApiResponse<T = unknown> {
  status: number;
  message: string;
  data: T;
}
