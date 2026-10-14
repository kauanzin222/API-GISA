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
| `CARGOS.md` | cargos e CBOs |
| `PERFIS.md` | perfis de acesso, lista de permissões e perfis padrão |
| `TERAPIAS.md` | terapias (consultas): agendamento, edição, status, listagens com filtros e presença |
| `USUARIOS.md` | usuário logado, senha e administração dos usuários de acesso (listar, trocar perfil, retirar acesso) |
| `AUDITORIA.md` | trilha de auditoria: logins, alterações e consultas a dados sensíveis |

---

## Informações gerais

- **URL base:** `https://api-gisa.onrender.com`
- **Formato:** JSON (`Content-Type: application/json`)
- **Autenticação:** token JWT em todos os endpoints, exceto o login
- **Permissões:** toda rota exige uma permissão do perfil do usuário, exceto as marcadas como "autenticado" (ver [Permissões por rota](#permissões-por-rota))
- **Atenção (hospedagem):** se a API estiver em plano gratuito do Render, a primeira requisição após um período sem uso pode demorar (a instância "acorda"). Se a primeira chamada demorar ou der timeout, tente de novo antes de investigar.

---

## Autenticação

### `POST /api/auth/login`

Credenciais administrativas iniciais (**somente para testes**; não use em produção e não publique este arquivo em repositório aberto com a senha):

```json
{
  "cpf": "99999999999",
  "senha": "Admin@12345"
}
```

**Resposta `200`:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Uso nas demais requisições:**

```
Authorization: Bearer <token>
```

- O token vale por **24 horas**, ou até a senha do usuário ser trocada ou o acesso ser retirado (ver `USUARIOS.md`).
- O token só identifica o CPF. Depois do login, chame `GET /api/usuarios/me` para obter nome, tipo de cadastro, perfil e permissões de quem entrou (ver `USUARIOS.md`).
- Trocar a própria senha devolve um token novo; o front deve substituir o token salvo.
- CPF ou senha errados, ou cadastro inativo: `401` com `message` = `Credenciais inválidas.`
- Token ausente, inválido ou expirado nas demais rotas: `401` ou `403`.
- Para encerrar a sessão, basta remover o token da aplicação.
- **Cadastros inativos não conseguem autenticar.**

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
| `POST /api/auth/login` | pública |
| `GET /api/usuarios/me`, `PATCH /api/usuarios/minha-senha` | autenticado |
| `GET /api/pacientes`, `GET /api/pacientes/{id}`, `GET /api/responsaveis`, `GET /api/responsaveis/{id}` | `VISUALIZAR_PACIENTES` |
| `GET /api/pacientes/{id}/historico` | `VISUALIZAR_HISTORICO_PACIENTE` |
| `POST /api/pacientes` | `CADASTRAR_PACIENTE` |
| `PATCH /api/pacientes/{id}`, `POST /api/pacientes/{id}/responsaveis`, `PATCH /api/responsaveis/{id}` | `EDITAR_PACIENTE` |
| `DELETE /api/pacientes/{id}`, `DELETE /api/responsaveis/{id}` | `INATIVAR_PACIENTE` |
| `GET /api/cids/{codigo}` | `CADASTRAR_PACIENTE` ou `EDITAR_PACIENTE` |
| `GET /api/profissionais` | `VISUALIZAR_PROFISSIONAIS` ou `VISUALIZAR_TERAPIAS` (a recepção lista especialistas para agendar) |
| `GET /api/profissionais/{id}`, `GET /api/especialistas/{id}`, `GET /api/especialistas/pj/{id}` | `VISUALIZAR_PROFISSIONAIS` |
| `POST /api/profissionais` | `CADASTRAR_PROFISSIONAL` |
| `POST /api/especialistas` | `CADASTRAR_ESPECIALISTA` |
| `POST /api/especialistas/pj` | `CADASTRAR_ESPECIALISTA_PJ` |
| `POST /api/profissionais/com-usuario`, `/api/especialistas/com-usuario`, `/api/especialistas/pj/com-usuario` | a permissão de cadastro do tipo **e** `GERENCIAR_USUARIOS` (ver nota abaixo) |
| `PATCH` de profissional, especialista e PJ | `EDITAR_PROFISSIONAL`; se o corpo trouxer `idPerfil`, exige **também** `GERENCIAR_USUARIOS` |
| `DELETE` de profissional, especialista e PJ | `INATIVAR_PROFISSIONAL` |
| `POST .../{id}/usuario` (conceder acesso) | `GERENCIAR_USUARIOS` |
| `GET /api/usuarios`, `GET /api/usuarios/{id}`, `PATCH /api/usuarios/{id}/perfil`, `PATCH /api/usuarios/{id}/senha`, `DELETE /api/usuarios/{id}` | `GERENCIAR_USUARIOS` |
| `GET /api/terapias` | `VISUALIZAR_TERAPIAS` |
| `GET /api/terapias/{id}` | `VISUALIZAR_TERAPIAS`, **ou** ser especialista participante da terapia (ver `TERAPIAS.md`) |
| `GET /api/terapias/minhas` | autenticado (e o usuário precisa ser especialista) |
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
| CPF | exatamente 11 dígitos, **sem máscara** |
| CNPJ | exatamente 14 dígitos, **sem máscara** |
| CPF único | não pode coincidir com o de nenhuma pessoa já cadastrada. Ao testar, não repita CPFs. O CPF `99999999999` é do administrador inicial |
| Campos opcionais | podem ser omitidos ou enviados como `null` |
| Listagens paginadas | parâmetros `page`, `size` (padrão `9`) e `sort`; os dados de paginação vêm no objeto `page` da resposta (ver `PACIENTES.md`) |
| Busca de pessoas | as listagens de pacientes, responsáveis e profissionais aceitam `cpf` (trecho, máscara ignorada) e `nome` (trecho, sem diferenciar maiúsculas de minúsculas); os dois juntos precisam ser atendidos |

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
| `tipoCadastro` / `tipo` | `PROFISSIONAL`, `ESPECIALISTA`, `ESPECIALISTA_PJ` (listagem de profissionais); em `GET /api/usuarios/me` e na administração de usuários (`USUARIOS.md`) também `PESSOA` (administrador inicial) |
| tipo de evento do histórico | ver `PACIENTES.md` |
| `diaSemana` | `1` = Segunda, `2` = Terça, `3` = Quarta, `4` = Quinta, `5` = Sexta, `6` = Sábado, `7` = Domingo |

---

## Status HTTP

| Status | Quando |
|---|---|
| `200 OK` | consulta, login ou atualização realizados |
| `201 Created` | cadastro realizado (inclusive concessão de acesso) |
| `204 No Content` | inativação ou exclusão (DELETE) realizada |
| `400 Bad Request` | validação de campos, corpo malformado ou regra de negócio violada |
| `401` / `403` | credenciais inválidas; token ausente, inválido, expirado, emitido antes da última troca de senha ou de um usuário cujo acesso foi retirado |
| `403 Forbidden` (com corpo) | usuário logado sem a permissão exigida pela rota: `Você não tem permissão para realizar esta ação.` |
| `404 Not Found` | o recurso da **URL** não existe (ex.: `GET /api/pacientes/999`). Um ID inexistente enviado no **corpo** (ex.: `idCargo` em um PATCH) torna o pedido inválido e responde `400` |
| `405 Method Not Allowed` | método HTTP que a rota não aceita (ex.: `DELETE /api/cbos`): `O método DELETE não é suportado nesta rota.` |
| `409 Conflict` | operação incompatível com o estado do cadastro (ex.: alterar perfil de quem não tem usuário) ou, raramente, violação de restrição do banco (`Os dados informados violam uma regra de integridade do banco.`) |
| `500 Internal Server Error` | erro inesperado no servidor: `Erro interno do servidor. Tente novamente mais tarde.` (o detalhe fica só no log do servidor) |

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
- No caso do paciente há uma regra adicional sobre `statusPaciente` (ver `PACIENTES.md`).

**Exceção: tabelas de apoio.** Perfis, cargos, CBOs, escolas e especialidades não têm status: o `DELETE` deles **exclui** o registro (`204`), e só é permitido quando nada está vinculado a ele (usuários, profissionais, cargos, pacientes e especialistas, respectivamente; inativos também contam). Ver `PERFIS.md`, `CARGOS.md`, `ESCOLAS.md` e `ESPECIALISTAS.md`.

**Exceção: usuário de acesso.** `DELETE /api/usuarios/{idCadastro}` apaga só o usuário (login, senha e perfil); o cadastro da pessoa continua como está. Ver `USUARIOS.md`.

---

## Acesso ao sistema (criar usuário para um cadastro existente)

Profissionais, especialistas e especialistas PJ já cadastrados **sem usuário** podem receber acesso ao sistema. Na tela, isso corresponde a um botão **"Conceder acesso ao sistema"** que exibe os campos de **perfil** e **senha**. Exige `GERENCIAR_USUARIOS`.

O mesmo vale para quem teve o acesso retirado (`DELETE /api/usuarios/{idCadastro}`): o acesso pode ser concedido de novo por estas rotas.

| Tipo de cadastro | Rota |
|---|---|
| Profissional | `POST /api/profissionais/{idCadastro}/usuario` |
| Especialista | `POST /api/especialistas/{idCadastro}/usuario` |
| Especialista PJ | `POST /api/especialistas/pj/{idCadastro}/usuario` |

**Requisição** (mesmo bloco `usuario` usado nos cadastros "com acesso"; `senha` entre 8 e 30 caracteres):

```json
{
  "senha": "SenhaTeste123",
  "idPerfil": 1
}
```

**Resposta `201 Created`** (a senha nunca é retornada):

```json
{
  "idCadastro": 41,
  "perfil": { "idPerfil": 1, "nome": "Administrador" },
  "permissoes": [
    { "idPermissao": 1, "nome": "ACESSO_TOTAL", "descricao": "Acesso total aos recursos da API" }
  ]
}
```

**Regras**

| Situação | Status | Mensagem |
|---|---|---|
| ID de PJ na rota de especialista | `400` | Este cadastro é de um especialista PJ. Use /api/especialistas/pj/{idCadastro}. |
| Cadastro já tem usuário | `400` | Esta pessoa já possui um usuário cadastrado. |
| Cadastro inativo | `400` | Não é possível conceder acesso a um cadastro inativo. |
| Perfil inexistente | `400` | Perfil não encontrado com ID: {id} |
| Cadastro inexistente | `404` | Profissional / Especialista / Especialista PJ não encontrado com ID: {id} |

Depois de criado o acesso, o login funciona com o CPF do cadastro e a senha enviada, e o PATCH com `idPerfil` passa a funcionar para esse cadastro (antes, era recusado com `409` por não haver usuário).

### Perfis e permissões

O `idPerfil` usado acima vem de `GET /api/perfis`. A gestão de perfis e das permissões de cada perfil está em `PERFIS.md`.

---

## Roteiro rápido de testes: permissões por perfil

Pré-requisito: com o token do administrador, crie três profissionais **sem** acesso (`POST /api/profissionais`) e conceda acesso a cada um (`POST /api/profissionais/{id}/usuario`) com os perfis "Recepção", "Especialista" e "Assistente Social" (IDs em `GET /api/perfis`). Faça login com cada um. Em todos os `403` abaixo, a `message` é `Você não tem permissão para realizar esta ação.`

**Recepção**

1. `GET /api/pacientes` e `PATCH /api/pacientes/{id}` com `{"nome": "Outro Nome"}`. Esperado: `200`.
2. `POST /api/pacientes` com um corpo **válido** (exemplo em `PACIENTES.md`). Esperado: `403`. `DELETE /api/pacientes/{id}`. Esperado: `403`.
3. `POST /api/terapias` (exemplo em `TERAPIAS.md`). Esperado: `201`. `GET /api/profissionais`. Esperado: `200`.
4. `GET /api/profissionais/{id}`, `GET /api/perfis`, `GET /api/usuarios`, `GET /api/escolas` e `GET /api/auditoria`. Esperado: `403` em todos.

**Especialista** (o profissional precisa ser um especialista que participa de uma terapia; conceda o acesso por `POST /api/especialistas/{id}/usuario`)

5. `GET /api/pacientes` e `GET /api/pacientes/{id}/historico`. Esperado: `200`.
6. `GET /api/terapias`. Esperado: `403`. `GET /api/terapias/{id}` de uma terapia dele. Esperado: `200`; de outra: `403`.
7. `PATCH /api/terapias/{id}/status` com `{"status": "EM_ATENDIMENTO"}`. Esperado: `403`.

**Assistente Social**

8. `GET /api/pacientes`, `GET /api/responsaveis`. Esperado: `200`.
9. `PATCH /api/pacientes/{id}`, `GET /api/terapias`, `GET /api/cids/F84.0`. Esperado: `403`.

**Ordem das verificações**

10. Com a recepção, `POST /api/pacientes` com `{}`. Esperado: `400` com os campos obrigatórios em `fields` (o corpo é validado antes da permissão).
