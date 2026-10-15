# API GISA — Terapias

Autenticação, convenções, enums e formato de erro: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

Uma **terapia** (consulta) reúne um ou mais pacientes e um ou mais especialistas (inclusive especialistas PJ) em uma data e horário.

- **Modalidade:** é calculada pelo back. Um paciente: `INDIVIDUAL`; mais de um: `COLETIVA`. O front não envia a modalidade; o seletor da tela pode ser só informativo.
- **Status da terapia:** `AGENDADA` no cadastro. Valores: `AGENDADA`, `EM_ATENDIMENTO`, `CONCLUIDA`, `CANCELADA`.
- **Presença de cada paciente:** `PENDENTE` no cadastro. Valores: `PENDENTE`, `PRESENTE`, `FALTA_JUSTIFICADA`, `FALTA_INJUSTIFICADA`.
- **Histórico do paciente:** agendar, editar, cancelar e registrar presença geram eventos no histórico de cada paciente envolvido (ver `GET /api/pacientes/{idCadastro}/historico` em `PACIENTES.md`). Justificativas e motivos de cancelamento **não** vão para o histórico.

## Rotas

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| `GET` | `/api/terapias` | `VISUALIZAR_TERAPIAS` | lista todas as terapias (paginado, 6 por página), com filtros por status, dia ou intervalo, paciente e especialista |
| `GET` | `/api/terapias/minhas` | autenticado (especialista) | lista as terapias do especialista logado (paginado, 3 por página), com filtros por status, dia ou intervalo e paciente |
| `GET` | `/api/terapias/{idTerapia}` | `VISUALIZAR_TERAPIAS` ou especialista participante | detalhes de uma terapia |
| `POST` | `/api/terapias/verificar-conflitos` | `AGENDAR_TERAPIA` ou `EDITAR_TERAPIA` | verifica conflitos de horário **sem gravar** |
| `POST` | `/api/terapias` | `AGENDAR_TERAPIA` ou `EDITAR_TERAPIA` | cadastra terapia |
| `PATCH` | `/api/terapias/{idTerapia}` | `EDITAR_TERAPIA` | edita terapia agendada: remarca e troca participantes |
| `PATCH` | `/api/terapias/{idTerapia}/status` | `ALTERAR_STATUS_TERAPIA` | muda o status (iniciar atendimento, concluir, cancelar) |
| `PATCH` | `/api/terapias/{idTerapia}/pacientes/{idPaciente}/presenca` | `REGISTRAR_PRESENCA` | registra presença ou falta de um paciente |

`ACESSO_TOTAL` também libera todas as rotas. Sem a permissão: `403`. Nos perfis padrão, só a recepção e o administrador agendam, editam, mudam status e registram presença. O especialista **não** tem `VISUALIZAR_TERAPIAS`: ele vê só as terapias de que participa (`/minhas` e o detalhe; ver abaixo).

> **Horário no passado:** não é possível agendar uma terapia (nem remarcar uma para) um horário que já passou, no fuso de São Paulo. Ex.: hoje às 14h, agendar para hoje às 13h responde `400`. Trocar só os participantes de uma terapia agendada cujo horário já chegou continua permitido.

---

## `POST /api/terapias` — cadastrar terapia

Corresponde à tela "Criar Consulta".

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `pacientesIds` | lista de número | **sim, ao menos 1** | `idCadastro` dos pacientes (ver `GET /api/pacientes?termo=`); IDs repetidos são unificados |
| `especialistasIds` | lista de número | **sim, ao menos 1** | `idCadastro` dos especialistas ou PJ (ver `GET /api/profissionais?tipo=ESPECIALISTA&tipo=ESPECIALISTA_PJ&statusCadastro=ATIVO&nome=`) |
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

**Na edição** (`PATCH /api/terapias/{idTerapia}`), envie também o campo opcional `idTerapiaIgnorada` com o ID da terapia que está sendo editada, para que ela não apareça como conflito consigo mesma. Envie o corpo **completo** (data, horários e as listas finais de participantes), já mesclado com os valores atuais.

