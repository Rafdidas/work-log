export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message)
  }
}

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    credentials: 'include',
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })
  const text = res.status === 204 ? '' : await res.text()
  const body = text ? JSON.parse(text) : undefined
  if (!res.ok) {
    throw new ApiError(res.status, body?.code ?? 'ERROR', body?.message ?? res.statusText)
  }
  return body as T
}

/** JSON 본문을 가진 요청 옵션을 만든다. */
export function jsonBody(method: 'POST' | 'PUT' | 'PATCH', data: unknown): RequestInit {
  return { method, body: JSON.stringify(data) }
}
