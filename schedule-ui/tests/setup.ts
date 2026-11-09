import { mockNuxtImport } from '@nuxt/test-utils/runtime';

mockNuxtImport('useI18n', () => () => useNuxtApp().$i18n);

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
                            : ['ROLE_USER'],
                        permissions: options.body.username === 'testdelegate'
                            ? ['PERMISSION_SCHEDULE_MANAGE', 'PERMISSION_ALARM_MANAGE']
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
        }
        // Default fallback
        return { data: { value: null }, error: {} }
    }
})