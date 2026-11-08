# Nuxt Minimal Starter

Look at the [Nuxt documentation](https://nuxt.com/docs/getting-started/introduction) to learn more.

## Setup

Make sure to install dependencies:

```bash
# npm
npm install

# pnpm
pnpm install

# yarn
yarn install

# bun
bun install
```

## Development Server

Start the development server on `http://localhost:3000`:

```bash
# npm
npm run dev

# pnpm
pnpm dev

# yarn
yarn dev

# bun
bun run dev
```

## Tests

```bash
# Unit- und Komponententests (Vitest)
pnpm test

# Einmalig: Browser für Playwright installieren
pnpm exec playwright install chromium webkit

# E2E-Tests (Playwright): bauen die App und antworten auf alle API-Aufrufe aus e2e/support/mock-api.ts,
# Backend und Datenbank sind nicht nötig. Mit E2E_FIREFOX=1 läuft zusätzlich Firefox.
pnpm test:e2e
```

## Production

Build the application for production:

```bash
# npm
npm run build

# pnpm
pnpm build

# yarn
yarn build

# bun
bun run build
```

Locally preview production build:

```bash
# npm
npm run preview

# pnpm
pnpm preview

# yarn
yarn preview

# bun
bun run preview
```

Check out the [deployment documentation](https://nuxt.com/docs/getting-started/deployment) for more information.
