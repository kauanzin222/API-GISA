# API GISA — Profissionais

Autenticação, convenções, enums, formato de erro, regras comuns de PATCH/DELETE e concessão de acesso ao sistema: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

Especialistas, especialistas PJ e assistentes sociais também são profissionais, mas têm rotas próprias: ver `ESPECIALISTAS.md` e `ASSISTENTES-SOCIAIS.md`. Cargos e CBOs: ver `CARGOS.md`. Perfis de acesso: ver `PERFIS.md`.

## Rotas

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| `GET` | `/api/profissionais` | `VISUALIZAR_PROFISSIONAIS` ou `VISUALIZAR_TERAPIAS` | lista profissionais (paginado), incluindo especialistas, PJ e assistentes sociais, com busca por CPF ou nome e filtros por tipo e status |
| `GET` | `/api/profissionais/{idCadastro}` | `VISUALIZAR_PROFISSIONAIS` | detalhes de um profissional |
| `POST` | `/api/profissionais` | `CADASTRAR_PROFISSIONAL` | cadastra profissional sem acesso ao sistema |
| `POST` | `/api/profissionais/com-usuario` | `CADASTRAR_PROFISSIONAL` **e** `GERENCIAR_USUARIOS` | cadastra profissional e cria o usuário de acesso |
| `POST` | `/api/profissionais/{idCadastro}/usuario` | `GERENCIAR_USUARIOS` | concede acesso a um profissional já cadastrado (ver `00-GERAL.md`) |
| `PATCH` | `/api/profissionais/{idCadastro}` | `EDITAR_PROFISSIONAL` (+ `GERENCIAR_USUARIOS` com `idPerfil`) | atualiza profissional |
| `DELETE` | `/api/profissionais/{idCadastro}` | `INATIVAR_PROFISSIONAL` | inativa profissional |

`ACESSO_TOTAL` também libera todas as rotas. Sem a permissão: `403`. A listagem aceita também `VISUALIZAR_TERAPIAS` porque a recepção escolhe os especialistas ao agendar. Os motivos de `/com-usuario` e do `idPerfil` exigirem `GERENCIAR_USUARIOS` estão em `00-GERAL.md` ("Permissões por rota").

---

## `GET /api/profissionais` — listar profissionais (paginado)

Lista os profissionais, incluindo especialistas, especialistas PJ e assistentes sociais, ativos e inativos. Aceita busca por CPF e/ou nome e filtros por tipo e status; tudo combinável.

**Query params (todos opcionais)**

| Param | Padrão | Descrição |
|---|---|---|
| `cpf` | — | trecho do CPF; a máscara é ignorada |
| `nome` | — | trecho do nome, sem diferenciar maiúsculas de minúsculas |
| `tipo` | todos | `PROFISSIONAL`, `ESPECIALISTA`, `ESPECIALISTA_PJ` ou `ASSISTENTE_SOCIAL`; aceita mais de um: `?tipo=ESPECIALISTA&tipo=ESPECIALISTA_PJ` |
| `statusCadastro` | todos | `ATIVO` ou `INATIVO` |
| `page` | `0` | número da página (começa em 0) |
| `size` | `9` | itens por página |
| `sort` | `nome,asc` | ordenação, ex.: `sort=nome,desc` |

```
GET /api/profissionais?tipo=ESPECIALISTA&tipo=ESPECIALISTA_PJ&statusCadastro=ATIVO&nome=ana
```

- O `tipo` é **exato**: `ESPECIALISTA` não inclui os PJ. Para "todos que atendem terapias", envie os dois.
- Valor inválido em `tipo` ou `statusCadastro` (ex.: `?tipo=XYZ`): `400` com `Valor inválido para 'tipo': XYZ`. `PESSOA` (que só aparece em `GET /api/usuarios/me`) também é recusado aqui.

**Resposta `200`**

