/*
 * Erzeugt die Mitglieder-Anleitung "Anleitung-Dienstplan.pdf" im Repository-Root.
 *
 * 1. startet einen kleinen Webserver für den statischen UI-Build (schedule-ui/.output/public),
 * 2. beantwortet alle API-Aufrufe mit erfundenen Beispieldaten (kein Backend nötig),
 * 3. nimmt die Screenshots auf (mit nummerierten Markierungen) und
 * 4. druckt anleitung.html mit Chromium als PDF.
 *
 * Aufruf (aus dem Repository-Root):
 *   (cd schedule-ui && pnpm exec nuxt generate)
 *   NODE_PATH=schedule-ui/node_modules node docs/anleitung-dienstplan/generate.cjs
 *
 * Voraussetzung: Playwright-Chromium ist installiert (pnpm exec playwright install chromium).
 */
const http = require('node:http')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright-core')

const HERE = __dirname
const ROOT = path.resolve(HERE, '../..')
const PUBLIC = path.join(ROOT, 'schedule-ui/.output/public')
const SHOTS = path.join(HERE, 'screenshots')
const OUTPUT = path.join(ROOT, 'Anleitung-Dienstplan.pdf')
const PORT = 3198
const NOW = new Date('2026-11-20T10:00:00+01:00')

// ---------------------------------------------------------------- Beispieldaten (frei erfunden)

// Wie in Produktion: Anmeldung über Vereinsflieger (VF-Modul) mit vereinsweiter Sicherheitsfrage.
const FEATURES = [
  { id: 'vfLoginFeature', description: 'Use Vereinsflieger.de for user authentication', active: true },
  { id: 'securityQuestionFeature', description: 'Security question on login', active: true }
]
const SECURITY_QUESTION = 'Wie lautet das Kennzeichen der ASK-21B?'
const USER = { id: 'u-maria', username: 'maria.muster@example.org', roles: ['ROLE_USER'], permissions: [] }
const day = date => `${date}T00:00:00.000+00:00`
const time = (date, hhmm) => `${date}T${hhmm}:00`

const setting = { rangeFrom: '01-01', rangeTo: '12-31', mandatoryAssignees: 1, saturdays: true, sundays: true, publicHolidays: true, startTime: '09:00', endTime: '17:00' }
const flugleiter = { id: 'svc-fl', servicename: 'Flugleiter', addAtImport: true, settings: [setting] }
const winde = { id: 'svc-wi', servicename: 'Windenfahrer', addAtImport: true, settings: [setting] }

const person = (id, firstname, lastname, email) => ({
  id, firstname, lastname, fullname: `${firstname} ${lastname}`, email, dateOfBirth: day('1975-05-01'), status: 'ACTIVE',
  minAssignments: 1, maxAssignments: 6, registrationKey: 'ACL-2027',
  participantServices: [{ service: flugleiter, autoAssign: true }, { service: winde, autoAssign: true }],
  services: [flugleiter, winde]
})
const maria = person('p-maria', 'Maria', 'Muster', USER.username)
const peter = person('p-peter', 'Peter', 'Beispiel', 'peter@example.org')
const jonas = person('p-jonas', 'Jonas', 'Probe', 'jonas@example.org')
const lena = person('p-lena', 'Lena', 'Test', 'lena@example.org')

const serviceDate = (id, date, service) => ({ id, date: day(date), mandatoryAssignees: 1, startTime: time(date, '09:00'), endTime: time(date, '17:00'), service })
const entry = (id, date, service, participant) => ({ id, date: day(date), startTime: time(date, '09:00'), endTime: time(date, '17:00'), service, participant })

