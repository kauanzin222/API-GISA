# API GISA — Pacientes, Escolas e CIDs

Payloads e regras dos endpoints de **Pacientes**, **Escolas** e **CIDs**.

> Este documento faz par com o `CADASTROS-API.md` (profissionais e especialistas). As seções "Informações gerais", "Autenticação", "Convenções" e "Padrão de respostas e erros" são iguais nos dois.

## Informações gerais

- **URL base:** `https://api-gisa.onrender.com`
- **Formato:** JSON (`Content-Type: application/json`)
- **Autenticação:** token JWT em todos os endpoints, exceto o login (ver abaixo)
- **Atenção (hospedagem):** se a API estiver em plano gratuito do Render, a primeira requisição após um período sem uso pode demorar (a instância "acorda"). Se a primeira chamada demorar ou der timeout, tente de novo antes de investigar.

---

## Autenticação

Todos os endpoints exigem token JWT. Primeiro faça o login, depois envie o token em cada requisição.

### `POST /api/auth/login`

Use as credenciais administrativas iniciais (**somente para testes**; não use em produção e não publique este arquivo em repositório aberto com a senha):

```json
{
  "cpf": "99999999999",
  "senha": "Admin@12345"
}
```

**Resposta `200`:** JSON contendo o token JWT.

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

> ⚠️ Conferir com o back: nome do campo do token na resposta e o formato do cabeçalho abaixo.

**Uso nas demais requisições:**

```
Authorization: Bearer <token>
```

**Encerrar a sessão:** basta remover o token da aplicação.

---

## Convenções

| Item | Regra |
|---|---|
| Datas | formato ISO `yyyy-MM-dd` (ex.: `"2015-03-21"`) |
| Horários | formato `HH:mm` (ex.: `"08:00"`) |
| CPF | exatamente 11 dígitos, **sem máscara** |
| CPF único | o CPF não pode coincidir com o de nenhuma pessoa já cadastrada no sistema. Ao testar, **não repita CPFs entre os exemplos** (os exemplos deste documento e do de profissionais já usam CPFs diferentes entre si). O CPF `99999999999` é do administrador inicial |
| Campos opcionais | podem ser omitidos ou enviados como `null` |

### Enums

| Campo | Valores aceitos |
|---|---|
| `estadoCivil` | `SOLTEIRO`, `CASADO`, `VIUVO`, `SEPARADO`, `DIVORCIADO` |
| `sexo` | um único caractere: `F`, `M` ou `O`; outros valores são rejeitados com `400` |
| `grauParentesco` | `PAI`, `MAE`, `AVO`, `TUTOR_LEGAL`, `OUTRO` |
| `tipoEscola` | `PUBLICA`, `PARTICULAR` |
| `statusPaciente` (somente resposta) | `MATRICULADO`, `ALTA`, `DESLIGADO` |

---

## Padrão de respostas e erros

### Status HTTP

| Status | Quando |
|---|---|
| `200 OK` | consulta realizada / login realizado |
| `201 Created` | cadastro realizado |
| `400 Bad Request` | validação de campos ou regra de negócio violada |
| `401` / `403` | token ausente, inválido ou expirado |
| `404 Not Found` | recurso não encontrado (usado em `GET /api/cids/{codigoCID}`) |

### Corpo de erro

```json
{
  "timestamp": "2026-09-22T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Erro de validacao dos dados.",
  "path": "/api/pacientes",
  "fields": {
    "cpf": "O CPF deve conter 11 dígitos numéricos"
  }
}
```

- Em erros de **validação de campos**, `fields` traz um item por campo inválido (nome do campo → mensagem). Exiba cada mensagem junto ao campo correspondente.
- Em erros de **regra de negócio** (ex.: CPF já cadastrado), o corpo segue o mesmo formato e a mensagem da regra vem em `message`.
- As listas de mensagens estão na seção 6.

---

## 1. Pacientes

### 1.1 `GET /api/pacientes` — listar pacientes (paginado)

Não há filtros; apenas paginação e ordenação.

**Query params (todos opcionais)**