```json
{
  "pacientesIds": [1, 4],
  "especialistasIds": [10],
  "data": "2026-10-28",
  "horaInicio": "10:00",
  "horaTermino": "11:00",
  "idTerapiaIgnorada": 3
}
```

- `idTerapiaIgnorada` de uma terapia que não existe: `400` com `Terapia não encontrada com ID: {id}`. Zero ou negativo: `400` em `fields.idTerapiaIgnorada` ("O ID da terapia ignorada deve ser positivo").

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

> A verificação é um aviso antecipado e **não reserva o horário**. Entre a verificação e o "Salvar", outro usuário pode ocupar o horário; nesse caso o `POST /api/terapias` (ou o `PATCH`) responde `400` com o conflito. O front deve sempre tratar esse erro.

---

## `PATCH /api/terapias/{idTerapia}` — editar terapia (remarcar ou trocar participantes)

Segue as regras comuns de PATCH (`00-GERAL.md`): **só o que foi enviado muda**, e as listas **substituem** as atuais.

| Campo | Observação |
|---|---|
| `data` | `yyyy-MM-dd`; não pode ser `null` |
| `horaInicio` | `HH:mm`; não pode ser `null` |
| `horaTermino` | `HH:mm`; não pode ser `null`; precisa ser depois do início (o enviado ou o atual) |
| `pacientesIds` | substitui a lista; ao menos 1; IDs repetidos são unificados |
| `especialistasIds` | substitui a lista; ao menos 1 |

```json
{
  "data": "2026-10-28",
  "horaInicio": "10:00",
  "horaTermino": "11:00",
  "pacientesIds": [1, 4]
}
```

**Resposta `200`:** os detalhes atualizados da terapia (mesmo formato do `GET /api/terapias/{idTerapia}`).

**Regras**

- Só terapias **`AGENDADA`** podem ser editadas. Em atendimento, concluída ou cancelada: `400`.
- **Revalida tudo, como no cadastro**, com os valores finais (enviados + atuais): horário, IDs existentes, cadastros **ativos** e conflitos. Por isso, se um participante que continua na terapia foi inativado depois do agendamento, a edição é recusada até ele ser retirado ou reativado.
- **Conflitos:** a própria terapia é ignorada (mover 09:00–10:00 para 09:30–10:30 não conflita consigo mesma). As demais regras de conflito são as do cadastro, inclusive a trava contra edições e cadastros simultâneos.
- **Presença:** paciente que continua na terapia **mantém** a presença e a justificativa; paciente novo entra como `PENDENTE`; paciente retirado sai da terapia com a sua marcação.
- A `modalidade` e o `titulo` são recalculados pelo número de pacientes.
- Corpo vazio (`{}`) não muda nada, mas ainda passa pela revalidação.

---

## `GET /api/terapias` — listar terapias (paginado)

Corresponde à tela "Controle de Agendamentos" e, com `idPaciente`, à tela "Terapias do paciente".

Todos os filtros são opcionais e **combináveis** entre si (a terapia precisa atender a todos).

| Param | Padrão | Descrição |
|---|---|---|
| `status` | — | filtra pelo status; aceita mais de um: `?status=AGENDADA&status=EM_ATENDIMENTO` |
| `data` | — | terapias de um dia (`yyyy-MM-dd`) |
| `inicio` / `fim` | — | intervalo de datas (`yyyy-MM-dd`), com **os dois dias incluídos**. Pode ser usado só um deles (ex.: só `inicio` = daquele dia em diante). Não pode ser usado junto com `data` |
| `idPaciente` | — | terapias das quais o paciente participa |
| `idEspecialista` | — | terapias das quais o especialista (ou PJ) participa |
| `page` | `0` | número da página |
| `size` | `6` | itens por página; **máximo `100`** |
| `sort` | `dataHoraInicio,desc` | ordenação; use `dataHoraInicio,asc` para as mais antigas primeiro (ex.: tela do dia) |

```
GET /api/terapias?status=AGENDADA
GET /api/terapias?data=2026-10-27&size=100&sort=dataHoraInicio,asc
GET /api/terapias?idPaciente=7&inicio=2026-10-01&fim=2026-10-31
```

