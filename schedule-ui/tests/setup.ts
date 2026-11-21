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

mockNuxtImport('useAPI', () => {
    return async (url: string, options: any) => {
        if (url === '/auth/signin') {
            return {
                data: {
                    value: {
                        id: '1',
                        username: options.body.username,
                        roles: options.body.username === 'testadmin'
                            ? ['ROLE_ADMIN']
                            : options.body.username === 'testscheduleadmin'
                            ? ['ROLE_SCHEDULE_ADMIN']
                            : options.body.username === 'testalarmadmin'
                            ? ['ROLE_ALARM_ADMIN']
                            : options.body.username === 'testmaintenanceadmin'
                            ? ['ROLE_MAINTENANCE_ADMIN']
                            : ['ROLE_USER'],
                        permissions: options.body.username === 'testdelegate'
                            ? ['PERMISSION_SCHEDULE_MANAGE', 'PERMISSION_ALARM_MANAGE']
                            : options.body.username === 'testmaintenancedelegate'
                            ? ['PERMISSION_MAINTENANCE_MANAGE']
                            : [],
                        token: 'testtoken',
                        type: 'Bearer'
                    }
                },
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
        } else if (url === '/user/permissions') {
            return { data: { value: ['PERMISSION_SCHEDULE_MANAGE', 'PERMISSION_ALARM_MANAGE'] }, error: {} }
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
            return { data: { value: [] }, error: {} }
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
        } else if (url === '/maintenance/aircraft/types') {
            return { data: { value: ['ASK 21', 'LS 8'] }, error: {} }
        }
        // Default fallback
        return { data: { value: null }, error: {} }
    }
})