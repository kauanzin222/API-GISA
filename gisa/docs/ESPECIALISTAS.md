# API GISA — Especialistas, Especialistas PJ e Especialidades

Autenticação, convenções, enums, formato de erro, regras comuns de PATCH/DELETE e concessão de acesso ao sistema: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

Um **especialista** é um profissional (ver `PROFISSIONAIS.md`) com registro em conselho, especialidades e jornada de trabalho. Um **especialista PJ** é um especialista que atende como pessoa jurídica (CNPJ, razão social etc.). Cada tipo tem suas próprias rotas.

## Rotas

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| `GET` | `/api/especialistas/me` | autenticado (especialista) | cadastro do especialista logado (comum ou PJ) |
| `GET` | `/api/especialistas/{idCadastro}` | `VISUALIZAR_PROFISSIONAIS` | detalhes de um especialista |
| `GET` | `/api/especialistas/{idCadastro}/jornadas` | `VISUALIZAR_PROFISSIONAIS`, `VISUALIZAR_TERAPIAS`, `AGENDAR_TERAPIA` ou `EDITAR_TERAPIA` | só as jornadas de trabalho, de especialista **ou** PJ (resumo no agendamento) |
| `POST` | `/api/especialistas` | `CADASTRAR_ESPECIALISTA` | cadastra especialista sem acesso ao sistema |
| `POST` | `/api/especialistas/com-usuario` | `CADASTRAR_ESPECIALISTA` **e** `GERENCIAR_USUARIOS` | cadastra especialista e cria o usuário de acesso |
| `POST` | `/api/especialistas/{idCadastro}/usuario` | `GERENCIAR_USUARIOS` | concede acesso a um especialista já cadastrado (ver `00-GERAL.md`) |
| `PATCH` | `/api/especialistas/{idCadastro}` | `EDITAR_PROFISSIONAL` (+ `GERENCIAR_USUARIOS` com `idPerfil`) | atualiza especialista |
| `DELETE` | `/api/especialistas/{idCadastro}` | `INATIVAR_PROFISSIONAL` | inativa especialista |
| `GET` | `/api/especialistas/pj/{idCadastro}` | `VISUALIZAR_PROFISSIONAIS` | detalhes de um especialista PJ |
| `POST` | `/api/especialistas/pj` | `CADASTRAR_ESPECIALISTA_PJ` | cadastra especialista PJ sem acesso ao sistema |
| `POST` | `/api/especialistas/pj/com-usuario` | `CADASTRAR_ESPECIALISTA_PJ` **e** `GERENCIAR_USUARIOS` | cadastra especialista PJ e cria o usuário de acesso |
| `POST` | `/api/especialistas/pj/{idCadastro}/usuario` | `GERENCIAR_USUARIOS` | concede acesso a um especialista PJ já cadastrado |
| `PATCH` | `/api/especialistas/pj/{idCadastro}` | `EDITAR_PROFISSIONAL` (+ `GERENCIAR_USUARIOS` com `idPerfil`) | atualiza especialista PJ |
| `DELETE` | `/api/especialistas/pj/{idCadastro}` | `INATIVAR_PROFISSIONAL` | inativa especialista PJ |
| `GET` | `/api/especialidades` | autenticado | lista especialidades |
| `GET` | `/api/especialidades/{idEspecialidade}` | `GERENCIAR_ESPECIALIDADES` | detalhes, com os especialistas vinculados |
| `POST` | `/api/especialidades` | `GERENCIAR_ESPECIALIDADES` | cadastra especialidade |
| `PATCH` | `/api/especialidades/{idEspecialidade}` | `GERENCIAR_ESPECIALIDADES` | edita especialidade |
| `DELETE` | `/api/especialidades/{idEspecialidade}` | `GERENCIAR_ESPECIALIDADES` | **exclui**, só sem especialistas vinculados |

`ACESSO_TOTAL` também libera todas as rotas. Sem a permissão: `403`. Os motivos de `/com-usuario` e do `idPerfil` exigirem `GERENCIAR_USUARIOS` estão em `00-GERAL.md` ("Permissões por rota").

> **Não há listagem própria de especialistas.** Eles aparecem em `GET /api/profissionais` (ver `PROFISSIONAIS.md`), com `registroConselho` preenchido.

> **Separação das rotas:** as rotas `/api/especialistas/{idCadastro}` (GET, PATCH, DELETE e `/usuario`) **recusam** IDs de especialistas PJ com `400`:
> `Este cadastro é de um especialista PJ. Use /api/especialistas/pj/{idCadastro}.`
> Já as rotas `/pj/...` com o ID de um especialista que não é PJ respondem `404`. IDs de assistentes sociais (que não são especialistas; ver `ASSISTENTES-SOCIAIS.md`) também respondem `404` aqui.

