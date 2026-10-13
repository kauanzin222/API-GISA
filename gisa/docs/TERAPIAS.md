# API GISA — Terapias

Autenticação, convenções, enums e formato de erro: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

Uma **terapia** (consulta) reúne um ou mais pacientes e um ou mais especialistas (inclusive especialistas PJ) em uma data e horário.

- **Modalidade:** é calculada pelo back. Um paciente: `INDIVIDUAL`; mais de um: `COLETIVA`. O front não envia a modalidade; o seletor da tela pode ser só informativo.
- **Status da terapia:** `AGENDADA` no cadastro. Valores: `AGENDADA`, `EM_ATENDIMENTO`, `CONCLUIDA`, `CANCELADA`.
- **Presença de cada paciente:** `PENDENTE` no cadastro. Valores: `PENDENTE`, `PRESENTE`, `FALTA_JUSTIFICADA`, `FALTA_INJUSTIFICADA`.

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/terapias` | lista todas as terapias (paginado, 6 por página), com filtro por status |
| `GET` | `/api/terapias/minhas` | lista as terapias do especialista logado (paginado, 3 por página) |
| `GET` | `/api/terapias/{idTerapia}` | detalhes de uma terapia |
| `POST` | `/api/terapias/verificar-conflitos` | verifica conflitos de horário **sem gravar** |
| `POST` | `/api/terapias` | cadastra terapia |
| `PATCH` | `/api/terapias/{idTerapia}/pacientes/{idPaciente}/presenca` | registra presença ou falta de um paciente |

---

## `POST /api/terapias` — cadastrar terapia

Corresponde à tela "Criar Consulta".

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `pacientesIds` | lista de número | **sim, ao menos 1** | `idCadastro` dos pacientes (ver `GET /api/pacientes?nome=`); IDs repetidos são unificados |
| `especialistasIds` | lista de número | **sim, ao menos 1** | `idCadastro` dos especialistas ou PJ (ver `GET /api/profissionais?nome=`, itens com `registroConselho`) |
| `data` | data | sim | `yyyy-MM-dd` |
| `horaInicio` | string | sim | `HH:mm` |
| `horaTermino` | string | sim | `HH:mm`; precisa ser **depois** de `horaInicio` (a terapia começa e termina no mesmo dia) |

**Regras**

- Pacientes e especialistas precisam existir e estar **ativos**.
- **Conflito de horário:** um especialista ou paciente não pode estar em duas terapias ao mesmo tempo. Terapias `CANCELADA` não contam. **Falta justificada libera horário:** o paciente que justificou a falta fica livre naquele horário, e quando todos os pacientes de uma terapia justificam, o horário do especialista também fica livre (ver "Registrar presença"). Encostar não é conflito: uma terapia que começa às 10:00 pode vir logo depois de outra que termina às 10:00. A mensagem de erro lista **todos** os conflitos encontrados.
- **Cadastros simultâneos:** se dois usuários salvarem ao mesmo tempo terapias conflitantes, só a primeira é gravada; a segunda recebe o erro de conflito. Antes de verificar, o cadastro trava no banco os pacientes e especialistas envolvidos, e um segundo cadastro com alguém em comum espera o primeiro terminar.
- A API **não** verifica a jornada de trabalho do especialista nem impede datas passadas.

**Exemplo (coletiva)**

```json
{
  "pacientesIds": [1, 4, 7],
  "especialistasIds": [10, 12],
  "data": "2026-10-27",
  "horaInicio": "09:00",
  "horaTermino": "10:00"
}
```

**Resposta `201 Created`:** os detalhes da terapia criada (mesmo formato do `GET /api/terapias/{idTerapia}`).

---

## `POST /api/terapias/verificar-conflitos` — verificar horário antes de salvar

Recebe **o mesmo corpo** do `POST /api/terapias` e aplica as mesmas validações (campos, horário, IDs e cadastros ativos), mas **não grava nada**. Use para avisar o usuário enquanto ele preenche a tela, por exemplo ao escolher o horário ou ao adicionar um paciente ou especialista.

**Resposta `200`** (com conflito)

```json
{
  "possuiConflito": true,
  "conflitos": [
    {
      "tipo": "ESPECIALISTA",
      "idCadastro": 10,
      "nome": "Ana Souza",
      "idTerapia": 3,
      "data": "2026-10-27",
      "horaInicio": "09:00",
      "horaTermino": "10:00"
    }
  ]
}
```

Sem conflito: `{ "possuiConflito": false, "conflitos": [] }`.

- `tipo`: `PACIENTE` ou `ESPECIALISTA`. Use `idCadastro` para destacar o campo correspondente na tela.
- `idTerapia`, `horaInicio` e `horaTermino` são da terapia **já existente** que ocupa o horário.
- Erros de validação (IDs inexistentes, término antes do início etc.) retornam `400`, como no cadastro.

> A verificação é um aviso antecipado e **não reserva o horário**. Entre a verificação e o "Salvar", outro usuário pode ocupar o horário; nesse caso o `POST /api/terapias` responde `400` com o conflito. O front deve sempre tratar esse erro.

---

## `GET /api/terapias` — listar terapias (paginado)

Corresponde à tela "Controle de Agendamentos".

| Param | Padrão | Descrição |
|---|---|---|
| `status` | — | filtra pelo status; aceita mais de um: `?status=AGENDADA&status=EM_ATENDIMENTO` |
| `page` | `0` | número da página |
| `size` | `6` | itens por página |
| `sort` | `dataHoraInicio,desc` | ordenação; use `dataHoraInicio,asc` para as mais antigas primeiro |

```
GET /api/terapias?status=AGENDADA
```

**Resposta `200`**

```json
{
  "content": [
    {
      "idTerapia": 3,
      "titulo": "Terapia coletiva",
      "modalidade": "COLETIVA",
      "data": "2026-10-09",
      "horaInicio": "10:00",
      "horaTermino": "11:00",
      "status": "AGENDADA",
      "horarioLiberado": false,
      "pacientes": [
        { "idCadastro": 4, "nome": "Beatriz Lima", "presenca": "PENDENTE", "justificativa": null },
        { "idCadastro": 1, "nome": "Roberto Almeida", "presenca": "PENDENTE", "justificativa": null }
      ],
      "especialistas": [
        { "idCadastro": 10, "nome": "Ana Souza", "especialidades": ["Psicologia"] }
      ]
    },
    {
      "idTerapia": 1,
      "titulo": "Lucas Oliveira Santos",
      "modalidade": "INDIVIDUAL",
      "data": "2026-10-05",
      "horaInicio": "09:00",
      "horaTermino": "09:50",
      "status": "AGENDADA",
      "horarioLiberado": false,
      "pacientes": [
        { "idCadastro": 7, "nome": "Lucas Oliveira Santos", "presenca": "PENDENTE", "justificativa": null }
      ],
      "especialistas": [
        { "idCadastro": 10, "nome": "Ana Souza", "especialidades": ["Psicologia"] }
      ]
    }
  ],
  "page": {
    "size": 6,
    "number": 0,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

- `titulo`: o nome do paciente na terapia individual; `"Terapia coletiva"` na coletiva.
- `pacientes` e `especialistas` vêm ordenados por nome. Na tela, "4 pacientes · 2 especialistas" é o tamanho dessas listas.
- `especialidades` permite montar textos como "Dra. Ana Souza (Psicologia)".
- Status inválido em `status` (ex.: `?status=XYZ`): `400`.

---

## `GET /api/terapias/minhas` — terapias do especialista logado

Lista apenas as terapias das quais o **usuário logado** participa como especialista. Ele é identificado pelo CPF do token, então o front não envia nenhum ID.

Aceita os mesmos parâmetros da listagem geral (`status`, `page`, `sort`), com `size` padrão **3**.

**Resposta `200`**

```json
{
  "content": [
    {
      "idTerapia": 2,
      "titulo": "Terapia coletiva",
      "modalidade": "COLETIVA",
      "data": "2026-09-28",
      "horaInicio": "14:00",
      "horaTermino": "15:30",
      "status": "CONCLUIDA",
      "horarioLiberado": false,
      "pacientes": [
        { "idCadastro": 1, "nome": "Roberto Almeida", "presenca": "PRESENTE", "justificativa": null },
        { "idCadastro": 4, "nome": "Beatriz Lima", "presenca": "FALTA_JUSTIFICADA", "justificativa": "Atestado médico" }
      ],
      "outrosEspecialistas": ["Carlos Lima"]
    }
  ],
  "page": {
    "size": 3,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

- `outrosEspecialistas`: os demais especialistas da terapia, sem o próprio usuário (`[]` quando ele atende sozinho).
- Usuário logado que não é especialista (por exemplo, o administrador inicial ou um profissional comum): `400` com `O usuário logado não é um especialista.`

---

## `GET /api/terapias/{idTerapia}` — detalhes da terapia

Mesmo formato de um item da listagem geral. **`404`:** `Terapia não encontrada com ID: {id}`.

---

## `PATCH /api/terapias/{idTerapia}/pacientes/{idPaciente}/presenca` — registrar presença

Corresponde aos botões **Compareceu**, **Falta justificada** e **Falta** da tela "Consultas Diárias". Registra a presença de **um** paciente; nas terapias coletivas, cada paciente é marcado separadamente.

| Campo | Obrigatório | Observação |
|---|---|---|
| `presenca` | sim | `PRESENTE`, `FALTA_JUSTIFICADA`, `FALTA_INJUSTIFICADA` ou `PENDENTE` (desfaz uma marcação feita por engano) |
| `justificativa` | só para `FALTA_JUSTIFICADA` | até 255 caracteres; ignorada (e apagada) nos demais casos |

```json
{
  "presenca": "FALTA_JUSTIFICADA",
  "justificativa": "Atestado médico apresentado pela mãe"
}
```

**Resposta `200`:** os detalhes atualizados da terapia (mesmo formato do `GET /api/terapias/{idTerapia}`), para a tela redesenhar o card.

**Regras**

| Situação | Regra |
|---|---|
| `PRESENTE` ou `FALTA_INJUSTIFICADA` | só a partir do **dia da terapia** (horário de Brasília) |
| `FALTA_JUSTIFICADA` | pode ser registrada **com antecedência** (ex.: a família avisa que o paciente vai faltar amanhã) |
| Terapia `CANCELADA` | não aceita marcação |
| Paciente que não é desta terapia | `404` |

**Horário liberado**

A falta justificada libera horários para novos agendamentos:

- O **paciente** que justificou a falta fica livre naquele horário e pode ser agendado em outra terapia.
- Quando **todos** os pacientes da terapia justificam, o horário do **especialista** também fica livre, e a resposta passa a ter `"horarioLiberado": true`. É o selo "Horário liberado" da tela. Numa terapia coletiva em que só parte dos pacientes justificou, o especialista continua ocupado.

**Desfazer uma falta justificada** (trocar para outro valor) volta a ocupar o horário. Se, nesse meio-tempo, outra terapia foi agendada no horário liberado, o pedido é recusado com `400`, porque desfazer criaria um agendamento duplo:

`Não é possível desfazer a falta justificada: o horário já foi ocupado por outra terapia. Conflito de horário: Ana Souza (especialista) já tem terapia das 09:00 às 10:00.`

Nesse caso, a outra terapia precisa ser cancelada ou remarcada antes.

---

## Mensagens de erro

### Validação de campos (`400`, em `fields`)

| Campo | Mensagem |
|---|---|
| `pacientesIds` | Informe ao menos um paciente |
| `pacientesIds[]` | O ID do paciente não pode ser nulo |
| `especialistasIds` | Informe ao menos um especialista |
| `especialistasIds[]` | O ID do especialista não pode ser nulo |
| `data` | A data é obrigatória |
| `horaInicio` | A hora de início é obrigatória |
| `horaTermino` | A hora de término é obrigatória |

Data ou hora em formato errado (ex.: `"9h"`): `400` com `Corpo da requisição inválido ou malformado.`

### Regras de negócio (`message`)

| Situação | Status | Mensagem |
|---|---|---|
| Término igual ou antes do início | `400` | A hora de término deve ser posterior à hora de início. |
| Paciente inexistente | `400` | Paciente(s) não encontrado(s): {ids} |
| Especialista inexistente (ou ID de quem não é especialista) | `400` | Especialista(s) não encontrado(s): {ids} |
| Paciente inativo | `400` | Paciente(s) inativo(s): {nomes} |
| Especialista inativo | `400` | Especialista(s) inativo(s): {nomes} |
| Paciente ou especialista ocupado no horário (só no cadastro) | `400` | Conflito de horário: {nome} ({paciente\|especialista}) já tem terapia das {HH:mm} às {HH:mm}; ... |
| `/minhas` com usuário que não é especialista | `400` | O usuário logado não é um especialista. |
| Terapia inexistente | `404` | Terapia não encontrada com ID: {id} |
| Presença: paciente não é desta terapia | `404` | O paciente com ID {id} não faz parte desta terapia. |
| Presença: terapia cancelada | `400` | Não é possível registrar presença em uma terapia cancelada. |
| Presença ou falta antes do dia da terapia | `400` | Presença e falta só podem ser registradas a partir do dia da terapia. |
| Falta justificada sem justificativa | `400` | Informe a justificativa da falta. |
| Desfazer falta justificada com horário já reocupado | `400` | Não é possível desfazer a falta justificada: o horário já foi ocupado por outra terapia. Conflito de horário: ... |

Exemplo de conflito: `Conflito de horário: Ana Souza (especialista) já tem terapia das 09:00 às 10:00; Lucas Martins (paciente) já tem terapia das 09:00 às 10:00.`

Na presença, `presenca` ausente gera `fields.presenca` = "Informe a presença"; justificativa com mais de 255 caracteres gera `fields.justificativa`.

---

## Roteiro rápido de testes

Pré-requisito: dois pacientes ativos (`{p1}`, `{p2}`) e dois especialistas (`{e1}`, `{e2}`), sendo `{e1}` com acesso ao sistema (`POST /api/especialistas/{e1}/usuario`).

1. `POST /api/terapias` com `pacientesIds: [{p1}, {p2}]`, `especialistasIds: [{e1}, {e2}]`, 09:00–10:00. Esperado: `201`, `COLETIVA`, `titulo: "Terapia coletiva"`, presenças `PENDENTE`.
2. `POST /api/terapias` com `pacientesIds: [{p1}]` e `especialistasIds: [{e2}]`, 14:00–14:50. Esperado: `201`, `INDIVIDUAL`, `titulo` = nome do paciente.
3. `POST /api/terapias/verificar-conflitos` com o corpo do passo 2 trocando para 14:30–15:00. Esperado: `200` com `possuiConflito: true` e dois itens em `conflitos` (o paciente `{p1}` e o especialista `{e2}`); nenhuma terapia nova gravada.
4. `POST /api/terapias` com esse mesmo corpo. Esperado: `400` com `Conflito de horário: ...`.
5. `POST /api/terapias` com `horaInicio: "10:00"` e `horaTermino: "09:00"`. Esperado: `400`.
6. `GET /api/terapias`. Esperado: as duas terapias, `page.size: 6`.
7. `GET /api/terapias?status=CANCELADA`. Esperado: `content: []`.
8. Login com o CPF de `{e1}` e `GET /api/terapias/minhas`. Esperado: só a terapia do passo 1, com `outrosEspecialistas` contendo `{e2}`.
9. `GET /api/terapias/minhas` com o token do administrador. Esperado: `400` (não é especialista).

**Presença** (use uma terapia de **hoje**, com um só paciente `{p1}` e o especialista `{e1}`; chame-a de `{t}`):

10. `PATCH /api/terapias/{t}/pacientes/{p1}/presenca` com `{"presenca": "PRESENTE"}`. Esperado: `200` e o paciente com `presenca: "PRESENTE"`.
11. O mesmo em uma terapia de **amanhã**. Esperado: `400` (antes do dia da terapia).
12. Na terapia de amanhã, `{"presenca": "FALTA_JUSTIFICADA"}` sem justificativa. Esperado: `400`. Com `"justificativa": "Consulta médica"`: `200` e `horarioLiberado: true`.
13. `POST /api/terapias` com outro paciente e o mesmo `{e1}` no horário da terapia de amanhã. Esperado: `201` (horário liberado).
14. Na terapia de amanhã, `{"presenca": "PENDENTE"}`. Esperado: `400` (o horário foi reocupado no passo 13).