const spring = ['2027-03-13', '2027-03-14', '2027-03-20', '2027-03-21']
const plans = [
  {
    id: 'plan-spring', name: 'Flugbetrieb Frühjahr 2027', description: 'Saisonstart im März',
    dateFrom: day('2027-03-13'), dateUntil: day('2027-03-31'), published: false, entries: [],
    serviceDates: spring.flatMap((date, index) => [serviceDate(`sd-f${index}`, date, flugleiter), serviceDate(`sd-w${index}`, date, winde)])
  },
  {
    id: 'plan-autumn', name: 'Flugbetrieb November/Dezember 2026', description: 'Letzte Flugtage des Jahres',
    dateFrom: day('2026-11-28'), dateUntil: day('2026-12-13'), published: true, serviceDates: [],
    entries: [
      entry('e1', '2026-11-28', flugleiter, maria), entry('e2', '2026-11-28', winde, peter),
      entry('e3', '2026-11-29', flugleiter, jonas), entry('e4', '2026-11-29', winde, lena),
      entry('e5', '2026-12-05', flugleiter, peter), entry('e6', '2026-12-05', winde, jonas),
      entry('e7', '2026-12-12', flugleiter, lena), entry('e8', '2026-12-12', winde, maria),
      entry('e9', '2026-12-13', flugleiter, jonas), entry('e10', '2026-12-13', winde, peter)
    ]
  },
  {
    id: 'plan-summer', name: 'Flugbetrieb Sommer 2027', description: 'Wird gerade vorbereitet',
    dateFrom: day('2027-06-01'), dateUntil: day('2027-08-31'), published: false, entries: [], serviceDates: []
  }
]

const myServices = [
  { id: 'e1', planId: 'plan-autumn', planName: 'Flugbetrieb November/Dezember 2026', serviceName: 'Flugleiter', startTime: '2026-11-28T09:00:00', endTime: '2026-11-28T17:00:00', colleagues: ['Peter Beispiel'] },
  { id: 'e8', planId: 'plan-autumn', planName: 'Flugbetrieb November/Dezember 2026', serviceName: 'Windenfahrer', startTime: '2026-12-12T09:00:00', endTime: '2026-12-12T17:00:00', colleagues: ['Lena Test'] }
]

let savedAvailabilities = []

const answer = (method, url, body) => {
  const p = url.pathname.replace(/^.*\/api\/v1/, '')
  let m
  if (p === '/featureflag/active') return FEATURES
  if (p === '/settings/common-setting/security_feature_question') return { settingValue: SECURITY_QUESTION }
  if (p === '/auth/signin') return { ...USER, token: 'token', type: 'Bearer' }
  if (p.startsWith('/participant/fetchByMail/')) return maria
  if (p === '/plan/list') return plans
  if ((m = p.match(/^\/plan\/fetch\/(.+)$/))) return plans.find(plan => plan.id === m[1])
  if (p.startsWith('/plan/participantAvailability/fetch/')) return savedAvailabilities
  if (p.startsWith('/plan/participantAvailability/save/') && method === 'POST') {
    savedAvailabilities = (body ?? []).map((row, index) => ({ id: `av${index}`, ...row, date: day(new Date(row.date).toISOString().slice(0, 10)) }))
    return null
  }
  if (p.startsWith('/plan/availableServices/')) return [flugleiter, winde]
  if (p === '/plan/mine') return myServices
  return null
}

// ---------------------------------------------------------------- Webserver für den statischen Build (SPA-Fallback)

const TYPES = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json', '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon', '.woff2': 'font/woff2', '.woff': 'font/woff', '.ttf': 'font/ttf' }

const serve = () => http.createServer((request, response) => {
  const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname)
  let file = path.join(PUBLIC, pathname)
  if (!file.startsWith(PUBLIC) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
    const index = path.join(file, 'index.html')
    file = fs.existsSync(index) ? index : path.join(PUBLIC, '200.html')
  }
  response.writeHead(200, { 'Content-Type': TYPES[path.extname(file)] ?? 'application/octet-stream' })
  fs.createReadStream(file).pipe(response)
}).listen(PORT)

// ---------------------------------------------------------------- Markierungen in den Screenshots

