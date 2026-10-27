# API GISA — Documentação geral

Este documento reúne o que vale para **todos** os módulos da API: autenticação, permissões por rota, convenções, enums, formato de erro e regras comuns de PATCH e DELETE. Os módulos têm documentos próprios.

## Documentos

| Documento | Conteúdo |
|---|---|
| `00-GERAL.md` | este documento |
| `ESCOLAS.md` | escolas: listagem, autocomplete, cadastro, edição e exclusão |
| `CIDS.md` | busca de CID por código e fluxo de cadastro com a API pública |
| `PACIENTES.md` | pacientes, responsáveis e histórico do paciente |
| `PROFISSIONAIS.md` | profissionais |
| `ESPECIALISTAS.md` | especialistas, especialistas PJ e especialidades |
| `ASSISTENTES-SOCIAIS.md` | assistentes sociais e os pacientes vinculados a eles |
| `CARGOS.md` | cargos e CBOs |
| `PERFIS.md` | perfis de acesso, lista de permissões e perfis padrão |
| `TERAPIAS.md` | terapias (consultas): agendamento, edição, status, listagens com filtros e presença |
| `USUARIOS.md` | usuário logado, senha e administração dos usuários de acesso (listar, trocar perfil, retirar acesso) |
| `AUDITORIA.md` | trilha de auditoria: alterações, logins que falharam e consultas aos detalhes de paciente |

---

## Informações gerais