| Param | Padrão | Descrição |
|---|---|---|
| `page` | `0` | número da página (começa em 0) |
| `size` | `10` | itens por página |
| `sort` | `nome,asc` | ordenação, ex.: `sort=nome,desc` |

**Exemplo**

```
GET /api/pacientes?page=0&size=10&sort=nome,asc
```

**Resposta `200`** (objeto `Page` do Spring; os campos principais são `content`, `totalElements`, `totalPages`, `number`, `size`, `first` e `last`; o Spring pode incluir outros campos de paginação, que podem ser ignorados)

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
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 10,
  "first": true,
  "last": true
}
```

Observações:
- `idade` é calculada pelo back (em anos completos).
- `responsaveis` e `cids` vêm como lista vazia (`[]`) quando não há itens.
- Os CIDs de cada paciente vêm ordenados pelo código.
- Para `sort`, use campos do paciente como `nome`, `dataCadastro` ou `statusPaciente`.

---

### 1.2 `POST /api/pacientes` — cadastrar paciente

Cadastra o paciente junto com prontuário, endereços, responsáveis e CIDs, tudo em uma única requisição.

#### Campos do paciente

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `nome` | string | sim | |
| `cpf` | string | sim | 11 dígitos numéricos; não pode já existir no sistema |
| `dataNascimento` | string (data) | sim | `yyyy-MM-dd` |
| `sexo` | string (1 caractere) | sim | `F`, `M` ou `O`; outros valores são rejeitados com `400` |
| `estadoCivil` | enum | sim | ver "Enums" |
| `celular` | string | não | |
| `numCNS` | string | não | |
| `enderecos` | lista de endereço | **sim, ao menos 1** | ver abaixo |
| `idEscola` | número | não | se enviado, deve ser positivo e a escola precisa existir |
| `convenio` | boolean | não | |
| `codigosCid` | lista de string | não | códigos de CIDs **que já existem** na API |
| `novosCids` | lista de `{codigoCID, descricao}` | não | CIDs **novos**, cadastrados junto com o paciente (ver seção 3) |
| `prontuario` | objeto | **sim** | pode ter todos os campos vazios, mas o objeto é obrigatório |
| `responsaveis` | lista de responsável | **sim, ao menos 1** | ver abaixo |

> O `statusPaciente` e a `dataCadastro` **não são enviados**: o back define `MATRICULADO` e a data do dia automaticamente.

#### Endereço (`enderecos`)

| Campo | Obrigatório |
|---|---|
| `cep` | sim |
| `rua` | sim |
| `numero` | sim |
| `complemento` | não |
| `bairro` | sim |
| `cidade` | sim |
| `estado` | sim |

#### Prontuário (`prontuario`)

| Campo | Obrigatório |
|---|---|
| `alergias` | não |
| `comorbidade` | não |
| `mobilidade` | não |

#### Responsável (`responsaveis[]`)

Um responsável é uma pessoa, então exige os mesmos dados básicos do paciente (`nome`, `cpf`, `dataNascimento`, `sexo`, `estadoCivil`), além de:

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `grauParentesco` | enum | sim | ver "Enums" |
| `naoResideComPaciente` | boolean | sim | ver regra abaixo |
| `ocupacao` | string | não | |
| `celular` | string | não | |
| `numCNS` | string | não | |
| `enderecos` | lista de endereço | só se `naoResideComPaciente = true` | |
| `idResponsavel` | número | não | uso opcional; ver regras de reaproveitamento |

**Regras dos responsáveis**

- **Endereço:** se `naoResideComPaciente` for `false`, o responsável usa automaticamente os endereços do paciente (qualquer `enderecos` enviado para ele é ignorado). Se for `true`, `enderecos` passa a ser obrigatório (ao menos 1).
- **Reaproveitamento:** se já existir um responsável com o mesmo CPF, ele é reaproveitado e os dados enviados **não atualizam** o cadastro existente. Mesmo assim, os campos obrigatórios de pessoa precisam ser enviados, pois a validação ocorre antes.
- **`idResponsavel`:** se enviado, o responsável precisa existir e o CPF informado precisa ser o mesmo dele.
- **CPF de pessoa que não é responsável:** se o CPF informado pertencer a alguém já cadastrado que não é responsável (por exemplo, o próprio paciente), o cadastro é recusado.
- **Duplicidade:** o mesmo CPF não pode aparecer duas vezes na lista `responsaveis` da mesma requisição.

#### Exemplo de requisição

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
  "codigosCid": ["F84.0"],
  "novosCids": [
    { "codigoCID": "Q90.9", "descricao": "Síndrome de Down, não especificada" }
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

#### Resposta `201 Created`

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

## 2. Escolas

### 2.1 `GET /api/escolas?nome=` — buscar escolas por nome (sugestões)

Não é uma listagem completa: serve para **autocomplete** ao digitar o nome da escola.

- Busca por **trecho** do nome, sem diferenciar maiúsculas de minúsculas.
- Retorna **no máximo 10** resultados, em ordem alfabética.
- O parâmetro `nome` é obrigatório: se não for enviado, a API responde `400`. Se for enviado em branco, a resposta é `[]`.

**Exemplo**

```
GET /api/escolas?nome=esc
```

**Resposta `200`**

```json
[
  {
    "idEscola": 3,
    "nome": "Escola Municipal Maria Silva",
    "tipoEscola": "PUBLICA",
    "telefone": "1533330000"
  }
]
```

O `idEscola` retornado é o valor a enviar no campo `idEscola` do `POST /api/pacientes`.

### 2.2 `POST /api/escolas` — cadastrar escola

| Campo | Tipo | Obrigatório |
|---|---|---|
| `nome` | string | sim |
| `tipoEscola` | enum (`PUBLICA` ou `PARTICULAR`) | sim |
| `telefone` | string | não |

**Requisição**

```json
{
  "nome": "Colégio Exemplo",
  "tipoEscola": "PARTICULAR",
  "telefone": "1533331111"
}
```

**Resposta `201 Created`**

```json
{
  "idEscola": 4,
  "nome": "Colégio Exemplo",
  "tipoEscola": "PARTICULAR",
  "telefone": "1533331111"
}
```

---

## 3. CIDs

### 3.1 `GET /api/cids/{codigoCID}` — buscar CID pelo código

**Exemplo**

```
GET /api/cids/F84.0
```

**Resposta `200`** (CID encontrado)

```json
{
  "codigoCID": "F84.0",
  "descricao": "Autismo infantil"
}
```

**Resposta `404`** (CID não encontrado): sem corpo. Significa que o CID ainda não existe na base da API e deve seguir o fluxo abaixo.

> **Importante:** a busca compara o código **exatamente como recebido**, sem ignorar maiúsculas/minúsculas nem formatação. `F84.0`, `f84.0` e `F840` são códigos diferentes. Padronize o código no front (sugestão: maiúsculas, sem espaços e sempre no mesmo formato) tanto ao buscar quanto ao cadastrar.

> **Não existe endpoint ou tela separada para cadastrar CID.** Todo CID novo é cadastrado pelo campo `novosCids` do `POST /api/pacientes`.

### 3.2 Fluxo de CID (busca e cadastro)

O front trabalha com o código CID digitado pelo usuário:

```
Usuário digita o código do CID
          │
          ▼
