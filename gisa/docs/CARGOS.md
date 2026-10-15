# API GISA — Cargos e CBOs

Autenticação, convenções, enums e formato de erro: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

- **CBO** = Classificação Brasileira de Ocupações: um código oficial de 6 dígitos com um título (ex.: `251510` — Psicólogo clínico).
- **Cargo** = a função usada internamente (ex.: "Psicóloga do Ambulatório"). Todo cargo está ligado a um CBO, e todo profissional, especialista e especialista PJ tem um cargo (`idCargo`).

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/cargos` | lista cargos, com busca por nome |
| `GET` | `/api/cargos/{idCargo}` | detalhes: CBO e profissionais do cargo |
| `POST` | `/api/cargos` | cadastra cargo |
| `PATCH` | `/api/cargos/{idCargo}` | renomeia o cargo e/ou troca o CBO |
| `DELETE` | `/api/cargos/{idCargo}` | exclui cargo sem profissionais |
| `GET` | `/api/cbos` | lista CBOs (paginado), com busca por código ou título |
| `GET` | `/api/cbos/{codigoCBO}` | detalhes: cargos que usam o CBO |
| `POST` | `/api/cbos` | cadastra CBO |
| `PATCH` | `/api/cbos/{codigoCBO}` | altera o título do CBO |
| `DELETE` | `/api/cbos/{codigoCBO}` | exclui CBO sem cargos |

**Permissão:** `GERENCIAR_CARGOS` em todas as rotas de cargos e CBOs, exceto `GET /api/cargos`, que aceita também `CADASTRAR_PROFISSIONAL`, `CADASTRAR_ESPECIALISTA`, `CADASTRAR_ESPECIALISTA_PJ` e `EDITAR_PROFISSIONAL` (o seletor de cargo dos formulários de profissional). `ACESSO_TOTAL` libera tudo. Sem a permissão: `403`.

**Fluxo das telas**

1. Tela de cargos: `GET /api/cargos` (com `?nome=` no campo de busca).
2. Ao abrir o cargo: `GET /api/cargos/{idCargo}` mostra o CBO e os profissionais (CPFs) do cargo.
3. No formulário de cargo, o seletor de CBO usa `GET /api/cbos?codigo=` ou `?titulo=`. Se o CBO não existir, cadastre com `POST /api/cbos` e use o `codigoCBO` retornado.

---

## Cargos

### `GET /api/cargos` — listar cargos

Sem paginação, ordenado por nome.

| Param | Obrigatório | Descrição |
|---|---|---|
| `nome` | não | trecho do nome, sem diferenciar maiúsculas de minúsculas |

```
GET /api/cargos?nome=admin
```

**Resposta `200`**

```json
[
  {
    "idCargo": 1,
    "nome": "Administrador de Sistema",
    "codigoCBO": "999999",
    "tituloCBO": "Cargo de Teste - Desenvolvimento"
  }
]
```

### `GET /api/cargos/{idCargo}` — detalhes do cargo

**Resposta `200`**

```json
{
  "idCargo": 1,
  "nome": "Administrador de Sistema",
  "codigoCBO": "999999",
  "tituloCBO": "Cargo de Teste - Desenvolvimento",
  "profissionais": [
    { "idCadastro": 8, "nome": "Maria Silva", "cpf": "12345678909", "statusCadastro": "ATIVO" }
  ]
}
```

- `profissionais` inclui profissionais, especialistas e especialistas PJ, ativos e inativos, ordenados por nome.

**Resposta `404`:** `Cargo não encontrado com ID: {id}`.

### `POST /api/cargos` — cadastrar cargo

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `nome` | string | sim | único, sem diferenciar maiúsculas de minúsculas; espaços nas pontas são removidos |
| `codigoCBO` | string | sim | o CBO precisa existir |

```json
{
  "nome": "Recepcionista",
  "codigoCBO": "422105"
}
```

**Resposta `201 Created`:** o cargo criado, no mesmo formato da listagem.

### `PATCH /api/cargos/{idCargo}` — atualizar cargo

Segue as regras comuns de PATCH (`00-GERAL.md`).

| Campo | Observação |
|---|---|
| `nome` | não pode ser vazio nem `null`; não pode repetir o nome de outro cargo |
| `codigoCBO` | não pode ser vazio nem `null`; o CBO precisa existir |

```json
{
  "codigoCBO": "422105"
}
```

**Resposta `200`:** os detalhes atualizados (mesmo formato do `GET /api/cargos/{idCargo}`). A mudança vale para todos os profissionais do cargo.

### `DELETE /api/cargos/{idCargo}` — excluir cargo

Cargos não têm status: o `DELETE` **exclui** o cargo e retorna `204 No Content`.

Só é possível excluir um cargo **sem profissionais** (nem inativos). Antes, troque o cargo deles pelo PATCH de profissional/especialista (`idCargo`).

---

## CBOs

### `GET /api/cbos` — listar CBOs (paginado)

A tabela oficial tem milhares de códigos, por isso esta listagem é paginada e serve também para autocomplete.

| Param | Padrão | Descrição |
|---|---|---|
| `codigo` | — | **início** do código; a máscara é ignorada (`2515-10` vira `251510`) |
| `titulo` | — | trecho do título, sem diferenciar maiúsculas de minúsculas |
| `page` | `0` | número da página |
| `size` | `9` | itens por página |
| `sort` | `codigoCBO,asc` | ordenação, ex.: `sort=tituloCBO,asc` |

```
GET /api/cbos?titulo=psic
```

**Resposta `200`**

```json
{
  "content": [
    { "codigoCBO": "251510", "tituloCBO": "Psicólogo clínico" }
  ],
  "page": {
    "size": 9,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### `GET /api/cbos/{codigoCBO}` — detalhes do CBO

**Resposta `200`**

```json
{
  "codigoCBO": "999999",
  "tituloCBO": "Cargo de Teste - Desenvolvimento",
  "cargos": [
    { "idCargo": 1, "nome": "Administrador de Sistema" }
  ]
}
```

**Resposta `404`:** `CBO não encontrado com código: {código}`.

### `POST /api/cbos` — cadastrar CBO

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `codigoCBO` | string | sim | exatamente 6 dígitos, **sem máscara** (`2515-10` → `251510`); único |
| `tituloCBO` | string | sim | espaços nas pontas são removidos |

```json
{
  "codigoCBO": "422105",
  "tituloCBO": "Recepcionista, em geral"
}
```

**Resposta `201 Created`**

```json
{
  "codigoCBO": "422105",
  "tituloCBO": "Recepcionista, em geral"
}
```

### `PATCH /api/cbos/{codigoCBO}` — atualizar CBO

| Campo | Observação |
|---|---|
| `tituloCBO` | não pode ser vazio nem `null` |

O `codigoCBO` é a identificação do registro e **não pode ser alterado** (se vier no corpo, é ignorado). Para corrigir um código errado, cadastre o CBO certo, mova os cargos para ele e exclua o antigo.

**Resposta `200`:** o CBO atualizado, no formato do cadastro.

### `DELETE /api/cbos/{codigoCBO}` — excluir CBO

Exclui o CBO e retorna `204 No Content`. Só é possível excluir um CBO **sem cargos**.

> A base vem só com o CBO de teste `999999`. A carga dos CBOs oficiais pode ser feita por migration no futuro; até lá, cadastre os necessários por `POST /api/cbos`.

---

## Mensagens de erro

### Validação de campos (`400`, em `fields`)

| Campo | Mensagem |
|---|---|
| cargo: `nome` | O nome do cargo é obrigatório |
| cargo: `codigoCBO` | O código CBO é obrigatório |
| CBO: `codigoCBO` | O código CBO é obrigatório / O código CBO deve conter 6 dígitos numéricos |
| CBO: `tituloCBO` | O título do CBO é obrigatório |

### Regras de negócio (`message`)

| Situação | Status | Mensagem |
|---|---|---|
| Nome de cargo já existente | `400` | Já existe um cargo com esse nome. |
| `nome` do cargo vazio (PATCH) | `400` | O nome do cargo não pode ser vazio. |
| `codigoCBO` vazio (PATCH de cargo) | `400` | O código CBO não pode ser vazio. |
| CBO inexistente ao criar/alterar cargo | `400` | CBO não encontrado com código: {código} |
| Excluir cargo com profissionais | `400` | Não é possível excluir o cargo: há profissionais vinculados a ele. |
| Cargo inexistente | `404` | Cargo não encontrado com ID: {id} |
| CBO já cadastrado | `400` | CBO já cadastrado. |
| `tituloCBO` vazio (PATCH) | `400` | O título do CBO não pode ser vazio. |
| Excluir CBO com cargos | `400` | Não é possível excluir o CBO: há cargos vinculados a ele. |
| CBO inexistente (GET/PATCH/DELETE) | `404` | CBO não encontrado com código: {código} |

---

## Roteiro rápido de testes

1. `POST /api/cbos` com o exemplo. Esperado: `201`. Repetir: `400` (`CBO já cadastrado.`).
2. `POST /api/cbos` com `"codigoCBO": "4221-05"`. Esperado: `400` (6 dígitos sem máscara).
3. `GET /api/cbos?codigo=4221`. Esperado: o CBO do passo 1.
4. `POST /api/cargos` com o exemplo. Esperado: `201`. Repetir com `"nome": "recepcionista"`: `400`.
5. `GET /api/cbos/422105`. Esperado: o cargo "Recepcionista" em `cargos`.
6. `DELETE /api/cbos/422105`. Esperado: `400` (há cargo vinculado).
7. Cadastrar um profissional com o `idCargo` do passo 4 e `GET /api/cargos/{id}`. Esperado: o CPF dele em `profissionais`.
8. `DELETE /api/cargos/{id}`. Esperado: `400`. Trocar o `idCargo` do profissional para `1` e repetir: `204`.
9. `DELETE /api/cbos/422105`. Esperado: `204`.
