package de.ct.manager.schedule.maintenance;

/** What happened to a task. Stored as text, the UI translates it. */
public enum TaskHistoryAction {
    CREATED, UPDATED, STARTED, RESET, FINISHED, REOPENED, SIGNED, UNSIGNED, NOTE_ADDED, NOTE_DELETED
}