---

## `GET /api/especialistas/{idCadastro}/jornadas` — jornadas de trabalho

Usada no formulário de agendamento para mostrar quando cada especialista atende. Diferente do GET de detalhes, aceita também IDs de especialistas PJ e não exige `VISUALIZAR_PROFISSIONAIS`.

**Resposta `200`**, ordenada por dia da semana e hora de início (`[]` se não houver jornada):

```json
[
  { "idJornada": 4, "diaSemana": 1, "horaInicio": "08:00", "horaTermino": "12:00" },
  { "idJornada": 5, "diaSemana": 1, "horaInicio": "14:00", "horaTermino": "18:00" },
  { "idJornada": 6, "diaSemana": 3, "horaInicio": "09:00", "horaTermino": "16:00" }
]
```

ID que não é de especialista nem de PJ: `404` `Especialista não encontrado com ID: {id}`.

---

## `GET /api/especialistas/me` — cadastro do especialista logado

O especialista lê o **próprio** cadastro: especialidades, registro no conselho e CBO do cargo, usados na assinatura do atestado e da receita e na tela "Minha Agenda". O perfil "Especialista" não tem `VISUALIZAR_PROFISSIONAIS` (exigida pelos GET por ID), então esta é a rota dele.

- **Permissão:** basta estar logado, como em `GET /api/terapias/minhas`. Nenhuma permissão específica é exigida, e `ACESSO_TOTAL` não muda nada: o que vale é o usuário ser especialista.
- O usuário é identificado pelo CPF do token; nenhum ID vai na URL.
- **Especialista comum:** a resposta é **exatamente** a de `GET /api/especialistas/{idCadastro}` (abaixo).
- **Especialista PJ:** a resposta é **exatamente** a de `GET /api/especialistas/pj/{idCadastro}` (ver "Especialista PJ"). O front diferencia pelo campo `especialista` aninhado, que só existe no corpo do PJ (no comum, o primeiro nível tem `profissional`).

**Resposta `200` (especialista comum)**

```json
{
  "profissional": {
    "pessoa": {
      "idCadastro": 9,
      "nome": "Mariana Costa",
      "cpf": "12345678062",
      "dataNascimento": "1985-08-20",
      "sexo": "F",
      "celular": null,
      "numCNS": null,
      "estadoCivil": "CASADO",
      "statusCadastro": "ATIVO",
      "enderecos": []
    },
    "email": "mariana.costa@email.com",
    "cargo": { "idCargo": 2, "nome": "Psicóloga", "codigoCBO": "251510", "tituloCBO": "Psicólogo clínico" },
    "usuario": {
      "idCadastro": 9,
      "perfil": { "idPerfil": 3, "nome": "Especialista" },
      "permissoes": [
        { "idPermissao": 19, "nome": "EMITIR_ATESTADO", "descricao": "Emitir atestados", "categoria": "Documentos" }
      ]
    }
  },
  "registroConselho": "CRP-06/12345",
  "especialidades": [
    { "idEspecialidade": 1, "nome": "Psicologia", "descricao": "Atendimento psicológico e acompanhamento terapêutico" }
  ],
  "jornadas": [
    { "idJornada": 1, "diaSemana": 1, "horaInicio": "08:00", "horaTermino": "17:00" }
  ]
}
```

**Resposta `200` (especialista PJ)**, resumida (o objeto `especialista` tem o mesmo formato do corpo acima):

```json
{
  "especialista": { "profissional": { "...": "..." }, "registroConselho": "CRM-99999", "especialidades": [], "jornadas": [] },
  "cnpj": "12345678000199",
  "razaoSocial": "Clínica Souza Ltda",
  "nomeFantasia": null,
  "inscricaoEstadual": null
}
```

**Erros**

| Situação | Status | Mensagem |
|---|---|---|
| Usuário logado que não é especialista nem especialista PJ (inclusive o administrador inicial) | `400` | O usuário logado não é um especialista. |
| Token ausente, inválido ou expirado | `401`/`403` | — |

> `/me` não é lido como ID: no `GET`, o caminho literal tem prioridade sobre `/{idCadastro}`. Em `PATCH` e `DELETE` não há rota `/me`, e `/api/especialistas/me` continua respondendo `400` (`Valor inválido para 'idCadastro': me`), como antes.

---

