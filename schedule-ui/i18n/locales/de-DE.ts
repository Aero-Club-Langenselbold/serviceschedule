export default defineI18nLocale(async locale => {
    return {
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
        },
        alarms: {
          menu: 'Alarmanlage',
          list: 'Ereignisprotokoll'
        }
      },
      app: {
        dateLocale: 'de-DE',
        name: 'Mitgleiderbereich V1.0',
        welcome: 'Herzlich willkommen im Mitgleiderbereich des ACL',
        user: {
          participantWelcome: 'Du bist in der Rolle als Benutzer angemeldet und kannst daher Dienstpläne einsehen und deine eigenen Verfügbarkeiten eintragen.',
          userWelcome: 'Du bist in der Rolle als Benutzer angemeldet, dir ist aber noch kein Teilnehmer zugeordnet. Bitte wende dich an den Administrator.',
          adminWelcome: 'Du bist in der Rolle als Administrator angemeldet und kannst daher alle Funktionen des Mitgleiderbereichs nutzen.'
        }
      },
      alarm: {
        list: 'Ereignisprotokoll',
        timestamp: 'Zeitstempel',
        message: 'Nachricht',
        detailMessage: 'Detail',
        rfidMessage: 'RFID-Leser: Schlüssel erkannt',
        rfidDetail: 'Zugang durch Schlüssel Nr. ',
        rfidDenied: 'Zugang verweigert für Schlüssel Nr. ',
        statusChangeMessage: 'Alarmanlage: Statusänderung',
        armed: 'Anlage Scharf',
        disarmed: 'Anlage Unscharf',
        sabotage: 'Sabotage',
        fault: 'Störung',
        alarm: 'Alarm',
        zone1: 'Zone 1',
        zone2: 'Zone 2',
        zone3: 'Zone 3',
        zone4: 'Zone 4',
        zone5: 'Zone 5',
        zone6: 'Zone 6',
        zone7: 'Zone 7',
        zone8: 'Zone 8'
      },
      participants: {
        filter: 'Filter',
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
})
