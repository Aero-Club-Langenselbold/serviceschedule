import { mockNuxtImport } from '@nuxt/test-utils/runtime';

mockNuxtImport('useI18n', () => () => useNuxtApp().$i18n);

// Workshop periods relative to today, so "current" and "past" stay stable whenever the tests run.
const dayFromToday = (offset: number) => {
    const date = new Date()
    date.setDate(date.getDate() + offset)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
const workshopSlots = () => [
    { id: 's0', aircraftId: 'a1', startDate: dayFromToday(-90), endDate: dayFromToday(-80), note: 'Jahresnachprüfung', taskCount: 2, doneCount: 2, signedCount: 2, stepCount: 5, stepDoneCount: 5 },
    { id: 's1', aircraftId: 'a1', startDate: dayFromToday(-3), endDate: dayFromToday(10), note: null, taskCount: 4, doneCount: 1, signedCount: 1, stepCount: 8, stepDoneCount: 3 },
    { id: 's2', aircraftId: 'a2', startDate: dayFromToday(5), endDate: dayFromToday(30), note: 'Lackierung', taskCount: 0, doneCount: 0, signedCount: 0, stepCount: 0, stepDoneCount: 0 }
]

// Defect book: an open defect assigned to the signed in user, one in progress on the winch, one waiting for sign-off and a signed off one.
const defects = () => {
    const base = { description: null, priority: 'NORMAL', reportedBy: { id: 'u1', name: 'Albers, Anna' }, reportedAt: '2026-11-10T10:00:00Z', doneBy: null, doneAt: null, signedBy: null, signedAt: null, assignees: [], assignedToMe: false, commentCount: 0, task: null, editable: false, deletable: false }
    return [
        { ...base, id: 'd1', aircraftId: 'a1', aircraftLabel: 'D-1234 · ASK 21', title: 'Riss in der Haube', priority: 'HIGH', status: 'OPEN', assignees: [{ id: '1', name: 'testuser' }], assignedToMe: true, commentCount: 1, editable: true },
        { ...base, id: 'd2', aircraftId: 'w1', aircraftLabel: 'Winde 1 · Winde', title: 'Seil ausgefranst', priority: 'LOW', status: 'IN_PROGRESS', assignees: [{ id: 'u3', name: 'Becker, Ben' }] },
        { ...base, id: 'd3', aircraftId: 'a1', aircraftLabel: 'D-1234 · ASK 21', title: 'Reifen platt', status: 'DONE', doneBy: { id: 'u3', name: 'Becker, Ben' }, doneAt: '2026-11-12T10:00:00Z' },
        { ...base, id: 'd4', aircraftId: 'a2', aircraftLabel: 'D-5678 · LS 8', title: 'Kratzer', priority: 'VERY_LOW', status: 'REJECTED', doneBy: { id: 'u3', name: 'Becker, Ben' }, doneAt: '2026-11-12T10:00:00Z', signedBy: { id: 'u2', name: 'testadmin' }, signedAt: '2026-11-13T10:00:00Z' }
    ]
}
const defectEvents = (defect: { id: string, reportedBy: unknown, reportedAt: string }) => [
    { id: `${defect.id}-e1`, action: 'REPORTED', content: null, actor: defect.reportedBy, subject: null, createdAt: defect.reportedAt, deletable: false },
    ...(defect.id === 'd1' ? [
        { id: 'd1-e2', action: 'ASSIGNED', content: null, actor: { id: 'u2', name: 'testadmin' }, subject: { id: '1', name: 'testuser' }, createdAt: '2026-11-10T11:00:00Z', deletable: false },
        { id: 'd1-e3', action: 'COMMENTED', content: 'Bitte vor dem Wochenende anschauen', actor: { id: 'u2', name: 'testadmin' }, subject: null, createdAt: '2026-11-10T12:00:00Z', deletable: false }
    ] : [])
]

// Operations checklists: D-1234 has both checklists, the start of today is in progress; D-5678 has none.
const checklistDetail = (overrides: Record<string, unknown> = {}, items?: unknown[]) => {
    const baseItems = [
        { id: 'ci1', position: 0, text: 'Haube reinigen', result: 'OK', comment: null, checkedBy: { id: 'u1', name: 'Albers, Anna' }, checkedAt: '2026-11-27T08:00:00Z', defect: null },
        { id: 'ci2', position: 1, text: 'Reifendruck prüfen', result: null, comment: null, checkedBy: null, checkedAt: null, defect: null }
    ]
    const list = (items ?? baseItems) as { result: string | null }[]
    return {
        checklist: {
            id: 'c1', aircraftId: 'a1', registration: 'D-1234', aircraftType: 'ASK 21', kind: 'START', date: dayFromToday(0), status: 'IN_PROGRESS',
            createdBy: { id: 'u1', name: 'Albers, Anna' }, createdAt: '2026-11-27T07:55:00Z', submittedBy: null, submittedAt: null, approvedBy: null, approvedAt: null,
            itemCount: list.length, checkedCount: list.filter(item => item.result).length, notOkCount: list.filter(item => item.result === 'NOT_OK').length,
            ...overrides
        },
        items: list,
        editable: true,
        submittable: list.every(item => item.result),
        approvable: false,
        reopenable: false
    }
}
const checklistTemplate = (aircraftId: string, kind: string, items: string[]) => ({ id: `tpl-${aircraftId}-${kind}`, aircraftId, kind, items, updatedAt: '2026-11-01T10:00:00Z' })
const operationsOverview = () => ({
    today: dayFromToday(0),
    aircraft: [
        { aircraftId: 'a1', registration: 'D-1234', aircraftType: 'ASK 21', startTemplate: checklistTemplate('a1', 'START', ['Haube reinigen', 'Reifendruck prüfen']),
          endTemplate: checklistTemplate('a1', 'END', ['Abdeckung drauf']), todayStart: checklistDetail().checklist, todayEnd: null },
        { aircraftId: 'a2', registration: 'D-5678', aircraftType: 'LS 8', startTemplate: null, endTemplate: null, todayStart: null, todayEnd: null }
    ]
})
// Winch book: two ropes; today Seil 1 has a rope break of the signed in user and a photo, earlier days are read only.
const winchTypes = () => [{ id: 'wt1', name: 'Seilriss', active: true }, { id: 'wt2', name: 'Schlaufe', active: true }]
const winchRemark = (text: string | null, version: number | null = text ? 3 : null) =>
    ({ text, version, updatedBy: text ? { id: 'u1', name: 'Albers, Anna' } : null, updatedAt: text ? '2026-11-27T09:00:00Z' : null })
const winchDay = (overrides: Record<string, unknown> = {}) => ({
    date: dayFromToday(0), today: true, exists: true,
    remark: winchRemark('Winde läuft rund'),
    drivers: [{ id: 'u1', name: 'Albers, Anna' }, { id: '1', name: 'testuser' }],
    ropes: [
        { rope: { id: 'r1', name: 'Seil 1', active: true }, remark: winchRemark(null),
          incidents: [{ id: 'wi1', type: winchTypes()[0], driver: { id: '1', name: 'testuser' }, createdAt: '2026-11-27T10:15:00Z', comment: 'am Startplatz', deletable: true }],
          photos: [{ id: 'wp1', filename: 'seil.jpg', contentType: 'image/jpeg', size: 1234, uploadedBy: { id: 'u1', name: 'Albers, Anna' }, uploadedAt: '2026-11-27T10:20:00Z', deletable: false }] },
        { rope: { id: 'r2', name: 'Seil 2', active: true }, remark: winchRemark('Kausch aufgeraut', 1), incidents: [], photos: [] }
    ],
    incidentTypes: winchTypes(),
    editable: true,
    remarksEditable: true,
    ...overrides
})
const winchSettings = () => ({
    ropes: [
        { id: 'r1', name: 'Seil 1', position: 0, active: true, used: true },
        { id: 'r2', name: 'Seil 2', position: 1, active: true, used: false },
        { id: 'r0', name: 'Altes Seil', position: 2, active: false, used: true }
    ],
    incidentTypes: [
        { id: 'wt1', name: 'Seilriss', position: 0, active: true, used: true },
        { id: 'wt2', name: 'Schlaufe', position: 1, active: true, used: false }
    ]
})
const recordedCalls = (): { url: string, options: any }[] => ((globalThis as any).__apiCalls ??= [])
const notifications = () => ({
    unreadCount: 1,
    items: [
        { id: 'n1', type: 'DEFECT_ASSIGNED', createdAt: '2026-11-10T11:00:00Z', read: false, actor: { id: 'u2', name: 'testadmin' }, defect: { id: 'd1', title: 'Riss in der Haube', aircraftId: 'a1', aircraftLabel: 'D-1234', status: 'OPEN' } },
        { id: 'n0', type: 'DEFECT_ASSIGNED', createdAt: '2026-11-01T11:00:00Z', read: true, actor: null, defect: { id: 'd2', title: 'Seil ausgefranst', aircraftId: 'w1', aircraftLabel: 'Winde 1', status: 'IN_PROGRESS' } },
        { id: 'n2', type: 'CHECKLIST_SUBMITTED', createdAt: '2026-10-30T11:00:00Z', read: true, actor: { id: 'u1', name: 'Albers, Anna' }, defect: null, checklist: { id: 'c1', aircraftId: 'a1', registration: 'D-1234', kind: 'START', date: '2026-10-30', status: 'SUBMITTED' } }
    ]
})

mockNuxtImport('useAPI', () => {
    return async (url: string, options: any) => {
        recordedCalls().push({ url, options })
        if (url === '/auth/signin') {
            return {
                data: {
                    value: {
                        id: '1',
                        username: options.body.username,
                        // only two roles exist; everything a member may administer is a right
                        roles: options.body.username === 'testadmin' ? ['ROLE_ADMIN'] : ['ROLE_USER'],
                        permissions: options.body.username === 'testdelegate'
                            ? ['PERMISSION_SCHEDULE_MANAGE', 'PERMISSION_ALARM_MANAGE']
                            : options.body.username === 'testscheduleadmin'
                            ? ['PERMISSION_SCHEDULE_MANAGE']
                            : options.body.username === 'testalarmadmin'
                            ? ['PERMISSION_ALARM_MANAGE']
                            : options.body.username === 'testmaintenanceadmin' || options.body.username === 'testmaintenancedelegate'
                            ? ['PERMISSION_MAINTENANCE_MANAGE']
                            : options.body.username === 'testoperationsadmin'
                            ? ['PERMISSION_OPERATIONS_MANAGE']
                            : [],
                        token: 'testtoken',
                        type: 'Bearer'
                    }
                },
                error: {}
            }
        } else if (url === '/auth/kiosk/signin') {
            // PIN 123456 signs in, 999999 stands for a locked account, everything else is wrong
            if (options.body.pin === '999999') {
                return { data: {}, error: { value: Object.assign(new Error('locked'), { statusCode: 429 }) } }
            }
            if (options.body.pin !== '123456') {
                return { data: {}, error: { value: Object.assign(new Error('invalid'), { statusCode: 401 }) } }
            }
            return {
                data: { value: { id: '1', username: options.body.username, roles: ['ROLE_USER'], permissions: [], kiosk: true, token: 'kiosktoken', type: 'Bearer' } },
                error: {}
            }
        } else if (url === '/user/failing/impersonate' || url === '/request/failing/comments') {
            return { data: {}, error: { value: new Error('failed') } }
        } else if (/^\/user\/[^/]+\/impersonate$/.test(url)) {
            return {
                data: { value: { id: url.split('/')[2], username: 'impersonated', roles: ['ROLE_USER'], permissions: [], impersonator: 'testadmin', token: 'impersonationtoken', type: 'Bearer' } },
                error: {}
            }
        } else if (url === '/user/impersonation/stop') {
            return {
                data: { value: { id: '1', username: 'testadmin', roles: ['ROLE_ADMIN'], permissions: [], impersonator: null, token: 'admintoken', type: 'Bearer' } },
                error: {}
            }
        } else if (url.startsWith('/user/page')) {
            return {
                data: { value: { content: [
                    { id: 'u1', username: 'anna@example.org', fullname: 'Albers, Anna', roles: ['ROLE_USER'], permissions: ['PERMISSION_ALARM_MANAGE'] },
                    { id: 'u2', username: 'testadmin', fullname: null, roles: ['ROLE_ADMIN'], permissions: [] }
                ], totalElements: 2 } },
                error: {}
            }
        } else if (url === '/user/roles') {
            return { data: { value: ['ROLE_ADMIN'] }, error: {} }
        } else if (/^\/user\/[^/]+\/roles$/.test(url)) {
            // the base role stays, as in the backend
            return { data: { value: { roles: ['ROLE_USER', ...options.body] } }, error: {} }
        } else if (url === '/notifications') {
            return { data: { value: notifications() }, error: {} }
        } else if (/^\/notifications\/(read-all|[^/]+\/read)$/.test(url)) {
            return { data: { value: null }, error: {} }
        } else if (url === '/user/permissions') {
            return { data: { value: ['PERMISSION_SCHEDULE_MANAGE', 'PERMISSION_ALARM_MANAGE', 'PERMISSION_MAINTENANCE_MANAGE', 'PERMISSION_OPERATIONS_MANAGE'] }, error: {} }
        } else if (/^\/user\/[^/]+\/permissions$/.test(url)) {
            return { data: { value: { permissions: options.body } }, error: {} }
        } else if (/^\/request\/[^/]+\/comments$/.test(url)) {
            return options.method === 'POST'
                ? { data: { value: { id: 'c2', content: options.body.content, createdAt: '2026-11-02T10:00:00Z', author: 'Albers, Anna' } }, error: {} }
                : { data: { value: [{ id: 'c1', content: 'Tritt bei mir auch auf', createdAt: '2026-11-01T10:00:00Z', author: 'Becker, Ben' }] }, error: {} }
        } else if (url.startsWith('/participant/fetchByMail/')) {
            const username = url.split('/').pop()
            return {
                data: {
                    value: {
                        id: '2',
                        email: username,
                        firstname: 'Lulli',
                        lastname: 'Palooza',
                        status: 'ACTIVE'
                    }
                },
                error: {}
            }
        } else if (url === '/featureflag/active') {
            return {
                data: { value: [{ id: 'testFeature', description: 'Test Feature', active: true }] },
                error: {}
            }
        } else if (url === '/auth/signup') {
            return { data: { value: { message: 'Registration successful' } }, error: {} }
        } else if (url.startsWith('/settings/common-setting/')) {
            return { data: { value: { settingValue: 'Test Question' } }, error: {} }
        } else if (url === '/settings/countries') {
            return { data: { value: [] }, error: {} }
        } else if (url === '/settings/common-settings') {
            return { data: { value: [] }, error: {} }
        } else if (url.startsWith('/alarm/status/page')) {
            return {
                data: { value: { content: [{ id: '1', timestamp: '2026-05-07T22:38:45', betrieb: 1, scharf: 0, unscharf: 1, sabotage: 0, stoerung: 0, alarm: 0, zone1: 0, zone2: 0, zone3: 0, zone4: 0, zone5: 0, zone6: 0, zone7: 0, zone8: 0 }], totalElements: 1 } },
                error: {}
            }
        } else if (url.startsWith('/alarm/page')) {
            return {
                data: { value: { content: [{ id: '1', timestamp: '2026-05-07T22:38:45', rfidId: 'AABBCC', internalTime: '22:38:45', rawData: 'raw', status: 1 }], totalElements: 1 } },
                error: {}
            }
        } else if (url.startsWith('/plan/mine/ics')) {
            return { data: { value: 'BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n' }, error: {} }
        } else if (url.startsWith('/plan/mine')) {
            const upcoming = { id: 'e2', planId: 'p1', planName: 'Saison 2099', serviceName: 'Flugleiter', startTime: '2099-04-12T09:00:00', endTime: '2099-04-12T13:30:00', colleagues: ['Kurt Kollege'] }
            const past = { id: 'e1', planId: 'p0', planName: 'Saison 2020', serviceName: 'Windenfahrer', startTime: '2020-05-02T09:00:00', endTime: '2020-05-02T18:00:00', colleagues: [] }
            return { data: { value: url.endsWith('past=true') ? [past, upcoming] : [upcoming] }, error: {} }
        } else if (url === '/maintenance/aircraft') {
            return {
                data: { value: [
                    { id: 'a1', type: 'ASK 21', registration: 'D-1234', workshop: workshopSlots()[1], unscheduledTaskCount: 0, openDefectCount: 2 },
                    { id: 'a2', type: 'LS 8', registration: 'D-5678', workshop: null, unscheduledTaskCount: 3, openDefectCount: 0 }
                ] },
                error: {}
            }
        } else if (url === '/maintenance/templates') {
            return {
                data: { value: [
                    { id: 't1', title: 'Rumpf reinigen', description: 'Innen und außen', level: 'BEGINNER', durationMinutes: 60, aircraftTypes: [], checklist: ['Innen', 'Außen'] },
                    { id: 't2', title: 'Haube polieren', description: null, level: 'EXPERIENCED', durationMinutes: null, aircraftTypes: ['ASK 21'], checklist: [] }
                ] },
                error: {}
            }
        } else if (url === '/maintenance/aircraft/a1') {
            return { data: { value: { id: 'a1', type: 'ASK 21', registration: 'D-1234', workshop: workshopSlots()[1], unscheduledTaskCount: 0, openDefectCount: 0 } }, error: {} }
        } else if (url === '/maintenance/aircraft/a1/tasks') {
            // the board of the visit s1 (the default), the past visit s0 has one done task
            if (options.query?.workshopId === 's0') {
                return { data: { value: [{ id: 'k0', aircraftId: 'a1', workshopId: 's0', templateId: null, title: 'Reifen wechseln', status: 'DONE', description: null, level: 'BEGINNER', durationMinutes: null, deadline: null, fromTemplate: false, defect: null, checklistTotal: 0, checklistDone: 0, noteCount: 0, doneBy: null, doneAt: null, signedBy: null, signedAt: null }] }, error: {} }
            }
            const task = { aircraftId: 'a1', workshopId: 's1', templateId: null, description: null, level: 'BEGINNER', durationMinutes: null, deadline: null, fromTemplate: false, defect: null, checklistTotal: 0, checklistDone: 0, noteCount: 0, doneBy: null, doneAt: null, signedBy: null, signedAt: null }
            return { data: { value: [
                { ...task, id: 'k1', title: 'Reifen prüfen', status: 'OPEN' },
                { ...task, id: 'k2', title: 'Haube polieren', templateId: 't2', fromTemplate: true, status: 'DONE', signedBy: { id: 'u2', name: 'testadmin' }, signedAt: '2026-11-10T10:00:00Z' }
            ] }, error: {} }
        } else if (url === '/maintenance/aircraft/a1/defects') {
            return { data: { value: defects().filter(defect => defect.aircraftId === 'a1') }, error: {} }
        } else if (url === '/maintenance/defects') {
            return { data: { value: defects() }, error: {} }
        } else if (url === '/maintenance/defects/assignable-users') {
            return { data: { value: [{ id: 'u1', name: 'Albers, Anna' }, { id: 'u3', name: 'Becker, Ben' }] }, error: {} }
        } else if (url === '/maintenance/defects/missing') {
            return { data: { value: null }, error: { value: Object.assign(new Error('not found'), { statusCode: 404 }) } }
        } else if (/^\/maintenance\/defects\/[^/]+$/.test(url) && options.method === 'GET') {
            const defect = defects().find(candidate => candidate.id === url.split('/')[3])
            return { data: { value: { defect, events: defectEvents(defect!) } }, error: {} }
        } else if (/^\/maintenance\/defects\/[^/]+\/status$/.test(url)) {
            const defect = defects().find(candidate => candidate.id === url.split('/')[3])!
            const closed = options.body.status === 'DONE' || options.body.status === 'REJECTED'
            const updated = { ...defect, status: options.body.status, doneBy: closed ? { id: '1', name: 'testuser' } : null, doneAt: closed ? '2026-11-20T10:00:00Z' : null }
            const event = { id: 'ev-status', action: { OPEN: 'REOPENED', IN_PROGRESS: 'STARTED', DONE: 'DONE', REJECTED: 'REJECTED' }[options.body.status as string], content: options.body.comment, actor: { id: '1', name: 'testuser' }, subject: null, createdAt: '2026-11-20T10:00:00Z', deletable: false }
            return { data: { value: { defect: updated, events: [...defectEvents(defect), event] } }, error: {} }
        } else if (/^\/maintenance\/defects\/[^/]+\/sign$/.test(url)) {
            const defect = defects().find(candidate => candidate.id === url.split('/')[3])!
            const signed = options.method === 'POST'
            return { data: { value: { defect: { ...defect, signedBy: signed ? { id: 'u2', name: 'testadmin' } : null, signedAt: signed ? '2026-11-21T10:00:00Z' : null }, events: defectEvents(defect) } }, error: {} }
        } else if (/^\/maintenance\/defects\/[^/]+\/comments$/.test(url)) {
            const defect = defects().find(candidate => candidate.id === url.split('/')[3])!
            const comment = { id: 'ev-new', action: 'COMMENTED', content: options.body.content, actor: { id: '1', name: 'testuser' }, subject: null, createdAt: '2026-11-20T11:00:00Z', deletable: true }
            return { data: { value: { defect: { ...defect, commentCount: defect.commentCount + 1 }, events: [...defectEvents(defect), comment] } }, error: {} }
        } else if (/^\/maintenance\/tasks\/[^/]+\/status$/.test(url)) {
            return { data: { value: { id: url.split('/')[3], status: options.body.status } }, error: {} }
        } else if (url === '/maintenance/workshop') {
            return { data: { value: workshopSlots() }, error: {} }
        } else if (url === '/maintenance/inventory') {
            const item = { location: null, minQuantity: null, unit: null, comment: null, aircraftTypes: [] }
            return { data: { value: [
                { ...item, id: 'i1', name: 'Politur', kind: 'CONSUMABLE', counting: 'COUNT', quantity: 2, minQuantity: 2, unit: 'Dosen', location: 'Regal A', status: 'LOW' },
                { ...item, id: 'i2', name: 'Mehrzweckfett', kind: 'CONSUMABLE', counting: 'LEVEL', quantity: 2, status: 'OK' },
                { ...item, id: 'i3', name: 'Reifenschlauch 4.00-4', kind: 'SPARE_PART', counting: 'COUNT', quantity: 0, unit: 'Stk.', aircraftTypes: ['ASK 21'], status: 'EMPTY' },
                { ...item, id: 'i4', name: 'Batterie 12 V', kind: 'SPARE_PART', counting: 'COUNT', quantity: 3, unit: 'Stk.', status: 'OK' },
                { ...item, id: 'i5', name: 'Bremsbeläge', kind: 'SPARE_PART', counting: 'COUNT', quantity: 1, unit: 'Satz', aircraftTypes: ['LS 8'], status: 'OK' }
            ] }, error: {} }
        } else if (/^\/maintenance\/inventory\/[^/]+\/quantity$/.test(url)) {
            return { data: { value: { id: url.split('/')[3], name: 'Mehrzweckfett', kind: 'CONSUMABLE', counting: 'LEVEL', quantity: options.body.quantity, location: null, minQuantity: null, unit: null, comment: null, aircraftTypes: [], status: options.body.quantity === 0 ? 'EMPTY' : options.body.quantity === 1 ? 'LOW' : 'OK' } }, error: {} }
        } else if (url === '/user/me/settings') {
            return { data: { value: { username: 'testuser', kioskPinSet: false } }, error: {} }
        } else if (url === '/user/me/kiosk-pin') {
            return { data: { value: { username: 'testuser', kioskPinSet: options.method === 'PUT' } }, error: {} }
        } else if (url === '/operations/overview') {
            return { data: { value: operationsOverview() }, error: {} }
        } else if (/^\/operations\/aircraft\/[^/]+\/templates\/(START|END)$/.test(url)) {
            const [, , , aircraftId, , kind] = url.split('/')
            return { data: { value: options.method === 'PUT' ? checklistTemplate(aircraftId!, kind!, options.body.items) : null }, error: {} }
        } else if (/^\/operations\/aircraft\/[^/]+\/checklists\/(START|END)\/today$/.test(url)) {
            const [, , , aircraftId, , kind] = url.split('/')
            if (aircraftId === 'missing') {
                return { data: { value: null }, error: { value: Object.assign(new Error('not found'), { statusCode: 404 }) } }
            }
            if (options.method === 'POST') {
                return { data: { value: checklistDetail({ id: 'c2', kind, createdBy: { id: '1', name: 'testuser' } }, [
                    { id: 'ci3', position: 0, text: 'Abdeckung drauf', result: null, comment: null, checkedBy: null, checkedAt: null, defect: null }]) }, error: {} }
            }
            const base = { aircraftId, registration: aircraftId === 'a1' ? 'D-1234' : 'D-5678', aircraftType: aircraftId === 'a1' ? 'ASK 21' : 'LS 8', kind, date: dayFromToday(0) }
            if (aircraftId !== 'a1') {
                return { data: { value: { ...base, templateItems: null, checklist: null } }, error: {} }
            }
            return { data: { value: kind === 'START'
                ? { ...base, templateItems: ['Haube reinigen', 'Reifendruck prüfen'], checklist: checklistDetail() }
                : { ...base, templateItems: ['Abdeckung drauf'], checklist: null } }, error: {} }
        } else if (/^\/operations\/checklists\/[^/]+\/items\/[^/]+\/defect$/.test(url)) {
            const detail = checklistDetail()
            detail.items = (detail.items as any[]).map(item => item.id === url.split('/')[5]
                ? { ...item, result: 'NOT_OK', checkedBy: { id: '1', name: 'testuser' }, defect: { id: 'd9', title: options.body.title, status: 'OPEN' } } : item)
            return { data: { value: checklistDetail({}, detail.items) }, error: {} }
        } else if (/^\/operations\/checklists\/[^/]+\/items\/[^/]+$/.test(url)) {
            const items = (checklistDetail().items as any[]).map(item => item.id === url.split('/')[5]
                ? { ...item, result: options.body.result, comment: options.body.comment, checkedBy: options.body.result ? { id: '1', name: 'testuser' } : null } : item)
            return { data: { value: checklistDetail({}, items) }, error: {} }
        } else if (/^\/operations\/checklists\/[^/]+\/(submit|approve|reopen)$/.test(url)) {
            const action = url.split('/')[4]
            const done = checklistDetail().items.map((item: any) => ({ ...item, result: item.result ?? 'OK' }))
            const status = { submit: 'SUBMITTED', approve: 'APPROVED', reopen: 'IN_PROGRESS' }[action!]
            return { data: { value: { ...checklistDetail({ status, submittedBy: status === 'IN_PROGRESS' ? null : { id: '1', name: 'testuser' }, submittedAt: status === 'IN_PROGRESS' ? null : '2026-11-27T09:00:00Z' }, done),
                editable: status === 'IN_PROGRESS', submittable: status === 'IN_PROGRESS', approvable: status === 'SUBMITTED', reopenable: status !== 'IN_PROGRESS' } }, error: {} }
        } else if (url === '/operations/checklists/c-old') {
            return { data: { value: null }, error: { value: Object.assign(new Error('forbidden'), { statusCode: 403 }) } }
        } else if (url === '/operations/checklists/c-submitted') {
            const done = checklistDetail().items.map((item: any) => ({ ...item, result: 'OK' }))
            return { data: { value: { ...checklistDetail({ id: 'c-submitted', status: 'SUBMITTED', submittedBy: { id: 'u1', name: 'Albers, Anna' }, submittedAt: '2026-11-27T09:00:00Z' }, done),
                editable: false, submittable: false, approvable: true, reopenable: true } }, error: {} }
        } else if (/^\/operations\/checklists\/[^/?]+$/.test(url)) {
            return { data: { value: checklistDetail({ id: url.split('/')[3] }) }, error: {} }
        } else if (url.startsWith('/operations/checklists?')) {
            const past = { ...checklistDetail().checklist, id: 'c0', date: '2026-11-20', status: 'APPROVED', checkedCount: 2, notOkCount: 1 }
            return { data: { value: { content: [checklistDetail().checklist, past], totalElements: 2 } }, error: {} }
        } else if (url === '/maintenance/winch/days/today') {
            return { data: { value: winchDay() }, error: {} }
        } else if (url === '/maintenance/winch/days/2026-11-20') {
            // an earlier day as a member sees it
            return { data: { value: winchDay({ date: '2026-11-20', today: false, editable: false, remarksEditable: false, incidentTypes: [],
                ropes: [{ ...winchDay().ropes[0], incidents: [{ ...winchDay().ropes[0]!.incidents[0], deletable: false }] }] }) }, error: {} }
        } else if (url === '/maintenance/winch/days/2026-11-21') {
            // an earlier day as a maintenance administrator sees it
            return { data: { value: winchDay({ date: '2026-11-21', today: false, editable: false, remarksEditable: true, incidentTypes: [] }) }, error: {} }
        } else if (/^\/maintenance\/winch\/days\/\d{4}-\d{2}-\d{2}$/.test(url)) {
            return { data: { value: null }, error: { value: Object.assign(new Error('not found'), { statusCode: 404 }) } }
        } else if (/^\/maintenance\/winch\/days\/[^/]+(\/ropes\/[^/]+)?\/remark$/.test(url)) {
            if (options.body.text === 'Konflikt') {
                return { data: { value: null }, error: { value: Object.assign(new Error('conflict'), { statusCode: 409 }) } }
            }
            const ropeId = url.split('/')[6]
            const day = winchDay()
            const remark = { ...winchRemark(options.body.text, 4), updatedBy: { id: '1', name: 'testuser' } }
            return { data: { value: ropeId
                ? { ...day, ropes: day.ropes.map(rope => rope.rope.id === ropeId ? { ...rope, remark } : rope) }
                : { ...day, remark } }, error: {} }
        } else if (url === '/maintenance/winch/incidents') {
            const day = winchDay()
            const incident = { id: 'wi-new', type: winchTypes().find(type => type.id === options.body.typeId), driver: { id: '1', name: 'testuser' },
                createdAt: '2026-11-27T11:00:00Z', comment: options.body.comment, deletable: true }
            return { data: { value: { ...day, ropes: day.ropes.map(rope => rope.rope.id === options.body.ropeId ? { ...rope, incidents: [...rope.incidents, incident] } : rope) } }, error: {} }
        } else if (/^\/maintenance\/winch\/incidents\/[^/]+$/.test(url)) {
            const day = winchDay()
            return { data: { value: { ...day, ropes: day.ropes.map(rope => ({ ...rope, incidents: rope.incidents.filter(incident => incident.id !== url.split('/')[4]) })) } }, error: {} }
        } else if (/^\/maintenance\/winch\/ropes\/[^/]+\/photos$/.test(url)) {
            const day = winchDay()
            const photo = { id: 'wp-new', filename: (options.body.get('file') as File).name, contentType: 'image/jpeg', size: 10, uploadedBy: { id: '1', name: 'testuser' },
                uploadedAt: '2026-11-27T11:00:00Z', deletable: true }
            return { data: { value: { ...day, ropes: day.ropes.map(rope => rope.rope.id === url.split('/')[4] ? { ...rope, photos: [...rope.photos, photo] } : rope) } }, error: {} }
        } else if (/^\/maintenance\/winch\/photos\/[^/]+$/.test(url)) {
            return { data: { value: options.method === 'GET' ? new Blob(['jpeg'], { type: 'image/jpeg' }) : winchDay() }, error: {} }
        } else if (url.startsWith('/maintenance/winch/days?')) {
            const query = new URLSearchParams(url.split('?')[1])
            const days = query.get('year') === '2024'
                ? [{ date: '2024-05-01', drivers: [{ id: 'u3', name: 'Becker, Ben' }], incidents: [{ typeId: 'wt2', name: 'Schlaufe', count: 1 }], incidentCount: 1, photoCount: 0, hasRemarks: false }]
                : [
                    { date: dayFromToday(0), drivers: [{ id: 'u1', name: 'Albers, Anna' }, { id: '1', name: 'testuser' }], incidents: [{ typeId: 'wt1', name: 'Seilriss', count: 2 }], incidentCount: 2, photoCount: 1, hasRemarks: true },
                    { date: '2026-11-20', drivers: [{ id: '1', name: 'testuser' }], incidents: [], incidentCount: 0, photoCount: 0, hasRemarks: true }
                ]
            return { data: { value: { content: days, totalElements: days.length } }, error: {} }
        } else if (url === '/maintenance/winch/years') {
            return { data: { value: [new Date().getFullYear(), 2024] }, error: {} }
        } else if (url === '/maintenance/winch/settings') {
            return { data: { value: winchSettings() }, error: {} }
        } else if (url.startsWith('/maintenance/winch/settings/')) {
            const [, , , , kind, id] = url.split('/')
            const settings = winchSettings()
            const key = kind === 'ropes' ? 'ropes' : 'incidentTypes'
            if (options.method === 'POST') {
                if (settings[key].some(option => option.name.toLowerCase() === options.body.name.toLowerCase())) {
                    return { data: { value: null }, error: { value: Object.assign(new Error('conflict'), { statusCode: 409 }) } }
                }
                settings[key].push({ id: 'new', name: options.body.name, position: settings[key].length, active: true, used: false })
            } else if (id === 'order') {
                settings[key].sort((a, b) => options.body.ids.indexOf(a.id) - options.body.ids.indexOf(b.id))
            } else if (options.method === 'PUT') {
                settings[key] = settings[key].map(option => option.id === id ? { ...option, name: options.body.name, active: options.body.active } : option)
            } else if (options.method === 'DELETE') {
                settings[key] = settings[key].flatMap(option => option.id !== id ? [option] : option.used ? [{ ...option, active: false }] : [])
            }
            return { data: { value: settings }, error: {} }
        } else if (url === '/maintenance/aircraft/types') {
            return { data: { value: ['ASK 21', 'LS 8'] }, error: {} }
        }
        // Default fallback
        return { data: { value: null }, error: {} }
    }
})