## `GET /api/especialistas/{idCadastro}` — detalhes do especialista

**Resposta `200`**

```json
{
  "profissional": {
    "pessoa": {
      "idCadastro": 10,
      "nome": "João Santos",
      "cpf": "23456789092",
      "dataNascimento": "1985-08-20",
      "sexo": "M",
      "celular": "11988888888",
      "numCNS": "234567890923456",
      "estadoCivil": "CASADO",
      "statusCadastro": "ATIVO",
      "enderecos": []
    },
    "email": "joao.santos@email.com",
    "cargo": {
      "idCargo": 1,
      "nome": "Administrador de Sistema",
      "codigoCBO": "999999",
      "tituloCBO": "Cargo de Teste - Desenvolvimento"
    },
    "usuario": null
  },
  "registroConselho": "CRP-12345",
  "especialidades": [
    { "idEspecialidade": 1, "nome": "Psicologia", "descricao": "Atendimento psicológico e acompanhamento terapêutico" }
  ],
  "jornadas": [
    { "idJornada": 4, "diaSemana": 1, "horaInicio": "08:00", "horaTermino": "17:00" },
    { "idJornada": 5, "diaSemana": 3, "horaInicio": "08:00", "horaTermino": "12:00" }
  ]
}
```

- O bloco `profissional` tem o mesmo formato do `GET /api/profissionais/{idCadastro}`, inclusive `usuario` (`null` quando não há acesso).
- `diaSemana`: `1` = Segunda ... `7` = Domingo.

**Respostas de erro:** `404` com `Especialista não encontrado com ID: {id}` quando o ID não é de um especialista; `400` quando é de um especialista PJ.

---

## `POST /api/especialistas` — cadastrar especialista

### Campos

Todos os campos do `POST /api/profissionais` (`nome`, `cpf`, `dataNascimento`, `sexo`, `estadoCivil`, `celular`, `numCNS`, `enderecos`, `email`, `idCargo`; ver `PROFISSIONAIS.md`), mais:

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `registroConselho` | string | sim | ex.: `CRP-12345` |
| `especialidadesIds` | lista de número | ver regra | IDs de especialidades existentes (`GET /api/especialidades`) |
| `novasEspecialidades` | lista de `{nome, descricao}` | ver regra | especialidades criadas junto com o especialista |
| `jornadas` | lista de jornada | **sim, ao menos 1** | ver abaixo |

**Regra das especialidades:** é preciso informar ao menos uma especialidade, existente (`especialidadesIds`) ou nova (`novasEspecialidades`).

- IDs repetidos em `especialidadesIds` são unificados, e uma especialidade informada pelo ID e pelo nome também é vinculada uma única vez: o especialista nunca fica com a mesma especialidade duas vezes. IDs inexistentes são listados no erro (`Especialidade(s) não encontrada(s): 7, 9`).
- Em `novasEspecialidades`, se já existir uma especialidade com o mesmo nome (sem diferenciar maiúsculas de minúsculas e ignorando espaços nas pontas), ela é reaproveitada e a descrição enviada é ignorada. `nome` é obrigatório; `descricao` é opcional.

### Jornada (`jornadas[]`)

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `diaSemana` | número | sim | `1` (Segunda) a `7` (Domingo) |
| `horaInicio` | string | sim | `HH:mm` |
| `horaTermino` | string | sim | `HH:mm` |

> A API **não valida** se `horaInicio` é anterior a `horaTermino` nem se há jornadas sobrepostas no mesmo dia. Faça essa validação no front. Uma jornada com término antes do início nunca comporta terapias.

> **A jornada define quando o especialista pode ser agendado.** Uma terapia só é aceita se couber inteira em uma única jornada dele no dia da semana da data (ver "Regras de agenda" em `TERAPIAS.md`). Para atender em dois turnos com intervalo (ex.: 08:00–12:00 e 14:00–18:00), cadastre duas jornadas no mesmo dia.

### Exemplo

```json
{
  "nome": "João Santos",
  "cpf": "23456789092",
  "dataNascimento": "1985-08-20",
  "sexo": "M",
  "celular": "11988888888",
  "numCNS": "234567890923456",
  "estadoCivil": "CASADO",
  "enderecos": [],
  "email": "joao.santos@email.com",
  "idCargo": 1,
  "registroConselho": "CRP-12345",
  "especialidadesIds": [1, 2],
  "novasEspecialidades": [
    { "nome": "Musicoterapia", "descricao": "Terapia com uso de música" }
  ],
  "jornadas": [
    { "diaSemana": 1, "horaInicio": "08:00", "horaTermino": "17:00" },
    { "diaSemana": 3, "horaInicio": "08:00", "horaTermino": "12:00" }
  ]
}
```

