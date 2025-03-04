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
      },
      nav: {
        home: 'Home',
        about: 'About',
        contact: 'Contact'
      },
      error: {
        credentials: 'Invalid credentials'
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
      },
      nav: {
        home: 'Home',
        about: 'About',
        contact: 'Kontakt'
      },
      error: {
        credentials: 'Ungültige Anmeldeinformationen'
      }
    }
  }
}))