- **URL base:** `https://api-gisa.onrender.com`
- **Formato:** JSON (`Content-Type: application/json`)
- **Autenticação:** token JWT em todos os endpoints, exceto login, "esqueci minha senha" e redefinição de senha
- **Permissões:** toda rota exige uma permissão do perfil do usuário, exceto as marcadas como "autenticado" (ver [Permissões por rota](#permissões-por-rota))
- **Atenção (hospedagem):** se a API estiver em plano gratuito do Render, a primeira requisição após um período sem uso pode demorar (a instância "acorda"). Se a primeira chamada demorar ou der timeout, tente de novo antes de investigar.

---

## Autenticação

O login é feito com **e-mail e senha**. Quem recebe acesso ganha uma **senha temporária**, gerada pelo back e enviada por e-mail; no primeiro acesso, a pessoa cadastra a senha definitiva. Há também "esqueci minha senha" por e-mail.

| Rota | Permissão | Descrição |
|---|---|---|
| `POST /api/auth/login` | pública | login com e-mail e senha |
| `POST /api/auth/esqueci-senha` | pública | envia um link de redefinição para o e-mail (sempre `204`) |
| `POST /api/auth/redefinir-senha` | pública | cadastra a senha nova (primeiro acesso ou "esqueci minha senha") e já devolve o token de login |

### `POST /api/auth/login`

Credenciais administrativas iniciais (**somente para testes**; não use em produção e não publique este arquivo em repositório aberto com a senha). O e-mail foi dado ao administrador pela migration `V16`:

```json
{
  "email": "admin@gisa.local",
  "senha": "Admin@12345"
}
```

O e-mail é comparado **sem diferenciar maiúsculas** (espaços nas pontas são ignorados).

**Resposta `200` (login normal):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "senhaTemporaria": false
}
```

**Resposta `200` (primeiro acesso, com a senha temporária):** **não** vem o token normal. Vem um token de redefinição, que vale **60 minutos**; leve o usuário para a tela "Cadastrar nova senha" e envie-o em `POST /api/auth/redefinir-senha`.

```json
{
  "senhaTemporaria": true,
  "tokenRedefinicao": "q8N1sX..."
}
```

**Uso nas demais requisições:**

```
Authorization: Bearer <token>
```

- O token vale por **24 horas**, ou até a senha do usuário ser trocada ou o acesso ser retirado (ver `USUARIOS.md`).
- O token identifica o usuário internamente. Depois do login, chame `GET /api/usuarios/me` para obter nome, e-mail, tipo de cadastro, perfil e permissões de quem entrou (ver `USUARIOS.md`).
- Trocar a própria senha devolve um token novo; o front deve substituir o token salvo.
- Token ausente, inválido ou expirado nas demais rotas: `401` com `message` = `Sessão expirada ou inválida. Faça login novamente.` (o front deve levar o usuário ao login).
- Para encerrar a sessão, basta remover o token da aplicação.
- **Cadastros inativos não conseguem autenticar.**

| Situação | Status | `message` |
|---|---|---|
| E-mail não cadastrado, senha errada ou cadastro inativo | `401` | Credenciais inválidas. |
| Senha temporária correta, mas vencida (vale **7 dias**) | `401` | Senha temporária expirada. Use 'Esqueci minha senha' ou peça um novo acesso ao administrador. |
| **5 falhas** seguidas para o mesmo e-mail em **15 minutos** (o e-mail fica bloqueado até a janela vencer, mesmo com a senha certa; um login certo antes disso zera a contagem) | `429` | Muitas tentativas de login sem sucesso. Aguarde alguns minutos e tente novamente. |

### `POST /api/auth/esqueci-senha`

```json
{ "email": "maria.silva@email.com" }
```

**Resposta: sempre `204 No Content`**, exista o e-mail ou não (para não revelar quais e-mails estão cadastrados). São aceitos até **3 pedidos por e-mail a cada 15 minutos**; acima disso a resposta continua `204`, mas nenhum link novo é enviado. Se o e-mail for de um usuário **ativo**, a API envia para ele o link:

```
{FRONT_URL}/redefinir-senha?token=<token>
```

- O link vale **60 minutos** e só pode ser usado uma vez.
- Pedir de novo invalida os links anteriores do mesmo usuário.
- A tela `/redefinir-senha` do front lê o `token` da URL e chama `POST /api/auth/redefinir-senha`.

### `POST /api/auth/redefinir-senha`

Mesma rota para o primeiro acesso (token da resposta do login) e para "esqueci minha senha" (token do link).

| Campo | Obrigatório | Observação |
|---|---|---|
| `token` | sim | o `tokenRedefinicao` do login ou o `token` do link |
| `novaSenha` | sim | 8 a 30 caracteres (a mesma política de senha de sempre); no primeiro acesso, diferente da senha temporária |
| `confirmacaoNovaSenha` | sim | igual a `novaSenha` |

```json
{
  "token": "q8N1sX...",
  "novaSenha": "MinhaSenha2026",
  "confirmacaoNovaSenha": "MinhaSenha2026"
}
```

**Resposta `200`:** o **token de login normal**; o front já entra direto, sem novo login.

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

- A senha deixa de ser temporária, o token de redefinição é marcado como usado e as sessões abertas com a senha anterior são encerradas.

| Situação | Status | `message` |
|---|---|---|
| Token inexistente | `400` | Link de redefinição inválido. Solicite um novo em 'Esqueci minha senha'. |
| Token já usado ou substituído por um mais novo | `400` | Este link de redefinição já foi usado ou foi substituído por um mais novo. Solicite um novo em 'Esqueci minha senha'. |
| Token vencido | `400` | Link de redefinição expirado. Solicite um novo em 'Esqueci minha senha'. |
| Confirmação diferente | `400` | A confirmação não corresponde à nova senha. |
| Nova senha igual à temporária | `400` | A nova senha deve ser diferente da senha temporária. |
| Campos ausentes ou senha fora do tamanho | `400` | em `fields`: `token`, `novaSenha`, `confirmacaoNovaSenha` |

> Os erros de token são `400` (e não `401`), para o front não confundir com sessão expirada.

### E-mails

A API envia dois e-mails, em texto simples: **boas-vindas** (e-mail de login, senha temporária e validade) e **link de redefinição** (com validade). O envio usa a API do Brevo (`BREVO_API_KEY`, recomendada) ou SMTP (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`); o remetente vem de `EMAIL_REMETENTE` e os links usam `FRONT_URL`. **Sem `BREVO_API_KEY` nem `SMTP_HOST`** (desenvolvimento local), nada é enviado: o conteúdo do e-mail, com a senha ou o link, vai para o **log do servidor**.

---

## Permissões por rota

Cada perfil de acesso tem um conjunto de permissões (lista completa e perfis padrão em `PERFIS.md`). A API confere a permissão em **toda** requisição, consultando o perfil atual do usuário: trocar o perfil ou as permissões de um perfil vale na próxima requisição, sem novo login.

