package com.shottrack.backend.application.accessory.gateway;

import com.shottrack.backend.application.accessory.model.Accessory;

public interface AccessoryGateway {

    Accessory save(Accessory accessory);
}
