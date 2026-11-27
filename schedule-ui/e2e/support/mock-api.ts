import type { Page, Route } from '@playwright/test'

/**
 * In-memory stand-in for schedule-api. It lets the E2E suite run without a
 * database and exercises the UI against realistic payloads. Every request is
 * recorded so tests can assert on what the frontend actually sent.
 */

export type RecordedRequest = { method: string, path: string, query: URLSearchParams, body: any }

const ROLES: Record<string, string[]> = {
  'admin@example.org': ['ROLE_ADMIN'],
  'schedule@example.org': ['ROLE_SCHEDULE_ADMIN'],
  'alarm@example.org': ['ROLE_ALARM_ADMIN'],
  'user@example.org': ['ROLE_USER'],
  'nobody@example.org': ['ROLE_USER'],
  'delegate@example.org': ['ROLE_USER'],
}

const PERMISSIONS: Record<string, string[]> = {
  'delegate@example.org': ['PERMISSION_SCHEDULE_MANAGE'],
}

export type Role = 'admin' | 'schedule' | 'alarm' | 'user' | 'nobody' | 'delegate'

export const sessionFor = (role: Role) => {
  const username = `${role}@example.org`
  return {
    user: { id: `u-${role}`, username, roles: ROLES[username], permissions: PERMISSIONS[username] ?? [] },
    session: { token: `token-${role}`, type: 'Bearer' },
  }
}

const day = (date: string) => `${date}T00:00:00.000+00:00`
const time = (date: string, hhmm: string) => `${date}T${hhmm}:00.000+00:00`

const setting = {
  rangeFrom: '01-01', rangeTo: '12-01', mandatoryAssignees: 2,
  saturdays: true, sundays: true, publicHolidays: false, startTime: '09:00', endTime: '17:00',
}

