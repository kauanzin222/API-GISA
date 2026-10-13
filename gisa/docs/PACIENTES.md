# API GISA — Pacientes e Responsáveis

Autenticação, convenções, enums, formato de erro e regras comuns de PATCH/DELETE: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/pacientes` | lista pacientes (paginado), com busca por CPF ou nome |
| `GET` | `/api/pacientes/{idCadastro}` | detalhes de um paciente |
| `POST` | `/api/pacientes` | cadastra paciente com prontuário, endereços, responsáveis e CIDs |
| `PATCH` | `/api/pacientes/{idCadastro}` | atualiza paciente |
| `POST` | `/api/pacientes/{idCadastro}/responsaveis` | cadastra um responsável e o vincula ao paciente |
| `DELETE` | `/api/pacientes/{idCadastro}` | inativa paciente |
| `GET` | `/api/responsaveis` | busca responsáveis por CPF ou nome (paginado) |
| `GET` | `/api/responsaveis/{idResponsavel}` | detalhes de um responsável |
| `PATCH` | `/api/responsaveis/{idResponsavel}` | atualiza dados pessoais do responsável |
| `DELETE` | `/api/responsaveis/{idResponsavel}` | inativa responsável |

> Escolas e CIDs usados no paciente têm documentos próprios: `ESCOLAS.md` e `CIDS.md`.

---

## `GET /api/pacientes` — listar pacientes (paginado)

Aceita busca por CPF e/ou nome, além de paginação e ordenação. Pacientes inativos também aparecem (confira `statusPaciente`).

**Query params (todos opcionais)**

| Param | Padrão | Descrição |
|---|---|---|
| `cpf` | — | trecho do CPF; a máscara é ignorada (`123.456` vira `123456`) |
| `nome` | — | trecho do nome, sem diferenciar maiúsculas de minúsculas |
| `page` | `0` | número da página (começa em 0) |
| `size` | `9` | itens por página |
| `sort` | `nome,asc` | ordenação, ex.: `sort=nome,desc`. Campos úteis: `nome`, `dataCadastro`, `statusPaciente` |

**Exemplo**

```
GET /api/pacientes?page=0&size=9&sort=nome,asc
GET /api/pacientes?nome=lucas
GET /api/pacientes?cpf=456789
```

Com `cpf` e `nome` juntos, o paciente precisa atender aos dois.

**Resposta `200`**

```json
{
  "content": [
    {
      "idCadastro": 1,
      "nome": "Lucas Almeida Souza",
      "idade": 11,
      "cpf": "45678901234",
      "responsaveis": [
        {
          "nome": "Maria Almeida Souza",
          "celular": "15999990000",
          "grauParentesco": "MAE"
        }
      ],
      "cids": [
        { "codigoCID": "F84.0", "descricao": "Autismo infantil" }
      ],
      "statusPaciente": "MATRICULADO"
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

- Os dados de paginação vêm dentro do objeto `page` (não no nível raiz).
- `idade` é calculada pelo back, em anos completos.
- `responsaveis` e `cids` vêm como `[]` quando não há itens. Os CIDs vêm ordenados pelo código.

---

## `GET /api/pacientes/{idCadastro}` — detalhes do paciente

Retorna dados pessoais, endereços, escola, CIDs, prontuário e responsáveis com o grau de parentesco. É a base para montar o formulário de edição.

**Resposta `200`**

```json
{
  "pessoa": {
    "idCadastro": 1,
    "nome": "Lucas Almeida Souza",
    "cpf": "45678901234",
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
        "cpf": "56789012345",
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
  ]
}
```

- `escola` e `prontuario` podem vir `null`.
- O `idCadastro` de cada `responsavel` é o valor usado como `idResponsavel` no PATCH do paciente e nas rotas `/api/responsaveis/{idResponsavel}`.

**Resposta `404`** (paciente não existe): corpo de erro com `message` = `Paciente não encontrado com ID: {id}`.

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
  "cpf": "45678901234",
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
      "cpf": "56789012345",
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
      "cpf": "67890123456",
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

> O `idEscola: 3` é ilustrativo: use um ID retornado por `GET /api/escolas?nome=` ou `POST /api/escolas`, ou remova o campo.

### Resposta `201 Created`

```json
{
  "idCadastro": 1,
  "nome": "Lucas Almeida Souza",
  "cpf": "45678901234",
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
| `prontuario` | atualiza só os campos enviados dentro do objeto; `"prontuario": null` remove o prontuário |
| `responsaveis` | substitui os vínculos; lista de `{ idResponsavel, grauParentesco }` (ver abaixo) |

**Ignorados (somente leitura):** `cpf`, `dataNascimento`, `sexo`, `dataCadastro`.

> **`novosCids` não existe no PATCH** e é ignorado se enviado. Para vincular um CID que ainda não existe na base, ele precisa ter sido cadastrado antes pelo `POST /api/pacientes` de algum paciente. Ver `CIDS.md`.

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
  "cpf": "78901234567",
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
- Retorna `204 No Content`. Paciente inexistente: `404`.

Para reativar, use o PATCH com `"statusCadastro": "ATIVO"` (e, se for o caso, `"statusPaciente": "MATRICULADO"`).

---

## `GET /api/responsaveis` — buscar responsáveis (paginado)

Serve para encontrar um responsável **já cadastrado**, por exemplo no pop-up "Adicionar responsável": o usuário digita o CPF, o front busca aqui e, se encontrar, preenche o formulário com os dados de `GET /api/responsaveis/{idResponsavel}` e envia o `idCadastro` como `idResponsavel` em `POST /api/pacientes/{idCadastro}/responsaveis`. Os campos obrigatórios de pessoa continuam sendo exigidos nesse POST, mas os dados do responsável existente não são alterados.

**Query params (todos opcionais):** `cpf`, `nome`, `page`, `size` (padrão `9`) e `sort` (padrão `nome,asc`), com as mesmas regras da listagem de pacientes. Responsáveis inativos também aparecem.

```
GET /api/responsaveis?cpf=56789012345
```

**Resposta `200`**

```json
{
  "content": [
    {
      "idCadastro": 2,
      "nome": "Maria Almeida Souza",
      "cpf": "56789012345",
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
    "cpf": "56789012345",
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
| `cpf` | O CPF é obrigatório / O CPF deve conter 11 dígitos numéricos |
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
| `idResponsavel` inexistente (PATCH) | `404` | Responsável não encontrado com ID: {id} |
| CPF não corresponde ao `idResponsavel` | `400` | O CPF informado não corresponde ao responsável selecionado |
| CPF pertence a pessoa que não é responsável | `400` | O CPF informado pertence a uma pessoa que não está cadastrada como responsável |
| `nome` vazio no PATCH | `400` | O nome não pode ser vazio. |
| `estadoCivil` `null` no PATCH | `400` | O estado civil não pode ser nulo. |
| `statusCadastro` `null` no PATCH | `400` | O status do cadastro não pode ser nulo. |
| `statusPaciente` `null` no PATCH | `400` | O status do paciente não pode ser nulo. |
| Paciente inexistente | `404` | Paciente não encontrado com ID: {id} |

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
