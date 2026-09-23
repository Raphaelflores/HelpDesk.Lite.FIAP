package br.com.helpdesk.service;

import br.com.helpdesk.domain.enums.Perfil;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.exception.AcessoNegadoException;
import br.com.helpdesk.domain.model.Chamado;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.domain.state.TransicaoStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * A tabela de permissoes da secao 3 do documento, traduzida em codigo (ADR-004).
 *
 * <p>Classe pura: nao acessa banco, nao conhece HTTP. Recebe o usuario por parametro
 * (ADR-013) e pode ser instanciada com {@code new} num teste.</p>
 *
 * <p><b>Responde "quem pode", nunca "pode agora"</b> (ADR-015). O status atual do chamado
 * e assunto da {@link TransicaoStatus}, que roda depois e devolve 422. Por isso um
 * atendente que tenta agir no chamado de outro recebe 403 e nao descobre em que estado
 * aquele chamado esta.</p>
 */
@Service
@RequiredArgsConstructor
public class PermissaoService {

    private final TransicaoStatus transicaoStatus;

    /** Abrir chamado: todos os perfis. Existe para deixar a regra explicita. */
    public void verificarAbertura(Usuario usuario) {
        // Solicitante, atendente e admin podem abrir chamado. Nada a negar.
    }

    /**
     * Ver o chamado: o solicitante so enxerga os proprios.
     *
     * <p>Na listagem a mesma regra e aplicada na query, pela ChamadoSpecification.</p>
     */
    public void verificarVisualizacao(Usuario usuario, Chamado chamado) {
        if (usuario.getPerfil() == Perfil.SOLICITANTE && !chamado.foiSolicitadoPor(usuario.getId())) {
            throw new AcessoNegadoException("Voce so pode acessar os chamados que abriu");
        }
    }

    /** Assumir: apenas atendente e admin. */
    public void verificarAssuncao(Usuario usuario, Chamado chamado) {
        if (usuario.getPerfil() == Perfil.SOLICITANTE) {
            throw new AcessoNegadoException("Apenas atendentes podem assumir chamados");
        }
    }

    /**
     * Alterar status: cruza o perfil, o status-alvo e a relacao com o chamado.
     *
     * <ul>
     *   <li>ADMIN: qualquer alvo alcancavel, sem restricao de relacao.</li>
     *   <li>ATENDENTE: so alvos que o perfil dele alcanca, e so em chamado que ele atende.</li>
     *   <li>SOLICITANTE: so alvos que o perfil dele alcanca, e so em chamado que ele abriu.</li>
     * </ul>
     *
     * <p>Voltar para ABERTO nao e alcancavel por ninguem, entao sempre cai em 403.</p>
     */
    public void verificarAlteracaoStatus(Usuario usuario, Chamado chamado, StatusChamado novoStatus) {
        Perfil perfil = usuario.getPerfil();

        Set<Perfil> perfisQuePodem = transicaoStatus.perfisQueLevamA(novoStatus);
        if (!perfisQuePodem.contains(perfil)) {
            throw new AcessoNegadoException(
                    "O perfil %s nao pode marcar um chamado como %s".formatted(perfil, novoStatus));
        }

        if (perfil == Perfil.ATENDENTE && !chamado.eAtendidoPor(usuario.getId())) {
            throw new AcessoNegadoException("Voce so pode alterar o status dos chamados que atende");
        }
        if (perfil == Perfil.SOLICITANTE && !chamado.foiSolicitadoPor(usuario.getId())) {
            throw new AcessoNegadoException("Voce so pode alterar o status dos chamados que abriu");
        }
    }

    /** Comentar segue a mesma visibilidade da leitura do chamado. */
    public void verificarComentario(Usuario usuario, Chamado chamado) {
        if (usuario.getPerfil() == Perfil.SOLICITANTE && !chamado.foiSolicitadoPor(usuario.getId())) {
            throw new AcessoNegadoException("Voce so pode comentar nos chamados que abriu");
        }
    }

    public void verificarGestaoCategorias(Usuario usuario) {
        exigirAdmin(usuario, "gerenciar categorias");
    }

    public void verificarGestaoUsuarios(Usuario usuario) {
        exigirAdmin(usuario, "gerenciar usuarios");
    }

    /** Dashboard: atendente e admin. O solicitante nao ve indicadores da base inteira. */
    public void verificarDashboard(Usuario usuario) {
        if (usuario.getPerfil() == Perfil.SOLICITANTE) {
            throw new AcessoNegadoException("Apenas atendentes e administradores veem o dashboard");
        }
    }

    private void exigirAdmin(Usuario usuario, String acao) {
        if (usuario.getPerfil() != Perfil.ADMIN) {
            throw new AcessoNegadoException("Apenas administradores podem " + acao);
        }
    }
}