function createState() {
  const early = { id: 's1', servicename: 'Frühdienst', addAtImport: true, settings: [{ ...setting }] }
  const late = { id: 's2', servicename: 'Spätdienst', addAtImport: false, settings: [{ ...setting, startTime: '13:00', endTime: '20:00' }] }

  const person = (id: string, firstname: string, lastname: string, status: string, services: any[]) => ({
    id, firstname, lastname, fullname: `${firstname} ${lastname}`,
    email: `${firstname.toLowerCase()}@example.org`, dateOfBirth: day('1990-03-15'), status,
    minAssignments: 2, maxAssignments: 5, registrationKey: `KEY-${id}`,
    participantServices: services.map(service => ({ service, autoAssign: true })),
    services,
  })

  const anna = person('p1', 'Anna', 'Albers', 'ACTIVE', [early, late])
  const ben = person('p2', 'Ben', 'Becker', 'INACTIVE', [early])
  // The participant linked to the "user" login.
  const user = { ...person('p3', 'Uta', 'User', 'ACTIVE', [early, late]), email: 'user@example.org' }

  const serviceDate = (id: string, date: string, service: any) => ({
    id, date: day(date), mandatoryAssignees: 2, startTime: '09:00', endTime: '17:00', service,
  })
  const entry = (id: string, date: string, service: any, participant: any) => ({
    id, date: day(date), startTime: time(date, '08:00'), endTime: time(date, '16:00'), service, participant,
  })

  const openPlan = {
    id: 'plan-open', name: 'Winterplan', description: 'Dezember 2026',
    dateFrom: day('2026-12-01'), dateUntil: day('2026-12-31'), published: false, entries: [],
    serviceDates: [
      serviceDate('sd1', '2026-12-05', early), serviceDate('sd2', '2026-12-05', late),
      serviceDate('sd3', '2026-12-06', early),
    ],
  }
  const publishedPlan = {
    id: 'plan-published', name: 'Herbstplan', description: 'Oktober 2026',
    dateFrom: day('2026-10-01'), dateUntil: day('2026-10-31'), published: true,
    serviceDates: [serviceDate('sd4', '2026-10-03', early)],
    entries: [
      entry('e1', '2026-10-03', early, anna),
      entry('e2', '2026-10-03', late, ben),
      entry('e3', '2026-10-04', early, ben),
    ],
  }
  const emptyPlan = {
    id: 'plan-empty', name: 'Neujahrsplan', description: 'Januar 2027',
    dateFrom: day('2027-01-01'), dateUntil: day('2027-01-11'), published: false, entries: [], serviceDates: [],
  }

  return {
    services: [early, late],
    participants: [anna, ben, user],
    plans: [openPlan, publishedPlan, emptyPlan] as any[],
    availabilities: [] as any[],
    features: [] as any[],
    importDiff: {
      newParticipants: [person('p9', 'Nora', 'Neu', 'ACTIVE', [early])],
      updatedParticipants: [{ ...anna, firstname: 'Annika' }],
      missingParticipants: [ben],
      unchangedParticipants: [user],
    },
    requests: [{
      id: 'r1', headline: 'Kalender hakt', description: 'Der Kalender reagiert nicht.', createdAt: time('2026-11-01', '10:00'),
      status: 'OPEN', type: 'BUG', author: anna,
    }, {
      id: 'r2', headline: 'Dunkles Logo', description: 'Bitte neues Logo.', createdAt: time('2026-11-02', '10:00'),
      status: 'IN_PROGRESS', type: 'FEATURE', author: ben,
    }] as any[],
    alarms: [
      { id: 'a1', timestamp: '2026-05-07T20:38:45', rfidId: 'AABBCC', status: 1 },
      { id: 'a2', timestamp: '2026-05-07T19:00:00', rfidId: 'DDEEFF', status: 0 },
      { id: 'a3', timestamp: '2026-05-07T18:00:00', rfidId: null, armed: true, zone2: true },
    ],
    commonSettings: [
      { settingKey: 'min_participation_count', settingValue: '3' },
      { settingKey: 'security_feature_question', settingValue: 'Lieblingsfarbe?' },
    ],
    selectedState: { country: { id: 'de' }, state: { id: 'be' } },
    users: [
      { id: 'u-admin', username: 'admin@example.org', fullname: null, roles: ['ROLE_ADMIN'], permissions: [] },
      { id: 'u-schedule', username: 'schedule@example.org', fullname: null, roles: ['ROLE_SCHEDULE_ADMIN'], permissions: [] },
      { id: 'u-user', username: 'user@example.org', fullname: 'User, Uta', roles: ['ROLE_USER'], permissions: ['PERMISSION_ALARM_MANAGE'] },
      { id: 'u-ben', username: 'ben@example.org', fullname: 'Becker, Ben', roles: ['ROLE_USER'], permissions: [] },
    ] as any[],
    // maintenance: an aircraft and the winch, with the defect book and the notification about an assignment
    aircraft: [
      { id: 'a1', type: 'ASK 21', registration: 'D-1234', workshop: null, unscheduledTaskCount: 0, openDefectCount: 1 },
      { id: 'w1', type: 'Winde', registration: 'Winde 1', workshop: null, unscheduledTaskCount: 0, openDefectCount: 1 },
    ] as any[],
    defects: [
      defect({ id: 'd1', aircraftId: 'a1', aircraftLabel: 'D-1234 · ASK 21', title: 'Riss in der Haube', priority: 'HIGH', assignees: [{ id: 'u-user', name: 'User, Uta' }], assignedToMe: true }),
      defect({ id: 'd2', aircraftId: 'w1', aircraftLabel: 'Winde 1 · Winde', title: 'Seil ausgefranst', status: 'IN_PROGRESS' }),
      defect({ id: 'd3', aircraftId: 'a1', aircraftLabel: 'D-1234 · ASK 21', title: 'Reifen platt', status: 'DONE', doneBy: { id: 'u-ben', name: 'Becker, Ben' }, doneAt: time('2026-11-12', '10:00') }),
    ] as any[],
    defectEvents: {} as Record<string, any[]>,
    notifications: [
      { id: 'n1', type: 'DEFECT_ASSIGNED', createdAt: time('2026-11-10', '11:00'), read: false, actor: { id: 'u-admin', name: 'admin@example.org' }, defect: { id: 'd1', title: 'Riss in der Haube', aircraftId: 'a1', aircraftLabel: 'D-1234', status: 'OPEN' } },
    ] as any[],
    comments: {
      r1: [{ id: 'c1', content: 'Tritt bei mir auch auf.', createdAt: time('2026-11-03', '09:30'), author: 'Becker, Ben' }],
    } as Record<string, any[]>,
  }
}

