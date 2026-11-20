package de.ct.manager.schedule.maintenance;

/** How the stock of an item is recorded: as a number of units or as a fill level (0 = empty, 1 = low, 2 = enough). */
public enum InventoryCounting {
    COUNT, LEVEL;

    public static final int LEVEL_EMPTY = 0;
    public static final int LEVEL_LOW = 1;
    public static final int LEVEL_FULL = 2;
}