- **`ACESSO_TOTAL` pode tudo.** Ela não aparece na tabela, mas vale em todas as linhas.
- Quando a célula tem mais de uma permissão, basta o usuário ter **uma** delas.
- **Autenticado** = qualquer usuário logado, sem permissão específica.
- Sem a permissão: `403` com `message` = `Você não tem permissão para realizar esta ação.`
- Para decidir o que mostrar na tela, use as `permissoes` de `GET /api/usuarios/me` (e trate `ACESSO_TOTAL` como "todas").

| Rota | Permissão |
|---|---|
| `POST /api/auth/login`, `POST /api/auth/esqueci-senha`, `POST /api/auth/redefinir-senha` | pública |
| `GET /api/usuarios/me`, `PATCH /api/usuarios/minha-senha` | autenticado |
| `GET /api/pacientes`, `GET /api/pacientes/{id}` | `VISUALIZAR_PACIENTES` (todos) ou `VISUALIZAR_PACIENTES_VINCULADOS` (só os vinculados ao usuário; ver `PACIENTES.md`) |
| `GET /api/responsaveis`, `GET /api/responsaveis/{id}` | `VISUALIZAR_PACIENTES` |
| `GET /api/pacientes/{id}/historico` | `VISUALIZAR_HISTORICO_PACIENTE` (com só `VISUALIZAR_PACIENTES_VINCULADOS`, apenas de paciente vinculado) |
| `POST /api/pacientes` | `CADASTRAR_PACIENTE` |
| `PATCH /api/pacientes/{id}`, `POST /api/pacientes/{id}/responsaveis`, `PATCH /api/responsaveis/{id}` | `EDITAR_PACIENTE` |
| `DELETE /api/pacientes/{id}`, `DELETE /api/responsaveis/{id}` | `INATIVAR_PACIENTE` |
| `GET /api/cids/{codigo}` | `CADASTRAR_PACIENTE` ou `EDITAR_PACIENTE` |
| `GET /api/profissionais` | `VISUALIZAR_PROFISSIONAIS` ou `VISUALIZAR_TERAPIAS` (a recepção lista especialistas para agendar) |
| `GET /api/profissionais/{id}`, `GET /api/especialistas/{id}`, `GET /api/especialistas/pj/{id}`, `GET /api/assistentes-sociais/{id}` | `VISUALIZAR_PROFISSIONAIS` |
| `GET /api/especialistas/{id}/jornadas` | `VISUALIZAR_PROFISSIONAIS`, `VISUALIZAR_TERAPIAS`, `AGENDAR_TERAPIA` ou `EDITAR_TERAPIA` (resumo das jornadas no agendamento) |
| `POST /api/profissionais`, `POST /api/assistentes-sociais` | `CADASTRAR_PROFISSIONAL` |
| `POST /api/especialistas` | `CADASTRAR_ESPECIALISTA` |
| `POST /api/especialistas/pj` | `CADASTRAR_ESPECIALISTA_PJ` |
| `POST /api/profissionais/com-usuario`, `/api/especialistas/com-usuario`, `/api/especialistas/pj/com-usuario`, `/api/assistentes-sociais/com-usuario` | a permissão de cadastro do tipo **e** `GERENCIAR_USUARIOS` (ver nota abaixo) |
| `PATCH` de profissional, especialista, PJ e assistente social | `EDITAR_PROFISSIONAL`; se o corpo trouxer `idPerfil`, exige **também** `GERENCIAR_USUARIOS` |
| `POST /api/assistentes-sociais/{id}/pacientes`, `DELETE /api/assistentes-sociais/{id}/pacientes/{idPaciente}` (vínculos) | `EDITAR_PROFISSIONAL` |
| `DELETE` de profissional, especialista, PJ e assistente social | `INATIVAR_PROFISSIONAL` |
| `POST .../{id}/usuario` (conceder acesso) | `GERENCIAR_USUARIOS` |
| `GET /api/usuarios`, `GET /api/usuarios/{id}`, `PATCH /api/usuarios/{id}/perfil`, `POST /api/usuarios/{id}/senha-temporaria`, `DELETE /api/usuarios/{id}` | `GERENCIAR_USUARIOS` |
| `GET /api/terapias` | `VISUALIZAR_TERAPIAS` |
| `GET /api/terapias/{id}` | `VISUALIZAR_TERAPIAS`, **ou** ser especialista participante da terapia (ver `TERAPIAS.md`) |
| `GET /api/terapias/minhas`, `GET /api/especialistas/me` | autenticado (e o usuário precisa ser especialista) |
| `GET /api/assistentes-sociais/me/pacientes` | autenticado (e o usuário precisa ser assistente social) |
| `POST /api/terapias`, `POST /api/terapias/verificar-conflitos` | `AGENDAR_TERAPIA` ou `EDITAR_TERAPIA` |
| `PATCH /api/terapias/{id}` | `EDITAR_TERAPIA` |
| `PATCH /api/terapias/{id}/status` | `ALTERAR_STATUS_TERAPIA` |
| `PATCH /api/terapias/{id}/pacientes/{idPaciente}/presenca` | `REGISTRAR_PRESENCA` |
| `GET /api/especialidades` | autenticado |
| `GET /api/especialidades/{id}`, `POST`, `PATCH` e `DELETE` de especialidades | `GERENCIAR_ESPECIALIDADES` |
| `GET /api/cargos` | `GERENCIAR_CARGOS`, `CADASTRAR_PROFISSIONAL`, `CADASTRAR_ESPECIALISTA`, `CADASTRAR_ESPECIALISTA_PJ` ou `EDITAR_PROFISSIONAL` |
| demais rotas de cargos e todas as de CBOs | `GERENCIAR_CARGOS` |
| `GET /api/perfis` | `GERENCIAR_PERFIS` ou `GERENCIAR_USUARIOS` |
| demais rotas de perfis e todas as de permissões | `GERENCIAR_PERFIS` |
| `GET /api/escolas/sugestoes`, `POST /api/escolas` | `CADASTRAR_PACIENTE`, `EDITAR_PACIENTE` ou `GERENCIAR_ESCOLAS` |
| `GET /api/escolas`, `GET /api/escolas/{id}`, `PATCH` e `DELETE` de escolas | `GERENCIAR_ESCOLAS` |
| `GET /api/auditoria` | `VISUALIZAR_AUDITORIA` |

