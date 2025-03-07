import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it } from 'vitest'
import Login from '~/pages/login.vue'

it ('Can render Login Page', async () => {
    const navMenu = await mountSuspended(Login)
    expect(navMenu.html()).toMatchSnapshot()

    expect(navMenu.html()).toContain('Username')
    expect(navMenu.html()).toContain('Password')
    expect(navMenu.html()).toContain('Login')
    expect(navMenu.html()).toContain('Register')
    expect(navMenu.html()).toContain('Forgot password?')
    expect(navMenu.html()).not.toContain('Invalid credentials')
})