const DEFAULT_TIMEOUT_MS = 8000;

const baseUrl = import.meta.env.VITE_GATEWAY_BASE_URL ?? '/api';

interface RequestOptions extends RequestInit {
  timeoutMs?: number;
}

export async function apiRequest<T>(input: string, init: RequestOptions = {}): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), init.timeoutMs ?? DEFAULT_TIMEOUT_MS);

  try {
    const response = await fetch(`${baseUrl}${input}`, {
      ...init,
      headers: {
        'content-type': 'application/json',
        ...(init.headers ?? {})
      },
      signal: controller.signal
    });

    if (!response.ok) {
      const payload = await safeJson(response);
      throw new Error(`API ${response.status}: ${JSON.stringify(payload)}`);
    }

    return await response.json() as T;
  } finally {
    clearTimeout(timeout);
  }
}

async function safeJson(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return { message: await response.text() };
  }
}
