package de.ct.manager.schedule.maintenance;

/** Derived from the stock: EMPTY at zero, LOW at the "low" level or at/below the minimum quantity, OK otherwise. */
public enum InventoryStockStatus {
    OK, LOW, EMPTY
}
