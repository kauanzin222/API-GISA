# API GISA — Pacientes e Responsáveis

Autenticação, convenções, enums, formato de erro e regras comuns de PATCH/DELETE: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

## Rotas

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| `GET` | `/api/pacientes` | `VISUALIZAR_PACIENTES` ou `VISUALIZAR_PACIENTES_VINCULADOS` | **busca** pacientes por CPF ou nome (até 20, sem paginação); lista os pacientes de uma escola |
| `GET` | `/api/pacientes/{idCadastro}` | `VISUALIZAR_PACIENTES` ou `VISUALIZAR_PACIENTES_VINCULADOS` | detalhes de um paciente |
| `GET` | `/api/pacientes/{idCadastro}/historico` | `VISUALIZAR_HISTORICO_PACIENTE` | linha do tempo do paciente (paginado) |
| `POST` | `/api/pacientes` | `CADASTRAR_PACIENTE` | cadastra paciente com prontuário, endereços, responsáveis e CIDs |
| `PATCH` | `/api/pacientes/{idCadastro}` | `EDITAR_PACIENTE` | atualiza paciente |
| `POST` | `/api/pacientes/{idCadastro}/responsaveis` | `EDITAR_PACIENTE` | cadastra um responsável e o vincula ao paciente |
| `DELETE` | `/api/pacientes/{idCadastro}` | `INATIVAR_PACIENTE` | inativa paciente e cuida das terapias futuras dele |
| `GET` | `/api/responsaveis` | `VISUALIZAR_PACIENTES` | busca responsáveis por CPF ou nome (paginado) |
| `GET` | `/api/responsaveis/{idResponsavel}` | `VISUALIZAR_PACIENTES` | detalhes de um responsável |
| `PATCH` | `/api/responsaveis/{idResponsavel}` | `EDITAR_PACIENTE` | atualiza dados pessoais do responsável |
| `DELETE` | `/api/responsaveis/{idResponsavel}` | `INATIVAR_PACIENTE` | inativa responsável |

`ACESSO_TOTAL` também libera todas as rotas. Sem a permissão: `403`. No formulário do paciente, o autocomplete de escolas (`GET /api/escolas/sugestoes`), o cadastro rápido de escola (`POST /api/escolas`) e a busca de CID (`GET /api/cids/{codigo}`) aceitam `CADASTRAR_PACIENTE` ou `EDITAR_PACIENTE`.

> Escolas e CIDs usados no paciente têm documentos próprios: `ESCOLAS.md` e `CIDS.md`.

### Escopo de acesso aos pacientes (LGPD)

| Permissão do usuário | O que ele vê |
|---|---|
| `VISUALIZAR_PACIENTES` (ou `ACESSO_TOTAL`) | todos os pacientes |
| só `VISUALIZAR_PACIENTES_VINCULADOS` | apenas os pacientes com **vínculo ativo com ele** (assistente social; ver `ASSISTENTES-SOCIAIS.md`). A busca traz só esses, e `GET /api/pacientes/{id}` e `GET /api/pacientes/{id}/historico` de paciente não vinculado respondem `403` (também para ID inexistente) |

O filtro é sempre feito no back; o front não precisa filtrar nada.

---

## `GET /api/pacientes` — buscar pacientes

Não existe listagem completa de pacientes: **só se encontra um paciente buscando por ele**. Sem paginação: no máximo **20** resultados, ordenados por nome. Pacientes inativos também aparecem (confira `statusCadastro`).

**Query params**

| Param | Obrigatório | Descrição |
|---|---|---|
| `termo` | sim, exceto com `idEscola` | só dígitos (ignorando `.` e `-`): busca por **CPF exato**, exige os 11 dígitos. Qualquer outro texto: busca por **nome**, por trecho, sem diferenciar acento nem maiúsculas, com **ao menos 3 caracteres** |
| `idEscola` | não | só pacientes desta escola (ativos e inativos); é assim que a tela da escola lista os pacientes dela. Com `idEscola`, o `termo` é opcional (e, se enviado, também filtra). Escola inexistente não é erro: a lista vem vazia |

`page`, `size` e `sort` não existem nesta rota (são ignorados).

**Exemplo**

```
GET /api/pacientes?termo=lucas
GET /api/pacientes?termo=Lúcás           (encontra "Lucas")
GET /api/pacientes?termo=456.789.012-49
GET /api/pacientes?idEscola=3
GET /api/pacientes?idEscola=3&termo=lucas
```

**Resposta `200`** (lista simples, sem o objeto `page`):

```json
[
  {
    "idCadastro": 1,
    "nome": "Lucas Almeida Souza",
    "cpfMascarado": "***.789.012-**",
    "statusCadastro": "ATIVO"
  }
]
```

- O CPF completo **não** sai nesta rota: `cpfMascarado` mostra só os dígitos do meio. Para os dados completos, use `GET /api/pacientes/{idCadastro}`.
- Nenhum resultado: `[]`.