### Resposta `201 Created`

```json
{
  "idCadastro": 10,
  "nome": "João Santos",
  "cpf": "23456789092",
  "email": "joao.santos@email.com",
  "idCargo": 1,
  "registroConselho": "CRP-12345",
  "acessoCriado": false
}
```

> **CPF ou e-mail repetido:** assim como no profissional, `400` com `CPF já cadastrado.` ou `E-mail já cadastrado.`

---

## `POST /api/especialistas/com-usuario` — cadastrar especialista com acesso

Corpo com dois blocos: `especialista` (mesmos campos do `POST /api/especialistas`) e `usuario` (só `idPerfil`: a senha temporária é gerada pelo back e enviada ao e-mail do especialista; ver `PROFISSIONAIS.md` e "Acesso ao sistema" em `00-GERAL.md`). Exige `CADASTRAR_ESPECIALISTA` **e** `GERENCIAR_USUARIOS` (ou `ACESSO_TOTAL`); a rota `/pj/com-usuario` exige `CADASTRAR_ESPECIALISTA_PJ` e `GERENCIAR_USUARIOS`.

```json
{
  "especialista": {
    "nome": "Pedro Almeida",
    "cpf": "23456788010",
    "dataNascimento": "1985-08-20",
    "sexo": "M",
    "celular": "11988888887",
    "estadoCivil": "CASADO",
    "enderecos": [],
    "email": "pedro.almeida@email.com",
    "idCargo": 1,
    "registroConselho": "CRP-12346",
    "especialidadesIds": [1],
    "jornadas": [
      { "diaSemana": 2, "horaInicio": "09:00", "horaTermino": "18:00" }
    ]
  },
  "usuario": {
    "idPerfil": 1
  }
}
```

**Resposta `201`:** mesmo formato do cadastro simples, com `"acessoCriado": true` e `"emailEnviado"` (`false` quando o e-mail com a senha temporária não saiu; o cadastro vale mesmo assim). Em erros de validação, os nomes em `fields` levam o prefixo do bloco (ex.: `especialista.jornadas`, `usuario.idPerfil`).

---

## `PATCH /api/especialistas/{idCadastro}` — atualizar especialista

Aceita todos os campos do `PATCH /api/profissionais/{idCadastro}` (`nome`, `celular`, `numCNS`, `estadoCivil`, `statusCadastro`, `enderecos`, `email`, `idCargo`, `idPerfil`; ver `PROFISSIONAIS.md`), mais:

| Campo | Observação |
|---|---|
| `especialidadesIds` | **substitui** as especialidades atuais pelas informadas |
| `novasEspecialidades` | cria (ou reaproveita pelo nome) e **acrescenta** especialidades |
| `jornadas` | **substitui** todas as jornadas; não pode ser vazia nem `null` |

**Combinações de especialidades:**

| Enviado | Resultado |
|---|---|
| só `especialidadesIds` | especialidades = as dos IDs |
| só `novasEspecialidades` | especialidades atuais **mantidas** + as novas |
| os dois | especialidades = as dos IDs + as novas |
| `especialidadesIds: []` sem novas | `400` (o especialista precisa de ao menos uma especialidade) |

**Jornadas:** as jornadas antigas são apagadas e recriadas a partir da lista enviada. O `idJornada`, se enviado, é ignorado, e as jornadas recebem IDs novos.

**Jornadas e terapias futuras:** se alguma terapia **futura** (início depois de agora) `AGENDADA` do especialista ficar fora das novas jornadas (mesma regra do agendamento: caber inteira em uma única jornada do dia), o PATCH é recusado com **`409`** e nada é gravado. Remarque ou cancele essas terapias antes. Terapias passadas, em atendimento, concluídas ou canceladas não entram nessa verificação. Vale também para o PATCH de especialista PJ.

```json
{
  "timestamp": "2026-10-15T14:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Não é possível alterar a jornada: 2 terapia(s) agendada(s) de João Santos ficariam fora do novo horário de trabalho. Remarque ou cancele essas terapias antes.",
  "path": "/api/especialistas/10",
  "fields": {},
  "terapias": [
    { "idTerapia": 42, "data": "2026-10-21", "horaInicio": "09:00", "horaTermino": "10:00" },
    { "idTerapia": 57, "data": "2026-10-23", "horaInicio": "17:00", "horaTermino": "18:00" }
  ]
}
```

