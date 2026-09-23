package br.com.helpdesk.web.dto.response;

public record CategoriaResponse(Long id, String nome, Integer slaHoras, boolean ativa) {
}
