# API GISA — Assistentes sociais

Autenticação, convenções, enums, formato de erro, regras comuns de PATCH/DELETE e concessão de acesso ao sistema: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

Um **assistente social** é um profissional (ver `PROFISSIONAIS.md`) com registro no CRESS e uma lista de **pacientes vinculados**. Ele é **só** assistente social: não acumula especialista nem especialista PJ (o CPF é único no sistema).

- Os vínculos são N:N: um paciente pode ter vários assistentes sociais, e um assistente social, vários pacientes.
- Com o perfil padrão "Assistente Social" (permissão `VISUALIZAR_PACIENTES_VINCULADOS`), ele busca e vê **só os pacientes vinculados a ele** (ver "Escopo de acesso aos pacientes" em `PACIENTES.md`).

## Rotas

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| `GET` | `/api/assistentes-sociais/me/pacientes` | autenticado (assistente social) | pacientes vinculados ao assistente social logado |
| `GET` | `/api/assistentes-sociais/{idCadastro}` | `VISUALIZAR_PROFISSIONAIS` | detalhes, com os pacientes vinculados |
| `POST` | `/api/assistentes-sociais` | `CADASTRAR_PROFISSIONAL` | cadastra sem acesso ao sistema |
| `POST` | `/api/assistentes-sociais/com-usuario` | `CADASTRAR_PROFISSIONAL` **e** `GERENCIAR_USUARIOS` | cadastra e cria o usuário de acesso |
| `POST` | `/api/assistentes-sociais/{idCadastro}/usuario` | `GERENCIAR_USUARIOS` | concede acesso a um assistente social já cadastrado (ver `00-GERAL.md`) |
| `PATCH` | `/api/assistentes-sociais/{idCadastro}` | `EDITAR_PROFISSIONAL` (+ `GERENCIAR_USUARIOS` com `idPerfil`) | atualiza; também reativa |
| `DELETE` | `/api/assistentes-sociais/{idCadastro}` | `INATIVAR_PROFISSIONAL` | inativa e encerra os vínculos |
| `POST` | `/api/assistentes-sociais/{idCadastro}/pacientes` | `EDITAR_PROFISSIONAL` | vincula um paciente |
| `DELETE` | `/api/assistentes-sociais/{idCadastro}/pacientes/{idPaciente}` | `EDITAR_PROFISSIONAL` | encerra o vínculo com um paciente |

`ACESSO_TOTAL` também libera todas as rotas. Sem a permissão: `403`.

> **Listagem:** não há listagem própria. Os assistentes sociais aparecem em `GET /api/profissionais` (ver `PROFISSIONAIS.md`), com `tipo: "ASSISTENTE_SOCIAL"` e `registroConselho` preenchido; use `?tipo=ASSISTENTE_SOCIAL` para listar só eles.

> **Vínculos a partir da tela do paciente:** use as mesmas rotas de vincular/desvincular acima (com o `idCadastro` do assistente social). Os vínculos ativos aparecem em `assistentesSociais` de `GET /api/pacientes/{idCadastro}`.

> As rotas de profissional (`GET`, `PATCH` e `DELETE /api/profissionais/{idCadastro}`, `POST /api/profissionais/{idCadastro}/usuario`) também funcionam com o ID de um assistente social, com as mesmas regras. As rotas de especialista respondem `404`.

---

## Vínculo paciente × assistente social

| Regra | Detalhe |
|---|---|
| Ativo | o vínculo tem `dataInicio` e ainda não tem `dataFim` |
| No máximo um ativo por par | vincular de novo um paciente já vinculado responde `409` |
| Desvincular não apaga | preenche a data de fim; o histórico de vínculos fica no banco |
| Revincular | depois de desvincular, vincular de novo cria um vínculo novo |
| Inativar o assistente social | encerra (data de fim) todos os vínculos ativos dele; reativá-lo **não** os restaura |
| Inativar o paciente | **não** mexe nos vínculos |

As datas de início e fim são as do dia (fuso de São Paulo) e não aparecem nas respostas.

---

## `GET /api/assistentes-sociais/me/pacientes` — meus pacientes

