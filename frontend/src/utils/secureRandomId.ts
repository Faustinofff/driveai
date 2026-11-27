/**
 * Identificador aleatorio generado solo con la API criptográfica del navegador
 * (SonarCloud S2245: sin Math.random ni valores predecibles como Date.now).
 * Usa crypto.randomUUID() y, en navegadores sin esa función, 16 bytes de
 * crypto.getRandomValues() en hexadecimal.
 */
export function secureRandomId(): string {
  const cryptoApi = globalThis.crypto
  if (cryptoApi && typeof cryptoApi.randomUUID === 'function') {
    return cryptoApi.randomUUID()
  }
  if (cryptoApi && typeof cryptoApi.getRandomValues === 'function') {
    const bytes = cryptoApi.getRandomValues(new Uint8Array(16))
    return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
  }
  throw new Error('El navegador no ofrece una API criptográfica para generar identificadores.')
}