/** Rahmt Elemente ein und setzt eine Nummer daneben (side: left, right, top, bottom), wie ① ② im Text der Anleitung. */
const mark = async (page, marks) => {
  for (const { target, n, pad = 6, side = 'left' } of marks) {
    const box = await target.boundingBox()
    if (!box) throw new Error(`Markierung ${n}: Element nicht sichtbar`)
    await page.evaluate(({ box, n, pad, side }) => {
      const x = box.x + window.scrollX
      const y = box.y + window.scrollY
      const ring = document.createElement('div')
      Object.assign(ring.style, {
        position: 'absolute', left: `${x - pad}px`, top: `${y - pad}px`, width: `${box.width + 2 * pad}px`, height: `${box.height + 2 * pad}px`,
        border: '3px solid #d9480f', borderRadius: '10px', zIndex: 9998, pointerEvents: 'none'
      })
      const badge = document.createElement('div')
      badge.textContent = String(n)
      const size = 30
      // the number sits next to the frame, so it never covers a label
      const middleX = x + box.width / 2 - size / 2
      const middleY = y + box.height / 2 - size / 2
      const left = { left: x - pad - size - 5, right: x + box.width + pad + 5, top: middleX, bottom: middleX }[side]
      const top = { left: middleY, right: middleY, top: y - pad - size - 5, bottom: y + box.height + pad + 5 }[side]
      Object.assign(badge.style, {
        position: 'absolute', left: `${left}px`, top: `${top}px`, width: `${size}px`, height: `${size}px`,
        borderRadius: '50%', background: '#d9480f', color: '#fff', font: 'bold 17px/30px sans-serif', textAlign: 'center',
        boxShadow: '0 0 0 3px #fff', zIndex: 9999, pointerEvents: 'none'
      })
      document.body.append(ring, badge)
    }, { box, n, pad, side })
  }
}

const clearMarks = page => page.evaluate(() => document.querySelectorAll('body > div[style*="z-index: 99"]').forEach(node => node.remove()))

// ---------------------------------------------------------------- Screenshots

const settle = page => page.waitForTimeout(1200)

const shoot = async (page, name, clip) => {
  await page.screenshot({ path: path.join(SHOTS, `${name}.png`), fullPage: !clip, clip })
  console.log('  ✓', name)
}