Painel do assistente social logado. Basta estar logado (como `GET /api/especialistas/me`): o que vale é o usuário ser assistente social. O usuário é identificado pelo token; nenhum ID vai na URL.

**Resposta `200`:** lista simples (sem paginação nem limite), ordenada por nome, no mesmo formato da busca de pacientes (`PACIENTES.md`):

```json
[
  { "idCadastro": 1, "nome": "Lucas Almeida Souza", "cpfMascarado": "***.789.012-**", "statusCadastro": "ATIVO" }
]
```

| Situação | Status | Mensagem |
|---|---|---|
| Usuário logado que não é assistente social (inclusive o administrador inicial) | `400` | O usuário logado não é um assistente social. |

---

## `GET /api/assistentes-sociais/{idCadastro}` — detalhes

**Resposta `200`**

```json
{
  "profissional": {
    "pessoa": {
      "idCadastro": 20,
      "nome": "Paula Ribeiro",
      "cpf": "11144477735",
      "dataNascimento": "1988-04-12",
      "sexo": "F",
      "celular": "15991112222",
      "numCNS": null,
      "estadoCivil": "CASADO",
      "statusCadastro": "ATIVO",
      "enderecos": []
    },
    "email": "paula.ribeiro@email.com",
    "cargo": { "idCargo": 1, "nome": "Administrador de Sistema", "codigoCBO": "999999", "tituloCBO": "Cargo de Teste - Desenvolvimento" },
    "usuario": null
  },
  "registroConselho": "CRESS-9/12345",
  "pacientes": [
    { "idCadastro": 1, "nome": "Lucas Almeida Souza", "cpfMascarado": "***.789.012-**", "statusCadastro": "ATIVO" }
  ]
}
```

- O bloco `profissional` tem o mesmo formato do `GET /api/profissionais/{idCadastro}`, inclusive `usuario` (`null` quando não há acesso).
- `pacientes`: só os vínculos **ativos**, ordenados por nome, no formato da busca de pacientes (sem o CPF completo).

**Resposta `404`:** `Assistente social não encontrado com ID: {id}` (também para o ID de um profissional de outro tipo).

---

## `POST /api/assistentes-sociais` — cadastrar

Todos os campos do `POST /api/profissionais` (`nome`, `cpf`, `dataNascimento`, `sexo`, `estadoCivil`, `celular`, `numCNS`, `enderecos`, `email`, `idCargo`; ver `PROFISSIONAIS.md`), mais:

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `registroConselho` | string | sim | número do CRESS, ex.: `CRESS-9/12345` |
| `idsPacientes` | lista de número | não | pacientes já vinculados no cadastro; IDs repetidos são unificados; todos precisam existir |

```json
{
  "nome": "Paula Ribeiro",
  "cpf": "11144477735",
  "dataNascimento": "1988-04-12",
  "sexo": "F",
  "celular": "15991112222",
  "estadoCivil": "CASADO",
  "enderecos": [],
  "email": "paula.ribeiro@email.com",
  "idCargo": 1,
  "registroConselho": "CRESS-9/12345",
  "idsPacientes": [1]
}
```

**Resposta `201 Created`**

```json
{
  "idCadastro": 20,
  "nome": "Paula Ribeiro",
  "cpf": "11144477735",
  "email": "paula.ribeiro@email.com",
  "idCargo": 1,
  "registroConselho": "CRESS-9/12345",
  "acessoCriado": false
}
```

> **CPF ou e-mail repetido:** `400` com `CPF já cadastrado.` ou `E-mail já cadastrado.`, como no profissional. CPF inválido: `400` com `CPF inválido` em `fields.cpf`.

---

## `POST /api/assistentes-sociais/com-usuario` — cadastrar com acesso

Corpo com dois blocos: **`assistenteSocial`** (mesmos campos do `POST /api/assistentes-sociais`) e `usuario` (só `idPerfil`). A senha **não** é enviada: o back gera uma senha temporária e a envia ao `email` do assistente social (ver "Acesso ao sistema" em `00-GERAL.md`).

