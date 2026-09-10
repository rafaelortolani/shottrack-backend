package com.shottrack.backend.application.ammunition.gateway;

import com.shottrack.backend.application.ammunition.model.Ammunition;

public interface AmmunitionGateway {

    Ammunition save(Ammunition ammunition);
}