- `terapias` vem ordenada por data e hora.
- A alteração de jornada e os agendamentos do mesmo especialista não rodam ao mesmo tempo (a mesma trava no banco do agendamento): um espera o outro terminar e já enxerga o resultado dele.

**Ignorados (somente leitura):** `cpf`, `dataNascimento`, `sexo`, `registroConselho`.

```json
{
  "especialidadesIds": [1, 3],
  "jornadas": [
    { "diaSemana": 2, "horaInicio": "08:00", "horaTermino": "12:00" },
    { "diaSemana": 4, "horaInicio": "13:00", "horaTermino": "18:00" }
  ]
}
```

**Resposta `200`:** os detalhes atualizados (mesmo formato do `GET /api/especialistas/{idCadastro}`).

---

## `DELETE /api/especialistas/{idCadastro}` — inativar especialista

Define `statusCadastro` como `INATIVO` e retorna `204 No Content`. Para reativar, use o PATCH com `"statusCadastro": "ATIVO"`.

---

## Especialista PJ

### `GET /api/especialistas/pj/{idCadastro}` — detalhes do especialista PJ

**Resposta `200`**

```json
{
  "especialista": {
    "profissional": {
      "pessoa": {
        "idCadastro": 12,
        "nome": "Ana Oliveira",
        "cpf": "34567890175",
        "dataNascimento": "1982-03-15",
        "sexo": "F",
        "celular": "11977777777",
        "numCNS": null,
        "estadoCivil": "DIVORCIADO",
        "statusCadastro": "ATIVO",
        "enderecos": []
      },
      "email": "ana.oliveira@clinica.com",
      "cargo": {
        "idCargo": 1,
        "nome": "Administrador de Sistema",
        "codigoCBO": "999999",
        "tituloCBO": "Cargo de Teste - Desenvolvimento"
      },
      "usuario": null
    },
    "registroConselho": "CRM-67890",
    "especialidades": [
      { "idEspecialidade": 1, "nome": "Psicologia", "descricao": "Atendimento psicológico e acompanhamento terapêutico" }
    ],
    "jornadas": [
      { "idJornada": 7, "diaSemana": 5, "horaInicio": "08:00", "horaTermino": "16:00" }
    ]
  },
  "cnpj": "12345678000195",
  "razaoSocial": "Clínica Exemplo Ltda",
  "nomeFantasia": "Clínica Exemplo",
  "inscricaoEstadual": "110042490114"
}
```

O bloco `especialista` tem o mesmo formato do `GET /api/especialistas/{idCadastro}`.

**Resposta `404`** (ID não é de um especialista PJ): `Especialista PJ não encontrado com ID: {id}`.

### `POST /api/especialistas/pj` — cadastrar especialista PJ

Todos os campos do `POST /api/especialistas`, mais:

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `cnpj` | string | sim | exatamente 14 dígitos, **sem máscara**; único |
| `razaoSocial` | string | sim | |
| `nomeFantasia` | string | não | |
| `inscricaoEstadual` | string | não | |

```json
{
  "nome": "Ana Oliveira",
  "cpf": "34567890175",
  "dataNascimento": "1982-03-15",
  "sexo": "F",
  "celular": "11977777777",
  "estadoCivil": "DIVORCIADO",
  "enderecos": [],
  "email": "ana.oliveira@clinica.com",
  "idCargo": 1,
  "registroConselho": "CRM-67890",
  "especialidadesIds": [1],
  "jornadas": [
    { "diaSemana": 5, "horaInicio": "08:00", "horaTermino": "16:00" }
  ],
  "cnpj": "12345678000195",
  "razaoSocial": "Clínica Exemplo Ltda",
  "nomeFantasia": "Clínica Exemplo",
  "inscricaoEstadual": "110042490114"
}
```

**Resposta `201 Created`**

```json
{
  "idCadastro": 12,
  "nome": "Ana Oliveira",
  "cpf": "34567890175",
  "email": "ana.oliveira@clinica.com",
  "idCargo": 1,
  "registroConselho": "CRM-67890",
  "cnpj": "12345678000195",
  "razaoSocial": "Clínica Exemplo Ltda",
  "acessoCriado": false
}
```

> **CPF, e-mail ou CNPJ repetido:** `400` com `CPF já cadastrado.`, `E-mail já cadastrado.` ou `CNPJ já cadastrado.`

### `POST /api/especialistas/pj/com-usuario` — cadastrar especialista PJ com acesso

Corpo com dois blocos: **`especialistaPJ`** (mesmos campos do `POST /api/especialistas/pj`) e `usuario`.

