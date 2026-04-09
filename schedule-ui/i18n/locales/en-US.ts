export default defineI18nLocale(async locale => {
    return {
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
        },
        alarms: {
          menu: 'Alarm system',
          list: 'Event log'
        }
      },
      app: {
        dateLocale: 'en-US',
        name: 'Member Area V1.0',
        welcome: 'Welcome to the Member Area of the ACL',
        user: {
          participantWelcome: 'You are logged in as a user and can therefore view service plans and enter your own availability.',
          userWelcome: 'You are logged in as a user, but no participant has been assigned to you yet. Please contact the administrator.',
          adminWelcome: 'You are logged in as an administrator and can therefore use all functions of the Member Area.'
        }
      },
      alarm: {
        list: 'Event Log',
        timestamp: 'Timestamp',
        rfidId: 'RFID ID',
        internalTime: 'Internal Time',
        status: 'Status',
        rawData: 'Raw Data'
      },
      participants: {
        filter: 'Filter',
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
      alarm: {
        list: 'Event log'
      },
      error: {
        credentials: 'Invalid credentials'
      }
    }
})