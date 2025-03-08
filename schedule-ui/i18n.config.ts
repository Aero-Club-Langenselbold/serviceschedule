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
      app: {
        dateLocale: 'en-US',
        name: 'Schedule Manager V1.0',
        welcome: 'Welcome to the Schedule Manager',
        user: {
          participantWelcome: 'You are logged in as a user and can therefore view service plans and enter your own availability.',
          userWelcome: 'You are logged in as a user, but no participant has been assigned to you yet. Please contact the administrator.',
          adminWelcome: 'You are logged in as an administrator and can therefore use all functions of the schedule manager.'
        }
      },
      participants: {
          add: 'Add new Participant',
          firstname: 'First Name',
          lastname: 'Last Name',
          email: 'Email',
          dateOfBirth: 'Date of Birth',
          status: 'Status',
          services: 'Assigned Services',
          save: 'Save',
          success: 'Participant saved successfully',
          error: 'Error saving participant',
          exists: 'Participant already exists',
          list: 'Participant List',
          active: 'Active',
          passive: 'Passive',
          statusInfo: 'Active participants (or members) are automatically considered for all available days in new service plans. Passive participants (or members) are only considered for days for which they explicitly register.'
      },
      service: {
          name: 'Service name',
          save: 'Save',
          list: 'Service list',
          success: 'Service saved successfully',
          error: 'Error saving service',
          exists: 'Service already exists',
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
      app: {
        dateLocale: 'de-DE',
        name: 'Dienstplanmanager V1.0',
        welcome: 'Herzlich willkommen beim Dienstplanmanager',
        user: {
          participantWelcome: 'Du bist in der Rolle als Benutzer angemeldet und kannst daher Dienstpläne einsehen und deine eigenen Verfügbarkeiten eintragen.',
          userWelcome: 'Du bist in der Rolle als Benutzer angemeldet, dir ist aber noch kein Teilnehmer zugeordnet. Bitte wende dich an den Administrator.',
          adminWelcome: 'Du bist in der Rolle als Administrator angemeldet und kannst daher alle Funktionen des Dienstplanmanagers nutzen.'
        }
      },
      participants: {
        add: 'Neuen Teilnehmer hinzufügen',
        firstname: 'Vorname',
        lastname: 'Nachname',
        email: 'Email',
        dateOfBirth: 'Geburtsdatum',
        status: 'Status',
        services: 'Zugeordnete Dienste',
        save: 'Speichern',
        success: 'Teilnehmer erfolgreich gespeichert',
        error: 'Fehler beim Speichern des Teilnehmers',
        exists: 'Teilnehmer existiert bereits',
        list: 'Teilnehmerliste',
        active: 'Aktiv',
        passive: 'Passiv',
        statusInfo: 'Aktive Teilnehmer (bzw. Mitglieder) werden automatisch bei neuen Dienstplänen für alle verfügbaren Tage berücksichtigt. Passive Teilnehmer (bzw. Mitglieder) werden nur für Tage berücksichtigt, für die sie sich explizit eintragen.'
      },
      service: {
          name: 'Dienst',
          save: 'Speichern',
          list: 'Dienstliste',
          success: 'Dienst erfolgreich gespeichert',
          error: 'Fehler beim Speichern des Dienstes',
          exists: 'Dienst existiert bereits',
      },
      error: {
        credentials: 'Ungültige Anmeldeinformationen'
      }
    }
  }
}))