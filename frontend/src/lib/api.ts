// Client HTTP de l'API : ajoute le jeton JWT et transforme les erreurs en ApiError.

const TOKEN_KEY = 'tunexa.token'

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch {
    // Stockage indisponible (navigation privée) : la session reste en mémoire
  }
}

export class ApiError extends Error {
  status: number
  code: string
  fields?: Record<string, string>

  constructor(status: number, code: string, message: string, fields?: Record<string, string>) {
    super(message)
    this.status = status
    this.code = code
    this.fields = fields
  }
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let res: Response
  try {
    res = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'network', 'Connexion impossible au serveur')
  }

  if (res.status === 204) return undefined as T
  const text = await res.text()
  let data: { code?: string; message?: string; fields?: Record<string, string> } | undefined
  try {
    data = text ? JSON.parse(text) : undefined
  } catch {
    data = undefined
  }
  if (!res.ok) {
    if (res.status === 401) {
      setToken(null)
      window.dispatchEvent(new Event('tunexa:logout'))
    }
    throw new ApiError(res.status, data?.code ?? 'error', data?.message ?? `Erreur ${res.status}`, data?.fields)
  }
  return data as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body ?? {}),
  delete: <T>(path: string) => request<T>('DELETE', path),
}

export function query(params: Record<string, string | number | boolean | undefined | null>): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') search.set(k, String(v))
  })
  const s = search.toString()
  return s ? `?${s}` : ''
}
