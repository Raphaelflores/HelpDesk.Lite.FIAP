package br.com.helpdesk.domain.model;

import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Chamado: o agregado central do sistema.
 *
 * <p>Todas as associacoes sao LAZY; as consultas de detalhe usam {@code @EntityGraph}.
 * Indices em status, solicitante e atendente cobrem os filtros da listagem.</p>
 */
@Entity
@Table(name = "chamado", indexes = {
        @Index(name = "idx_chamado_status", columnList = "status"),
        @Index(name = "idx_chamado_solicitante", columnList = "solicitante_id"),
        @Index(name = "idx_chamado_atendente", columnList = "atendente_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusChamado status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    /** Nulo ate o chamado ser assumido. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendente_id")
    private Usuario atendente;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    /** Preenchido ao entrar em RESOLVIDO; zerado ao reabrir. */
    @Column(name = "resolvido_em")
    private Instant resolvidoEm;

    @Column(name = "fechado_em")
    private Instant fechadoEm;

    public boolean pertenceA(Usuario usuario) {
        return solicitante != null && usuario != null
                && Objects.equals(solicitante.getId(), usuario.getId());
    }

    public boolean eAtendidoPor(Long usuarioId) {
        return atendente != null && Objects.equals(atendente.getId(), usuarioId);
    }

    public boolean foiSolicitadoPor(Long usuarioId) {
        return solicitante != null && Objects.equals(solicitante.getId(), usuarioId);
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof Chamado chamado && id != null && Objects.equals(id, chamado.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