> **Cadastro "com acesso" (`/com-usuario`).** Esses cadastros criam o usuário já com um perfil, o que equivale a conceder acesso. Por isso exigem, além da permissão de cadastro, `GERENCIAR_USUARIOS` (a mesma regra do `idPerfil` no PATCH). Sem isso, quem só cadastra profissionais poderia criar um usuário com perfil de administrador e entrar com ele. Quem não tem `GERENCIAR_USUARIOS` cadastra sem acesso (`POST /api/profissionais`) e um gestor concede o acesso depois.

> **Ordem das verificações.** O corpo da requisição é validado **antes** da permissão. Um usuário sem permissão que envie um corpo inválido recebe `400` (erros de validação), e não `403`; com o corpo válido, recebe `403`. O mesmo vale para IDs com formato errado na URL (`/api/pacientes/abc` responde `400`). Recursos inexistentes não mudam a resposta: sem permissão, `DELETE /api/pacientes/999` responde `403`, e não `404`.

> **Toda rota nova precisa de permissão.** Um teste automatizado (`PermissoesPorRotaTest`) falha se alguma rota da API ficar sem `@PreAuthorize`, exceto as marcadas como "autenticado" acima.

---

## Convenções

| Item | Regra |
|---|---|
| Datas | formato ISO `yyyy-MM-dd` (ex.: `"2015-03-21"`) |
| Horários | formato `HH:mm` (ex.: `"08:00"`) |
| CPF | exatamente 11 dígitos, **sem máscara**, e **válido**: os dois dígitos verificadores são conferidos e sequências repetidas (`00000000000` … `99999999999`) são recusadas. Vale em toda entrada de CPF (paciente, responsável, profissional, especialista, PJ e assistente social): `400` com `CPF inválido` no campo. Cadastros que já estavam no banco não são revalidados |
| E-mail | é o **login** do usuário de acesso: único entre todos os cadastros, sem diferenciar maiúsculas. Obrigatório para profissionais (e para quem recebe acesso) |
| CNPJ | exatamente 14 dígitos, **sem máscara** |
| CPF único | não pode coincidir com o de nenhuma pessoa já cadastrada. Ao testar, não repita CPFs e use CPFs válidos (há geradores de CPF de teste). O CPF `99999999999` é do administrador inicial |
| Campos opcionais | podem ser omitidos ou enviados como `null` |
| Listagens paginadas | parâmetros `page`, `size` (padrão `9`) e `sort`; os dados de paginação vêm no objeto `page` da resposta (ver `PROFISSIONAIS.md`) |
| Busca de pessoas | as listagens de responsáveis e profissionais aceitam `cpf` (trecho, máscara ignorada) e `nome` (trecho, sem diferenciar maiúsculas de minúsculas); os dois juntos precisam ser atendidos. **Pacientes** não têm listagem: só busca por `termo`, sem paginação (ver `PACIENTES.md`) |