| Situação | Status | Mensagem |
|---|---|---|
| Sem `termo` e sem `idEscola` | `400` | O parâmetro 'termo' é obrigatório. |
| `termo` só com dígitos, mas sem 11 | `400` | A busca por CPF exige os 11 dígitos. |
| `termo` de nome com menos de 3 caracteres | `400` | A busca por nome exige ao menos 3 caracteres. |
| `idEscola` zero ou negativo | `400` | Valor inválido para 'idEscola': {valor} |

---

## `GET /api/pacientes/{idCadastro}` — detalhes do paciente

Retorna dados pessoais, endereços, escola, CIDs, prontuário, responsáveis com o grau de parentesco e os assistentes sociais vinculados. É a base para montar o formulário de edição.

**Resposta `200`**

```json
{
  "pessoa": {
    "idCadastro": 1,
    "nome": "Lucas Almeida Souza",
    "cpf": "45678901249",
    "dataNascimento": "2015-03-21",
    "sexo": "M",
    "celular": "15988887777",
    "numCNS": null,
    "estadoCivil": "SOLTEIRO",
    "statusCadastro": "ATIVO",
    "enderecos": [
      {
        "cep": "18010000",
        "rua": "Rua das Flores",
        "numero": "120",
        "complemento": "Casa 2",
        "bairro": "Centro",
        "cidade": "Sorocaba",
        "estado": "SP"
      }
    ]
  },
  "statusPaciente": "MATRICULADO",
  "dataCadastro": "2026-10-08",
  "convenio": false,
  "escola": {
    "idEscola": 3,
    "nome": "Escola Municipal Maria Silva",
    "tipoEscola": "PUBLICA",
    "telefone": "1533330000"
  },
  "cids": [
    { "codigoCID": "F84.0", "descricao": "Autismo infantil" }
  ],
  "prontuario": {
    "alergias": "Nenhuma conhecida",
    "comorbidade": "Rinite alérgica",
    "mobilidade": "Anda sem auxílio"
  },
  "responsaveis": [
    {
      "responsavel": {
        "idCadastro": 2,
        "nome": "Maria Almeida Souza",
        "cpf": "56789012303",
        "dataNascimento": "1988-07-02",
        "sexo": "F",
        "celular": "15999990000",
        "numCNS": null,
        "estadoCivil": "CASADO",
        "statusCadastro": "ATIVO",
        "enderecos": []
      },
      "ocupacao": "Professora",
      "grauParentesco": "MAE"
    }
  ],
  "assistentesSociais": [
    { "idCadastro": 20, "nome": "Paula Ribeiro" }
  ]
}
```

- `escola` e `prontuario` podem vir `null`.
- O `idCadastro` de cada `responsavel` é o valor usado como `idResponsavel` no PATCH do paciente e nas rotas `/api/responsaveis/{idResponsavel}`.
- `assistentesSociais`: vínculos **ativos**, ordenados pelo nome (`[]` quando não há). Para vincular ou desvincular a partir da tela do paciente, use as rotas do assistente social: `POST /api/assistentes-sociais/{idCadastro}/pacientes` e `DELETE /api/assistentes-sociais/{idCadastro}/pacientes/{idPaciente}` (ver `ASSISTENTES-SOCIAIS.md`).
- Consultar esta rota gera um registro `CONSULTA` na auditoria (acesso a dados de saúde).

**Resposta `404`** (paciente não existe): corpo de erro com `message` = `Paciente não encontrado com ID: {id}`. **`403`** para quem só vê os pacientes vinculados e não tem vínculo com este (ver "Escopo de acesso").

---

## `GET /api/pacientes/{idCadastro}/historico` — histórico de atividades do paciente

Linha do tempo exibida na tela do paciente: cadastro, mudanças de status, responsáveis e terapias.

> **Não é a auditoria** (ver `AUDITORIA.md`). A auditoria registra toda requisição (quem, quando, rota, status) para fins de segurança. O histórico registra **eventos de negócio** com descrição legível, e:
> - é gravado na **mesma transação** da operação: se ela falhar (ex.: `400`), o evento não fica gravado;
> - **nunca** guarda dado de saúde (CIDs, prontuário, alergias) nem textos livres digitados pelos usuários (justificativa de falta e motivo de cancelamento), que podem conter esse tipo de dado. Alterações nesses campos aparecem só como "informações clínicas";
> - nunca cita, no histórico de um paciente, os nomes dos outros pacientes de uma terapia coletiva;
> - começa a partir desta versão da API: operações anteriores não têm evento (sem carga retroativa).

**Query params (todos opcionais)**

| Param | Padrão | Descrição |
|---|---|---|
| `tipo` | todos | um ou mais tipos de evento: `?tipo=TERAPIA_AGENDADA&tipo=TERAPIA_CANCELADA` |
| `inicio` | — | data inicial (`yyyy-MM-dd`, horário de Brasília) em que o **evento** aconteceu |
| `fim` | — | data final, **dia inteiro incluído** |
| `page` | `0` | número da página |
| `size` | `20` | itens por página |
| `sort` | `dataHora,desc` | mais recentes primeiro; eventos da mesma operação vêm na ordem inversa à de gravação |