```json
{
  "especialistaPJ": {
    "nome": "Beatriz Lima",
    "cpf": "34567890094",
    "dataNascimento": "1982-03-15",
    "sexo": "F",
    "celular": "11977777776",
    "estadoCivil": "DIVORCIADO",
    "enderecos": [],
    "email": "beatriz.lima@clinica.com",
    "idCargo": 1,
    "registroConselho": "CRM-67891",
    "especialidadesIds": [1],
    "jornadas": [
      { "diaSemana": 5, "horaInicio": "08:00", "horaTermino": "16:00" }
    ],
    "cnpj": "98765432000110",
    "razaoSocial": "Lima Terapias Ltda",
    "nomeFantasia": "Lima Terapias",
    "inscricaoEstadual": null
  },
  "usuario": {
    "idPerfil": 1
  }
}
```

**Resposta `201`:** mesmo formato do cadastro simples de PJ, com `"acessoCriado": true` e `"emailEnviado"` (ver `PROFISSIONAIS.md`).

### `PATCH /api/especialistas/pj/{idCadastro}` — atualizar especialista PJ

Aceita todos os campos do `PATCH /api/especialistas/{idCadastro}` (com as mesmas regras de especialidades e jornadas), mais:

| Campo | Observação |
|---|---|
| `razaoSocial` | não pode ser vazia nem `null` |
| `nomeFantasia` | `null` limpa |
| `inscricaoEstadual` | `null` limpa |

**Ignorados (somente leitura):** `cpf`, `dataNascimento`, `sexo`, `registroConselho`, `cnpj`.

```json
{
  "nomeFantasia": "Clínica Exemplo Sorocaba",
  "inscricaoEstadual": null
}
```

**Resposta `200`:** os detalhes atualizados (mesmo formato do `GET /api/especialistas/pj/{idCadastro}`).

### `DELETE /api/especialistas/pj/{idCadastro}` — inativar especialista PJ

Define `statusCadastro` como `INATIVO` e retorna `204 No Content`.

---

## Especialidades

Especialidades não têm status. A listagem é aberta a qualquer usuário logado (preenche seletores em vários formulários); o restante da gestão exige `GERENCIAR_ESPECIALIDADES`.

> O cadastro de especialista também pode criar especialidades (`novasEspecialidades`) com a permissão de cadastro do especialista, sem `GERENCIAR_ESPECIALIDADES`.

### `GET /api/especialidades` — listar especialidades

Retorna todas as especialidades, ordenadas por nome (sem paginação). Use para preencher o seletor de `especialidadesIds`.

```json
[
  { "idEspecialidade": 2, "nome": "Fonoaudiologia", "descricao": "Avaliação e terapia da comunicação, fala e linguagem", "quantidadeEspecialistas": 3 },
  { "idEspecialidade": 1, "nome": "Psicologia", "descricao": "Atendimento psicológico e acompanhamento terapêutico", "quantidadeEspecialistas": 5 }
]
```

- `quantidadeEspecialistas` conta especialistas e especialistas PJ, **ativos e inativos** (os mesmos que impedem a exclusão).
- Cinco especialidades vêm cadastradas por padrão: Psicologia, Fonoaudiologia, Terapia Ocupacional, Fisioterapia e Psicopedagogia. Não conte com os IDs fixos: use sempre os retornados por esta rota.

### `GET /api/especialidades/{idEspecialidade}` — detalhes

**Resposta `200`**

```json
{
  "idEspecialidade": 1,
  "nome": "Psicologia",
  "descricao": "Atendimento psicológico e acompanhamento terapêutico",
  "quantidadeEspecialistas": 2,
  "especialistas": [
    { "idCadastro": 9, "nome": "Mariana Costa", "tipo": "ESPECIALISTA", "statusCadastro": "ATIVO" },
    { "idCadastro": 14, "nome": "Rafael Souza", "tipo": "ESPECIALISTA_PJ", "statusCadastro": "INATIVO" }
  ]
}
```

- `especialistas` traz especialistas e PJ, ativos e inativos, ordenados por nome. O `idCadastro` abre o cadastro em `/api/especialistas/{id}` ou `/api/especialistas/pj/{id}`, conforme o `tipo`.

**Resposta `404`:** `Especialidade não encontrada com ID: {id}`.

### `POST /api/especialidades` — cadastrar especialidade

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `nome` | string | sim | único, sem diferenciar maiúsculas de minúsculas |
| `descricao` | string | não | |

```json
{
  "nome": "Musicoterapia",
  "descricao": "Terapia com uso de música"
}
```

