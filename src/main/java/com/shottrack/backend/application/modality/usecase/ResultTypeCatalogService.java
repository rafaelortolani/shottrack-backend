package com.shottrack.backend.application.modality.usecase;

import com.shottrack.backend.application.modality.dto.ResultTypeResponse;
import com.shottrack.backend.application.modality.gateway.ResultTypeGateway;
import com.shottrack.backend.application.modality.mapper.ModalityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultTypeCatalogService {

    private final ResultTypeGateway resultTypeGateway;
    private final ModalityMapper modalityMapper;

    public List<ResultTypeResponse> listAll() {
        return resultTypeGateway.findAll().stream().map(modalityMapper::toResponse).toList();
    }
}