> `inicio` e `fim` filtram pelo dia em que o evento foi registrado, não pelo dia da terapia. Para as terapias de um período, use `GET /api/terapias?idPaciente=`.

**Resposta `200`**

```json
{
  "content": [
    {
      "idEvento": 15,
      "dataHora": "2026-10-13T17:32:10Z",
      "tipo": "TERAPIA_AGENDADA",
      "descricao": "Terapia agendada para 27/10/2026, das 09:00 às 10:00, com Ana Souza.",
      "idTerapia": 3,
      "usuario": { "idCadastro": 1, "nome": "Administrador do Sistema" }
    },
    {
      "idEvento": 12,
      "dataHora": "2026-10-13T14:05:41Z",
      "tipo": "STATUS_PACIENTE_ALTERADO",
      "descricao": "Status do paciente alterado de Matriculado para Alta.",
      "idTerapia": null,
      "usuario": { "idCadastro": 1, "nome": "Administrador do Sistema" }
    }
  ],
  "page": { "size": 20, "number": 0, "totalElements": 2, "totalPages": 1 }
}
```

- `dataHora` vem em UTC (o `Z` no final); converta para o horário local na tela.
- `idTerapia` vem preenchido só nos eventos de terapia (use-o para abrir `GET /api/terapias/{idTerapia}`); nos demais, `null`.
- `usuario` é quem fez a ação; `null` se não houver usuário identificado ou se o cadastro dessa pessoa tiver sido excluído.

**Tipos de evento**

| `tipo` | Quando | Exemplo de `descricao` |
|---|---|---|
| `CADASTRO` | `POST /api/pacientes` | Paciente cadastrado. |
| `DADOS_ALTERADOS` | `PATCH` que **muda de fato** algum dado (reenviar o mesmo valor não gera evento) | Dados do cadastro alterados: nome, endereços e informações clínicas. |
| `STATUS_PACIENTE_ALTERADO` | `statusPaciente` mudou (no `PATCH` ou na inativação) | Status do paciente alterado de Matriculado para Alta. |
| `INATIVADO` | `DELETE` ou `PATCH` com `statusCadastro: INATIVO` | Cadastro inativado. |
| `REATIVADO` | `PATCH` com `statusCadastro: ATIVO` em paciente inativo | Cadastro reativado. |
| `RESPONSAVEL_VINCULADO` | responsável incluído no cadastro, no `PATCH` ou em `POST /responsaveis` | Responsável Maria Almeida Souza (mãe) vinculado. |
| `RESPONSAVEL_DESVINCULADO` | responsável retirado no `PATCH` | Responsável Carlos Souza (pai) desvinculado. |
| `TERAPIA_AGENDADA` | terapia cadastrada, ou paciente incluído numa terapia existente | Terapia agendada para 27/10/2026, das 09:00 às 10:00, com Ana Souza. / Paciente incluído na terapia de 28/10/2026, das 10:00 às 11:00, com Ana Souza. |
| `TERAPIA_ALTERADA` | terapia editada (remarcada, especialistas ou pacientes trocados), ou paciente retirado dela | Terapia de 27/10/2026, das 09:00 às 10:00, alterada: remarcada para 28/10/2026, das 10:00 às 11:00. / Paciente retirado da terapia de 27/10/2026, das 09:00 às 10:00, com Ana Souza. |
| `TERAPIA_CANCELADA` | terapia cancelada (`PATCH /status`) | Terapia de 27/10/2026, das 09:00 às 10:00, com Ana Souza, cancelada. |
| `PRESENCA_REGISTRADA` | presença ou falta registrada (só quando o valor muda) | Presença registrada na terapia de 27/10/2026, das 09:00 às 10:00: falta justificada. |

- Em terapia **coletiva**, os eventos de terapia entram no histórico de **cada** paciente envolvido. `PRESENCA_REGISTRADA` entra só no do paciente marcado.
- Iniciar e concluir o atendimento (`EM_ATENDIMENTO`, `CONCLUIDA`) não geram evento.
- Na descrição de `DADOS_ALTERADOS`, os campos se chamam: nome, celular, CNS, estado civil, endereços, convênio, escola, informações clínicas (CIDs e prontuário) e "parentesco de {responsável}".

**Erros**

| Situação | Status | Mensagem |
|---|---|---|
| Paciente inexistente | `404` | Paciente não encontrado com ID: {id} |
| Usuário com só `VISUALIZAR_PACIENTES_VINCULADOS` e paciente não vinculado (ou inexistente) | `403` | Você não tem permissão para realizar esta ação. |
| `tipo` inválido | `400` | Valor inválido para 'tipo': {valor} |
| Data em formato errado | `400` | Valor inválido para 'inicio': {valor} |
| `fim` antes de `inicio` | `400` | A data final deve ser igual ou posterior à data inicial. |

O histórico é uma consulta comum: **não** gera registro na auditoria.

---