- O dia da terapia é o da data de início (horário de Brasília, como o agendamento).
- Uma terapia coletiva aparece **uma vez** mesmo que o filtro de paciente ou especialista bata com mais de um participante.
- `idPaciente` ou `idEspecialista` de alguém que não participa de nenhuma terapia (ou que não existe) devolve a página vazia, sem erro.
- O limite de `size` (100) cobre a tela do dia com folga e evita páginas grandes demais, já que cada item traz pacientes e especialistas.

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
      ],
      "motivoCancelamento": null
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
      ],
      "motivoCancelamento": null
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
- `motivoCancelamento`: o motivo informado ao cancelar (ver "Mudar status"); `null` nas terapias não canceladas.

**Erros dos filtros** (todos `400`, com `fields` vazio):

| Situação | Exemplo | `message` |
|---|---|---|
| Status inválido | `?status=XYZ` | Valor inválido para 'status': XYZ |
| Data em formato errado | `?data=27/10/2026`, `?inicio=2026-13-01` | Valor inválido para 'data': 27/10/2026 |
| ID que não é número, zero ou negativo | `?idPaciente=abc`, `?idEspecialista=0` | Valor inválido para 'idPaciente': abc |
| `data` junto com `inicio` ou `fim` | `?data=2026-10-27&fim=2026-10-30` | Use 'data' ou o intervalo 'inicio'/'fim', não os dois juntos. |
| `fim` antes de `inicio` | `?inicio=2026-10-10&fim=2026-10-01` | A data final deve ser igual ou posterior à data inicial. |
| `size` acima de 100 | `?size=500` | O parâmetro 'size' deve ser no máximo 100. |

---

## `GET /api/terapias/minhas` — terapias do especialista logado

Lista apenas as terapias das quais o **usuário logado** participa como especialista. Ele é identificado pelo CPF do token, então o front não envia nenhum ID.

Aceita `status`, `data`, `inicio`, `fim`, `idPaciente`, `page`, `size` e `sort`, com as mesmas regras e erros da listagem geral (inclusive o máximo de 100 por página), e `size` padrão **3**. Não exige permissão específica: basta estar logado e ser especialista.

- `idPaciente`: só as terapias **dele** com esse paciente. É o que o prontuário usa para mostrar as terapias do paciente ao especialista, que não tem `VISUALIZAR_TERAPIAS`. Combinável com os demais filtros.

A tela "Minhas terapias do dia" usa:

```
GET /api/terapias/minhas?data=2026-10-27&size=100&sort=dataHoraInicio,asc
GET /api/terapias/minhas?idPaciente=12&sort=dataHoraInicio,desc
```

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
      "outrosEspecialistas": ["Carlos Lima"],
      "motivoCancelamento": null
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

Mesmo formato de um item da listagem geral.

**Quem pode abrir:** quem tem `VISUALIZAR_TERAPIAS` (ou `ACESSO_TOTAL`), **ou** o especialista que **participa** da terapia, mesmo sem a permissão. É o que permite ao especialista abrir a sessão de origem a partir do prontuário.

| Situação | Status |
|---|---|
| Com `VISUALIZAR_TERAPIAS`, terapia existente | `200` |
| Com `VISUALIZAR_TERAPIAS`, terapia inexistente | `404` `Terapia não encontrada com ID: {id}` |
| Sem a permissão, especialista participante | `200` |
| Sem a permissão e sem participar (inclusive terapia inexistente) | `403` `Você não tem permissão para realizar esta ação.` |

---

## `PATCH /api/terapias/{idTerapia}/status` — mudar status (iniciar, concluir, cancelar)

| Campo | Obrigatório | Observação |
|---|---|---|
| `status` | sim | novo status: `EM_ATENDIMENTO`, `CONCLUIDA` ou `CANCELADA` |
| `motivo` | só para `CANCELADA` | até 255 caracteres; ignorado nos demais casos |

```json
{ "status": "CANCELADA", "motivo": "Especialista em licença" }
```