const desktop = async (browser) => {
  const context = await browser.newContext({ viewport: { width: 1100, height: 760 }, deviceScaleFactor: 2, locale: 'de-DE', timezoneId: 'Europe/Berlin' })
  const page = await context.newPage()
  await page.clock.setFixedTime(NOW)
  await page.route(/\/api\/v1\//, route => {
    const request = route.request()
    const body = answer(request.method(), new URL(request.url()), request.postDataJSON?.() ?? null)
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) })
  })
  const base = `http://localhost:${PORT}`

  // 1. Anmelden – die Antwort auf die Sicherheitsfrage bleibt bewusst leer, sie gehört nicht in die Anleitung
  await page.goto(`${base}/login`)
  await settle(page)
  await page.getByLabel('Benutzername').fill(USER.username)
  await page.getByLabel('Passwort', { exact: true }).fill('Segelflug!2027')
  await page.getByText(SECURITY_QUESTION).waitFor()
  const answerField = page.getByRole('textbox', { name: 'Antwort auf Sicherheitsfrage' })
  const code = page.getByLabel('Sicherheitscode (bei aktivierter 2FA)')
  await mark(page, [
    { target: page.getByLabel('Benutzername'), n: 1 },
    { target: page.getByLabel('Passwort', { exact: true }), n: 2 },
    { target: answerField, n: 3 },
    { target: code, n: 4 },
    { target: page.getByRole('button', { name: 'Anmelden' }), n: 5 }
  ])
  const card = await page.locator('form').boundingBox()
  await shoot(page, '01-anmelden', { x: card.x - 90, y: card.y - 110, width: card.width + 180, height: card.height + 150 })
  await clearMarks(page)
  await page.getByRole('button', { name: 'Anmelden' }).click()
  await page.waitForURL(`${base}/`)
  await settle(page)

  // 2. Startseite
  await mark(page, [
    { target: page.getByRole('main').getByRole('link', { name: 'Meine Dienste' }), n: 1 },
    { target: page.getByRole('main').getByRole('link', { name: 'Dienstpläne' }), n: 2, side: 'right' },
    { target: page.getByRole('button', { name: 'Benutzerkonto' }), n: 3, pad: 4, side: 'bottom' }
  ])
  await shoot(page, '02-startseite', { x: 0, y: 0, width: 1100, height: 440 })
  await clearMarks(page)

  // 3. Übersicht der Dienstpläne
  await page.goto(`${base}/plan/list`)
  await settle(page)
  const cards = page.locator('article')
  await mark(page, [
    { target: cards.nth(0).locator('span', { hasText: /^Offen$/ }), n: 1, pad: 4, side: 'right' },
    { target: cards.nth(0).getByRole('button', { name: 'Eintragen' }), n: 2 },
    { target: cards.nth(1).locator('span', { hasText: /^Veröffentlicht$/ }), n: 3, pad: 4, side: 'right' },
    { target: cards.nth(1).getByRole('button', { name: 'Dienstplan' }), n: 4 },
    { target: cards.nth(1).getByRole('button', { name: 'PDF' }), n: 5, side: 'right' },
    { target: cards.nth(2).locator('span', { hasText: /^In Vorbereitung$/ }), n: 6, pad: 4, side: 'right' }
  ])
  await shoot(page, '03-dienstplaene', { x: 0, y: 70, width: 1100, height: 520 })
  await clearMarks(page)

  // 4. Verfügbarkeit eintragen
  await page.goto(`${base}/plan/availabilities/me/plan-spring`)
  await settle(page)
  const rows = page.getByTestId('availability')

  await rows.nth(1).getByRole('button', { name: 'Ich kann nicht' }).click()
  await rows.nth(2).getByRole('button', { name: 'Möchte ich gerne' }).click()
  await settle(page)
  await rows.nth(2).getByRole('combobox').click()
  await page.getByRole('option', { name: 'Windenfahrer' }).click()
  await page.waitForTimeout(400)
  await mark(page, [
    { target: rows.nth(0).getByRole('button', { name: 'Ich kann', exact: true }), n: 1, pad: 4 },
    { target: rows.nth(1).getByRole('button', { name: 'Ich kann nicht' }), n: 2, pad: 4, side: 'bottom' },
    { target: rows.nth(2).getByRole('button', { name: 'Möchte ich gerne' }), n: 3, pad: 4, side: 'right' },
    { target: rows.nth(2).getByRole('combobox'), n: 4, pad: 4 },
    { target: page.getByText('Noch nicht gespeichert'), n: 5, pad: 4 },
    { target: page.getByRole('button', { name: 'Speichern' }), n: 6, pad: 4, side: 'right' }
  ])
  await shoot(page, '04-verfuegbarkeit-ausgefuellt', { x: 120, y: 70, width: 900, height: 650 })
  await clearMarks(page)

  await page.getByRole('button', { name: 'Speichern' }).click()
  await settle(page)
  // after saving a success message moves the content down: cut around the save bar itself
  const saved = page.getByText('Gespeichert', { exact: true })
  await mark(page, [{ target: saved, n: 1, pad: 4 }])
  const bar = await saved.locator('xpath=ancestor::div[1]').boundingBox()
  await shoot(page, '05-verfuegbarkeit-gespeichert', { x: bar.x - 50, y: bar.y - 25, width: bar.width + 100, height: bar.height + 50 })
  await clearMarks(page)

  // 5. Fertiger Dienstplan
  await page.goto(`${base}/plan/generated/plan-autumn`)
  await settle(page)
  await mark(page, [
    { target: page.getByRole('cell', { name: /Muster, Maria/ }).first(), n: 1, pad: 2 },
    { target: page.getByRole('navigation').last(), n: 2, pad: 4 }
  ])
  await shoot(page, '06-fertiger-dienstplan', { x: 0, y: 70, width: 1100, height: 620 })
  await clearMarks(page)

  // 6. Meine Dienste
  await page.goto(`${base}/plan/mine`)
  await settle(page)
  await mark(page, [
    { target: page.locator('[data-service="e1"]'), n: 1, pad: 4 },
    { target: page.getByRole('button', { name: 'Kalender (ICS)' }), n: 2, pad: 4, side: 'right' },
    { target: page.getByRole('switch'), n: 3, pad: 4 }
  ])
  await shoot(page, '07-meine-dienste', { x: 0, y: 70, width: 1100, height: 520 })
  await clearMarks(page)

  // 7. Abmelden
  await page.getByRole('button', { name: 'Benutzerkonto' }).click()
  await page.waitForTimeout(500)
  await mark(page, [{ target: page.getByRole('menuitem', { name: 'Abmelden' }), n: 1, pad: 4 }])
  await shoot(page, '10-abmelden', { x: 650, y: 0, width: 450, height: 150 })

  await context.close()
}