## `POST /api/pacientes` — cadastrar paciente

Cadastra o paciente junto com prontuário, endereços, responsáveis e CIDs, em uma única requisição.

### Campos do paciente

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `nome` | string | sim | |
| `cpf` | string | sim | 11 dígitos; não pode já existir no sistema |
| `dataNascimento` | data | sim | `yyyy-MM-dd` |
| `sexo` | string (1 caractere) | sim | `F`, `M` ou `O` |
| `estadoCivil` | enum | sim | ver `00-GERAL.md` |
| `celular` | string | não | |
| `numCNS` | string | não | |
| `enderecos` | lista de endereço | **sim, ao menos 1** | ver abaixo |
| `idEscola` | número | não | se enviado, deve ser positivo e a escola precisa existir (ver `ESCOLAS.md`) |
| `convenio` | boolean | não | |
| `codigosCid` | lista de string | não | códigos de CIDs **que já existem** (ver `CIDS.md`) |
| `novosCids` | lista de `{codigoCID, descricao}` | não | CIDs **novos**, cadastrados junto com o paciente (ver `CIDS.md`) |
| `prontuario` | objeto | **sim** | os campos internos podem ser vazios, mas o objeto é obrigatório |
| `responsaveis` | lista de responsável | **sim, ao menos 1** | ver abaixo |

> `statusPaciente` e `dataCadastro` **não são enviados**: o back define `MATRICULADO` e a data do dia.

### Endereço (`enderecos[]`)

| Campo | Obrigatório |
|---|---|
| `cep` | sim |
| `rua` | sim |
| `numero` | sim |
| `complemento` | não |
| `bairro` | sim |
| `cidade` | sim |
| `estado` | sim |

### Prontuário (`prontuario`)

| Campo | Obrigatório |
|---|---|
| `alergias` | não |
| `comorbidade` | não |
| `mobilidade` | não |

### Responsável (`responsaveis[]`)

Um responsável é uma pessoa: exige os mesmos dados básicos do paciente (`nome`, `cpf`, `dataNascimento`, `sexo`, `estadoCivil`), além de:

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `grauParentesco` | enum | sim | `PAI`, `MAE`, `AVO`, `TUTOR_LEGAL`, `OUTRO` |
| `naoResideComPaciente` | boolean | sim | ver regra de endereço |
| `ocupacao` | string | não | |
| `celular` | string | não | |
| `numCNS` | string | não | |
| `enderecos` | lista de endereço | só se `naoResideComPaciente = true` | |
| `idResponsavel` | número | não | para selecionar um responsável já cadastrado |

**Regras dos responsáveis**

- **Endereço:** com `naoResideComPaciente = false`, o responsável recebe os endereços do paciente (qualquer `enderecos` enviado para ele é ignorado). Com `true`, `enderecos` é obrigatório (ao menos 1).
- **Reaproveitamento por CPF:** se já existir um responsável com o mesmo CPF, ele é reaproveitado e os dados enviados **não alteram** o cadastro existente, inclusive os endereços (os endereços do paciente ou os enviados só são associados a responsáveis criados nesta requisição). Para alterar dados de um responsável existente, use `PATCH /api/responsaveis/{idResponsavel}`. Os campos obrigatórios de pessoa precisam ser enviados mesmo assim, pois a validação ocorre antes.
- **`idResponsavel`:** se enviado, o responsável precisa existir e o CPF informado precisa ser o dele.
- **CPF de quem não é responsável:** se o CPF pertencer a alguém já cadastrado que não é responsável (por exemplo, um paciente ou profissional), o cadastro é recusado.
- **Duplicidade:** o mesmo CPF não pode aparecer duas vezes em `responsaveis` na mesma requisição.

### Exemplo de requisição

```json
{
  "nome": "Lucas Almeida Souza",
  "cpf": "45678901249",
  "dataNascimento": "2015-03-21",
  "sexo": "M",
  "estadoCivil": "SOLTEIRO",
  "celular": "15988887777",
  "enderecos": [
    {
      "cep": "18010000",
      "rua": "Rua das Flores",
      "numero": "120",
      "complemento": "Casa 2",
      "bairro": "Centro",
      "cidade": "Sorocaba",
      "estado": "SP"
    }
  ],
  "idEscola": 3,
  "convenio": false,
  "codigosCid": [],
  "novosCids": [
    { "codigoCID": "F84.0", "descricao": "Autismo infantil" }
  ],
  "prontuario": {
    "alergias": "Nenhuma conhecida",
    "comorbidade": "Rinite alérgica",
    "mobilidade": "Anda sem auxílio"
  },
  "responsaveis": [
    {
      "nome": "Maria Almeida Souza",
      "cpf": "56789012303",
      "dataNascimento": "1988-07-02",
      "sexo": "F",
      "estadoCivil": "CASADO",
      "celular": "15999990000",
      "ocupacao": "Professora",
      "grauParentesco": "MAE",
      "naoResideComPaciente": false
    },
    {
      "nome": "Carlos Souza",
      "cpf": "67890123469",
      "dataNascimento": "1985-01-15",
      "sexo": "M",
      "estadoCivil": "DIVORCIADO",
      "grauParentesco": "PAI",
      "naoResideComPaciente": true,
      "enderecos": [
        {
          "cep": "18020000",
          "rua": "Av. Principal",
          "numero": "45",
          "bairro": "Jardim Novo",
          "cidade": "Sorocaba",
          "estado": "SP"
        }
      ]
    }
  ]
}
```

