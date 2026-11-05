package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.model.Role;
import com.shottrack.backend.application.user.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserGateway {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    User save(User user);

    /** S22: grava e envia ao banco na hora — a violação de unicidade do e-mail aparece aqui, não só no commit. */
    User saveAndFlush(User user);

    boolean existsById(UUID id);

    Optional<User> findByIdForUpdate(UUID id);

    /** Consulta escalar: sempre vai ao banco, nunca devolve papel de entidade em cache. */
    Optional<Role> findRoleById(UUID id);

    void deleteById(UUID id);

    List<User> findAllByIdIn(Collection<UUID> ids);

    /** UC65: busca por trecho do e-mail (sem diferenciar caixa), ordenada por e-mail, no máximo `limit` resultados. */
    List<User> searchByEmailFragment(String fragment, int limit);

    /** UC63: plano → quantidade de atletas; plano sem atleta não aparece no mapa. */
    Map<UUID, Long> countByPlanIds();

    long countByPlanId(UUID planId);

    /** Consulta escalar (sem carregar a entidade): usada pelo cache de limites. */
    Optional<UUID> findPlanIdById(UUID id);
}
