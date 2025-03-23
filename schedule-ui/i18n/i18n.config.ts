export default defineI18nConfig(() => ({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      general: {
        yes: 'Yes',
        no: 'No',
        january: 'January',
        february: 'February',
        march: 'March',
        april: 'April',
        may: 'May',
        june: 'June',
        july: 'July',
        august: 'August',
        september: 'September',
        october: 'October',
        november: 'November',
        december: 'December'
      },
      permissionDenied: {
        title: 'Permission Denied',
        description: 'You do not have permission to access this page.'
      },
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
        actions: 'Actions',
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
        statusInfo: 'Active participants (or members) are automatically considered for all available days in new service plans. Passive participants (or members) are only considered for days for which they explicitly register.',
        updateFailure: 'Error updating participant',
        updateSuccess: 'Participant updated successfully',
        deleteSuccess: 'Participant deleted successfully',
        deleteFailure: 'Error deleting participant'
      },
      service: {
        add: 'Add new Service',
        name: 'Service name',
        save: 'Save',
        list: 'Service list',
        success: 'Service saved successfully',
        error: 'Error saving service',
        exists: 'Service already exists',
        addSetting: 'Add setting',
        rangeFrom: 'From',
        rangeTo: 'To',
        mandatoryAssignees: 'Participants',
        saturdays: 'Saturdays',
        sundays: 'Sundays',
        publicHolidays: 'Public Holidays',
        startTime: 'Start time',
        endTime: 'End time',
        actions: 'Actions',
        removeSetting: 'Remove setting'
      },
      plan: {
        add: 'Add new Service Plan',
        actions: 'Actions',
        addDate: 'Add Date',
        name: 'Service Plan name',
        description: 'Description',
        dateFrom: 'From',
        dateUntil: 'Until',
        backToList: 'Back to list',
        save: 'Save',
        date: 'Date',
        details: 'Add plan details',
        detailsInfo: 'Add or edit the details of the service plan.',
        generateInfo: 'Generate final service plan',
        availabilityInfo: 'View/edit availabilities',
        pdfInfo: 'Download as PDF',
        generatedDetailsInfo: 'Details of the final service plan',
        editDetails: 'Details',
        saveSuccess: 'Service plan saved successfully',
        saveError: 'Error saving service plan',
        saveException: 'Severe error saving service plan'
      },
      availabilities: {
        list: 'Manage availabilities',
        me: 'Your availabilities',
        yes: 'I\'m available',
        no: 'I\'m not available',
        please: 'I would like to',
        save: 'Save',
        success: 'Availabilities saved successfully'
      },
      settings: {
        title: 'Settings',
        holidayInfo: 'Set your country and state here to automatically consider the valid public holidays in service plans.',
        country: 'Country',
        state: 'State',
        save: 'Save',
        success: 'Settings saved successfully',
        error: 'Error saving settings'
      },
      error: {
        credentials: 'Invalid credentials'
      }
    },
    de: {
      general: {
        yes: 'Ja',
        no: 'Nein',
        january: 'Januar',
        february: 'Februar',
        march: 'März',
        april: 'April',
        may: 'Mai',
        june: 'Juni',
        july: 'Juli',
        august: 'August',
        september: 'September',
        october: 'Oktober',
        november: 'November',
        december: 'Dezember'
      },
      permissionDenied: {
        title: 'Zugang verweigert',
        description: 'Du hast keine Berechtigung, auf diese Seite zuzugreifen.'
      },
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
        actions: 'Aktionen',
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
        statusInfo: 'Aktive Teilnehmer (bzw. Mitglieder) werden automatisch bei neuen Dienstplänen für alle verfügbaren Tage berücksichtigt. Passive Teilnehmer (bzw. Mitglieder) werden nur für Tage berücksichtigt, für die sie sich explizit eintragen.',
        updateFailure: 'Fehler beim Aktualisieren des Teilnehmers',
        updateSuccess: 'Teilnehmer erfolgreich aktualisiert',
        deleteSuccess: 'Teilnehmer erfolgreich gelöscht',
        deleteFailure: 'Fehler beim Löschen des Teilnehmers'
      },
      service: {
        add: 'Neuen Dienst hinzufügen',
        name: 'Dienst',
        save: 'Speichern',
        list: 'Dienstliste',
        success: 'Dienst erfolgreich gespeichert',
        error: 'Fehler beim Speichern des Dienstes',
        exists: 'Dienst existiert bereits',
        addSetting: 'Einstellung hinzufügen',
        rangeFrom: 'Von',
        rangeTo: 'Bis',
        mandatoryAssignees: 'Teilnehmer',
        saturdays: 'Samstage',
        sundays: 'Sonntage',
        publicHolidays: 'Feiertage',
        startTime: 'Startzeit',
        endTime: 'Endzeit',
        actions: 'Aktionen',
        removeSetting: 'Einstellung entfernen'
      },
      plan: {
        add: 'Neuen Dienstplan hinzufügen',
        actions: 'Aktionen',
        addDate: 'Datum hinzufügen',
        name: 'Dienstplan Name',
        description: 'Beschreibung',
        dateFrom: 'Von',
        dateUntil: 'Bis',
        save: 'Speichern',
        backToList: 'Zurück zur Übersicht',
        date: 'Datum',
        details: 'Dienstplan Details festlegen',
        detailsInfo: 'Erstelle oder bearbeite die Details dieses Dienstplans.',
        availabilityInfo: 'Verfügbarkeiten einsehen/editieren',
        pdfInfo: 'Download als PDF',
        generatedDetailsInfo: 'Details des finalen Dienstplans',
        generateInfo: 'Dienstplan final generieren',
        editDetails: 'Details',
        saveSuccess: 'Dienstplan erfolgreich gespeichert',
        saveError: 'Fehler beim Speichern des Dienstplans',
        saveException: 'Schwerer Fehler beim Speichern des Dienstplans'
      },
      availabilities: {
        list: 'Verfügbarkeiten verwalten',
        me: 'Deine Verfügbarkeiten',
        yes: 'Ich kann',
        no: 'Ich kann nicht',
        please: 'Möchte ich gerne',
        save: 'Speichern',
        success: 'Verfügbarkeiten erfolgreich gespeichert'
      },
      settings: {
        title: 'Einstellungen',
        holidayInfo: 'Lege hier dein Land und Bundesland fest, um bei Dienstplänen automatisch die gültigen Feiertage zu berücksichtigen.',
        country: 'Land',
        state: 'Bundesland',
        save: 'Speichern',
        success: 'Einstellungen erfolgreich gespeichert',
        error: 'Fehler beim Speichern der Einstellungen'
      },
      error: {
        credentials: 'Ungültige Anmeldeinformationen'
      }
    }
  }
}))