> O `idEscola: 3` é ilustrativo: use um ID retornado por `GET /api/escolas/sugestoes?nome=` ou `POST /api/escolas`, ou remova o campo.

### Resposta `201 Created`

```json
{
  "idCadastro": 1,
  "nome": "Lucas Almeida Souza",
  "cpf": "45678901249",
  "statusPaciente": "MATRICULADO",
  "dataCadastro": "2026-10-08"
}
```

---

## `PATCH /api/pacientes/{idCadastro}` — atualizar paciente

Segue as regras comuns de PATCH (`00-GERAL.md`): envie só o que quer alterar; listas substituem por completo.

### Campos aceitos

| Campo | Observação |
|---|---|
| `nome` | não pode ser vazio nem `null` |
| `celular`, `numCNS` | `null` limpa |
| `estadoCivil` | não pode ser `null` |
| `statusCadastro` | `ATIVO` ou `INATIVO`; não pode ser `null`. Permite **reativar** um paciente inativado |
| `statusPaciente` | `MATRICULADO`, `ALTA` ou `DESLIGADO`; não pode ser `null` |
| `convenio` | `null` limpa |
| `idEscola` | troca a escola; `null` remove a escola do paciente |
| `enderecos` | substitui a lista; `[]` remove todos |
| `codigosCid` | substitui a lista; aceita **somente CIDs já existentes**; `[]` remove todos |
| `novosCids` | lista de `{codigoCID, descricao}`; CIDs **novos**, cadastrados na base e vinculados ao paciente. Somados a `codigosCid` (ou aos CIDs atuais, se `codigosCid` não for enviado) |
| `prontuario` | atualiza só os campos enviados dentro do objeto; `"prontuario": null` remove o prontuário |
| `responsaveis` | substitui os vínculos; lista de `{ idResponsavel, grauParentesco }` (ver abaixo) |

**Ignorados (somente leitura):** `cpf`, `dataNascimento`, `sexo`, `dataCadastro`.

> **CIDs no PATCH:** funciona como no cadastro. CIDs que já existem vão em `codigosCid`; CIDs que ainda não existem na base vão em `novosCids` e são cadastrados junto. Ver `CIDS.md`.

### Responsáveis no PATCH

Cada item referencia um responsável **já cadastrado** pelo ID:

```json
"responsaveis": [
  { "idResponsavel": 2, "grauParentesco": "MAE" },
  { "idResponsavel": 3, "grauParentesco": "PAI" }
]
```

- A lista precisa ter ao menos um responsável e não pode repetir o mesmo `idResponsavel`.
- Responsável já vinculado: só o `grauParentesco` é atualizado. Responsável não vinculado: passa a ser vinculado.
- Responsável que estava vinculado e não veio na lista é **desvinculado**. Se ele não tiver mais nenhum paciente e não tiver usuário de acesso, o cadastro dele é **excluído** da base (não apenas inativado).
- O PATCH do paciente **não altera dados pessoais** do responsável; para isso, use `PATCH /api/responsaveis/{idResponsavel}`.
- Para **adicionar** um responsável (novo ou já cadastrado) sem reenviar a lista inteira, use `POST /api/pacientes/{idCadastro}/responsaveis`.

### Exemplo

```json
{
  "nome": "Lucas Almeida",
  "convenio": true,
  "codigosCid": ["F84.0"],
  "prontuario": {
    "alergias": null
  },
  "responsaveis": [
    { "idResponsavel": 2, "grauParentesco": "MAE" }
  ]
}
```

**Resposta `200`:** os detalhes atualizados do paciente (mesmo formato do `GET /api/pacientes/{idCadastro}`).

---

## `POST /api/pacientes/{idCadastro}/responsaveis` — adicionar responsável ao paciente

Cadastra um responsável e o vincula a um paciente já existente, sem mexer nos demais responsáveis. Na tela de detalhes do paciente, corresponde ao botão **"Adicionar responsável"** (pop-up com o formulário).

**Corpo:** um único responsável, com **os mesmos campos e regras** de um item de `responsaveis[]` do `POST /api/pacientes` (ver "Responsável" acima): reaproveitamento por CPF, `idResponsavel` opcional, `naoResideComPaciente` e endereços.

- Com `naoResideComPaciente = false`, o responsável novo recebe os endereços **atuais** do paciente.
- Se o CPF (ou `idResponsavel`) for de um responsável já cadastrado, ele é apenas vinculado, sem alterar os dados dele.
- Se o responsável já estiver vinculado a este paciente, o pedido é recusado.

