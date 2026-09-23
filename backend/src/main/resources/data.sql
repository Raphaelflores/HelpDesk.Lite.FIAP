-- ---------------------------------------------------------------------------------------
-- Seed de demonstracao do HelpDesk Lite (secao 9 de .ai/business-rules.md).
--
-- Roda a cada boot, sobre um H2 em memoria recriado do zero.
--
-- Os instantes sao RELATIVOS ao momento em que a aplicacao sobe (DATEADD sobre
-- CURRENT_TIMESTAMP). Assim os cenarios "fora do SLA" continuam verdadeiros em qualquer
-- dia, sem precisar editar datas fixas.
--
-- ATENCAO: este arquivo e fixture de DEMONSTRACAO. Nenhum teste automatizado pode
-- depender dele -- testes usam os builders de src/test/java/br/com/helpdesk/support/.
-- ---------------------------------------------------------------------------------------

-- ----------------------------------------------------------------------------- USUARIOS
-- Um de cada perfil. Estes sao os ids usados no header X-User-Id.
INSERT INTO usuario (id, nome, email, perfil, ativo) VALUES
  (1, 'Ana Solicitante', 'ana@empresa.com',   'SOLICITANTE', TRUE),
  (2, 'Bruno Atendente', 'bruno@empresa.com', 'ATENDENTE',   TRUE),
  (3, 'Carla Admin',     'carla@empresa.com', 'ADMIN',       TRUE);

-- --------------------------------------------------------------------------- CATEGORIAS
-- Quatro categorias com SLAs bem diferentes (4h ate 48h), para o dashboard ter contraste.
INSERT INTO categoria (id, nome, sla_horas, ativa) VALUES
  (1, 'TI - Infraestrutura', 4,  TRUE),
  (2, 'TI - Sistemas',       8,  TRUE),
  (3, 'Facilities',          24, TRUE),
  (4, 'RH',                  48, TRUE);

-- ----------------------------------------------------------------------------- CHAMADOS
-- Oito chamados cobrindo os 4 status, as 3 prioridades e as 4 categorias.
-- Tres deles estao fora do SLA: #2 e #5 ainda abertos, #7 resolvido depois do prazo.
INSERT INTO chamado
  (id, titulo, descricao, status, prioridade, categoria_id, solicitante_id, atendente_id,
   criado_em, atualizado_em, resolvido_em, fechado_em)