**Resposta `201 Created`:** `idEspecialidade`, `nome` e `descricao` da especialidade criada. Espaços nas pontas do nome são removidos. Nome já existente (sem diferenciar maiúsculas de minúsculas, ex.: `psicologia`): `400` com `Já existe uma especialidade com esse nome.`

### `PATCH /api/especialidades/{idEspecialidade}` — editar especialidade

Segue as regras comuns de PATCH (`00-GERAL.md`).

| Campo | Observação |
|---|---|
| `nome` | não pode ser vazio nem `null`; único sem diferenciar maiúsculas (a própria especialidade não conta, então dá para só trocar maiúsculas e minúsculas) |
| `descricao` | opcional; `null` limpa |

```json
{
  "descricao": "Terapia com uso de música e ritmo"
}
```

**Resposta `200`:** os detalhes atualizados (mesmo formato do `GET /api/especialidades/{idEspecialidade}`). A mudança de nome aparece em todos os especialistas vinculados.

### `DELETE /api/especialidades/{idEspecialidade}` — excluir especialidade

O `DELETE` **exclui** a especialidade e retorna `204 No Content`. Só é possível excluir uma especialidade **sem especialistas vinculados**, contando também os inativos. Antes, retire-a desses especialistas (`PATCH` com `especialidadesIds`); a lista está no `GET` de detalhes.

---

## Mensagens de erro

### Validação de campos (`400`, em `fields`)

Valem também todas as mensagens de profissional (ver `PROFISSIONAIS.md`).

| Campo | Mensagem |
|---|---|
| `registroConselho` | O registro no conselho é obrigatório |
| `jornadas` | Informe ao menos um horário de jornada de trabalho |
| `jornadas[].diaSemana` | O dia da semana é obrigatório / Dia da semana deve ser entre 1 (Segunda) e 7 (Domingo) |
| `jornadas[].horaInicio` | A hora de início é obrigatória |
| `jornadas[].horaTermino` | A hora de término é obrigatória |
| `novasEspecialidades[].nome` | O nome da especialidade é obrigatório |
| `cnpj` | O CNPJ é obrigatório / O CNPJ deve conter 14 dígitos numéricos |
| `razaoSocial` | A razão social é obrigatória |
| `especialista` / `especialistaPJ` | Os dados do especialista são obrigatórios. / Os dados do especialista PJ são obrigatórios. |
| especialidade: `nome` | O nome da especialidade é obrigatório |

Hora em formato diferente de `HH:mm` (ex.: `"8h"`) não chega à validação de campo: a API responde `400` com `Corpo da requisição inválido ou malformado.`

### Regras de negócio (`message`)

| Situação | Status | Mensagem |
|---|---|---|
| ID de PJ nas rotas de especialista | `400` | Este cadastro é de um especialista PJ. Use /api/especialistas/pj/{idCadastro}. |
| Nenhuma especialidade informada | `400` | O especialista deve possuir ao menos uma especialidade. |
| ID de especialidade inexistente | `400` | Especialidade(s) não encontrada(s): {ids} |
| Especialidade com nome já existente (POST e PATCH de especialidade) | `400` | Já existe uma especialidade com esse nome. |
| `nome` vazio ou `null` no PATCH de especialidade | `400` | O nome da especialidade não pode ser vazio. |
| Excluir especialidade com especialistas | `400` | Não é possível excluir a especialidade: há especialistas vinculados a ela. |
| Especialidade inexistente na URL | `404` | Especialidade não encontrada com ID: {id} |
| Sem permissão (inclusive `idPerfil` no PATCH sem `GERENCIAR_USUARIOS`) | `403` | Você não tem permissão para realizar esta ação. |
| `jornadas` vazia ou `null` no PATCH | `400` | O especialista deve possuir ao menos uma jornada de trabalho. |
| Novas `jornadas` deixariam terapias futuras agendadas fora do horário | `409` | Não é possível alterar a jornada: {n} terapia(s) agendada(s) de {nome} ficariam fora do novo horário de trabalho. Remarque ou cancele essas terapias antes. (lista em `terapias`) |
| `razaoSocial` vazia no PATCH | `400` | A razão social não pode ser vazia. |
| `idCargo` inexistente (POST) | `400` | Cargo não encontrado com ID: {id} |
| CPF já cadastrado | `400` | CPF já cadastrado. |
| E-mail já cadastrado | `400` | E-mail já cadastrado. |
| CNPJ já cadastrado (PJ) | `400` | CNPJ já cadastrado. |
| Especialista inexistente (PATCH/DELETE) | `404` | Especialista não encontrado com ID: {id} |
| Especialista PJ inexistente (PATCH/DELETE) | `404` | Especialista PJ não encontrado com ID: {id} |

