import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 90000,
  expect: {
    toHaveScreenshot: {
      maxDiffPixelRatio: 0.01,
      animations: 'disabled',
    },
  },
  use: {
    baseURL: 'http://127.0.0.1:8081',
    headless: true,
  },
  webServer: {
    command: 'cd .. && bash v25-it-server-start.sh',
    url: 'http://127.0.0.1:8081/erte-test/toolbar',
    reuseExistingServer: true,
    timeout: 120000,
  },
});
