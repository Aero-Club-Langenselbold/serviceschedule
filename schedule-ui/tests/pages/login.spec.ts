import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it } from 'vitest'
import Login from '~/pages/login.vue'

it ('Can render Login Page', async () => {
    const login = await mountSuspended(Login)
    expect(login.html()).toMatchSnapshot()

    expect(login.html()).toContain('Username')
    expect(login.html()).toContain('Password')
    expect(login.html()).toContain('Login')
    expect(login.html()).toContain('Register')
    expect(login.html()).toContain('Forgot password?')
    expect(login.html()).not.toContain('Invalid credentials')
})