package de.ct.manager.schedule.maintenance;

/** OPEN and IN_PROGRESS are open defects; DONE and REJECTED close a defect, a maintenance administrator then signs it off. */
public enum DefectStatus {
    OPEN, IN_PROGRESS, DONE, REJECTED;

    public boolean isClosed() {
        return this == DONE || this == REJECTED;
    }
}
