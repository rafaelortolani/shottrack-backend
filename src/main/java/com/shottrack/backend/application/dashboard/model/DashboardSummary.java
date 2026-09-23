package com.shottrack.backend.application.dashboard.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ADR-0015: um registro por atleta, recalculado do zero a cada evento
 * DashboardRecalculationRequested — nunca atualizado incrementalmente.
 * Só guarda o que depende de histórico inteiro de séries/resultados; as
 * seções do UC42 que dependem de dados sem evento (onboarding, ação
 * principal, últimos treinos, acervo) ficam fora (ADR-0015).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "dashboard_summary")
public class DashboardSummary extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private final UUID userId;

    @Column(name = "trainings_this_month", nullable = false)
    private int trainingsThisMonth;

    @Column(name = "shots_this_month", nullable = false)
    private int shotsThisMonth;

    /**
     * EAGER: quem carrega um DashboardSummary sempre precisa das
     * modalidades junto (é um "read model" completo, não uma entidade de
     * navegação parcial) — sem isso, acessar a lista fora da sessão que
     * carregou o resumo (UC42 não abre transação só pra ler) lançaria
     * LazyInitializationException.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dashboard_summary_modalities", joinColumns = @JoinColumn(name = "dashboard_summary_id"))
    @Column(name = "modality_name")
    private List<String> practicedModalities = new ArrayList<>();

    /**
     * EAGER pelo mesmo motivo de practicedModalities. FetchMode.SELECT
     * carrega numa query separada: duas listas EAGER no mesmo JOIN fazem o
     * Hibernate lançar MultipleBagFetchException.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @CollectionTable(name = "dashboard_summary_modality_stats", joinColumns = @JoinColumn(name = "dashboard_summary_id"))
    private List<ModalityStats> modalityStats = new ArrayList<>();

    /**
     * ADR-0016: EAGER + FetchMode.SELECT pelo mesmo motivo de modalityStats.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @CollectionTable(name = "dashboard_summary_records", joinColumns = @JoinColumn(name = "dashboard_summary_id"))
    private List<ResultRecord> records = new ArrayList<>();

    @Builder
    private DashboardSummary(UUID userId) {
        this.userId = userId;
    }

    /**
     * ADR-0015: único jeito de mudar o resumo — sempre a substituição
     * completa de um recálculo do zero, nunca ajuste incremental de um
     * campo isolado.
     */
    public void replaceWith(int trainingsThisMonth, int shotsThisMonth, List<String> practicedModalities,
                             List<ModalityStats> modalityStats, List<ResultRecord> records) {
        this.trainingsThisMonth = trainingsThisMonth;
        this.shotsThisMonth = shotsThisMonth;
        this.practicedModalities.clear();
        this.practicedModalities.addAll(practicedModalities);
        this.modalityStats.clear();
        this.modalityStats.addAll(modalityStats);
        this.records.clear();
        this.records.addAll(records);
    }
}
