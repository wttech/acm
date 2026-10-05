import { GRANITE_CSRF_TOKEN } from './paths';
import type { ApiResponse } from './response';

export type AcmAuth =
  | { mode: 'bearer'; token: string }
  // Called on every request, so a refreshed cookie is picked up without restarting.
  | { mode: 'cookie'; cookie: () => string | undefined }
  | { mode: 'basic'; user: string; password: string };

/** Tool-specific hints, e.g. which environment variable or setting to fix. */
export interface AcmClientMessages {
  unauthorized?: string;
  missingCookie?: string;
}

export interface AcmConnection {
  baseUrl: string;
  auth: AcmAuth;
  timeoutMs?: number;
  messages?: AcmClientMessages;
}

export type HttpMethod = 'GET' | 'POST' | 'DELETE';

export class AcmHttpError extends Error {
  constructor(
    public readonly httpStatus: number,
    message: string,
    public readonly body?: string,
  ) {
    super(message);
  }
}

/** Accepts either a bare `login-token` value or a full `name=value` pair. */
export function normalizeCookie(raw: string | undefined): string | undefined {
  const value = raw?.trim();
  if (!value) return undefined;
  return value.includes('=') ? value : `login-token=${value}`;
}

function base64(text: string): string {
  let binary = '';
  for (const byte of new TextEncoder().encode(text)) binary += String.fromCharCode(byte);
  return btoa(binary);
}

const DEFAULT_TIMEOUT_MS = 30000;

/** HTTP client for the ACM API with bearer / cookie (+CSRF) / basic auth. */
export class AcmClient {
  private csrfToken: string | null = null;
  private csrfTokenCookie: string | undefined;
  readonly baseUrl: string;

  constructor(private readonly connection: AcmConnection) {
    this.baseUrl = connection.baseUrl.replace(/\/+$/, '');
  }

  get authDescription(): string {
    const auth = this.connection.auth;
    switch (auth.mode) {
      case 'bearer':
        return 'Bearer token (IMS user token)';
      case 'cookie':
        return 'Browser login-token cookie (+ CSRF token)';
      case 'basic':
        return `Basic auth (user: ${auth.user})`;
    }
  }

  private baseHeaders(): Record<string, string> {
    const h: Record<string, string> = {
      Accept: 'application/json',
      // Some environments enable the Sling referrer filter for non-GET
      // requests; sending a same-origin Referer is the standard workaround.
      Referer: this.baseUrl + '/',
    };
    const auth = this.connection.auth;
    switch (auth.mode) {
      case 'bearer':
        h['Authorization'] = `Bearer ${auth.token}`;
        break;
      case 'cookie': {
        const cookie = auth.cookie();
        if (!cookie) {
          throw new AcmHttpError(401, this.connection.messages?.missingCookie ?? 'No login-token cookie configured.');
        }
        h['Cookie'] = cookie;
        break;
      }
      case 'basic':
        h['Authorization'] = 'Basic ' + base64(`${auth.user}:${auth.password}`);
        break;
    }
    return h;
  }

  /** Cookie-authenticated POST/DELETE on AEM require a Granite CSRF token. */
  private async ensureCsrfToken(force = false, timeoutMs?: number): Promise<string | null> {
    const auth = this.connection.auth;
    if (auth.mode !== 'cookie') return null;
    const cookie = auth.cookie();
    // A CSRF token belongs to one session, so a refreshed cookie invalidates it.
    if (cookie !== this.csrfTokenCookie) this.csrfToken = null;
    if (this.csrfToken && !force) return this.csrfToken;
    const res = await this.rawFetch(GRANITE_CSRF_TOKEN, { method: 'GET', headers: this.baseHeaders() }, timeoutMs);
    if (!res.ok) {
      throw new AcmHttpError(
        res.status,
        `Failed to obtain CSRF token (HTTP ${res.status}). The login-token cookie may have expired — copy a fresh one from the browser.`,
      );
    }
    const json = (await res.json()) as { token?: string };
    this.csrfToken = json.token || null;
    this.csrfTokenCookie = cookie;
    return this.csrfToken;
  }

