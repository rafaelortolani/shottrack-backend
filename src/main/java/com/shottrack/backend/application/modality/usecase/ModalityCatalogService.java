package com.shottrack.backend.application.modality.usecase;

import com.shottrack.backend.application.modality.dto.ModalityResponse;
import com.shottrack.backend.application.modality.gateway.ModalityGateway;
import com.shottrack.backend.application.modality.mapper.ModalityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModalityCatalogService {

    private final ModalityGateway modalityGateway;
    private final ModalityMapper modalityMapper;

    public List<ModalityResponse> listAll() {
        return modalityGateway.findAll().stream().map(modalityMapper::toResponse).toList();
    }
}