const defect = (values: Record<string, any>) => ({
  description: null, priority: 'NORMAL', status: 'OPEN', reportedBy: { id: 'u-ben', name: 'Becker, Ben' }, reportedAt: time('2026-11-10', '10:00'),
  doneBy: null, doneAt: null, signedBy: null, signedAt: null, assignees: [], assignedToMe: false, commentCount: 0, task: null,
  editable: true, deletable: false, ...values,
})

const page_ = <T>(items: T[], query: URLSearchParams) => {
  const page = Number(query.get('page') ?? 0)
  const size = Number(query.get('size') ?? 20)
  return { content: items.slice(page * size, (page + 1) * size), totalElements: items.length }
}

export class MockApi {
  state = createState()
  requests: RecordedRequest[] = []
  private failures: { method: string, path: RegExp, status: number, body: any }[] = []

  /** Makes matching requests answer with the given error until the test ends. */
  fail(method: string, path: RegExp, status = 500, body: any = { message: 'Serverfehler' }) {
    this.failures.push({ method, path, status, body })
  }

  /** All recorded requests for a method/path, e.g. to assert on a payload. */
  sent(method: string, path: string | RegExp) {
    return this.requests.filter(r => r.method === method && (typeof path === 'string' ? r.path === path : path.test(r.path)))
  }

