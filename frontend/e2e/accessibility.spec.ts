import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page, type TestInfo } from '@playwright/test'

/**
 * HU-UX — Accesibilidad (criterio Eje A: WCAG 2.2 AA sin violaciones críticas).
 *
 * axe-core analiza el DOM renderizado con las reglas WCAG 2.0/2.1/2.2 A y AA.
 * El test falla ante cualquier violación de impacto "critical". Las "serious"
 * se adjuntan al reporte de Playwright para registrarlas como defecto y
 * corregirlas, sin cortar el pipeline.
 */
const WCAG_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

const VIEWPORTS = [
  { width: 360, height: 800, label: '360px' },
  { width: 1280, height: 800, label: '1280px' },
] as const

async function analyze(page: Page, testInfo: TestInfo, name: string) {
  const results = await new AxeBuilder({ page }).withTags(WCAG_TAGS).analyze()

  await testInfo.attach(`axe-${name}.json`, {
    body: JSON.stringify(results.violations, null, 2),
    contentType: 'application/json',
  })

  const serious = results.violations.filter((v) => v.impact === 'serious')
  for (const v of serious) {
    testInfo.annotations.push({
      type: 'a11y-serious',
      description: `${v.id}: ${v.help} (${v.nodes.length} nodo/s)`,
    })
  }

  const critical = results.violations
    .filter((v) => v.impact === 'critical')
    .map((v) => `${v.id}: ${v.help} → ${v.nodes.map((n) => n.target.join(' ')).join(' | ')}`)

  expect(critical, `Violaciones críticas de accesibilidad en ${name}`).toEqual([])
}

test.describe('HU-UX — Accesibilidad WCAG 2.2 AA (axe)', () => {
  for (const viewport of VIEWPORTS) {
    test(`home sin violaciones críticas a ${viewport.label}`, async ({ page }, testInfo) => {
      await page.setViewportSize(viewport)
      await page.goto('/')
      await expect(page.getByRole('heading', { level: 1 }).first()).toBeVisible()
      await analyze(page, testInfo, `home-${viewport.label}`)
    })

    test(`catálogo sin violaciones críticas a ${viewport.label}`, async ({ page }, testInfo) => {
      await page.setViewportSize(viewport)
      await page.goto('/#/catalog')
      await expect(page.getByRole('button', { name: /Ver detalle de / }).first()).toBeVisible()
      await analyze(page, testInfo, `catalogo-${viewport.label}`)
    })

    test(`login sin violaciones críticas a ${viewport.label}`, async ({ page }, testInfo) => {
      await page.setViewportSize(viewport)
      await page.goto('/#/login')
      await expect(page.getByRole('heading', { name: 'Tu cuenta DriveAI' })).toBeVisible()
      await analyze(page, testInfo, `login-${viewport.label}`)
    })
  }

  test('modal de detalle sin violaciones críticas', async ({ page }, testInfo) => {
    await page.goto('/#/catalog')
    await page.getByRole('button', { name: /Ver detalle de / }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await analyze(page, testInfo, 'detalle-modal')
  })
})