### Enums

| Campo | Valores aceitos |
|---|---|
| `estadoCivil` | `SOLTEIRO`, `CASADO`, `VIUVO`, `SEPARADO`, `DIVORCIADO` |
| `sexo` | um único caractere: `F`, `M` ou `O` (outros valores: `400`, "O sexo deve ser F, M ou O") |
| `grauParentesco` | `PAI`, `MAE`, `AVO`, `TUTOR_LEGAL`, `OUTRO` |
| `tipoEscola` | `PUBLICA`, `PARTICULAR` |
| `statusPaciente` | `MATRICULADO`, `ALTA`, `DESLIGADO` (definido pelo back no cadastro; pode ser alterado no PATCH do paciente) |
| `statusCadastro` | `ATIVO`, `INATIVO` (pode ser alterado nos PATCH) |
| `modalidade` (resposta) | `INDIVIDUAL`, `COLETIVA` (calculada pelo número de pacientes) |
| `statusTerapia` | `AGENDADA`, `EM_ATENDIMENTO`, `CONCLUIDA`, `CANCELADA` |
| `presenca` | `PENDENTE`, `PRESENTE`, `FALTA_JUSTIFICADA`, `FALTA_INJUSTIFICADA` |
| `tipoCadastro` / `tipo` | `PROFISSIONAL`, `ESPECIALISTA`, `ESPECIALISTA_PJ`, `ASSISTENTE_SOCIAL` (listagem de profissionais); em `GET /api/usuarios/me` e na administração de usuários (`USUARIOS.md`) também `PESSOA` (administrador inicial) |
| tipo de evento do histórico | ver `PACIENTES.md` |
| `diaSemana` | `1` = Segunda, `2` = Terça, `3` = Quarta, `4` = Quinta, `5` = Sexta, `6` = Sábado, `7` = Domingo |

---

## Status HTTP

| Status | Quando |
|---|---|
| `200 OK` | consulta, login ou atualização realizados |
| `201 Created` | cadastro realizado (inclusive concessão de acesso) |
| `204 No Content` | inativação ou exclusão (DELETE) realizada (exceção: a inativação de paciente responde `200` com um resumo; ver `PACIENTES.md`) |
| `400 Bad Request` | validação de campos, corpo malformado ou regra de negócio violada |
| `401 Unauthorized` | credenciais inválidas; token ausente, inválido, expirado, emitido antes da última troca de senha ou de um usuário cujo acesso foi retirado |
| `403 Forbidden` (com corpo) | usuário logado sem a permissão exigida pela rota: `Você não tem permissão para realizar esta ação.` |
| `404 Not Found` | o recurso da **URL** não existe (ex.: `GET /api/pacientes/999`). Um ID inexistente enviado no **corpo** (ex.: `idCargo` em um PATCH) torna o pedido inválido e responde `400` |
| `405 Method Not Allowed` | método HTTP que a rota não aceita (ex.: `DELETE /api/cbos`): `O método DELETE não é suportado nesta rota.` |
| `409 Conflict` | operação incompatível com o estado do cadastro (ex.: alterar perfil de quem não tem usuário; vínculo de paciente que já existe), conflito de agenda nas terapias (horário ocupado ou fora da jornada; ver `TERAPIAS.md`), jornada que deixaria terapias futuras fora do horário (ver `ESPECIALISTAS.md`), operação que deixaria o sistema sem quem gerencie perfis (ver abaixo) ou, raramente, violação de restrição do banco (`Os dados informados violam uma regra de integridade do banco.`) |
| `429 Too Many Requests` | login bloqueado por muitas falhas seguidas para o mesmo e-mail (ver "Autenticação") |
| `500 Internal Server Error` | erro inesperado no servidor: `Erro interno do servidor. Tente novamente mais tarde.` (o detalhe fica só no log do servidor) |
| `503 Service Unavailable` | o e-mail da nova senha temporária não pôde ser enviado e nada foi alterado (`POST /api/usuarios/{id}/senha-temporaria`; ver `USUARIOS.md`) |

