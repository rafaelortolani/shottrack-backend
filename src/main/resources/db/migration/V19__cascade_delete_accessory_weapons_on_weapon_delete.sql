ALTER TABLE accessory_weapons
    DROP CONSTRAINT accessory_weapons_weapon_id_fkey,
    ADD CONSTRAINT accessory_weapons_weapon_id_fkey
        FOREIGN KEY (weapon_id) REFERENCES weapons(id) ON DELETE CASCADE;
