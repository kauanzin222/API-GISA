# API GISA — Profissionais

Autenticação, convenções, enums, formato de erro, regras comuns de PATCH/DELETE e concessão de acesso ao sistema: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

Especialistas e especialistas PJ também são profissionais, mas têm rotas próprias: ver `ESPECIALISTAS.md`. Cargos e CBOs: ver `CARGOS.md`. Perfis de acesso: ver `PERFIS.md`.

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/profissionais` | lista profissionais (paginado), incluindo especialistas e PJ, com busca por CPF ou nome |
| `GET` | `/api/profissionais/{idCadastro}` | detalhes de um profissional |
| `POST` | `/api/profissionais` | cadastra profissional sem acesso ao sistema |
| `POST` | `/api/profissionais/com-usuario` | cadastra profissional e cria o usuário de acesso |
| `POST` | `/api/profissionais/{idCadastro}/usuario` | concede acesso a um profissional já cadastrado (ver `00-GERAL.md`) |
| `PATCH` | `/api/profissionais/{idCadastro}` | atualiza profissional |
| `DELETE` | `/api/profissionais/{idCadastro}` | inativa profissional |

---

## `GET /api/profissionais` — listar profissionais (paginado)

Lista os profissionais, incluindo especialistas e especialistas PJ, ativos e inativos. Aceita busca por CPF e/ou nome.

**Query params (todos opcionais)**

| Param | Padrão | Descrição |
|---|---|---|
| `cpf` | — | trecho do CPF; a máscara é ignorada |
| `nome` | — | trecho do nome, sem diferenciar maiúsculas de minúsculas |
| `page` | `0` | número da página (começa em 0) |
| `size` | `9` | itens por página |
| `sort` | `nome,asc` | ordenação, ex.: `sort=nome,desc` |

**Resposta `200`**

```json
{
  "content": [
    {
      "idProfissional": 10,
      "nome": "João Santos",
      "nomeCargo": "Administrador de Sistema",
      "registroConselho": "CRP-12345",
      "statusCadastro": "ATIVO"
    },
    {
      "idProfissional": 8,
      "nome": "Maria Silva",
      "nomeCargo": "Administrador de Sistema",
      "registroConselho": null,
      "statusCadastro": "ATIVO"
    }
  ],
  "page": {
    "size": 9,
    "number": 0,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

- Os dados de paginação vêm dentro do objeto `page`.
- O ID vem no campo **`idProfissional`** (é o mesmo valor do `idCadastro` usado nas demais rotas).
- `registroConselho` vem preenchido para especialistas e PJ e `null` para profissionais comuns. Use-o para saber qual tela de detalhes abrir; para distinguir especialista de PJ, consulte `GET /api/especialistas/{id}` (que recusa PJ com `400`) ou `GET /api/especialistas/pj/{id}`.

---

## `GET /api/profissionais/{idCadastro}` — detalhes do profissional

**Resposta `200`**

```json
{
  "pessoa": {
    "idCadastro": 8,
    "nome": "Maria Silva",
    "cpf": "12345678901",
    "dataNascimento": "1990-05-10",
    "sexo": "F",
    "celular": "11999999999",
    "numCNS": "123456789012345",
    "estadoCivil": "SOLTEIRO",
    "statusCadastro": "ATIVO",
    "enderecos": [
      {
        "cep": "01001000",
        "rua": "Praça da Sé",
        "numero": "100",
        "complemento": "Apto 12",
        "bairro": "Sé",
        "cidade": "São Paulo",
        "estado": "SP"
      }
    ]
  },
  "email": "maria.silva@email.com",
  "cargo": {
    "idCargo": 1,
    "nome": "Administrador de Sistema",
    "codigoCBO": "999999",
    "tituloCBO": "Cargo de Teste - Desenvolvimento"
  },
  "usuario": null
}
```

- `usuario` é `null` quando o profissional não tem acesso ao sistema. Quando tem, segue o formato abaixo (a senha nunca é retornada):

```json
"usuario": {
  "idCadastro": 9,
  "perfil": { "idPerfil": 1, "nome": "Administrador" },
  "permissoes": [
    { "idPermissao": 1, "nome": "ACESSO_TOTAL", "descricao": "Acesso total aos recursos da API" }
  ]
}
```

**Resposta `404`** (profissional não existe): `Profissional não encontrado com ID: {id}`.

---

## `POST /api/profissionais` — cadastrar profissional

### Campos

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `nome` | string | sim | |
| `cpf` | string | sim | 11 dígitos; único no sistema |
| `dataNascimento` | data | sim | `yyyy-MM-dd` |
| `sexo` | string (1 caractere) | sim | `F`, `M` ou `O` |
| `estadoCivil` | enum | sim | ver `00-GERAL.md` |
| `celular` | string | não | |
| `numCNS` | string | não | |
| `enderecos` | lista de endereço | não | mesmos campos do endereço do paciente (ver `PACIENTES.md`); pode ser omitido ou `[]` |
| `email` | string | sim | formato de e-mail válido; único entre profissionais |
| `idCargo` | número | sim | ID de um cargo existente (ver `CARGOS.md`) |

### Exemplo

```json
{
  "nome": "Maria Silva",
  "cpf": "12345678901",
  "dataNascimento": "1990-05-10",
  "sexo": "F",
  "celular": "11999999999",
  "numCNS": "123456789012345",
  "estadoCivil": "SOLTEIRO",
  "enderecos": [
    {
      "cep": "01001000",
      "rua": "Praça da Sé",
      "numero": "100",
      "complemento": "Apto 12",
      "bairro": "Sé",
      "cidade": "São Paulo",
      "estado": "SP"
    }
  ],
  "email": "maria.silva@email.com",
  "idCargo": 1
}
```

### Resposta `201 Created`

```json
{
  "idCadastro": 8,
  "nome": "Maria Silva",
  "cpf": "12345678901",
  "email": "maria.silva@email.com",
  "idCargo": 1,
  "acessoCriado": false
}
```

> **CPF ou e-mail repetido:** `400` com `message` = `CPF já cadastrado.` ou `E-mail já cadastrado.` O CPF é verificado contra todas as pessoas do sistema (pacientes, responsáveis e profissionais); o e-mail, contra todos os profissionais.

---

## `POST /api/profissionais/com-usuario` — cadastrar profissional com acesso

Cadastra o profissional e já cria o usuário de acesso. O corpo tem dois blocos: `profissional` (mesmos campos do `POST /api/profissionais`) e `usuario`.

| Campo de `usuario` | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `senha` | string | sim | entre 8 e 30 caracteres |
| `idPerfil` | número | sim | ID de um perfil existente (ver `PERFIS.md`) |

```json
{
  "profissional": {
    "nome": "Mariana Costa",
    "cpf": "12345678902",
    "dataNascimento": "1990-05-10",
    "sexo": "F",
    "celular": "11999999998",
    "numCNS": "123456789012346",
    "estadoCivil": "SOLTEIRO",
    "enderecos": [],
    "email": "mariana.costa@email.com",
    "idCargo": 1
  },
  "usuario": {
    "senha": "SenhaSegura123",
    "idPerfil": 1
  }
}
```

**Resposta `201`:** mesmo formato do cadastro simples, com `"acessoCriado": true`. Depois disso, o login funciona com o CPF do profissional e a senha enviada.

Nos erros de validação, os nomes em `fields` levam o bloco como prefixo, por exemplo `profissional.cpf` ou `usuario.senha`.

---

## `PATCH /api/profissionais/{idCadastro}` — atualizar profissional

Segue as regras comuns de PATCH (`00-GERAL.md`).

| Campo | Observação |
|---|---|
| `nome` | não pode ser vazio nem `null` |
| `celular`, `numCNS` | `null` limpa |
| `estadoCivil` | não pode ser `null` |
| `statusCadastro` | `ATIVO` ou `INATIVO`; não pode ser `null`. Permite **reativar** um cadastro inativado |
| `enderecos` | substitui a lista; `[]` remove todos |
| `email` | não pode ser vazio nem `null`; formato válido; não pode ser de outro profissional |
| `idCargo` | não pode ser `null`; o cargo precisa existir |
| `idPerfil` | altera o perfil do usuário de acesso; **só funciona se o profissional já tiver usuário**. Não cria usuário e não altera senha |

**Ignorados (somente leitura):** `cpf`, `dataNascimento`, `sexo`.

```json
{
  "celular": "11911112222",
  "email": "maria.s@email.com",
  "idCargo": 1
}
```

**Resposta `200`:** os detalhes atualizados (mesmo formato do `GET /api/profissionais/{idCadastro}`).

---

## `DELETE /api/profissionais/{idCadastro}` — inativar profissional

Define `statusCadastro` como `INATIVO` e retorna `204 No Content`. Profissional inativo não consegue fazer login. Para reativar, use o PATCH com `"statusCadastro": "ATIVO"`.

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
| endereço | O CEP é obrigatório / A rua é obrigatório / O número é obrigatório / O bairro é obrigatório / A cidade é obrigatória / O estado é obrigatório |
| `email` | O e-mail é obrigatório / Formato de e-mail inválido |
| `idCargo` | O ID do cargo é obrigatório / O ID do cargo deve ser positivo (PATCH) |
| `idPerfil` (PATCH) | O ID do perfil deve ser positivo |
| `profissional` / `usuario` | Os dados do profissional são obrigatórios. / Os dados do usuário são obrigatórios. |
| `usuario.senha` | A senha é obrigatória. / A senha deve conter entre 8 e 30 caracteres. |
| `usuario.idPerfil` | O ID do perfil é obrigatório. |

### Regras de negócio (`message`)

| Situação | Status | Mensagem |
|---|---|---|
| CPF já cadastrado (POST) | `400` | CPF já cadastrado. |
| E-mail já cadastrado (POST) | `400` | E-mail já cadastrado. |
| `idCargo` inexistente (POST) | `400` | Cargo não encontrado com ID: {id} |
| `idPerfil` inexistente (POST com usuário) | `400` | Perfil não encontrado com ID: {id} |
| Profissional inexistente (PATCH/DELETE) | `404` | Profissional não encontrado com ID: {id} |
| `nome` vazio (PATCH) | `400` | O nome não pode ser vazio. |
| `estadoCivil` `null` (PATCH) | `400` | O estado civil não pode ser nulo. |
| `statusCadastro` `null` (PATCH) | `400` | O status do cadastro não pode ser nulo. |
| `email` vazio (PATCH) | `400` | O e-mail não pode ser vazio. |
| `email` de outro profissional (PATCH) | `400` | E-mail já cadastrado. |
| `idCargo` `null` (PATCH) | `400` | O cargo não pode ser nulo. |
| `idCargo` inexistente (PATCH) | `400` | Cargo não encontrado com ID: {id} |
| `idPerfil` `null` (PATCH) | `400` | O perfil de acesso não pode ser nulo. |
| `idPerfil` sem usuário cadastrado (PATCH) | `409` | Não é possível alterar o perfil: o profissional não possui usuário. |
| `idPerfil` inexistente (PATCH) | `400` | Perfil não encontrado com ID: {id} |

---

## Roteiro rápido de testes

1. `GET /api/cargos`. Esperado: ao menos o cargo `1` (Administrador de Sistema).
2. `POST /api/profissionais` com o exemplo acima. Esperado: `201` com `acessoCriado: false`.
3. Repetir o passo 2. Esperado: `400` com `CPF já cadastrado.`
4. Repetir o passo 2 trocando só o CPF. Esperado: `400` com `E-mail já cadastrado.`
5. `POST /api/profissionais/com-usuario` com o exemplo. Esperado: `201` com `acessoCriado: true`.
6. `POST /api/auth/login` com o CPF `12345678902` e a senha `SenhaSegura123`. Esperado: `200` com token.
7. `GET /api/profissionais`. Esperado: os dois profissionais no `content`.
8. `GET /api/profissionais/{id}` do passo 2. Esperado: `usuario: null`.
9. `PATCH /api/profissionais/{id}` do passo 2 com `{"idPerfil": 1}`. Esperado: `409` (sem usuário).
10. `POST /api/profissionais/{id}/usuario` com `{"senha": "SenhaTeste123", "idPerfil": 1}`. Esperado: `201`; repetir o passo 9 agora retorna `200`.
11. `DELETE /api/profissionais/{id}` do passo 5. Esperado: `204`; o login do passo 6 passa a falhar com `401`.