```json
{
  "nome": "Ana Souza",
  "cpf": "78901234505",
  "dataNascimento": "1960-11-30",
  "sexo": "F",
  "estadoCivil": "VIUVO",
  "celular": "15997776666",
  "ocupacao": "Aposentada",
  "grauParentesco": "AVO",
  "naoResideComPaciente": false
}
```

**Resposta `201 Created`:** os detalhes atualizados do paciente (mesmo formato do `GET /api/pacientes/{idCadastro}`), já com o novo responsável em `responsaveis`.

**Erros:** os mesmos de validação e de regra de negócio dos responsáveis no `POST /api/pacientes` (ver "Mensagens de erro"), mais:

| Situação | Status | Mensagem |
|---|---|---|
| Paciente inexistente | `404` | Paciente não encontrado com ID: {id} |
| Responsável já vinculado a este paciente | `400` | Este responsável já está vinculado ao paciente. |

---

## `DELETE /api/pacientes/{idCadastro}` — inativar paciente

Não apaga o registro nem vínculos e histórico.

- `statusCadastro` passa a `INATIVO`.
- `statusPaciente` passa a `DESLIGADO`; se o paciente já estiver em `ALTA`, esse status é **preservado**.
- **Terapias futuras**, na mesma transação (se algo falhar, nada é gravado). Só contam as terapias `AGENDADA` que ainda não começaram (data e hora de início depois de agora, no fuso de São Paulo):
  1. **individuais** (o paciente é o único paciente): passam a `CANCELADA`, com `motivoCancelamento` = `Paciente inativado`;
  2. **coletivas**: o paciente é **retirado** e a terapia continua para os demais. A modalidade é recalculada: se sobrar 1 paciente, ela vira `INDIVIDUAL`.
  3. Terapias passadas, em atendimento, concluídas ou canceladas **não** são tocadas.
- Os vínculos com assistentes sociais **não** são alterados.
- As mudanças nas terapias geram os eventos de sempre no histórico (`TERAPIA_CANCELADA`, e `TERAPIA_ALTERADA` para quem sai e para quem continua na coletiva).
- Paciente inexistente: `404`.

**Resposta `200`** (antes era `204`):

```json
{
  "terapiasCanceladas": 3,
  "terapiasColetivasRemovido": 2
}
```

| Campo | Significado |
|---|---|
| `terapiasCanceladas` | terapias individuais canceladas |
| `terapiasColetivasRemovido` | terapias coletivas de que o paciente foi retirado |

Para reativar, use o PATCH com `"statusCadastro": "ATIVO"` (e, se for o caso, `"statusPaciente": "MATRICULADO"`). A reativação **não** devolve o paciente às terapias canceladas ou de que foi retirado.

---

## `GET /api/responsaveis` — buscar responsáveis (paginado)

Serve para encontrar um responsável **já cadastrado**, por exemplo no pop-up "Adicionar responsável": o usuário digita o CPF, o front busca aqui e, se encontrar, preenche o formulário com os dados de `GET /api/responsaveis/{idResponsavel}` e envia o `idCadastro` como `idResponsavel` em `POST /api/pacientes/{idCadastro}/responsaveis`. Os campos obrigatórios de pessoa continuam sendo exigidos nesse POST, mas os dados do responsável existente não são alterados.

**Query params (todos opcionais):** `cpf`, `nome`, `page`, `size` (padrão `9`) e `sort` (padrão `nome,asc`), com as mesmas regras da listagem de pacientes. Responsáveis inativos também aparecem.

```
GET /api/responsaveis?cpf=56789012303
```

**Resposta `200`**

