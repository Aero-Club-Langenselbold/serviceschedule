-- Workshop schedule: an aircraft can be planned into the workshop several times, the period no longer belongs to the aircraft.
CREATE TABLE maintenance_workshop_slot (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    aircraft_id character varying(36) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    note character varying(255),
    CONSTRAINT maintenance_workshop_slot_pkey PRIMARY KEY (id),
    CONSTRAINT maintenance_workshop_slot_period CHECK (end_date >= start_date),
    CONSTRAINT fk_maintenance_workshop_slot_aircraft FOREIGN KEY (aircraft_id) REFERENCES maintenance_aircraft (id) ON DELETE CASCADE
);

CREATE INDEX idx_maintenance_workshop_slot_aircraft ON maintenance_workshop_slot (aircraft_id);
CREATE INDEX idx_maintenance_workshop_slot_start ON maintenance_workshop_slot (start_date);

-- Keep the existing periods. There is at most one per aircraft, so the aircraft id serves as the id of the slot.
-- A period with only one end becomes a single day.
INSERT INTO maintenance_workshop_slot (id, version, aircraft_id, start_date, end_date)
SELECT id, 0, id, COALESCE(workshop_start, workshop_end), COALESCE(workshop_end, workshop_start)
FROM maintenance_aircraft
WHERE workshop_start IS NOT NULL OR workshop_end IS NOT NULL;

ALTER TABLE maintenance_aircraft DROP CONSTRAINT maintenance_aircraft_workshop_period;
ALTER TABLE maintenance_aircraft DROP COLUMN workshop_start;
ALTER TABLE maintenance_aircraft DROP COLUMN workshop_end;

-- Inventory: consumables and spare parts with their stock. Spare parts name the aircraft types they fit.
CREATE TABLE maintenance_inventory_item (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    name character varying(255) NOT NULL,
    kind character varying(20) NOT NULL,
    location character varying(255),
    counting character varying(20) NOT NULL,
    quantity int4 DEFAULT 0 NOT NULL,
    min_quantity int4,
    unit character varying(30),
    comment text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT maintenance_inventory_item_pkey PRIMARY KEY (id),
    CONSTRAINT maintenance_inventory_item_kind CHECK (kind IN ('CONSUMABLE', 'SPARE_PART')),
    CONSTRAINT maintenance_inventory_item_counting CHECK (counting IN ('COUNT', 'LEVEL')),
    -- LEVEL stores 0 = empty, 1 = low, 2 = enough
    CONSTRAINT maintenance_inventory_item_quantity CHECK (quantity >= 0 AND (counting = 'COUNT' OR quantity <= 2)),
    CONSTRAINT maintenance_inventory_item_min_quantity CHECK (min_quantity IS NULL OR (min_quantity >= 0 AND counting = 'COUNT'))
);

-- Aircraft types a spare part fits; no row means it fits every aircraft.
CREATE TABLE maintenance_inventory_item_aircraft_type (
    item_id character varying(36) NOT NULL,
    aircraft_type character varying(100) NOT NULL,
    CONSTRAINT maintenance_inventory_item_aircraft_type_pkey PRIMARY KEY (item_id, aircraft_type),
    CONSTRAINT fk_maintenance_inventory_item_aircraft_type_item FOREIGN KEY (item_id) REFERENCES maintenance_inventory_item (id) ON DELETE CASCADE
);

CREATE TABLE maintenance_inventory_history (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    item_id character varying(36) NOT NULL,
    actor_id character varying(36),
    action character varying(30) NOT NULL,
    quantity int4,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT maintenance_inventory_history_pkey PRIMARY KEY (id),
    CONSTRAINT maintenance_inventory_history_action CHECK (action IN ('CREATED', 'UPDATED', 'COUNTED')),
    CONSTRAINT fk_maintenance_inventory_history_item FOREIGN KEY (item_id) REFERENCES maintenance_inventory_item (id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_inventory_history_actor FOREIGN KEY (actor_id) REFERENCES auth_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_maintenance_inventory_history_item ON maintenance_inventory_history (item_id);
