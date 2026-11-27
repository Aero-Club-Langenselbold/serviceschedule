package de.ct.manager.schedule.maintenance;

/** Entries of the activity log of a defect; the UI translates them (maintenance.defect.actions.*). */
public enum DefectEventAction {
    REPORTED, UPDATED, COMMENTED, STARTED, DONE, REJECTED, REOPENED, ASSIGNED, UNASSIGNED, SIGNED, UNSIGNED
}
