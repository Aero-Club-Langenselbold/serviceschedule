
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
        forgot: 'Forgot password?',
        authSecret: 'Authentication Secret (if 2FA is enabled)',
        securityAnswer: 'Antwort auf Sicherheitsfrage'
      },
      register: {
        title: 'Register to the schedule manager',
        username: 'E-Mail-Address (as provided to the schedule admin)',
        password: 'Password',
        key: 'Registration Key',
        confirmPassword: 'Confirm Password',
        submit: 'Register',
        mustContain: 'Must contain:',
        enterPassword: 'Enter a password',
        weakPassword: 'Weak password',
        mediumPassword: 'Medium password',
        strongPassword: 'Strong password',
        numberCharacters: 'At least 8 characters',
        numberMandatory: 'At least 1 number',
        lowercaseMandatory: 'At least 1 lowercase letter',
        uppercaseMandatory: 'At least 1 uppercase letter',
        passwordMismatch: 'Passwords do not match',
        success: 'Registration successful',
      },
      nav: {
        home: 'Home',
        about: 'About',
        contact: 'Contact',
        participants: {
          menu: 'Manage Participants',
          list: 'Show Participants',
          add: 'Add Particpant',
          import: 'Import Participants'
        },
        service: {
          menu: 'Manage Services',
          list: 'Show Services',
          add: 'Add Service'
        },
        plan: {
          menu: 'Manage Service Plans',
          usermenu: 'Service Plans',
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
        fullname: 'Name',
        email: 'Email',
        dateOfBirth: 'Date of Birth',
        status: 'Status',
        maxAssignments: 'Max. Assignments/Service Plan',
        minAssignments: 'Min. Assignments/Service Plan',
        registrationKey: 'Registration Key',
        services: 'Assigned Services',
        save: 'Save',
        success: 'Participant saved successfully',
        error: 'Error saving participant',
        exists: 'Participant already exists',
        list: 'Participant List',
        active: 'Active Participant',
        inactive: 'Passive Participant',
        autoAssign: 'Auto-assign',
        statusInfo: 'Active participants (or members) are automatically considered for all available days in new service plans. Passive participants (or members) are only considered for days for which they explicitly register.',
        updateFailure: 'Error updating participant',
        updateSuccess: 'Participant updated successfully',
        deleteSuccess: 'Participant deleted successfully',
        deleteFailure: 'Error deleting participant',
        import: 'Import Participants',
        importSuccess: 'Participants imported successfully',
        importFailure: 'Error importing participants',
        registrationKeyCopied: 'The registration key has been copied to the clipboard.',
        diff: {
          success: 'No changes',
          warning: 'Changes',
          error: 'Missing in import',
          secondary: 'New in import'
        }
      },
      service: {
        add: 'Add new Service',
        name: 'Service name',
        addAtImport: 'Add at import',
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
        deleteInfo: 'Delete service plan',
        availabilityInfo: 'View/edit availabilities',
        pdfInfo: 'Download as PDF',
        csvInfo: 'Download as CSV',
        generatedDetailsInfo: 'Details of the final service plan',
        statisticsInfo: 'View statistics of this plan',
        editDetails: 'Details',
        saveSuccess: 'Service plan saved successfully',
        saveError: 'Error saving service plan',
        saveException: 'Severe error saving service plan',
        deleteConfirmationTitle: 'Really delete service plan?',
        deleteConfirmation: 'Confirm',
        cancel: 'Cancel',
        legend: {
          button: 'Legend',
          title: 'Meaning of the buttons',
          set: {
            unavailable: 'Unavailable (saved)',
            available: 'Available (saved)',
            requested: 'Requested (saved)'
          },
          open: {
            unavailable: 'Unavailable (not saved yet)',
            available: 'Available (not saved yet)',
            requested: 'Requested (not saved yet)'
          }
        },
        statistics: {
          totalCount: 'Gesamtanzahl Dienste'
        }
      },
      availabilities: {
        list: 'Manage availabilities',
        me: 'Your availabilities',
        yes: 'I\'m available',
        no: 'I\'m not available',
        please: 'I would like to',
        requestedService: 'Service request',
        noServiceAvailable: 'No service available',
        selectAll: 'Select all',
        save: 'Save',
        success: 'Availabilities saved successfully',
        invalid: 'Minimum days to select: '
      },
      settings: {
        title: 'Settings',
        holidayInfo: 'Set your country and state here to automatically consider the valid public holidays in service plans.',
        country: 'Country',
        state: 'State',
        save: 'Save',
        success: 'Settings saved successfully',
        error: 'Error saving settings',
        name: 'Setting',
        value: 'Value',
        keys: {
          min_participation_count: 'Minimum number of available days',
          security_feature_question: 'Security question',
          security_feature_answer: 'Answer'
        },
        countries: {
          germany: 'Germany'
        },
        states: {
          badenwürttemberg: 'Baden-Württemberg',
          bavaria: 'Bavaria',
          berlin: 'Berlin',
          brandenburg: 'Brandenburg',
          bremen: 'Bremen',
          hamburg: 'Hamburg',
          hesse: 'Hesse',
          lowersaxony: 'Lower Saxony',
          mecklenburgvorpommern: 'Mecklenburg-Vorpommern',
          northrhinewestphalia: 'North Rhine-Westphalia',
          rhinelandpalatinate: 'Rhineland-Palatinate',
          saarland: 'Saarland',
          saxony: 'Saxony',
          saxonyanhalt: 'Saxony-Anhalt',
          schleswigholstein: 'Schleswig-Holstein',
          thuringia: 'Thuringia'
        }
      },
      request: {
        title: 'Bug/Feature List',
        headline: 'Subject',
        description: 'Description',
        status: {
          name: 'Status',
          open: 'Open',
          in_progress: 'In Progress',
          closed: 'Closed',
          rejected: 'Rejected'
        },
        type: {
          name: 'Type',
          bug: 'Bug',
          bugdesc: 'Report a bug in the system',
          feature: 'Feature',
          featuredesc: 'Report a request for a new feature'
        },
        author: 'Author',
        createdAt: 'Created at',
        add: {
          open: 'Create new',
          title: 'Create new Bug/Feature',
          submit: 'Save',
          cancel: 'Cancel',
          error: 'Error creating',
          success: 'Bug/Feature created successfully',
        },
        edit: 'Edit'
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
        forgot: 'Password vergessen?',
        authSecret: 'Sicherheitscode (bei aktivierter 2FA)',
        securityAnswer: 'Antwort auf Sicherheitsfrage'
      },
      register: {
        title: 'Registrieren beim Dienstplanmanager',
        username: 'E-Mail-Adresse (wie vom Administrator bereitgestellt)',
        password: 'Passwort',
        key: 'Registrierungsschlüssel',
        confirmPassword: 'Passwort bestätigen',
        submit: 'Registrieren',
        mustContain: 'Muss enthalten:',
        enterPassword: 'Gib ein Passwort ein',
        weakPassword: 'Schwaches Passwort',
        mediumPassword: 'Mittleres Passwort',
        strongPassword: 'Starkes Passwort',
        numberCharacters: 'Mindestens 8 Zeichen',
        numberMandatory: 'Mindestens 1 Zahl',
        lowercaseMandatory: 'Mindestens 1 Kleinbuchstabe',
        uppercaseMandatory: 'Mindestens 1 Großbuchstabe',
        passwordMismatch: 'Passwörter stimmen nicht überein',
        success: 'Registrierung erfolgreich',
      },
      nav: {
        home: 'Home',
        about: 'About',
        contact: 'Kontakt',
        participants: {
          menu: 'Teilnehmer verwalten',
          list: 'Teilnehmer anzeigen',
          add: 'Teilnehmer hinzufügen',
          import: 'Teilnehmer importieren'
        },
        service: {
          menu: 'Dienste verwalten',
          list: 'Dienste anzeigen',
          add: 'Dienst hinzufügen'
        },
        plan: {
          menu: 'Dienstpläne verwalten',
          usermenu: 'Dienstpläne',
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
        fullname: 'Name',
        email: 'Email',
        dateOfBirth: 'Geburtsdatum',
        status: 'Status',
        maxAssignments: 'Maximale Dienste/Dienstplan',
        minAssignments: 'Minimale Dienste/Dienstplan',
        registrationKey: 'Registrierungsschlüssel',
        services: 'Zugeordnete Dienste',
        save: 'Speichern',
        success: 'Teilnehmer erfolgreich gespeichert',
        error: 'Fehler beim Speichern des Teilnehmers',
        exists: 'Teilnehmer existiert bereits',
        list: 'Teilnehmerliste',
        active: 'Aktiver Teilnehmer',
        inactive: 'Passiver Teilnehmer',
        autoAssign: 'Automatisch zuordnen',
        statusInfo: 'Aktive Teilnehmer (bzw. Mitglieder) werden automatisch bei neuen Dienstplänen für alle verfügbaren Tage berücksichtigt. Passive Teilnehmer (bzw. Mitglieder) werden nur für Tage berücksichtigt, für die sie sich explizit eintragen.',
        updateFailure: 'Fehler beim Aktualisieren des Teilnehmers',
        updateSuccess: 'Teilnehmer erfolgreich aktualisiert',
        deleteSuccess: 'Teilnehmer erfolgreich gelöscht',
        deleteFailure: 'Fehler beim Löschen des Teilnehmers',
        import: 'Teilnehmer importieren',
        importSuccess: 'Teilnehmer erfolgreich importiert',
        importFailure: 'Fehler beim Importieren der Teilnehmer',
        registrationKeyCopied: 'Der Registrierungsschlüssel wurde in die Zwischenablage kopiert.',
        diff: {
          success: 'Keine Änderungen',
          warning: 'Änderungen',
          error: 'Fehlend im Import',
          secondary: 'Neu im Import'
        }
      },
      service: {
        add: 'Neuen Dienst hinzufügen',
        name: 'Dienst',
        addAtImport: 'Bei Import hinzufügen',
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
        csvInfo: 'Download als CSV',
        generatedDetailsInfo: 'Details des finalen Dienstplans',
        statisticsInfo: 'Statistiken dieses Plans einsehen',
        generateInfo: 'Dienstplan final generieren',
        deleteInfo: 'Dienstplan löschen',
        editDetails: 'Details',
        saveSuccess: 'Dienstplan erfolgreich gespeichert',
        saveError: 'Fehler beim Speichern des Dienstplans',
        saveException: 'Schwerer Fehler beim Speichern des Dienstplans',
        deleteConfirmationTitle: 'Dienstplan wirklich löschen?',
        deleteConfirmation: 'Bestätigen',
        cancel: 'Abbrechen',
        legend: {
          button: 'Legende',
          title: 'Bedeutung der Buttons',
          set: {
            unavailable: 'Nicht verfügbar (gespeichert)',
            available: 'Verfügbar (gespeichert)',
            requested: 'Terminwunsch (gespeichert)'
          },
          open: {
            unavailable: 'Nicht verfügbar (noch nicht gespeichert)',
            available: 'Verfügbar (noch nicht gespeichert)',
            requested: 'Terminwunsch (noch nicht gespeichert)'
          }
        },
        statistics: {
          totalCount: 'Gesamtanzahl Dienste'
        }
      },
      availabilities: {
        list: 'Verfügbarkeiten verwalten',
        me: 'Deine Verfügbarkeiten',
        yes: 'Ich kann',
        no: 'Ich kann nicht',
        please: 'Möchte ich gerne',
        requestedService: 'Dienstwunsch',
        noServiceAvailable: 'Kein Dienst verfügbar',
        selectAll: 'Alle auswählen',
        save: 'Speichern',
        success: 'Verfügbarkeiten erfolgreich gespeichert',
        invalid: 'Minimale Anzahl an verfügbaren Tagen: '
      },
      settings: {
        title: 'Einstellungen',
        holidayInfo: 'Lege hier dein Land und Bundesland fest, um bei Dienstplänen automatisch die gültigen Feiertage zu berücksichtigen.',
        country: 'Land',
        state: 'Bundesland',
        save: 'Speichern',
        success: 'Einstellungen erfolgreich gespeichert',
        error: 'Fehler beim Speichern der Einstellungen',
        countries: {
          germany: 'Deutschland'
        },
        name: 'Einstellung',
        value: 'Wert',
        keys: {
          min_participation_count: 'Minimale Anzahl an verfügbaren Tagen',
          security_feature_question: 'Sicherheitsfrage',
          security_feature_answer: 'Antwort'
        },
        states: {
          badenwürttemberg: 'Baden-Württemberg',
          bavaria: 'Bayern',
          berlin: 'Berlin',
          brandenburg: 'Brandenburg',
          bremen: 'Bremen',
          hamburg: 'Hamburg',
          hesse: 'Hessen',
          lowersaxony: 'Niedersachsen',
          mecklenburgvorpommern: 'Mecklenburg-Vorpommern',
          northrhinewestphalia: 'Nordrhein-Westfalen',
          rhinelandpalatinate: 'Rheinland-Pfalz',
          saarland: 'Saarland',
          saxony: 'Sachsen',
          saxonyanhalt: 'Sachsen-Anhalt',
          schleswigholstein: 'Schleswig-Holstein',
          thuringia: 'Thüringen'
        }
      },
      request: {
        title: 'Bug/Feature Liste',
        headline: 'Thema',
        description: 'Beschreibung',
        status: {
          name: 'Status',
          open: 'Offen',
          in_progress: 'In Bearbeitung',
          closed: 'Geschlossen',
          rejected: 'Abgelehnt'
        },
        type: {
          name: 'Typ',
          bug: 'Bug',
          bugdesc: 'Melde einen Fehler im System',
          feature: 'Feature',
          featuredesc: 'Melde einen Wunsch für eine neue Funktion'
        },
        author: 'Ersteller',
        createdAt: 'Erstelldatum',
        add: {
          open: 'Neu erstellen',
          title: 'Neuen Bug/Neues Feature erstellen',
          submit: 'Speichern',
          cancel: 'Abbrechen',
          error: 'Fehler beim Erstellen',
          success: 'Bug/Feature erfolgreich erstellt',
        },
        edit: 'Bearbeiten'
      },
      error: {
        credentials: 'Ungültige Anmeldeinformationen'
      }
    }
  }
}))