As demais mensagens de PATCH (nome, e-mail, cargo, perfil, status) são as mesmas de profissional (ver `PROFISSIONAIS.md`).

---

## Roteiro rápido de testes

1. `GET /api/especialidades`. Esperado: as cinco especialidades padrão.
2. `POST /api/especialistas` com o exemplo. Esperado: `201`; `GET /api/especialidades` passa a incluir "Musicoterapia".
3. `POST /api/especialistas` com `especialidadesIds: []` e sem `novasEspecialidades` (outro CPF e e-mail). Esperado: `400`.
4. `POST /api/especialistas/pj` com o exemplo. Esperado: `201`.
5. `GET /api/especialistas/{idPJ}` (rota sem `/pj`). Esperado: `400` com a mensagem de separação de rotas.
6. `GET /api/especialistas/pj/{idPJ}`. Esperado: `200` com CNPJ e razão social.
7. `PATCH /api/especialistas/{id}` do passo 2 com `{"novasEspecialidades": [{"nome": "Psicomotricidade"}]}`. Esperado: `200`, especialidades anteriores mantidas e a nova acrescentada.
8. `PATCH /api/especialistas/{id}` com `{"jornadas": []}`. Esperado: `400`.
9. `PATCH /api/especialistas/pj/{idPJ}` com `{"cnpj": "11111111000111", "nomeFantasia": "Novo nome"}`. Esperado: `200`, `nomeFantasia` alterado e `cnpj` inalterado.
10. `POST /api/especialistas/pj/{idPJ}/usuario` com `{"idPerfil": 1}`. Esperado: `201` com `emailEnviado: true`; repetir. Esperado: `400` (já possui usuário).
11. `DELETE /api/especialistas/pj/{idPJ}`. Esperado: `204`; o GET mostra `statusCadastro: "INATIVO"`.
12. `PATCH /api/especialistas/{id}` do passo 2 com `{"especialidadesIds": [1, 1]}`. Esperado: `200` com "Psicologia" uma única vez.

**Especialidades**

13. `GET /api/especialidades`. Esperado: cada item com `quantidadeEspecialistas`; "Psicologia" com ao menos 1 (o especialista do passo 12).
14. `GET /api/especialidades/{idPsicologia}`. Esperado: o especialista do passo 2 em `especialistas`, com `tipo: "ESPECIALISTA"`.
15. `PATCH /api/especialidades/{idMusicoterapia}` com `{"nome": "psicologia"}`. Esperado: `400` `Já existe uma especialidade com esse nome.` Com `{"descricao": null}`: `200` sem `descricao`.
16. `DELETE /api/especialidades/{idPsicologia}`. Esperado: `400` `Não é possível excluir a especialidade: há especialistas vinculados a ela.`
17. `POST /api/especialidades` com `{"nome": "Equoterapia"}` e `DELETE` dela. Esperado: `204`; o `GET` responde `404`.

**Permissões** (usuários nos perfis padrão; ver `00-GERAL.md`)

18. Com um usuário "Recepção": `GET /api/especialidades`. Esperado: `200`. `GET /api/especialidades/1`, `POST`, `PATCH` e `DELETE` de especialidade, `GET /api/especialistas/{id}` e `PATCH /api/especialistas/{id}`. Esperado: `403`.
19. Com um usuário que tem `EDITAR_PROFISSIONAL` mas não `GERENCIAR_USUARIOS` (crie um perfil assim): `PATCH /api/especialistas/{id}` com `{"nome": "Outro"}`. Esperado: `200`. Com `{"idPerfil": 2}`: `403`.

**Cadastro do especialista logado** (`/me`)

20. Conceder acesso ao especialista do passo 2 com o perfil "Especialista" e fazer o primeiro acesso com ele (login com o e-mail e a senha temporária, depois `POST /api/auth/redefinir-senha`). `GET /api/especialistas/me`. Esperado: `200`, com o mesmo corpo de `GET /api/especialistas/{id}` (feito com o administrador). `GET /api/especialistas/{id}` com o token dele: `403`.
21. O mesmo com o PJ do passo 10 (perfil "Especialista"). Esperado: `200`, com o mesmo corpo de `GET /api/especialistas/pj/{idPJ}` (campo `especialista` aninhado).
22. `GET /api/especialistas/me` com o token do administrador. Esperado: `400` `O usuário logado não é um especialista.`