```json
{
  "content": [
    {
      "idCadastro": 2,
      "nome": "Maria Almeida Souza",
      "cpf": "56789012303",
      "celular": "15999990000",
      "ocupacao": "Professora",
      "statusCadastro": "ATIVO"
    }
  ],
  "page": {
    "size": 9,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## `GET /api/responsaveis/{idResponsavel}` — detalhes do responsável

**Resposta `200`**

```json
{
  "pessoa": {
    "idCadastro": 2,
    "nome": "Maria Almeida Souza",
    "cpf": "56789012303",
    "dataNascimento": "1988-07-02",
    "sexo": "F",
    "celular": "15999990000",
    "numCNS": null,
    "estadoCivil": "CASADO",
    "statusCadastro": "ATIVO",
    "enderecos": []
  },
  "ocupacao": "Professora"
}
```

**Resposta `404`:** `Responsável não encontrado com ID: {id}`.

---

## `PATCH /api/responsaveis/{idResponsavel}` — atualizar responsável

Atualiza os dados pessoais do responsável (o vínculo com o paciente e o grau de parentesco são alterados no PATCH do paciente).

| Campo | Observação |
|---|---|
| `nome` | não pode ser vazio nem `null` |
| `celular`, `numCNS`, `ocupacao` | `null` limpa |
| `estadoCivil` | não pode ser `null` |
| `statusCadastro` | `ATIVO` ou `INATIVO`; não pode ser `null` |
| `enderecos` | substitui a lista; `[]` remove todos |

**Ignorados:** `cpf`, `dataNascimento`, `sexo`.

```json
{
  "celular": "15999991111",
  "ocupacao": "Coordenadora pedagógica"
}
```

**Resposta `200`:** mesmo formato do `GET /api/responsaveis/{idResponsavel}`.

> Os endereços do responsável são independentes dos do paciente depois do cadastro: alterar o endereço de um não altera o do outro.

---

## `DELETE /api/responsaveis/{idResponsavel}` — inativar responsável

Define `statusCadastro` como `INATIVO` e retorna `204 No Content`. O responsável **continua vinculado** aos pacientes; para desvinculá-lo, use o PATCH do paciente.

---

## Mensagens de erro

### Validação de campos (`400`, em `fields`)

| Campo | Mensagem |
|---|---|
| `nome` | O nome é obrigatório |
| `cpf` | O CPF é obrigatório / O CPF deve conter 11 dígitos numéricos / CPF inválido (dígitos verificadores errados ou sequência repetida) |
| `dataNascimento` | A data de nascimento é obrigatória |
| `sexo` | O sexo é obrigatório / O sexo deve ser F, M ou O |
| `estadoCivil` | O estado civil é obrigatório |
| `enderecos` (paciente) | Informe ao menos um endereço para o paciente |
| endereço | O CEP é obrigatório / A rua é obrigatório / O número é obrigatório / O bairro é obrigatório / A cidade é obrigatória / O estado é obrigatório |
| `idEscola` | O ID da escola deve ser positivo |
| `codigosCid[]` | O código CID não pode ser vazio |
| `novosCids[]` | O código CID é obrigatório / A descrição do CID é obrigatória |
| `prontuario` | Os dados do prontuário são obrigatórios |
| `responsaveis` | Informe ao menos um responsável |
| responsável | O grau de parentesco é obrigatório / Informe se o responsável não reside com o paciente |
| responsável | Informe ao menos um endereço para o responsável que não reside com o paciente |
| `idResponsavel` | O ID do responsável deve ser positivo |

Os nomes em `fields` seguem o caminho do campo, por exemplo `responsaveis[1].enderecos` ou `enderecos[0].cep`.

No PATCH, `responsaveis[].idResponsavel` e `responsaveis[].grauParentesco` ausentes geram a mensagem padrão do validador (por exemplo, `must not be null`).

### Regras de negócio (`message`)

| Situação | Status | Mensagem |
|---|---|---|
| CPF do paciente já cadastrado | `400` | CPF já cadastrado. |
| Responsável já vinculado ao paciente (`POST /responsaveis`) | `400` | Este responsável já está vinculado ao paciente. |
| Mesmo CPF repetido em `responsaveis` (POST) | `400` | O mesmo responsável foi informado mais de uma vez |
| Mesmo `idResponsavel` repetido (PATCH) | `400` | O mesmo responsável foi informado mais de uma vez. |
| `responsaveis` vazio ou `null` no PATCH | `400` | O paciente deve possuir ao menos um responsável. |
| `idEscola` inexistente | `400` | Escola não encontrada com ID: {id} |
| CID inexistente em `codigosCid` | `400` | CID(s) não encontrado(s): {códigos} |
| `idResponsavel` inexistente (POST) | `400` | Responsável não encontrado com ID: {id} |
| `idResponsavel` inexistente (PATCH) | `400` | Responsável não encontrado com ID: {id} |
| CPF não corresponde ao `idResponsavel` | `400` | O CPF informado não corresponde ao responsável selecionado |
| CPF pertence a pessoa que não é responsável | `400` | O CPF informado pertence a uma pessoa que não está cadastrada como responsável |
| `nome` vazio no PATCH | `400` | O nome não pode ser vazio. |
| `estadoCivil` `null` no PATCH | `400` | O estado civil não pode ser nulo. |
| `statusCadastro` `null` no PATCH | `400` | O status do cadastro não pode ser nulo. |
| `statusPaciente` `null` no PATCH | `400` | O status do paciente não pode ser nulo. |
| Paciente inexistente | `404` | Paciente não encontrado com ID: {id} |

Erros do histórico: ver a seção `GET /api/pacientes/{idCadastro}/historico`.

---

## Roteiro rápido de testes

1. `POST /api/pacientes` com o exemplo acima (ajuste ou remova `idEscola`). Esperado: `201`.
2. `GET /api/pacientes`. Esperado: o paciente com `idade`, `responsaveis` e `cids`, e o objeto `page`.
3. `GET /api/pacientes/{id}`. Esperado: detalhes com os dois responsáveis; anotar os `idCadastro` deles.
4. Repetir o passo 1 com o mesmo CPF do paciente. Esperado: `400` (CPF já cadastrado).
5. `PATCH /api/pacientes/{id}` com `{"nome": "Lucas Almeida"}`. Esperado: `200` e só o nome alterado.
6. `PATCH /api/pacientes/{id}` com `responsaveis` contendo só a mãe. Esperado: `200`; o pai é desvinculado e, sem outros vínculos, `GET /api/responsaveis/{idPai}` passa a dar `404`.
7. `PATCH /api/pacientes/{id}` com `"responsaveis": []`. Esperado: `400`.
8. `PATCH /api/responsaveis/{idMae}` com `{"ocupacao": "Coordenadora"}`. Esperado: `200`.
9. `DELETE /api/pacientes/{id}`. Esperado: `204`; o GET mostra `statusCadastro: "INATIVO"` e `statusPaciente: "DESLIGADO"`.
10. `PATCH /api/pacientes/{id}` com `{"statusCadastro": "ATIVO", "statusPaciente": "MATRICULADO"}`. Esperado: `200`, paciente reativado.
11. `POST /api/pacientes/{id}/responsaveis` com o exemplo da avó. Esperado: `201` e três responsáveis no detalhe; a avó com os endereços do paciente.
12. Repetir o passo 11. Esperado: `400` com `Este responsável já está vinculado ao paciente.`
13. `GET /api/pacientes?nome=LUC`. Esperado: o paciente aparece (busca sem diferenciar maiúsculas).
14. `GET /api/responsaveis?cpf=567.890`. Esperado: a mãe aparece (máscara ignorada).

**Histórico** (do paciente `{id}` dos passos acima)

15. `GET /api/pacientes/{id}/historico`. Esperado: `page.size: 20`, o mais recente primeiro: `RESPONSAVEL_VINCULADO` (avó, passo 11), `STATUS_PACIENTE_ALTERADO` e `REATIVADO` (passo 10), `STATUS_PACIENTE_ALTERADO` e `INATIVADO` (passo 9), `RESPONSAVEL_DESVINCULADO` (pai, passo 6), `DADOS_ALTERADOS` com "nome" (passo 5), dois `RESPONSAVEL_VINCULADO` e `CADASTRO` (passo 1). Os passos 4, 7 e 12 (recusados) não geram evento. `usuario.nome` = "Administrador do Sistema".
16. `PATCH /api/pacientes/{id}` com `{"prontuario": {"alergias": "Dipirona"}}` e depois o histórico. Esperado: `DADOS_ALTERADOS` com "informações clínicas", sem citar "Dipirona".
17. Agendar uma terapia para o paciente (ver `TERAPIAS.md`) e `GET /api/pacientes/{id}/historico?tipo=TERAPIA_AGENDADA`. Esperado: um evento, com `idTerapia` preenchido.
18. `GET /api/pacientes/{id}/historico?inicio=2026-10-10&fim=2026-10-01`. Esperado: `400`. `GET /api/pacientes/999999/historico`: `404`.

**Filtro por escola**

19. `GET /api/pacientes?idEscola={idEscola}` com a escola do passo 1. Esperado: o paciente. Com outra escola (sem pacientes): `[]`. Com `idEscola=0`: `400`.

**Busca**

20. `GET /api/pacientes` (sem parâmetros). Esperado: `400` `O parâmetro 'termo' é obrigatório.`
21. `GET /api/pacientes?termo=lu`. Esperado: `400` (menos de 3 caracteres). `GET /api/pacientes?termo=4567890`: `400` (CPF incompleto).
22. `GET /api/pacientes?termo=lúcas` e `?termo=456.789.012-49`. Esperado: o paciente, com `cpfMascarado: "***.789.012-**"` e sem o CPF completo.

**Inativação com terapias**

23. Agende para o paciente uma terapia individual e uma coletiva (com outro paciente), ambas no futuro. `DELETE /api/pacientes/{id}`. Esperado: `200` com `{"terapiasCanceladas": 1, "terapiasColetivasRemovido": 1}`. A individual fica `CANCELADA` com `motivoCancelamento: "Paciente inativado"`; a coletiva continua `AGENDADA`, só com o outro paciente e `modalidade: "INDIVIDUAL"`.

**Permissões** (crie usuários nos perfis padrão; ver `00-GERAL.md`)

24. Com um usuário "Recepção": `GET /api/pacientes?termo=lucas`, `GET /api/pacientes/{id}/historico` e `PATCH /api/pacientes/{id}`. Esperado: `200`. `POST /api/pacientes` com o corpo do passo 1 (CPF novo) e `DELETE /api/pacientes/{id}`. Esperado: `403`.
25. Com um usuário "Especialista": `GET /api/pacientes?termo=lucas` e `GET /api/responsaveis`. Esperado: `200`. `PATCH /api/pacientes/{id}` e `PATCH /api/responsaveis/{idMae}`. Esperado: `403`.
26. Com um usuário "Assistente Social" (só `VISUALIZAR_PACIENTES_VINCULADOS`) sem vínculos: `GET /api/pacientes?termo=lucas`. Esperado: `[]`. `GET /api/pacientes/{id}` e `GET /api/pacientes/{id}/historico`: `403`. Vincule o paciente a ele (`POST /api/assistentes-sociais/{idAssistente}/pacientes`) e repita. Esperado: `200` nos três.