```json
{
  "content": [
    {
      "idProfissional": 10,
      "nome": "João Santos",
      "nomeCargo": "Administrador de Sistema",
      "registroConselho": "CRP-12345",
      "statusCadastro": "ATIVO",
      "tipo": "ESPECIALISTA"
    },
    {
      "idProfissional": 8,
      "nome": "Maria Silva",
      "nomeCargo": "Administrador de Sistema",
      "registroConselho": null,
      "statusCadastro": "ATIVO",
      "tipo": "PROFISSIONAL"
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
- `tipo` diz qual tela de detalhes abrir, sem chamadas extras:

  | `tipo` | Rota de detalhes |
  |---|---|
  | `PROFISSIONAL` | `GET /api/profissionais/{id}` |
  | `ESPECIALISTA` | `GET /api/especialistas/{id}` |
  | `ESPECIALISTA_PJ` | `GET /api/especialistas/pj/{id}` |
  | `ASSISTENTE_SOCIAL` | `GET /api/assistentes-sociais/{id}` |

- `registroConselho` vem preenchido para especialistas, PJ e assistentes sociais (número do CRESS) e `null` para profissionais comuns.

---

## `GET /api/profissionais/{idCadastro}` — detalhes do profissional

**Resposta `200`**

```json
{
  "pessoa": {
    "idCadastro": 8,
    "nome": "Maria Silva",
    "cpf": "12345678909",
    "dataNascimento": "1990-05-10",
    "sexo": "F",
    "celular": "11999999999",
    "numCNS": "123456789092345",
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
    { "idPermissao": 1, "nome": "ACESSO_TOTAL", "descricao": "Acesso total: libera todas as funcionalidades do sistema", "categoria": "Segurança" }
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
| `cpf` | string | sim | 11 dígitos, válido (dígitos verificadores); único no sistema |
| `dataNascimento` | data | sim | `yyyy-MM-dd` |
| `sexo` | string (1 caractere) | sim | `F`, `M` ou `O` |
| `estadoCivil` | enum | sim | ver `00-GERAL.md` |
| `celular` | string | não | |
| `numCNS` | string | não | |
| `enderecos` | lista de endereço | não | mesmos campos do endereço do paciente (ver `PACIENTES.md`); pode ser omitido ou `[]` |
| `email` | string | sim | formato de e-mail válido; único entre **todos** os cadastros, sem diferenciar maiúsculas. É o login, se o profissional receber acesso |
| `idCargo` | número | sim | ID de um cargo existente (ver `CARGOS.md`) |

### Exemplo

```json
{
  "nome": "Maria Silva",
  "cpf": "12345678909",
  "dataNascimento": "1990-05-10",
  "sexo": "F",
  "celular": "11999999999",
  "numCNS": "123456789092345",
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
  "cpf": "12345678909",
  "email": "maria.silva@email.com",
  "idCargo": 1,
  "acessoCriado": false
}
```

> **CPF ou e-mail repetido:** `400` com `message` = `CPF já cadastrado.` ou `E-mail já cadastrado.` O CPF e o e-mail são verificados contra todas as pessoas do sistema (o e-mail sem diferenciar maiúsculas; ele também é o login).

---

## `POST /api/profissionais/com-usuario` — cadastrar profissional com acesso

Cadastra o profissional e já cria o usuário de acesso. O corpo tem dois blocos: `profissional` (mesmos campos do `POST /api/profissionais`) e `usuario`.

| Campo de `usuario` | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `idPerfil` | número | sim | ID de um perfil existente (ver `PERFIS.md`) |

A senha **não** é enviada: o back gera uma senha temporária e a envia ao `email` do profissional (ver "Acesso ao sistema" em `00-GERAL.md`).

```json
{
  "profissional": {
    "nome": "Mariana Costa",
    "cpf": "12345678062",
    "dataNascimento": "1990-05-10",
    "sexo": "F",
    "celular": "11999999998",
    "numCNS": "123456789092346",
    "estadoCivil": "SOLTEIRO",
    "enderecos": [],
    "email": "mariana.costa@email.com",
    "idCargo": 1
  },
  "usuario": {
    "idPerfil": 1
  }
}
```

**Resposta `201`:** mesmo formato do cadastro simples, com `"acessoCriado": true` e `emailEnviado`:

```json
{
  "idCadastro": 9,
  "nome": "Mariana Costa",
  "cpf": "12345678062",
  "email": "mariana.costa@email.com",
  "idCargo": 1,
  "acessoCriado": true,
  "emailEnviado": true
}
```

- `emailEnviado: false`: o cadastro e o acesso **foram criados**, mas o e-mail com a senha temporária não saiu. Avise o administrador e ofereça o reenvio (`POST /api/usuarios/{idCadastro}/senha-temporaria`, ver `USUARIOS.md`).
- `emailEnviado` não aparece no cadastro sem acesso (`POST /api/profissionais`).
- Depois disso, o primeiro login é feito com o **e-mail** do profissional e a senha temporária (ver `00-GERAL.md`).

Nos erros de validação, os nomes em `fields` levam o bloco como prefixo, por exemplo `profissional.cpf` ou `usuario.idPerfil`.

---

## `PATCH /api/profissionais/{idCadastro}` — atualizar profissional

Segue as regras comuns de PATCH (`00-GERAL.md`).

| Campo | Observação |
|---|---|
| `nome` | não pode ser vazio nem `null` |
| `celular`, `numCNS` | `null` limpa |
| `estadoCivil` | não pode ser `null` |
| `statusCadastro` | `ATIVO` ou `INATIVO`; não pode ser `null`. Permite **reativar** um cadastro inativado. Inativar por aqui segue as mesmas regras do `DELETE` (proteção contra trancar o sistema) |
| `enderecos` | substitui a lista; `[]` remove todos |
| `email` | não pode ser vazio nem `null`; formato válido; não pode ser de outro cadastro (sem diferenciar maiúsculas). Se o profissional tiver acesso, muda também o login |
| `idCargo` | não pode ser `null`; o cargo precisa existir |
| `idPerfil` | altera o perfil do usuário de acesso; **só funciona se o profissional já tiver usuário**. Não cria usuário e não altera senha. Exige **também** `GERENCIAR_USUARIOS` (sem ela: `403`, e nada do PATCH é gravado) e segue as travas de `PATCH /api/usuarios/{idCadastro}/perfil` (ver `USUARIOS.md`), que faz o mesmo para qualquer tipo de cadastro |

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

- Se o profissional for o último usuário **ativo** cujo perfil tem `GERENCIAR_PERFIS` ou `ACESSO_TOTAL`, a inativação é recusada com `409` (`Operação bloqueada: o sistema precisa de ao menos um usuário ativo com permissão para gerenciar perfis (GERENCIAR_PERFIS ou ACESSO_TOTAL).`). Vale para todos os tipos de profissional e também para o PATCH com `statusCadastro: INATIVO`.

---

## Mensagens de erro

### Validação de campos (`400`, em `fields`)

| Campo | Mensagem |
|---|---|
| `nome` | O nome é obrigatório |
| `cpf` | O CPF é obrigatório / O CPF deve conter 11 dígitos numéricos / CPF inválido |
| `dataNascimento` | A data de nascimento é obrigatória |
| `sexo` | O sexo é obrigatório / O sexo deve ser F, M ou O |
| `estadoCivil` | O estado civil é obrigatório |
| endereço | O CEP é obrigatório / A rua é obrigatório / O número é obrigatório / O bairro é obrigatório / A cidade é obrigatória / O estado é obrigatório |
| `email` | O e-mail é obrigatório / Formato de e-mail inválido |
| `idCargo` | O ID do cargo é obrigatório / O ID do cargo deve ser positivo (PATCH) |
| `idPerfil` (PATCH) | O ID do perfil deve ser positivo |
| `profissional` / `usuario` | Os dados do profissional são obrigatórios. / Os dados do usuário são obrigatórios. |
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
| `email` de outro cadastro (PATCH) | `400` | E-mail já cadastrado. |
| `idCargo` `null` (PATCH) | `400` | O cargo não pode ser nulo. |
| `idCargo` inexistente (PATCH) | `400` | Cargo não encontrado com ID: {id} |
| `idPerfil` `null` (PATCH) | `400` | O perfil de acesso não pode ser nulo. |
| `idPerfil` sem usuário cadastrado (PATCH) | `409` | Não é possível alterar o perfil: o profissional não possui usuário. |
| `idPerfil` inexistente (PATCH) | `400` | Perfil não encontrado com ID: {id} |
| `idPerfil` que deixaria o sistema sem administrador ativo (PATCH) | `400` | O sistema precisa de ao menos um usuário ativo com acesso total. |
| `idPerfil` ou inativação (PATCH/DELETE) que deixaria o sistema sem usuário ativo que gerencie perfis | `409` | Operação bloqueada: o sistema precisa de ao menos um usuário ativo com permissão para gerenciar perfis (GERENCIAR_PERFIS ou ACESSO_TOTAL). |
| Conceder acesso a cadastro sem e-mail | `400` | O cadastro precisa ter um e-mail para receber acesso ao sistema. |
| `idPerfil` no PATCH sem `GERENCIAR_USUARIOS`, ou rota sem a permissão exigida | `403` | Você não tem permissão para realizar esta ação. |
| Listagem: `tipo` ou `statusCadastro` inválido (inclusive `tipo=PESSOA`) | `400` | Valor inválido para 'tipo': {valor} / Valor inválido para 'statusCadastro': {valor} |

---

## Roteiro rápido de testes

1. `GET /api/cargos`. Esperado: ao menos o cargo `1` (Administrador de Sistema).
2. `POST /api/profissionais` com o exemplo acima. Esperado: `201` com `acessoCriado: false`.
3. Repetir o passo 2. Esperado: `400` com `CPF já cadastrado.`
4. Repetir o passo 2 trocando só o CPF. Esperado: `400` com `E-mail já cadastrado.`
5. `POST /api/profissionais/com-usuario` com o exemplo. Esperado: `201` com `acessoCriado: true` e `emailEnviado: true` (sem SMTP configurado, a senha temporária aparece no log do servidor).
6. `POST /api/auth/login` com o e-mail `mariana.costa@email.com` e a senha temporária. Esperado: `200` com `senhaTemporaria: true` e `tokenRedefinicao`. Cadastre a senha definitiva (`POST /api/auth/redefinir-senha`, ver `00-GERAL.md`). Esperado: `200` com token.
7. `GET /api/profissionais`. Esperado: os dois profissionais no `content`.
8. `GET /api/profissionais/{id}` do passo 2. Esperado: `usuario: null`.
9. `PATCH /api/profissionais/{id}` do passo 2 com `{"idPerfil": 1}`. Esperado: `409` (sem usuário).
10. `POST /api/profissionais/{id}/usuario` com `{"idPerfil": 1}`. Esperado: `201` com `emailEnviado: true`; repetir o passo 9 agora retorna `200`.
11. `DELETE /api/profissionais/{id}` do passo 5. Esperado: `204`; o login do passo 6 passa a falhar com `401`.
12. `GET /api/profissionais`. Esperado: cada item com `tipo` (`PROFISSIONAL` nos dois profissionais acima).
13. `GET /api/profissionais?tipo=ESPECIALISTA&tipo=ESPECIALISTA_PJ`. Esperado: só especialistas e PJ (ver `ESPECIALISTAS.md` para cadastrá-los).
14. `GET /api/profissionais?statusCadastro=INATIVO`. Esperado: o profissional inativado no passo 11.
15. `GET /api/profissionais?tipo=XYZ`. Esperado: `400` com `Valor inválido para 'tipo': XYZ`.

**Permissões** (usuários nos perfis padrão; ver `00-GERAL.md`)

16. Com um usuário "Recepção": `GET /api/profissionais`. Esperado: `200` (tem `VISUALIZAR_TERAPIAS`). `GET /api/profissionais/{id}`, `POST /api/profissionais` (corpo válido), `PATCH` e `DELETE`. Esperado: `403`.
17. Com um usuário que tem `CADASTRAR_PROFISSIONAL` mas não `GERENCIAR_USUARIOS` (crie um perfil assim): `POST /api/profissionais` com o exemplo (CPF e e-mail novos). Esperado: `201`. `POST /api/profissionais/com-usuario`. Esperado: `403`.
18. Com um usuário que tem `EDITAR_PROFISSIONAL` mas não `GERENCIAR_USUARIOS`: `PATCH /api/profissionais/{id}` com `{"celular": "15999990000"}`. Esperado: `200`. Com `{"idPerfil": 1}`: `403`.