```json
{
  "assistenteSocial": {
    "nome": "Paula Ribeiro",
    "cpf": "11144477735",
    "dataNascimento": "1988-04-12",
    "sexo": "F",
    "estadoCivil": "CASADO",
    "enderecos": [],
    "email": "paula.ribeiro@email.com",
    "idCargo": 1,
    "registroConselho": "CRESS-9/12345",
    "idsPacientes": [1]
  },
  "usuario": {
    "idPerfil": 4
  }
}
```

**Resposta `201`:** mesmo formato do cadastro simples, com `"acessoCriado": true` e `"emailEnviado"` (`false` quando o e-mail não saiu; o cadastro vale mesmo assim e o reenvio é `POST /api/usuarios/{idCadastro}/senha-temporaria`). Use o `idPerfil` do perfil "Assistente Social" (`GET /api/perfis`). Em erros de validação, os nomes em `fields` levam o prefixo do bloco (ex.: `assistenteSocial.registroConselho`, `usuario.idPerfil`).

---

## `PATCH /api/assistentes-sociais/{idCadastro}` — atualizar

Aceita os mesmos campos do `PATCH /api/profissionais/{idCadastro}` (`nome`, `celular`, `numCNS`, `estadoCivil`, `statusCadastro`, `enderecos`, `email`, `idCargo`, `idPerfil`), com as mesmas regras (ver `PROFISSIONAIS.md`).

- **Reativação:** `"statusCadastro": "ATIVO"`. Os vínculos encerrados na inativação **não** voltam: vincule os pacientes de novo.
- `"statusCadastro": "INATIVO"` tem o mesmo efeito do `DELETE` (encerra os vínculos).
- **Ignorados (somente leitura):** `cpf`, `dataNascimento`, `sexo`, `registroConselho`.
- Os vínculos com pacientes **não** são alterados por aqui: use as rotas `/pacientes`.

**Resposta `200`:** os detalhes atualizados (mesmo formato do `GET /api/assistentes-sociais/{idCadastro}`).

---

## `DELETE /api/assistentes-sociais/{idCadastro}` — inativar

Define `statusCadastro` como `INATIVO`, **encerra todos os vínculos ativos** com pacientes e retorna `204 No Content`. Assistente social inativo não consegue fazer login.

Segue a proteção contra trancar o sistema (`409` se ele for o último usuário ativo que gerencia perfis; ver `00-GERAL.md`).

---

## `POST /api/assistentes-sociais/{idCadastro}/pacientes` — vincular paciente

```json
{ "idPaciente": 1 }
```

**Resposta `201 Created`:** os detalhes atualizados do assistente social (mesmo formato do `GET /api/assistentes-sociais/{idCadastro}`), já com o paciente em `pacientes`.

| Situação | Status | Mensagem |
|---|---|---|
| `idPaciente` ausente ou não positivo | `400` | em `fields.idPaciente`: O ID do paciente é obrigatório. / O ID do paciente deve ser positivo |
| Paciente inexistente | `400` | Paciente não encontrado com ID: {id} |
| Assistente social inativo | `400` | Não é possível vincular pacientes a um assistente social inativo. |
| Já existe vínculo ativo entre os dois | `409` | O paciente já está vinculado a este assistente social. |
| Assistente social inexistente | `404` | Assistente social não encontrado com ID: {id} |

Pacientes inativos podem ser vinculados.

---

## `DELETE /api/assistentes-sociais/{idCadastro}/pacientes/{idPaciente}` — desvincular paciente

Encerra o vínculo ativo (preenche a data de fim; o registro não é apagado). **Resposta `204 No Content`.**

| Situação | Status | Mensagem |
|---|---|---|
| Não há vínculo ativo entre os dois | `404` | O paciente com ID {idPaciente} não tem vínculo ativo com este assistente social. |
| Assistente social inexistente | `404` | Assistente social não encontrado com ID: {id} |

---

## Mensagens de erro

### Validação de campos (`400`, em `fields`)

Valem também todas as mensagens de profissional (ver `PROFISSIONAIS.md`).

| Campo | Mensagem |
|---|---|
| `registroConselho` | O registro no conselho é obrigatório |
| `idsPacientes[]` | O ID do paciente não pode ser nulo / O ID do paciente deve ser positivo |
| `assistenteSocial` / `usuario` (com acesso) | Os dados do assistente social são obrigatórios. / Os dados do usuário são obrigatórios. |
| `idPaciente` (vincular) | O ID do paciente é obrigatório. / O ID do paciente deve ser positivo |