  private async rawFetch(
    path: string,
    init: RequestInit,
    timeoutMs = this.connection.timeoutMs ?? DEFAULT_TIMEOUT_MS,
  ): Promise<Response> {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      return await fetch(this.baseUrl + path, { ...init, signal: controller.signal });
    } finally {
      clearTimeout(timer);
    }
  }

  private unauthorizedMessage(): string {
    const custom = this.connection.messages?.unauthorized;
    if (custom) return custom;
    switch (this.connection.auth.mode) {
      case 'bearer':
        return '401 Unauthorized — the bearer token is invalid or expired.';
      case 'cookie':
        return '401 Unauthorized — the login-token cookie is invalid or expired.';
      case 'basic':
        return '401 Unauthorized — check the user and password.';
    }
  }

  /**
   * Perform a request against an ACM endpoint and unwrap the
   * { status, message, data } envelope. Throws AcmHttpError with a
   * human-actionable message on auth failures.
   */
  async request<T>(
    method: HttpMethod,
    path: string,
    body?: unknown,
    retryOnCsrf = true,
    timeoutMs?: number,
  ): Promise<ApiResponse<T>> {
    // A timeout bounds the whole call, CSRF fetch and retry included.
    const deadline = timeoutMs !== undefined ? Date.now() + timeoutMs : undefined;
    const remaining = () => (deadline !== undefined ? Math.max(1, deadline - Date.now()) : undefined);
    const headers = this.baseHeaders();
    if (method !== 'GET') {
      const csrf = await this.ensureCsrfToken(false, remaining());
      if (csrf) headers['CSRF-Token'] = csrf;
      if (body !== undefined) headers['Content-Type'] = 'application/json';
    }

    const res = await this.rawFetch(
      path,
      { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined },
      remaining(),
    );

    const text = await res.text();

    if (res.status === 401) {
      throw new AcmHttpError(401, this.unauthorizedMessage(), text);
    }
    if (res.status === 403) {
      // Could be missing ACM permissions (3-level: api node, feature node,
      // script path) — or, in cookie mode, a stale CSRF token.
      if (this.connection.auth.mode === 'cookie' && retryOnCsrf && method !== 'GET') {
        await this.ensureCsrfToken(true, remaining());
        return this.request<T>(method, path, body, false, remaining());
      }
      throw new AcmHttpError(
        403,
        `403 Forbidden for ${path}. The user behind the credentials likely lacks ACM permissions. ACM authorizes at three levels (jcr:read required on each): the API node under /apps/acm/api, the feature node under /apps/acm/feature, and the script path under /conf/acm/settings/script. By default only administrators have access.`,
        text,
      );
    }

    let json: ApiResponse<T>;
    try {
      json = JSON.parse(text) as ApiResponse<T>;
    } catch {
      throw new AcmHttpError(
        res.status,
        `Non-JSON response (HTTP ${res.status}) from ${path}. Is ACM installed on this instance? First 500 chars:\n${text.slice(0, 500)}`,
        text,
      );
    }

    if (!res.ok) {
      throw new AcmHttpError(res.status, json.message || `HTTP ${res.status} from ${path}`, text);
    }
    return json;
  }

  /** GET that returns the raw body as text (for execution output download). */
  async requestRaw(path: string): Promise<{ status: number; contentType: string; text: string }> {
    const res = await this.rawFetch(path, { method: 'GET', headers: this.baseHeaders() });
    const text = await res.text();
    if (res.status === 401 || res.status === 403) {
      throw new AcmHttpError(res.status, `HTTP ${res.status} fetching ${path}`, text);
    }
    return { status: res.status, contentType: res.headers.get('content-type') || '', text };
  }
}