GET /api/cids/{codigo}
          │
   ┌──────┴───────┐
 200 (achou)     404 (não achou)
   │                  │
   │                  ▼
   │        Consulta a API pública de CID
   │        (a definir)
   │                  │
   │        ┌─────────┴──────────┐
   │     Encontrou           Não encontrou
   │        │                    │
   │        │            Código inválido: avisar o usuário
   │        ▼            e não adicionar à lista
   │   Usa a DESCRIÇÃO retornada pela API pública
   │   e adiciona o CID à lista "novosCids"
   │
   ▼
Adiciona o código à lista "codigosCid"

          │
          ▼
No POST /api/pacientes, enviar as duas listas
```

**Como a descrição é gravada**

Quando o CID não existe na API do GISA e é encontrado na API pública, a **descrição retornada pela API pública é usada no campo `descricao`** e enviada em `novosCids`. Ao receber o `POST /api/pacientes`, o back **cadastra e armazena esse CID** (código + descrição) e o vincula ao paciente. A partir daí, o CID passa a ser encontrado em `GET /api/cids/{codigo}` (retorna `200`).

> A API pública de CID ainda **não foi definida**. Quando for escolhida, este documento será atualizado com a URL e o formato da resposta.

**Exemplo de uso no front (JavaScript)**

```js
const BASE_URL = "https://api-gisa.onrender.com";

