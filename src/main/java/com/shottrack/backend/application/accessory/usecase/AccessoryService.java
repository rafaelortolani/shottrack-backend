package com.shottrack.backend.application.accessory.usecase;

import com.shottrack.backend.application.accessory.dto.AccessoryRegisterRequest;
import com.shottrack.backend.application.accessory.dto.AccessoryResponse;
import com.shottrack.backend.application.accessory.dto.AccessoryUpdateRequest;
import com.shottrack.backend.application.accessory.gateway.AccessoryGateway;
import com.shottrack.backend.application.accessory.gateway.AccessoryWeaponGateway;
import com.shottrack.backend.application.accessory.mapper.AccessoryMapper;
import com.shottrack.backend.application.accessory.model.Accessory;
import com.shottrack.backend.application.accessory.model.AccessoryWeapon;
import com.shottrack.backend.application.weapon.dto.WeaponResponse;
import com.shottrack.backend.application.weapon.usecase.WeaponService;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessoryService {

    private final AccessoryGateway accessoryGateway;
    private final AccessoryWeaponGateway accessoryWeaponGateway;
    private final WeaponService weaponService;
    private final AccessoryMapper accessoryMapper;

    public AccessoryResponse register(UUID userId, AccessoryRegisterRequest request) {
        Accessory accessory = Accessory.builder()
                .userId(userId)
                .name(request.name())
                .type(request.type())
                .notes(request.notes())
                .build();

        Accessory saved = accessoryGateway.save(accessory);
        return toResponseWithWeapons(saved);
    }

    public List<AccessoryResponse> listByUser(UUID userId) {
        return accessoryGateway.findAllByUserId(userId).stream()
                .map(this::toResponseWithWeapons)
                .toList();
    }

    /**
     * UC20: edição parcial — só os campos enviados (não nulos) são atualizados.
     * "Nome vazio" só é possível de checar contra o campo enviado (diferente da
     * identificação de munição, aqui não há estado combinado a considerar): se
     * enviado em branco, a edição é rejeitada antes de tocar a entidade.
     */
    public AccessoryResponse update(UUID userId, UUID accessoryId, AccessoryUpdateRequest request) {
        Accessory accessory = findOwnedAccessoryOrThrow(userId, accessoryId);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BusinessException("ACCESSORY_NAME_REQUIRED", HttpStatus.BAD_REQUEST);
            }
            accessory.setName(request.name());
        }
        if (request.type() != null) {
            accessory.setType(request.type());
        }
        if (request.notes() != null) {
            accessory.setNotes(request.notes());
        }

        Accessory saved = accessoryGateway.save(accessory);
        return toResponseWithWeapons(saved);
    }

    /**
     * UC21/ADR-0006: bloqueia a exclusão (nunca arquiva) se o acessório já foi
     * usado em alguma série. Remove as associações a armas junto — associação
     * não conta como "uso" (ver ADR-0006/ADR-0008).
     */
    public void delete(UUID userId, UUID accessoryId) {
        Accessory accessory = findOwnedAccessoryOrThrow(userId, accessoryId);

        if (isUsedInAnySeries(accessory)) {
            throw new BusinessException("ACCESSORY_IN_USE", HttpStatus.CONFLICT);
        }

        accessoryWeaponGateway.deleteAllByAccessoryId(accessoryId);
        accessoryGateway.delete(accessory);
    }

    /**
     * O domínio de Série ainda não existe, então nenhum acessório pode estar
     * "em uso" — sempre retorna false até lá. Quando Série existir, troca-se
     * este método pela consulta real, sem reescrever o restante de delete().
     */
    private boolean isUsedInAnySeries(Accessory accessory) {
        return false;
    }

    /**
     * Reaproveitado por AccessoryWeaponService (UC19) pra validar posse antes
     * de associar/desassociar uma arma.
     */
    public Accessory findOwnedAccessoryOrThrow(UUID userId, UUID accessoryId) {
        return accessoryGateway.findById(accessoryId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException("ACCESSORY_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    /**
     * Reaproveitado por AccessoryWeaponService (UC19) pra devolver o acessório
     * com a lista de armas associadas já atualizada após associar/desassociar.
     */
    public AccessoryResponse toResponseWithWeapons(Accessory accessory) {
        List<WeaponResponse> weapons = accessoryWeaponGateway.findAllByAccessoryId(accessory.getId()).stream()
                .map(AccessoryWeapon::getWeaponId)
                .map(weaponService::getResponseById)
                .toList();
        return accessoryMapper.toResponse(accessory, weapons);
    }
}