> **Proteção contra trancar o sistema.** Sempre precisa existir ao menos um usuário **ativo** cujo perfil tenha `GERENCIAR_PERFIS` ou `ACESSO_TOTAL`. Editar as permissões de um perfil, excluir um perfil, trocar o perfil de um usuário, retirar o acesso de um usuário ou inativar um profissional (de qualquer tipo, pelo `DELETE` ou pelo PATCH com `statusCadastro: INATIVO`) que quebre essa regra responde `409` com `Operação bloqueada: o sistema precisa de ao menos um usuário ativo com permissão para gerenciar perfis (GERENCIAR_PERFIS ou ACESSO_TOTAL).`

> CPF, e-mail e CNPJ repetidos são recusados com `400` e mensagem simples: `CPF já cadastrado.`, `E-mail já cadastrado.` ou `CNPJ já cadastrado.`

> Todo erro, inclusive `404`, `405` e `500`, vem com o corpo padrão abaixo. Rotas inexistentes respondem `404` com `Rota não encontrada.`

---

## Formato de erro

Todos os erros seguem o mesmo corpo.

**Validação de campos** (`fields` traz um item por campo inválido, no formato campo → mensagem; exiba cada mensagem junto ao campo):

```json
{
  "timestamp": "2026-09-22T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Erro de validação dos dados.",
  "path": "/api/profissionais",
  "fields": {
    "cpf": "O CPF deve conter 11 dígitos numéricos"
  }
}
```

**Regra de negócio** (a mensagem da regra vem em `message` e `fields` vem vazio):

```json
{
  "timestamp": "2026-10-13T03:08:33.455131157Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Este cadastro é de um especialista PJ. Use /api/especialistas/pj/{idCadastro}.",
  "path": "/api/especialistas/3",
  "fields": {}
}
```

**Corpo malformado:** JSON inválido, valor de enum inexistente (ex.: `"grauParentesco": "TIO"`), data ou hora em formato errado e `sexo` com mais de um caractere não chegam à validação de campos. A resposta é `400` com `message` = `Corpo da requisição inválido ou malformado.` e `fields` vazio. Use seletores e máscaras no front para evitar esses casos.

**Erros na URL** (também `400`, com `fields` vazio):

| Situação | Exemplo | `message` |
|---|---|---|
| Parâmetro obrigatório ausente | — | O parâmetro '{nome}' é obrigatório. |
| Valor inválido em parâmetro ou ID | `?status=XYZ`, `/api/profissionais/abc` | Valor inválido para 'status': XYZ |

Regra prática para o front: se `fields` tiver itens, mostre cada mensagem no campo correspondente; se estiver vazio, mostre `message` como aviso geral.

---

## Regras comuns de PATCH

Valem para os PATCH de pacientes, responsáveis, profissionais, especialistas e especialistas PJ.

