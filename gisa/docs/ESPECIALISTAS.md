# API GISA — Especialistas, Especialistas PJ e Especialidades

Autenticação, convenções, enums, formato de erro, regras comuns de PATCH/DELETE e concessão de acesso ao sistema: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

Um **especialista** é um profissional (ver `PROFISSIONAIS.md`) com registro em conselho, especialidades e jornada de trabalho. Um **especialista PJ** é um especialista que atende como pessoa jurídica (CNPJ, razão social etc.). Cada tipo tem suas próprias rotas.

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/especialistas/{idCadastro}` | detalhes de um especialista |
| `POST` | `/api/especialistas` | cadastra especialista sem acesso ao sistema |
| `POST` | `/api/especialistas/com-usuario` | cadastra especialista e cria o usuário de acesso |
| `POST` | `/api/especialistas/{idCadastro}/usuario` | concede acesso a um especialista já cadastrado (ver `00-GERAL.md`) |
| `PATCH` | `/api/especialistas/{idCadastro}` | atualiza especialista |
| `DELETE` | `/api/especialistas/{idCadastro}` | inativa especialista |
| `GET` | `/api/especialistas/pj/{idCadastro}` | detalhes de um especialista PJ |
| `POST` | `/api/especialistas/pj` | cadastra especialista PJ sem acesso ao sistema |
| `POST` | `/api/especialistas/pj/com-usuario` | cadastra especialista PJ e cria o usuário de acesso |
| `POST` | `/api/especialistas/pj/{idCadastro}/usuario` | concede acesso a um especialista PJ já cadastrado |
| `PATCH` | `/api/especialistas/pj/{idCadastro}` | atualiza especialista PJ |
| `DELETE` | `/api/especialistas/pj/{idCadastro}` | inativa especialista PJ |
| `GET` | `/api/especialidades` | lista especialidades |
| `POST` | `/api/especialidades` | cadastra especialidade |

> **Não há listagem própria de especialistas.** Eles aparecem em `GET /api/profissionais` (ver `PROFISSIONAIS.md`), com `registroConselho` preenchido.

> **Separação das rotas:** as rotas `/api/especialistas/{idCadastro}` (GET, PATCH, DELETE e `/usuario`) **recusam** IDs de especialistas PJ com `400`:
> `Este cadastro é de um especialista PJ. Use /api/especialistas/pj/{idCadastro}.`
> Já as rotas `/pj/...` com o ID de um especialista que não é PJ respondem `404`.

---

## `GET /api/especialistas/{idCadastro}` — detalhes do especialista

**Resposta `200`**

```json
{
  "profissional": {
    "pessoa": {
      "idCadastro": 10,
      "nome": "João Santos",
      "cpf": "23456789012",
      "dataNascimento": "1985-08-20",
      "sexo": "M",
      "celular": "11988888888",
      "numCNS": "234567890123456",
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

> A API **não valida** se `horaInicio` é anterior a `horaTermino` nem se há jornadas sobrepostas no mesmo dia. Faça essa validação no front.

### Exemplo

```json
{
  "nome": "João Santos",
  "cpf": "23456789012",
  "dataNascimento": "1985-08-20",
  "sexo": "M",
  "celular": "11988888888",
  "numCNS": "234567890123456",
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
  "cpf": "23456789012",
  "email": "joao.santos@email.com",
  "idCargo": 1,
  "registroConselho": "CRP-12345",
  "acessoCriado": false
}
```

> **CPF ou e-mail repetido:** assim como no profissional, `400` com `CPF já cadastrado.` ou `E-mail já cadastrado.`

---

## `POST /api/especialistas/com-usuario` — cadastrar especialista com acesso

Corpo com dois blocos: `especialista` (mesmos campos do `POST /api/especialistas`) e `usuario` (`senha` de 8 a 30 caracteres e `idPerfil`; ver `PROFISSIONAIS.md`).

```json
{
  "especialista": {
    "nome": "Pedro Almeida",
    "cpf": "23456789013",
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
    "senha": "SenhaSegura123",
    "idPerfil": 1
  }
}
```

**Resposta `201`:** mesmo formato do cadastro simples, com `"acessoCriado": true`. Em erros de validação, os nomes em `fields` levam o prefixo do bloco (ex.: `especialista.jornadas`, `usuario.senha`).

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
        "cpf": "34567890123",
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
  "cpf": "34567890123",
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
  "cpf": "34567890123",
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
    "cpf": "34567890124",
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
    "senha": "SenhaSegura123",
    "idPerfil": 1
  }
}
```

**Resposta `201`:** mesmo formato do cadastro simples de PJ, com `"acessoCriado": true`.

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

### `GET /api/especialidades` — listar especialidades

Retorna todas as especialidades (sem paginação). Use para preencher o seletor de `especialidadesIds`.

```json
[
  { "idEspecialidade": 1, "nome": "Psicologia", "descricao": "Atendimento psicológico e acompanhamento terapêutico" },
  { "idEspecialidade": 2, "nome": "Fonoaudiologia", "descricao": "Avaliação e terapia da comunicação, fala e linguagem" },
  { "idEspecialidade": 3, "nome": "Terapia Ocupacional", "descricao": "Desenvolvimento de habilidades motoras e de autonomia" },
  { "idEspecialidade": 4, "nome": "Fisioterapia", "descricao": "Reabilitação motora e funcional" },
  { "idEspecialidade": 5, "nome": "Psicopedagogia", "descricao": "Acompanhamento de dificuldades de aprendizagem" }
]
```

As cinco especialidades acima vêm cadastradas por padrão. Não conte com os IDs fixos: use sempre os retornados por esta rota.

### `POST /api/especialidades` — cadastrar especialidade

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `nome` | string | sim | único |
| `descricao` | string | não | |

```json
{
  "nome": "Musicoterapia",
  "descricao": "Terapia com uso de música"
}
```

**Resposta `201 Created`:** a especialidade criada, no mesmo formato da listagem. Espaços nas pontas do nome são removidos. Nome já existente (sem diferenciar maiúsculas de minúsculas, ex.: `psicologia`): `400` com `Já existe uma especialidade com esse nome.`

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
| Especialidade com nome já existente (`POST /api/especialidades`) | `400` | Já existe uma especialidade com esse nome. |
| `jornadas` vazia ou `null` no PATCH | `400` | O especialista deve possuir ao menos uma jornada de trabalho. |
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
10. `POST /api/especialistas/pj/{idPJ}/usuario` com `{"senha": "SenhaTeste123", "idPerfil": 1}`. Esperado: `201`; repetir. Esperado: `400` (já possui usuário).
11. `DELETE /api/especialistas/pj/{idPJ}`. Esperado: `204`; o GET mostra `statusCadastro: "INATIVO"`.
12. `PATCH /api/especialistas/{id}` do passo 2 com `{"especialidadesIds": [1, 1]}`. Esperado: `200` com "Psicologia" uma única vez.
