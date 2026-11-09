import { expect, test } from '@playwright/test'

const VIEWPORTS = [
  { width: 360, height: 800, label: 'mobile 360px' },
  { width: 768, height: 1024, label: 'tablet 768px' },
  { width: 1280, height: 800, label: 'desktop 1280px' },
] as const

test.describe('HU-UX — Responsive', () => {
  for (const viewport of VIEWPORTS) {
    test(`home y catálogo sin overflow horizontal en ${viewport.label}`, async ({ page }) => {
      await page.setViewportSize({ width: viewport.width, height: viewport.height })

      await page.goto('/')
      await expect(page.getByRole('heading', { level: 1 }).first()).toBeVisible()
      const homeOverflow = await page.evaluate(
        () => document.documentElement.scrollWidth - window.innerWidth,
      )
      expect(homeOverflow).toBeLessThanOrEqual(1)

      await page.goto('/#/catalog')
      await expect(
        page.getByRole('heading', { name: 'Catálogo de autos certificados' }),
      ).toBeVisible()
      const catalogOverflow = await page.evaluate(
        () => document.documentElement.scrollWidth - window.innerWidth,
      )
      expect(catalogOverflow).toBeLessThanOrEqual(1)

      if (viewport.width < 1024) {
        await expect(page.getByRole('button', { name: 'Abrir menú' })).toBeVisible()
      } else {
        await expect(
          page.getByRole('banner').getByRole('button', { name: 'Productos' }),
        ).toBeVisible()
      }
    })
  }
})
