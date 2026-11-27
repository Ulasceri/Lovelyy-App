// Renders tools/store-assets/src/*.html to PNGs at the exact Play sizes.
// Usage: node tools/store-assets/render.mjs [outDir]   (default assets/store/tr)
import { createRequire } from 'node:module';
import { readdirSync, mkdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

// require() also finds a globally installed Playwright (NODE_PATH), which ESM import does not.
const { chromium } = createRequire(import.meta.url)('playwright');
const here = dirname(fileURLToPath(import.meta.url));
const out = resolve(process.argv[2] ?? join(here, '../../assets/store/tr'));
mkdirSync(out, { recursive: true });
const browser = await chromium.launch();
for (const file of readdirSync(join(here, 'src')).filter((f) => f.endsWith('.html')).sort()) {
  const feature = file.startsWith('feature');
  const page = await browser.newPage({
    viewport: feature ? { width: 1024, height: 500 } : { width: 1080, height: 1920 },
    deviceScaleFactor: 1,
  });
  await page.goto(pathToFileURL(join(here, 'src', file)).href);
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: join(out, file.replace('.html', '.png')), omitBackground: false });
  await page.close();
}
await browser.close();
