import { mountSuspended } from '@nuxt/test-utils/runtime'
import { expect, it } from 'vitest'
import Index from '~/pages/index.vue'

const userStore = useUserStore()
const { login } = userStore

it ('Shows the correct user information', async () => {
    await login({username: "testuser", password: "howwouldiknow"})

    const index = await mountSuspended(Index)
    expect(index.html()).toContain('Welcome to the Schedule Manager')
    expect(index.html()).toContain('You are logged in as a user and can therefore view service plans and enter your own availability.')
})

it ('Shows the correct admin information', async () => {
  await login({username: "testadmin", password: "howwouldiknow"})

  const index = await mountSuspended(Index)
  expect(index.html()).toContain('Welcome to the Schedule Manager')
  expect(index.html()).toContain('You are logged in as an administrator and can therefore use all functions of the schedule manager.')
})