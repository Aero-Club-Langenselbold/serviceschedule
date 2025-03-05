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
        contact: 'Contact',
        participants: {
          menu: 'Manage Participants',
          list: 'Show Participants',
          add: 'Add Particpant'
        },
        service: {
          menu: 'Manage Services',
          list: 'Show Services',
          add: 'Add Service'
        },
        plan: {
          menu: 'Manage Service Plans',
          list: 'Show Service Plans',
          add: 'Add Service Plan'
        }
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
        contact: 'Kontakt',
        participants: {
          menu: 'Teilnehmer verwalten',
          list: 'Teilnehmer anzeigen',
          add: 'Teilnehmer hinzufügen'
        },
        service: {
          menu: 'Dienste verwalten',
          list: 'Dienste anzeigen',
          add: 'Dienst hinzufügen'
        },
        plan: {
          menu: 'Dienstpläne verwalten',
          list: 'Dienstpläne anzeigen',
          add: 'Dienstplan hinzufügen'
        }
      },
      error: {
        credentials: 'Ungültige Anmeldeinformationen'
      }
    }
  }
}))