### Regras de negócio (`message`)

| Situação | Status | Mensagem |
|---|---|---|
| CPF já cadastrado | `400` | CPF já cadastrado. |
| E-mail já cadastrado | `400` | E-mail já cadastrado. |
| `idCargo` inexistente | `400` | Cargo não encontrado com ID: {id} |
| ID inexistente em `idsPacientes` | `400` | Paciente(s) não encontrado(s): {ids} |
| Paciente inexistente (vincular) | `400` | Paciente não encontrado com ID: {id} |
| Vincular a assistente social inativo | `400` | Não é possível vincular pacientes a um assistente social inativo. |
| `/me/pacientes` com usuário que não é assistente social | `400` | O usuário logado não é um assistente social. |
| Vínculo ativo já existente | `409` | O paciente já está vinculado a este assistente social. |
| Inativação que deixaria o sistema sem usuário ativo que gerencie perfis | `409` | Operação bloqueada: o sistema precisa de ao menos um usuário ativo com permissão para gerenciar perfis (GERENCIAR_PERFIS ou ACESSO_TOTAL). |
| Assistente social inexistente | `404` | Assistente social não encontrado com ID: {id} |
| Desvincular sem vínculo ativo | `404` | O paciente com ID {idPaciente} não tem vínculo ativo com este assistente social. |

As demais mensagens de PATCH (nome, e-mail, cargo, perfil, status) são as mesmas de profissional.

---

## Roteiro rápido de testes

Pré-requisito: dois pacientes cadastrados (`{idLucas}` e `{idBia}`; ver `PACIENTES.md`) e o token do administrador.

1. `POST /api/assistentes-sociais` com o exemplo (`idsPacientes: [{idLucas}]`). Esperado: `201` com `acessoCriado: false`.
2. `GET /api/assistentes-sociais/{id}`. Esperado: `registroConselho` e só o Lucas em `pacientes`, com `cpfMascarado`.
3. `GET /api/profissionais?tipo=ASSISTENTE_SOCIAL`. Esperado: o assistente social, com `tipo: "ASSISTENTE_SOCIAL"` e `registroConselho`.
4. `POST /api/assistentes-sociais/{id}/pacientes` com `{"idPaciente": {idBia}}`. Esperado: `201` com os dois pacientes. Repetir. Esperado: `409`.
5. `GET /api/pacientes/{idBia}`. Esperado: o assistente social em `assistentesSociais`.
6. `DELETE /api/assistentes-sociais/{id}/pacientes/{idBia}`. Esperado: `204`; repetir: `404`. `POST` de novo com a Bia: `201` (vínculo novo).
7. `POST /api/assistentes-sociais/{id}/usuario` com `{"idPerfil": <id do perfil "Assistente Social">}`. Esperado: `201` com `emailEnviado: true`. Faça o primeiro acesso dele (senha temporária no log, se não houver SMTP).
8. Com o token dele: `GET /api/assistentes-sociais/me/pacientes`. Esperado: Lucas e Bia. `GET /api/pacientes?termo=<nome de um paciente não vinculado>`: `[]`. `GET /api/pacientes/{idNaoVinculado}`: `403`. `GET /api/pacientes/{idLucas}` e `/historico`: `200`.
9. `GET /api/assistentes-sociais/me/pacientes` com o token do administrador. Esperado: `400` `O usuário logado não é um assistente social.`
10. Com o administrador, `DELETE /api/assistentes-sociais/{id}`. Esperado: `204`; `GET /api/assistentes-sociais/{id}` mostra `statusCadastro: "INATIVO"` e `pacientes: []`. `POST /api/assistentes-sociais/{id}/pacientes`: `400` (inativo).
11. `PATCH /api/assistentes-sociais/{id}` com `{"statusCadastro": "ATIVO"}`. Esperado: `200`, ativo e ainda `pacientes: []`.
12. `POST /api/assistentes-sociais` com o CPF `11111111111`. Esperado: `400` com `CPF inválido` em `fields.cpf`.
