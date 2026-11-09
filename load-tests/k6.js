import http from 'k6/http'
import { check, sleep } from 'k6'
import { Rate } from 'k6/metrics'

/**
 * DriveAI — prueba de carga obligatoria.
 *
 * Escenario: 50 usuarios virtuales durante 5 minutos sobre tres endpoints
 * críticos del e-commerce:
 *   1. GET /api/cars            → listado del catálogo (camino más transitado)
 *   2. GET /api/cars/{id}       → detalle de vehículo
 *   3. GET /api/cart            → carrito del usuario autenticado (JWT)
 *
 * Objetivos (thresholds):
 *   - p95 < 500 ms
 *   - tasa de error < 1 %
 *
 * Ejecución:
 *   k6 run load-tests/k6.js
 *   BASE_URL=http://localhost:8080 k6 run load-tests/k6.js
 *
 * El endpoint /api/cart requiere un usuario real; se crea en setup() vía
 * /api/auth/register. No se prueba /api/auth/login para no mezclar el rate
 * limiting anti fuerza bruta (5/min) con la medición de performance.
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const CAR_ID = __ENV.CAR_ID || '1'

const errorRate = new Rate('business_errors')

export const options = {
  scenarios: {
    critical_paths: {
      executor: 'constant-vus',
      vus: 50,
      duration: '5m',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    business_errors: ['rate<0.01'],
  },
}

function uniqueEmail() {
  return `load-${Date.now()}-${Math.random().toString(36).slice(2, 10)}@driveai.test`
}

export function setup() {
  const email = uniqueEmail()
  const password = 'LoadTestPassw0rd!'
  const register = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ email, password, fullName: 'Load Test User' }),
    { headers: { 'Content-Type': 'application/json' } },
  )

  if (register.status !== 201) {
    throw new Error(`No se pudo crear el usuario de carga: ${register.status} ${register.body}`)
  }

  const token = JSON.parse(register.body).token

  // Se resuelve un ID real del catálogo para que el detalle mida un 200 y no
  // un 404 si el seed no incluye el ID configurado por defecto.
  let carId = CAR_ID
  const catalog = http.get(`${BASE_URL}/api/cars?page=0&size=1`, {
    headers: { Accept: 'application/json' },
  })
  if (catalog.status === 200) {
    try {
      const first = JSON.parse(catalog.body).content?.[0]
      if (first?.id) carId = String(first.id)
    } catch (_) {
      // Si el catálogo no es parseable se conserva el CAR_ID de entorno.
    }
  }

  return { token, carId }
}

export default function (data) {
  const headers = { Accept: 'application/json' }
  const choice = Math.floor(Math.random() * 3)
  let response

  if (choice === 0) {
    response = http.get(`${BASE_URL}/api/cars?page=0&size=12`, { headers, tags: { endpoint: 'catalog' } })
  } else if (choice === 1) {
    response = http.get(`${BASE_URL}/api/cars/${data.carId}`, { headers, tags: { endpoint: 'detail' } })
  } else {
    response = http.get(`${BASE_URL}/api/cart`, {
      headers: { ...headers, Authorization: `Bearer ${data.token}` },
      tags: { endpoint: 'cart' },
    })
  }

  const ok = check(response, {
    'status es 200': (r) => r.status === 200,
    'respuesta JSON': (r) => (r.headers['Content-Type'] || '').includes('application/json'),
  })
  // La latencia se mide con el threshold p(95)<500; no se mezcla con la tasa
  // de error funcional, que debe reflejar solo respuestas inválidas.
  check(response, { 'latencia < 500 ms': (r) => r.timings.duration < 500 })
  errorRate.add(!ok)

  sleep(1)
}

export function handleSummary(data) {
  const summary = {
    generatedAt: new Date().toISOString(),
    baseUrl: BASE_URL,
    scenario: { vus: 50, duration: '5m', endpoints: ['/api/cars', '/api/cars/{id}', '/api/cart'] },
    metrics: {
      http_req_duration: data.metrics.http_req_duration,
      http_req_failed: data.metrics.http_req_failed,
      iterations: data.metrics.iterations,
      business_errors: data.metrics.business_errors,
    },
    thresholds: data.root_group.checks,
  }
  return {
    'load-tests/summary.json': JSON.stringify(summary, null, 2),
    stdout: `\nDriveAI load test summary written to load-tests/summary.json\n`,
  }
}
