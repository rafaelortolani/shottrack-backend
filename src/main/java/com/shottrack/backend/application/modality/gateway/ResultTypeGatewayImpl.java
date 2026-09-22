package com.shottrack.backend.application.modality.gateway;

import com.shottrack.backend.application.modality.gateway.repository.ResultTypeRepository;
import com.shottrack.backend.application.modality.model.ResultType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ResultTypeGatewayImpl implements ResultTypeGateway {

    private final ResultTypeRepository resultTypeRepository;

    @Override
    public List<ResultType> findAll() {
        return resultTypeRepository.findAll();
    }

    @Override
    public Optional<ResultType> findById(UUID id) {
        return resultTypeRepository.findById(id);
    }

    @Override
    public Optional<ResultType> findByName(String name) {
        return resultTypeRepository.findByName(name);
    }
}
