package br.com.helpdesk.domain.enums;

/** Perfis de acesso do sistema. A tabela de permissoes vive em PermissaoService. */
public enum Perfil {

    /** Abre chamados e acompanha apenas os proprios. */
    SOLICITANTE,

    /** Assume e resolve chamados. Enxerga a base inteira. */
    ATENDENTE,

    /** Faz tudo, mais os cadastros de usuario e categoria. */
    ADMIN
}