VALUES
  -- #1 ABERTO / ALTA / 2h de vida, prazo de 4h -> dentro do SLA
  (1, 'Notebook nao liga',
      'O notebook parou de ligar hoje de manha. A luz de carga acende, mas a tela fica preta.',
      'ABERTO', 'ALTA', 1, 1, NULL,
      DATEADD('HOUR', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', -2, CURRENT_TIMESTAMP), NULL, NULL),

  -- #2 ABERTO / MEDIA / 30h de vida, prazo de 8h -> FORA DO SLA
  (2, 'Sem acesso ao ERP',
      'Recebo "usuario ou senha invalidos" ao entrar no ERP, mesmo apos redefinir a senha.',
      'ABERTO', 'MEDIA', 2, 1, NULL,
      DATEADD('HOUR', -30, CURRENT_TIMESTAMP), DATEADD('HOUR', -30, CURRENT_TIMESTAMP), NULL, NULL),

  -- #3 ABERTO / BAIXA / 6h de vida, prazo de 24h -> dentro do SLA
  (3, 'Ar-condicionado com defeito',
      'O ar-condicionado da sala 302 esta pingando agua sobre as mesas.',
      'ABERTO', 'BAIXA', 3, 3, NULL,
      DATEADD('HOUR', -6, CURRENT_TIMESTAMP), DATEADD('HOUR', -6, CURRENT_TIMESTAMP), NULL, NULL),

  -- #4 EM_ATENDIMENTO / ALTA / 5h de vida, prazo de 8h -> dentro do SLA. Tem comentarios.
  (4, 'Erro ao emitir relatorio',
      'O relatorio de fechamento mensal quebra com erro 500 ao selecionar mais de um mes.',
      'EM_ATENDIMENTO', 'ALTA', 2, 1, 2,
      DATEADD('HOUR', -5, CURRENT_TIMESTAMP), DATEADD('HOUR', -3, CURRENT_TIMESTAMP), NULL, NULL),

  -- #5 EM_ATENDIMENTO / BAIXA / 20h de vida, prazo de 4h -> FORA DO SLA
  (5, 'Troca de monitor',
      'O monitor esta com uma faixa vertical piscando no lado direito da tela.',
      'EM_ATENDIMENTO', 'BAIXA', 1, 3, 2,
      DATEADD('HOUR', -20, CURRENT_TIMESTAMP), DATEADD('HOUR', -18, CURRENT_TIMESTAMP), NULL, NULL),

  -- #6 RESOLVIDO / MEDIA / criado ha 72h, resolvido ha 50h (22h de atendimento),
  --    prazo de 48h -> dentro do SLA. Aguardando o solicitante fechar.
  (6, 'Duvida sobre folha de ponto',
      'As horas extras do mes passado nao aparecem no espelho de ponto.',
      'RESOLVIDO', 'MEDIA', 4, 1, 2,
      DATEADD('HOUR', -72, CURRENT_TIMESTAMP), DATEADD('HOUR', -50, CURRENT_TIMESTAMP),
      DATEADD('HOUR', -50, CURRENT_TIMESTAMP), NULL),

  -- #7 RESOLVIDO / BAIXA / criado ha 96h, resolvido ha 60h (36h de atendimento),
  --    prazo de 24h -> FORA DO SLA por ter sido resolvido depois do prazo
  (7, 'Cadeira quebrada',
      'O pistao da cadeira da estacao 14 cedeu e o assento nao para na altura.',
      'RESOLVIDO', 'BAIXA', 3, 3, 2,
      DATEADD('HOUR', -96, CURRENT_TIMESTAMP), DATEADD('HOUR', -60, CURRENT_TIMESTAMP),
      DATEADD('HOUR', -60, CURRENT_TIMESTAMP), NULL),

  -- #8 FECHADO / ALTA / criado ha 48h, resolvido ha 46h (2h), fechado ha 45h,
  --    prazo de 4h -> dentro do SLA. Ciclo completo, ja confirmado pelo solicitante.
  (8, 'Instalar VPN',
      'Preciso da VPN instalada no notebook novo para acessar o ambiente interno de casa.',
      'FECHADO', 'ALTA', 1, 1, 2,
      DATEADD('HOUR', -48, CURRENT_TIMESTAMP), DATEADD('HOUR', -45, CURRENT_TIMESTAMP),
      DATEADD('HOUR', -46, CURRENT_TIMESTAMP), DATEADD('HOUR', -45, CURRENT_TIMESTAMP));

-- -------------------------------------------------------------------------- COMENTARIOS
-- Concentrados nos chamados 4, 6 e 8 para que a demonstracao tenha uma linha do tempo de
-- verdade em cada estagio: em atendimento, resolvido e fechado.
INSERT INTO comentario (id, chamado_id, autor_id, texto, criado_em) VALUES
  (1, 4, 1, 'Acontece toda vez que seleciono janeiro e fevereiro juntos.',
      DATEADD('HOUR', -5, CURRENT_TIMESTAMP)),
  (2, 4, 2, 'Consegue me mandar um print da tela de erro? Ja reproduzi aqui e vou investigar.',
      DATEADD('HOUR', -4, CURRENT_TIMESTAMP)),
  (3, 4, 1, 'Enviei o print por e-mail. O erro aparece so depois de uns 30 segundos.',
      DATEADD('HOUR', -3, CURRENT_TIMESTAMP)),
  (4, 6, 2, 'As horas extras entram no espelho no fechamento seguinte. Vao aparecer no proximo mes.',
      DATEADD('HOUR', -51, CURRENT_TIMESTAMP)),
  (5, 6, 1, 'Entendi, obrigada pelo retorno.',
      DATEADD('HOUR', -50, CURRENT_TIMESTAMP)),
  (6, 8, 2, 'VPN instalada e testada com o usuario de rede. Perfil de acesso interno liberado.',
      DATEADD('HOUR', -46, CURRENT_TIMESTAMP));

-- ---------------------------------------------------------------------------------------
-- As colunas de id sao IDENTITY. Inserir ids explicitos NAO avanca o contador, entao sem
-- este RESTART o primeiro chamado aberto pela interface colidiria com o id 1.
-- ---------------------------------------------------------------------------------------
ALTER TABLE usuario    ALTER COLUMN id RESTART WITH 4;
ALTER TABLE categoria  ALTER COLUMN id RESTART WITH 5;
ALTER TABLE chamado    ALTER COLUMN id RESTART WITH 9;
ALTER TABLE comentario ALTER COLUMN id RESTART WITH 7;