  async install(page: Page) {
    await page.route(/\/api\/v1\//, route => this.handle(route))
  }

  private async handle(route: Route) {
    const request = route.request()
    const headers = {
      'access-control-allow-origin': '*',
      'access-control-allow-headers': '*',
      'access-control-allow-methods': '*',
    }
    if (request.method() === 'OPTIONS') {
      return route.fulfill({ status: 204, headers })
    }

    const url = new URL(request.url())
    const path = url.pathname.replace(/^.*\/api\/v1/, '')
    let body: any = null
    try {
      body = request.postDataJSON()
    } catch {
      body = request.postData()
    }
    this.requests.push({ method: request.method(), path, query: url.searchParams, body })

    const failure = this.failures.find(f => f.method === request.method() && f.path.test(path))
    if (failure) {
      return route.fulfill({ status: failure.status, headers, json: failure.body })
    }

    const answer = this.respond(request.method(), path, url.searchParams, body)
    if (answer === undefined) {
      return route.fulfill({ status: 404, headers, json: { message: `Kein Mock für ${request.method()} ${path}` } })
    }
    if (answer instanceof HttpError) {
      return route.fulfill({ status: answer.status, headers, json: { message: answer.message } })
    }
    if (answer instanceof RawResponse) {
      return route.fulfill({ status: 200, headers, contentType: answer.contentType, body: answer.body })
    }
    return route.fulfill({ status: answer === null ? 204 : 200, headers, json: answer === null ? undefined : answer })
  }

  private respond(method: string, path: string, query: URLSearchParams, body: any): any {
    const s = this.state
    const key = `${method} ${path}`
    let m: RegExpMatchArray | null

    if (key === 'GET /featureflag/active') return s.features
    if (key === 'POST /auth/signin') {
      const roles = ROLES[body.username]
      if (!roles || body.password === 'wrong') return new HttpError(401)
      return { id: `u-${body.username}`, username: body.username, roles, token: 'token', type: 'Bearer' }
    }
    if (key === 'POST /auth/signup') return { message: 'ok' }

    // participants
    if ((m = path.match(/^\/participant\/fetchByMail\/(.+)$/))) {
      const mail = decodeURIComponent(m[1]!)
      return s.participants.find(p => p.email === mail) ?? (mail === 'nobody@example.org' ? null : s.participants[0])
    }
    if (key === 'GET /participant/list') return s.participants
    if (key === 'GET /participant/page') {
      const search = (query.get('search') ?? '').toLowerCase()
      const [sort = 'lastname', direction] = (query.get('sort') ?? '').split(',')
      const items = s.participants
        .filter(p => `${p.firstname} ${p.lastname} ${p.email}`.toLowerCase().includes(search))
        .sort((a: any, b: any) => String(a[sort]).localeCompare(String(b[sort])) * (direction === 'desc' ? -1 : 1))
      return page_(items, query)
    }
    if (key === 'POST /participant/save') {
      const saved = { ...(typeof body === 'string' ? JSON.parse(body) : body) }
      const index = s.participants.findIndex(p => p.id === saved.id)
      if (index >= 0) s.participants[index] = saved
      else s.participants.push({ ...saved, id: `p${Date.now()}` })
      return saved
    }
    if ((m = path.match(/^\/participant\/delete\/(.+)$/)) && method === 'DELETE') {
      s.participants = s.participants.filter(p => p.id !== m![1])
      return null
    }
    if (key === 'POST /participant/import') return s.importDiff

    // services
    if (key === 'GET /service/list') return s.services
    if (key === 'POST /service/save') {
      const saved = typeof body === 'string' ? JSON.parse(body) : body
      const index = s.services.findIndex(x => x.id === saved.id)
      if (index >= 0) s.services[index] = saved
      else s.services.push({ ...saved, id: `s${Date.now()}` })
      return saved
    }
    if ((m = path.match(/^\/service\/delete\/(.+)$/)) && method === 'DELETE') {
      s.services = s.services.filter(x => x.id !== m![1])
      return null
    }

    // plans
    if (key === 'GET /plan/list') return s.plans
    if (key === 'POST /plan/save') {
      const saved = { ...body, id: 'plan-new', serviceDates: [], entries: [] }
      s.plans.push(saved)
      return saved
    }
    if ((m = path.match(/^\/plan\/fetch\/(.+)$/))) return s.plans.find(p => p.id === m![1]) ?? new HttpError(404)
    if ((m = path.match(/^\/plan\/delete\/(.+)$/)) && method === 'DELETE') {
      s.plans = s.plans.filter(p => p.id !== m![1])
      return null
    }
    if ((m = path.match(/^\/plan\/generate\/(.+)$/))) {
      const plan = s.plans.find(p => p.id === m![1])
      if (plan) plan.published = true
      return null
    }
    if ((m = path.match(/^\/plan\/unfilled\/(.+)$/))) {
      // the generator found nobody for the late service on December 5th
      return m[1] === 'plan-open'
        ? [{ date: '2026-12-05', serviceId: 'late', servicename: 'Spätdienst', startTime: '09:00:00', endTime: '17:00:00', required: 2, assigned: 0 }]
        : []
    }
    if ((m = path.match(/^\/plan\/serviceDates\/save\/(.+)$/))) return null
    if ((m = path.match(/^\/plan\/participantAvailability\/fetch\/([^/]+)\/([^/]+)$/))) {
      return s.availabilities.filter(a => a.participant.id === m![2])
    }
    if (path.startsWith('/plan/participantAvailability/fetch/')) return s.availabilities
    if (path.startsWith('/plan/participantAvailability/save/')) return null
    if (path.startsWith('/plan/availableServices/')) return s.services
    if (path.startsWith('/plan/updatePlanEntry/')) {
      return body.map((entry: any, i: number) => ({ ...entry, id: `updated-${i}` }))
    }
    if (path.startsWith('/plan/statistics/')) {
      return [
        { participant: s.participants[0], serviceCount: { s1: 3, s2: 1 }, totalCount: 4 },
        { participant: s.participants[1], serviceCount: { s1: 2 }, totalCount: 2 },
      ]
    }
    if (path.startsWith('/plan/download/pdf/')) return new RawResponse('application/pdf', '%PDF-1.4 mock')
    if (path.startsWith('/plan/download/csv/')) return new RawResponse('text/csv', 'Datum;Name\n03.10.2026;Müller')

    // settings
    if (path.startsWith('/settings/holidays/')) return ['2027-01-06T00:00:00.000+00:00']
    if (path.startsWith('/settings/common-setting/')) return { settingValue: 'Wie heißt dein Haustier?' }
    if (key === 'GET /settings/common-settings') return s.commonSettings
    if (key === 'POST /settings/common-settings') return null
    if (key === 'GET /settings/countries') return [{ id: 'de', name: 'Germany' }]
    if (path.startsWith('/settings/states/')) return [{ id: 'be', name: 'Berlin' }, { id: 'by', name: 'Bavaria' }]
    if (key === 'GET /settings/selected-state') return s.selectedState
    if (key === 'POST /settings/selected-state') return null

    // requests
    if (key === 'GET /request/page') return page_(s.requests, query)
    if (key === 'POST /request/save') {
      const saved = { ...body, id: body.id || `r${Date.now()}`, createdAt: time('2026-11-05', '10:00') }
      const index = s.requests.findIndex(r => r.id === saved.id)
      if (index >= 0) s.requests[index] = saved
      else s.requests.push(saved)
      return saved
    }

    if ((m = path.match(/^\/request\/([^/]+)\/comments$/))) {
      const comments = s.comments[m[1]!] ??= []
      if (method === 'GET') return comments
      const saved = { id: `c${Date.now()}`, content: body.content, createdAt: time('2026-11-05', '10:00'), author: 'User, Uta' }
      comments.push(saved)
      const request = s.requests.find(r => r.id === m![1])
      if (body.status && request) request.status = body.status
      return saved
    }

    // users
    if (key === 'GET /user/permissions') return ['PERMISSION_SCHEDULE_MANAGE', 'PERMISSION_ALARM_MANAGE']
    if (key === 'GET /user/page') {
      const search = (query.get('search') ?? '').toLowerCase()
      return page_(s.users.filter(u => `${u.username} ${u.fullname}`.toLowerCase().includes(search)), query)
    }
    if ((m = path.match(/^\/user\/([^/]+)\/permissions$/)) && method === 'PUT') {
      const target = s.users.find(u => u.id === m![1])
      if (!target) return new HttpError(404)
      target.permissions = body
      return target
    }
    if ((m = path.match(/^\/user\/([^/]+)\/impersonate$/)) && method === 'POST') {
      const target = s.users.find(u => u.id === m![1])
      if (!target) return new HttpError(404)
      return { id: target.id, username: target.username, roles: target.roles, permissions: target.permissions, impersonator: 'admin@example.org', token: 'token-impersonated', type: 'Bearer' }
    }
    if (key === 'POST /user/impersonation/stop') {
      return { id: 'u-admin', username: 'admin@example.org', roles: ['ROLE_ADMIN'], permissions: [], impersonator: null, token: 'token-admin', type: 'Bearer' }
    }

    if (key === 'GET /user/roles') return ['ROLE_ADMIN', 'ROLE_ALARM_ADMIN', 'ROLE_SCHEDULE_ADMIN', 'ROLE_MAINTENANCE_ADMIN']
    if ((m = path.match(/^\/user\/([^/]+)\/roles$/)) && method === 'PUT') {
      const target = s.users.find(u => u.id === m![1])
      if (!target) return new HttpError(404)
      if (target.id === 'u-admin' && !body.includes('ROLE_ADMIN')) return new HttpError(400)
      target.roles = [...target.roles.filter((role: string) => role === 'ROLE_USER'), ...body]
      return target
    }

    // notifications of the signed in user
    if (key === 'GET /notifications') return { unreadCount: s.notifications.filter(n => !n.read).length, items: s.notifications }
    if (key === 'PUT /notifications/read-all') {
      s.notifications.forEach((n) => { n.read = true })
      return null
    }
    if ((m = path.match(/^\/notifications\/([^/]+)\/read$/)) && method === 'PUT') {
      const target = s.notifications.find(n => n.id === m![1])
      if (!target) return new HttpError(404)
      target.read = true
      return null
    }

    // maintenance: the defect book
    if (key === 'GET /maintenance/aircraft') return s.aircraft
    if (key === 'GET /maintenance/defects') return s.defects
    if (key === 'GET /maintenance/defects/assignable-users') return s.users.map(u => ({ id: u.id, name: u.fullname ?? u.username }))
    const detail = (d: any) => ({ defect: d, events: s.defectEvents[d.id] ??= [{ id: `${d.id}-reported`, action: 'REPORTED', content: null, actor: d.reportedBy, subject: null, createdAt: d.reportedAt, deletable: false }] })
    const log = (d: any, action: string, content: string | null = null) =>
      detail(d).events.push({ id: `${d.id}-${detail(d).events.length}`, action, content, actor: { id: 'u-user', name: 'User, Uta' }, subject: null, createdAt: time('2026-11-20', '10:00'), deletable: action === 'COMMENTED' })
    if ((m = path.match(/^\/maintenance\/aircraft\/([^/]+)\/defects$/)) && method === 'POST') {
      const aircraft = s.aircraft.find(a => a.id === m![1])
      if (!aircraft) return new HttpError(404)
      const users = s.users.filter(u => (body.assigneeIds ?? []).includes(u.id))
      const created = defect({ id: `d${s.defects.length + 1}`, aircraftId: aircraft.id, aircraftLabel: `${aircraft.registration} · ${aircraft.type}`,
        title: body.title, description: body.description || null, priority: body.priority, reportedBy: { id: 'u-admin', name: 'admin@example.org' },
        assignees: users.map(u => ({ id: u.id, name: u.fullname ?? u.username })), deletable: true })
      s.defects.unshift(created)
      return detail(created)
    }
    if ((m = path.match(/^\/maintenance\/defects\/([^/]+)(\/.*)?$/))) {
      const target = s.defects.find(d => d.id === m![1])
      if (!target) return new HttpError(404)
      const action = `${method} ${m[2] ?? ''}`
      if (action === 'GET ') return detail(target)
      if (action === 'PUT /status') {
        if (body.status === 'REJECTED' && !body.comment?.trim()) return new HttpError(400)
        const closed = body.status === 'DONE' || body.status === 'REJECTED'
        Object.assign(target, { status: body.status, doneBy: closed ? { id: 'u-user', name: 'User, Uta' } : null, doneAt: closed ? time('2026-11-20', '10:00') : null })
        log(target, { OPEN: 'REOPENED', IN_PROGRESS: 'STARTED', DONE: 'DONE', REJECTED: 'REJECTED' }[body.status as string]!, body.comment)
        return detail(target)
      }
      if (action === 'POST /comments') {
        target.commentCount++
        log(target, 'COMMENTED', body.content)
        return detail(target)
      }
      if (action === 'POST /sign') {
        Object.assign(target, { signedBy: { id: 'u-admin', name: 'admin@example.org' }, signedAt: time('2026-11-21', '10:00') })
        log(target, 'SIGNED')
        return detail(target)
      }
      if (action === 'DELETE /sign') {
        Object.assign(target, { signedBy: null, signedAt: null })
        log(target, 'UNSIGNED')
        return detail(target)
      }
    }

    // alarm system
    if (key === 'GET /alarm/page') return page_(s.alarms, query)

    return undefined
  }
}

class RawResponse {
  constructor(public contentType: string, public body: string) {}
}

class HttpError {
  message = 'Fehler'
  constructor(public status: number) {}
}
