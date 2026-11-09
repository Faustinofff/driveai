import { defineConfig, devices } from '@playwright/test'

/**
 * Configuración E2E de DriveAI.
 *
 * Requiere el backend en http://localhost:8080 y PostgreSQL. El frontend se
 * levanta automáticamente con `npm run dev` (o se reutiliza si ya está arriba).
 *
 * Variables opcionales:
 *   E2E_BASE_URL   URL del frontend (default http://localhost:5173)
 *   ADMIN_EMAIL    email del administrador de la demo (habilita el escenario admin)
 *   ADMIN_PASSWORD contraseña del administrador
 */
const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI
    ? [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]]
    : [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  timeout: 60_000,
  expect: { timeout: 15_000 },
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 15_000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } },
    },
  ],
  webServer: process.env.E2E_NO_WEB_SERVER
    ? undefined
    : {
        command: 'npm run dev',
        url: baseURL,
        reuseExistingServer: true,
        timeout: 120_000,
        env: {
          VITE_API_URL: process.env.VITE_API_URL ?? 'http://localhost:8080',
        },
      },
})
