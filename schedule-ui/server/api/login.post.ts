import { z } from 'zod'

const bodySchema = z.object({
  user: z.object({
    username: z.string().email(),
    id: z.string(),
    roles: z.array(z.string())
  }),
  session: z.object({
    token: z.string(),
    type: z.string()
  }),
  loggedIn: z.boolean()
})

export default defineEventHandler(async (event) => {
  const { user, session, loggedIn } = await readValidatedBody(event, bodySchema.parse)

  // set the user session in the cookie
  // this server util is auto-imported by the auth-utils module
  await setUserSession(event, {
    user: user,
    session: session,
    loggedIn: loggedIn
  })
  return {}
})