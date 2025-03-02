export default defineI18nConfig(() => ({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      login: {
        title: 'Login',
        username: 'Username',
        password: 'Password',
        submit: 'Login',
        register: 'Register',
        forgot: 'Forgot password?'
      }
    },
    de: {
      login: {
        title: 'Anmelden',
        username: 'Benutzername',
        password: 'Passwort',
        submit: 'Anmelden',
        register: 'Registrieren',
        forgot: 'Password vergessen?'
      }
    }
  }
}))