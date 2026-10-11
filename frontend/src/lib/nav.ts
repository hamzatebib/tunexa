/** N'accepte que les chemins internes pour la redirection après connexion. */
export function safeNext(next: string | null): string {
  return next && next.startsWith('/') && !next.startsWith('//') ? next : '/'
}
