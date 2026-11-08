import { defineConfig, devices } from '@playwright/test'

const PORT = Number(process.env.E2E_PORT ?? 3100)

/**
 * The suite runs against a production build and answers every API call from
 * e2e/support/mock-api.ts, so neither a backend nor a database is needed.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? 'github' : [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: `http://localhost:${PORT}`,
    locale: 'de-DE',
    timezoneId: 'Europe/Berlin',
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    // Opt-in: the Playwright Firefox build fails to start on some macOS versions ("Could not find profile folder").
    ...(process.env.E2E_FIREFOX ? [{ name: 'firefox', use: { ...devices['Desktop Firefox'] } }] : []),
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
    { name: 'mobile-chrome', use: { ...devices['Pixel 7'] } },
    { name: 'mobile-safari', use: { ...devices['iPhone 14'] } },
  ],
  webServer: {
    command: 'pnpm nuxt build && node .output/server/index.mjs',
    url: `http://localhost:${PORT}`,
    env: { PORT: String(PORT) },
    timeout: 240_000,
    reuseExistingServer: !process.env.CI,
  },
})