**Resposta `200`:** os detalhes atualizados da terapia (mesmo formato do `GET /api/terapias/{idTerapia}`), com `motivoCancelamento` preenchido quando cancelada.

```json
{
  "idTerapia": 3,
  "titulo": "Terapia coletiva",
  "modalidade": "COLETIVA",
  "data": "2026-10-27",
  "horaInicio": "09:00",
  "horaTermino": "10:00",
  "status": "CANCELADA",
  "horarioLiberado": false,
  "pacientes": [
    { "idCadastro": 4, "nome": "Beatriz Lima", "presenca": "PENDENTE", "justificativa": null }
  ],
  "especialistas": [
    { "idCadastro": 10, "nome": "Ana Souza", "especialidades": ["Psicologia"] }
  ],
  "motivoCancelamento": "Especialista em licença"
}
```

**Transições permitidas**

| De | Para |
|---|---|
| `AGENDADA` | `EM_ATENDIMENTO` ou `CANCELADA` |
| `EM_ATENDIMENTO` | `CONCLUIDA` ou `CANCELADA` |
| `CONCLUIDA` | nenhuma (estado final) |
| `CANCELADA` | nenhuma (estado final) |

- Não há atalho de `AGENDADA` direto para `CONCLUIDA`: a terapia passa por `EM_ATENDIMENTO`.
- **Cancelar libera o horário** de todos os participantes: terapias `CANCELADA` não contam nos conflitos, e o horário pode ser reagendado na hora.
- Terapia cancelada não aceita mais registro de presença nem edição.
- A API não confere a data ao mudar o status (é possível iniciar uma terapia de outro dia); o front deve oferecer "Iniciar" só no dia.
- Pedidos simultâneos na mesma terapia (ex.: dois usuários cancelando, ou um cancelando enquanto outro edita) são aplicados um depois do outro; o segundo já enxerga o resultado do primeiro.

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

## Inativação do paciente e as terapias dele

Inativar um paciente (`DELETE /api/pacientes/{idCadastro}`, ver `PACIENTES.md`) mexe nas terapias dele **na mesma transação**, mas só nas `AGENDADA` que ainda não começaram (início depois de agora, no fuso de São Paulo):

| Terapia | O que acontece |
|---|---|
| Individual (o paciente é o único paciente) | passa a `CANCELADA`, com `motivoCancelamento` = `Paciente inativado`; o horário fica livre |
| Coletiva | o paciente é **retirado** (com a marcação de presença dele) e a terapia continua para os demais. `modalidade` e `titulo` são recalculados: com 1 paciente restante, ela vira `INDIVIDUAL` |
| Passada, em atendimento, concluída ou cancelada | nada muda |