async function resolverCid(codigoDigitado, token) {
  const codigo = codigoDigitado.trim().toUpperCase();

  // 1. Busca na API do GISA (envia o token JWT)
  const resposta = await fetch(`${BASE_URL}/api/cids/${encodeURIComponent(codigo)}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (resposta.ok) {
    return { tipo: "existente", codigoCID: codigo };
  }

  // 2. Não achou (404): busca na API pública
  //    (a definir: trocar buscarCidNaApiPublica pela chamada real)
  const publico = await buscarCidNaApiPublica(codigo);
  if (!publico) {
    return { tipo: "invalido" };
  }

  // 3. Usa a descrição da API pública como "descricao" do novo CID
  return { tipo: "novo", codigoCID: codigo, descricao: publico.descricao };
}

// Ao montar o corpo do POST /api/pacientes:
//   existentes -> codigosCid: ["F84.0"]
//   novos      -> novosCids:  [{ codigoCID: "Q90.9", descricao: "..." }]
```

> Atenção: a chamada à API **pública** não deve levar o token do GISA no cabeçalho.

**Regras do back ao receber os CIDs**

- Códigos repetidos na mesma requisição são unificados.
- Código em `codigosCid` que não existe na base: erro `400` com a mensagem `CID(s) não encontrado(s): X, Y`.
- Código em `novosCids` que **já existe** na base: o CID existente é reaproveitado e a descrição enviada é **ignorada** (a descrição já cadastrada não é alterada).
- Em `novosCids`, `codigoCID` e `descricao` são obrigatórios.

---

## 4. Resposta de sucesso

Os cadastros retornam `201 Created`. Exemplo de paciente:

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

## 5. Roteiro rápido de testes

0. `POST /api/auth/login` e guardar o token; enviá-lo em todas as chamadas abaixo.
1. `GET /api/escolas?nome=` com um trecho de nome. Esperado: lista (vazia se não houver escolas).
2. `POST /api/escolas` para criar uma escola e anotar o `idEscola`.
3. `GET /api/cids/F84.0` antes de cadastrar. Esperado: `404`.
4. `POST /api/pacientes` com o CID em `novosCids` (exemplo da seção 1.2). Esperado: `201`.
5. `GET /api/cids/F84.0` de novo. Esperado: `200` com a descrição enviada.
6. `GET /api/pacientes`. Esperado: o paciente com `idade`, `responsaveis` e `cids`.
7. Repetir o passo 4 com o mesmo CPF. Esperado: `400` (CPF já cadastrado).
8. `POST /api/pacientes` com um código inexistente em `codigosCid`. Esperado: `400` (CID não encontrado).

---

## 6. Mensagens de erro

### 6.1 Validação de campos (`400`, em `fields`)

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
| escola (`POST`) | O nome da escola é obrigatório / O tipo da escola é obrigatório |

### 6.2 Regras de negócio (`400`, em `message`)

| Situação | Mensagem |
|---|---|
| CPF do paciente já cadastrado | Já existe uma pessoa cadastrada com este CPF |
| Mesmo CPF repetido em `responsaveis` | O mesmo responsável foi informado mais de uma vez |
| `idEscola` inexistente | Escola não encontrada com ID: {id} |
| CID inexistente em `codigosCid` | CID(s) não encontrado(s): {códigos} |
| `idResponsavel` inexistente | Responsável não encontrado com ID: {id} |
| CPF não corresponde ao `idResponsavel` | O CPF informado não corresponde ao responsável selecionado |
| CPF pertence a pessoa que não é responsável | O CPF informado pertence a uma pessoa que não está cadastrada como responsável |