const mobile = async (browser) => {
  const context = await browser.newContext({ viewport: { width: 390, height: 780 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true, locale: 'de-DE', timezoneId: 'Europe/Berlin' })
  await context.addInitScript(user => {
    localStorage.setItem('user', JSON.stringify(user))
    localStorage.setItem('session', JSON.stringify({ token: 'token', type: 'Bearer' }))
  }, USER)
  const page = await context.newPage()
  await page.clock.setFixedTime(NOW)
  await page.route(/\/api\/v1\//, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(answer(route.request().method(), new URL(route.request().url()), null)) }))
  await page.goto(`http://localhost:${PORT}/`)
  await settle(page)
  await mark(page, [{ target: page.getByRole('button', { name: 'Menü öffnen' }), n: 1, pad: 4 }])
  await shoot(page, '08-handy-start', { x: 0, y: 0, width: 390, height: 640 })
  await clearMarks(page)
  await page.getByRole('button', { name: 'Menü öffnen' }).click()
  await page.waitForTimeout(800)
  await mark(page, [
    { target: page.getByRole('link', { name: 'Dienstpläne anzeigen' }), n: 2, pad: 2 },
    { target: page.getByRole('link', { name: 'Meine Dienste' }), n: 3, pad: 2 }
  ])
  await shoot(page, '09-handy-menue', { x: 0, y: 0, width: 390, height: 400 })
  await context.close()
}

// ---------------------------------------------------------------- Ablauf

;(async () => {
  if (!fs.existsSync(path.join(PUBLIC, '200.html'))) {
    throw new Error(`Kein UI-Build gefunden. Zuerst: (cd schedule-ui && pnpm exec nuxt generate)`)
  }
  fs.rmSync(SHOTS, { recursive: true, force: true })
  fs.mkdirSync(SHOTS, { recursive: true })
  const server = serve()
  const browser = await chromium.launch()
  try {
    console.log('Screenshots')
    await desktop(browser)
    await mobile(browser)

    console.log('PDF')
    const page = await browser.newPage()
    await page.goto(`file://${path.join(HERE, 'anleitung.html')}`)
    await page.evaluate(() => document.fonts.ready)
    await page.pdf({
      path: OUTPUT, format: 'A4', printBackground: true,
      margin: { top: '16mm', bottom: '18mm', left: '16mm', right: '16mm' },
      displayHeaderFooter: true,
      headerTemplate: '<span></span>',
      footerTemplate: '<div style="width:100%;font:9px sans-serif;color:#78716c;padding:0 16mm;display:flex;justify-content:space-between"><span>Dienstplan – Anleitung für Mitglieder</span><span>Seite <span class="pageNumber"></span> von <span class="totalPages"></span></span></div>'
    })
    console.log('  ✓', path.relative(ROOT, OUTPUT))
  } finally {
    await browser.close()
    server.close()
  }
})().catch(error => {
  console.error(error)
  process.exit(1)
})