- Os especialistas e o horário das terapias coletivas não mudam.
- Os eventos do histórico são os de sempre: `TERAPIA_CANCELADA` para a individual; na coletiva, `TERAPIA_ALTERADA` para o paciente retirado ("Paciente retirado da terapia...") e para os que continuam.
- A resposta do `DELETE` informa quantas terapias foram canceladas e de quantas coletivas o paciente saiu.

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
| Agendar ou remarcar para um horário que já passou | `400` | Não é possível agendar uma terapia para um horário que já passou. |
| Sem a permissão da rota (ou especialista que não participa, no detalhe) | `403` | Você não tem permissão para realizar esta ação. |
| Paciente inexistente | `400` | Paciente(s) não encontrado(s): {ids} |
| Especialista inexistente (ou ID de quem não é especialista) | `400` | Especialista(s) não encontrado(s): {ids} |
| Paciente inativo | `400` | Paciente(s) inativo(s): {nomes} |
| Especialista inativo | `400` | Especialista(s) inativo(s): {nomes} |
| Paciente ou especialista ocupado no horário (cadastro e edição) | `400` | Conflito de horário: {nome} ({paciente\|especialista}) já tem terapia das {HH:mm} às {HH:mm}; ... |
| `/minhas` com usuário que não é especialista | `400` | O usuário logado não é um especialista. |
| Terapia inexistente | `404` | Terapia não encontrada com ID: {id} |
| Presença: paciente não é desta terapia | `404` | O paciente com ID {id} não faz parte desta terapia. |
| Presença: terapia cancelada | `400` | Não é possível registrar presença em uma terapia cancelada. |
| Presença ou falta antes do dia da terapia | `400` | Presença e falta só podem ser registradas a partir do dia da terapia. |
| Falta justificada sem justificativa | `400` | Informe a justificativa da falta. |
| Desfazer falta justificada com horário já reocupado | `400` | Não é possível desfazer a falta justificada: o horário já foi ocupado por outra terapia. Conflito de horário: ... |
| Status: terapia já concluída ou cancelada | `400` | A terapia está concluída e não pode mais mudar de status. / A terapia está cancelada e não pode mais mudar de status. |
| Status: igual ao atual | `400` | A terapia já está com o status {STATUS}. |
| Status: transição não permitida | `400` | Não é possível alterar o status de AGENDADA para CONCLUIDA. A partir de AGENDADA, os status permitidos são: EM_ATENDIMENTO, CANCELADA. |
| Status: cancelamento sem motivo | `400` | Informe o motivo do cancelamento. |
| Edição: terapia que não está agendada | `400` | Só é possível editar terapias agendadas. Esta terapia está {em atendimento\|concluída\|cancelada}. |
| Edição: `data`, `horaInicio` ou `horaTermino` enviados como `null` | `400` | A data não pode ser nula. / A hora de início não pode ser nula. / A hora de término não pode ser nula. |
| Edição: `pacientesIds` ou `especialistasIds` vazios ou `null` | `400` | A terapia deve ter ao menos um paciente. / A terapia deve ter ao menos um especialista. |
| Verificação: `idTerapiaIgnorada` inexistente | `400` | Terapia não encontrada com ID: {id} |

Exemplo de conflito: `Conflito de horário: Ana Souza (especialista) já tem terapia das 09:00 às 10:00; Lucas Martins (paciente) já tem terapia das 09:00 às 10:00.`

Na presença, `presenca` ausente gera `fields.presenca` = "Informe a presença"; justificativa com mais de 255 caracteres gera `fields.justificativa`.

Na mudança de status, `status` ausente gera `fields.status` = "Informe o status"; `motivo` com mais de 255 caracteres gera `fields.motivo` = "O motivo deve ter no máximo 255 caracteres". Status inexistente (ex.: `"XYZ"`) responde `Corpo da requisição inválido ou malformado.`

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
8. Login com o e-mail de `{e1}` (com acesso concedido e primeiro acesso feito; ver `00-GERAL.md`) e `GET /api/terapias/minhas`. Esperado: só a terapia do passo 1, com `outrosEspecialistas` contendo `{e2}`.
9. `GET /api/terapias/minhas` com o token do administrador. Esperado: `400` (não é especialista).

**Filtros** (supondo os passos 1 e 2 no mesmo dia, `{dia}`)

- `GET /api/terapias?data={dia}&sort=dataHoraInicio,asc&size=100`. Esperado: as terapias dos passos 1 e 2, a das 09:00 primeiro.
- `GET /api/terapias?idPaciente={p2}`. Esperado: só a do passo 1. Com `idPaciente={p1}`: as duas.
- `GET /api/terapias?idEspecialista={e2}&idPaciente={p2}`. Esperado: só a do passo 1.
- `GET /api/terapias?inicio={dia}&fim={dia}`. Esperado: as duas (os dois dias são incluídos).
- `GET /api/terapias?data={dia}&inicio={dia}`. Esperado: `400`. `?inicio=2026-10-10&fim=2026-10-01`: `400`. `?size=101`: `400`.
- Login com `{e1}` e `GET /api/terapias/minhas?data={dia}`. Esperado: só a do passo 1.

**Presença** (use uma terapia de **hoje**, num horário ainda por vir, com um só paciente `{p1}` e o especialista `{e1}`; chame-a de `{t}`):