- **Envie somente o que quer alterar.** Campos omitidos permanecem iguais.
- **Campos opcionais** enviados como `null` são limpos.
- **Campos obrigatórios** (`nome`, `estadoCivil`, `statusCadastro` e os indicados em cada módulo) enviados como `null` ou vazios são recusados com `400`.
- **Listas substituem por completo** a lista atual (`enderecos`, `codigosCid`, `responsaveis`, `jornadas`, `especialidadesIds`). Para manter um item, ele precisa ser reenviado junto com os novos. Use o GET de detalhes para montar a lista completa antes de enviar. A exceção é `novasEspecialidades`, que acrescenta (ver `ESPECIALISTAS.md`).
- **Reativação:** enviar `"statusCadastro": "ATIVO"` reativa um cadastro inativado pelo DELETE.
- **Campos imutáveis são ignorados.** Se vierem no corpo (por exemplo `cpf`, `dataNascimento`, `sexo`, `dataCadastro`, `registroConselho`, `cnpj`), a API responde `200` e simplesmente não altera o valor. O front deve tratar esses campos como somente leitura (desabilitados no formulário) e não esperar erro.
- **Atenção a nomes de campo digitados errados:** um campo que a API não conhece também é ignorado sem erro. Se o PATCH responder `200` e nada mudar, confira os nomes.

---

## Regras comuns de DELETE

`DELETE` **não apaga** o registro: ele **inativa**.

- Retorna `204 No Content`.
- O cadastro passa a `statusCadastro: "INATIVO"` e continua podendo ser consultado pelo GET de detalhes.
- Cadastros inativos não autenticam.
- No caso do paciente há regras adicionais sobre `statusPaciente` e as terapias futuras, e a resposta é `200` com um resumo (ver `PACIENTES.md`).
- Inativar um assistente social encerra os vínculos ativos dele com pacientes (ver `ASSISTENTES-SOCIAIS.md`).
- Inativar um profissional segue a proteção contra trancar o sistema (`409`; ver "Status HTTP").

**Exceção: tabelas de apoio.** Perfis, cargos, CBOs, escolas e especialidades não têm status: o `DELETE` deles **exclui** o registro (`204`), e só é permitido quando nada está vinculado a ele (usuários, profissionais, cargos, pacientes e especialistas, respectivamente; inativos também contam). Ver `PERFIS.md`, `CARGOS.md`, `ESCOLAS.md` e `ESPECIALISTAS.md`.

**Exceção: usuário de acesso.** `DELETE /api/usuarios/{idCadastro}` apaga só o usuário (login, senha e perfil); o cadastro da pessoa continua como está. Ver `USUARIOS.md`.

---

## Acesso ao sistema (criar usuário para um cadastro existente)

Profissionais, especialistas, especialistas PJ e assistentes sociais já cadastrados **sem usuário** podem receber acesso ao sistema. Na tela, isso corresponde a um botão **"Conceder acesso ao sistema"** que exibe só o campo de **perfil**: a senha **não** é enviada. Exige `GERENCIAR_USUARIOS`.

O mesmo vale para quem teve o acesso retirado (`DELETE /api/usuarios/{idCadastro}`): o acesso pode ser concedido de novo por estas rotas.

| Tipo de cadastro | Rota |
|---|---|
| Profissional | `POST /api/profissionais/{idCadastro}/usuario` |
| Especialista | `POST /api/especialistas/{idCadastro}/usuario` |
| Especialista PJ | `POST /api/especialistas/pj/{idCadastro}/usuario` |
| Assistente social | `POST /api/assistentes-sociais/{idCadastro}/usuario` |

**Requisição** (mesmo bloco `usuario` usado nos cadastros "com acesso"):

```json
{
  "idPerfil": 1
}
```

O back gera uma **senha temporária** forte (12 caracteres), grava só o hash e a envia ao **e-mail do cadastro**, que passa a ser o login. A senha temporária vale **7 dias**; no primeiro login, a pessoa cadastra a senha definitiva (ver "Autenticação"). Um campo `senha` enviado no corpo é ignorado.

**Resposta `201 Created`** (a senha nunca é retornada):

```json
{
  "idCadastro": 41,
  "perfil": { "idPerfil": 1, "nome": "Administrador" },
  "permissoes": [
    { "idPermissao": 1, "nome": "ACESSO_TOTAL", "descricao": "Acesso total: libera todas as funcionalidades do sistema", "categoria": "Segurança" }
  ],
  "emailEnviado": true
}
```

