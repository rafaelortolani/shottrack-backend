ALTER TABLE accessory_weapons
    DROP CONSTRAINT accessory_weapons_accessory_id_fkey,
    ADD CONSTRAINT accessory_weapons_accessory_id_fkey
        FOREIGN KEY (accessory_id) REFERENCES accessories(id) ON DELETE CASCADE;