10. `PATCH /api/terapias/{t}/pacientes/{p1}/presenca` com `{"presenca": "PRESENTE"}`. Esperado: `200` e o paciente com `presenca: "PRESENTE"`.
11. O mesmo em uma terapia de **amanhã**. Esperado: `400` (antes do dia da terapia).
12. Na terapia de amanhã, `{"presenca": "FALTA_JUSTIFICADA"}` sem justificativa. Esperado: `400`. Com `"justificativa": "Consulta médica"`: `200` e `horarioLiberado: true`.
13. `POST /api/terapias` com outro paciente e o mesmo `{e1}` no horário da terapia de amanhã. Esperado: `201` (horário liberado).
14. Na terapia de amanhã, `{"presenca": "PENDENTE"}`. Esperado: `400` (o horário foi reocupado no passo 13).

**Status** (use a terapia do passo 2, `{t2}`)

15. `PATCH /api/terapias/{t2}/status` com `{"status": "CONCLUIDA"}`. Esperado: `400` (de `AGENDADA` só para `EM_ATENDIMENTO` ou `CANCELADA`).
16. `{"status": "EM_ATENDIMENTO"}` e depois `{"status": "CONCLUIDA"}`. Esperado: `200` nos dois; `status: "CONCLUIDA"`.
17. `{"status": "CANCELADA", "motivo": "Teste"}` na mesma terapia. Esperado: `400` (estado final).
18. Na terapia do passo 1, `{"status": "CANCELADA"}` sem motivo. Esperado: `400`. Com `"motivo": "Especialista em licença"`: `200`, `status: "CANCELADA"` e `motivoCancelamento` preenchido.
19. Repetir o `POST` do passo 1. Esperado: `201` (o horário cancelado ficou livre). Chame a nova terapia de `{t3}`.

**Edição** (em `{t3}`, agendada, com `{p1}`, `{p2}`, `{e1}` e `{e2}` às 09:00–10:00)

20. `POST /api/terapias/verificar-conflitos` com o corpo de `{t3}` trocando para 09:30–10:30. Esperado: `possuiConflito: true` (conflita com ela mesma). Com `"idTerapiaIgnorada": {t3}`: `possuiConflito: false`.
21. `PATCH /api/terapias/{t3}` com `{"horaInicio": "09:30", "horaTermino": "10:30"}`. Esperado: `200`, horário novo e as presenças mantidas.
22. `PATCH /api/terapias/{t3}` com `{"pacientesIds": [{p1}]}`. Esperado: `200`, `INDIVIDUAL`, `titulo` = nome de `{p1}`.
23. `PATCH /api/terapias/{t3}` com `{"horaInicio": "14:00", "horaTermino": "14:50"}`. Esperado: `400` com conflito, porque `{p1}` está na terapia do passo 2 nesse horário (concluída também ocupa; só cancelada libera).
24. `PATCH /api/terapias/{t3}` com `{"pacientesIds": []}`. Esperado: `400`.
25. Cancelar `{t3}` e repetir o passo 21. Esperado: `400` (só terapias agendadas).

**Horário no passado**

26. `POST /api/terapias` para hoje, num horário que já passou (ex.: 00:00–00:30). Esperado: `400` `Não é possível agendar uma terapia para um horário que já passou.`

**Permissões** (crie usuários nos perfis padrão; ver `00-GERAL.md`)

27. Com um usuário "Recepção": `POST /api/terapias`, `PATCH .../status` e `PATCH .../presenca`. Esperado: `201`/`200`.
28. Login com `{e1}` no perfil "Especialista". `GET /api/terapias`. Esperado: `403`. `GET /api/terapias/{t1}` (do passo 1, de que ele participa). Esperado: `200`. `GET /api/terapias/{t2}` (passo 2, só `{e2}`). Esperado: `403`. `GET /api/terapias/999999`. Esperado: `403`.
29. Com `{e1}`: `GET /api/terapias/minhas?idPaciente={p2}`. Esperado: só a do passo 1. `PATCH /api/terapias/{t1}/status` com `{"status": "EM_ATENDIMENTO"}` e `POST /api/terapias` com um corpo válido. Esperado: `403`.
30. Com um usuário "Assistente Social": `GET /api/terapias` e `GET /api/terapias/{t1}`. Esperado: `403`.