- **`emailEnviado: false`**: o acesso **foi criado**, mas o e-mail com a senha temporária não pôde ser enviado (o erro fica no log do servidor). Avise o administrador e ofereça o reenvio, que é `POST /api/usuarios/{idCadastro}/senha-temporaria` (ver `USUARIOS.md`). O mesmo campo aparece na resposta dos cadastros "com acesso" (`/com-usuario`).

**Regras**

| Situação | Status | Mensagem |
|---|---|---|
| ID de PJ na rota de especialista | `400` | Este cadastro é de um especialista PJ. Use /api/especialistas/pj/{idCadastro}. |
| Cadastro já tem usuário | `400` | Esta pessoa já possui um usuário cadastrado. |
| Cadastro inativo | `400` | Não é possível conceder acesso a um cadastro inativo. |
| Cadastro sem e-mail | `400` | O cadastro precisa ter um e-mail para receber acesso ao sistema. |
| Perfil inexistente | `400` | Perfil não encontrado com ID: {id} |
| Cadastro inexistente | `404` | Profissional / Especialista / Especialista PJ / Assistente social não encontrado com ID: {id} |

Depois de criado o acesso, o login funciona com o **e-mail** do cadastro e a senha temporária (primeiro acesso), e o PATCH com `idPerfil` passa a funcionar para esse cadastro (antes, era recusado com `409` por não haver usuário).

### Perfis e permissões

O `idPerfil` usado acima vem de `GET /api/perfis`. A gestão de perfis e das permissões de cada perfil está em `PERFIS.md`.

---

## Roteiro rápido de testes: permissões por perfil

Pré-requisito: com o token do administrador, crie dois profissionais **sem** acesso (`POST /api/profissionais`) e um assistente social (`POST /api/assistentes-sociais`, ver `ASSISTENTES-SOCIAIS.md`) e conceda acesso a cada um (`POST .../{id}/usuario`) com os perfis "Recepção", "Especialista" e "Assistente Social" (IDs em `GET /api/perfis`). Sem SMTP configurado, a senha temporária de cada um aparece no log do servidor. Faça o primeiro acesso de cada um (login com o e-mail e a senha temporária, depois `POST /api/auth/redefinir-senha`). Em todos os `403` abaixo, a `message` é `Você não tem permissão para realizar esta ação.`

**Recepção**

1. `GET /api/pacientes?termo=luc` e `PATCH /api/pacientes/{id}` com `{"nome": "Outro Nome"}`. Esperado: `200`.
2. `POST /api/pacientes` com um corpo **válido** (exemplo em `PACIENTES.md`). Esperado: `403`. `DELETE /api/pacientes/{id}`. Esperado: `403`.
3. `POST /api/terapias` (exemplo em `TERAPIAS.md`). Esperado: `201`. `GET /api/profissionais`. Esperado: `200`.
4. `GET /api/profissionais/{id}`, `GET /api/perfis`, `GET /api/usuarios`, `GET /api/escolas` e `GET /api/auditoria`. Esperado: `403` em todos.

**Especialista** (o profissional precisa ser um especialista que participa de uma terapia; conceda o acesso por `POST /api/especialistas/{id}/usuario`)

5. `GET /api/pacientes?termo=luc` e `GET /api/pacientes/{id}/historico`. Esperado: `200`.
6. `GET /api/terapias`. Esperado: `403`. `GET /api/terapias/{id}` de uma terapia dele. Esperado: `200`; de outra: `403`.
7. `PATCH /api/terapias/{id}/status` com `{"status": "EM_ATENDIMENTO"}`. Esperado: `403`.

**Assistente Social**

8. Vincule um paciente ao assistente social (com o administrador, `POST /api/assistentes-sociais/{id}/pacientes`). Com o assistente social: `GET /api/pacientes?termo=<nome dele>` e `GET /api/assistentes-sociais/me/pacientes`. Esperado: `200`, só com o paciente vinculado. `GET /api/pacientes/{id}` dele: `200`; de outro paciente: `403`.
9. `PATCH /api/pacientes/{id}`, `GET /api/responsaveis`, `GET /api/terapias`, `GET /api/cids/F84.0`. Esperado: `403`.

**Ordem das verificações**

10. Com a recepção, `POST /api/pacientes` com `{}`. Esperado: `400` com os campos obrigatórios em `fields` (o corpo é validado antes da permissão).
