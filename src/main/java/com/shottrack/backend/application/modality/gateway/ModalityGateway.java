package com.shottrack.backend.application.modality.gateway;

import com.shottrack.backend.application.modality.model.Modality;

import java.util.List;

public interface ModalityGateway {

    List<Modality> findAll();
}
