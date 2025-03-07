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
                        roles: options.body.username === 'testadmin' ? ['ROLE_ADMIN'] : ['ROLE_USER'],
                        token: 'testtoken',
                        type: 'Bearer'
                    }
                },
                status: { value: 'success' }
            }
        }
        // Add more mock responses as needed
        return { data: { value: {} } }
    }
})