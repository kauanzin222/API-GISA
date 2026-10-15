# API GISA — Escolas

Autenticação, convenções, enums e formato de erro: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

## Rotas

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| `GET` | `/api/escolas` | `GERENCIAR_ESCOLAS` | listagem paginada, com filtros |
| `GET` | `/api/escolas/sugestoes?nome=` | `CADASTRAR_PACIENTE`, `EDITAR_PACIENTE` ou `GERENCIAR_ESCOLAS` | sugestões para o autocomplete (até 10) |
| `GET` | `/api/escolas/{idEscola}` | `GERENCIAR_ESCOLAS` | detalhes |
| `POST` | `/api/escolas` | `CADASTRAR_PACIENTE`, `EDITAR_PACIENTE` ou `GERENCIAR_ESCOLAS` | cadastro de escola |
| `PATCH` | `/api/escolas/{idEscola}` | `GERENCIAR_ESCOLAS` | edição |
| `DELETE` | `/api/escolas/{idEscola}` | `GERENCIAR_ESCOLAS` | **exclusão**, só sem pacientes vinculados |

`ACESSO_TOTAL` também libera todas as rotas. Sem a permissão: `403`.

> **Mudança para o front:** o autocomplete, que era `GET /api/escolas?nome=`, agora é **`GET /api/escolas/sugestoes?nome=`**, com o mesmo comportamento e a mesma resposta. `GET /api/escolas` passou a ser a listagem paginada da tela de gestão de escolas (resposta com `content` e `page`).

O autocomplete e o cadastro rápido continuam liberados para quem cadastra ou edita pacientes, porque são usados no formulário do paciente. A listagem, os detalhes, a edição e a exclusão são da tela de gestão de escolas.

**Pacientes da escola:** use a busca de pacientes com o filtro `idEscola` (`GET /api/pacientes?idEscola={idEscola}`; ver `PACIENTES.md`); com `idEscola`, o `termo` é opcional. A resposta é uma lista simples de até 20 pacientes, ordenados por nome. Ela exige `VISUALIZAR_PACIENTES`.

---

## `GET /api/escolas` — listar escolas (paginado)

| Param | Padrão | Descrição |
|---|---|---|
| `nome` | — | trecho do nome, sem diferenciar maiúsculas de minúsculas |
| `tipoEscola` | todos | `PUBLICA` ou `PARTICULAR` |
| `page` / `size` / `sort` | `0` / `9` / `nome,asc` | `sort` aceita `nome`, `tipoEscola` e `telefone` |

```
GET /api/escolas?nome=municipal&tipoEscola=PUBLICA
```

**Resposta `200`**

```json
{
  "content": [
    {
      "idEscola": 3,
      "nome": "Escola Municipal Maria Silva",
      "tipoEscola": "PUBLICA",
      "telefone": "1533330000",
      "quantidadePacientes": 12
    }
  ],
  "page": { "size": 9, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

- `quantidadePacientes` conta os pacientes vinculados à escola, **ativos e inativos** (os mesmos que impedem a exclusão).

---

## `GET /api/escolas/sugestoes?nome=` — sugestões para o autocomplete

Não é uma listagem completa: serve para **autocomplete** ao digitar o nome da escola.

- Busca por **trecho** do nome, sem diferenciar maiúsculas de minúsculas.
- Retorna **no máximo 10** resultados, em ordem alfabética, sem paginação.
- O parâmetro `nome` é **opcional**: sem ele (ou em branco), a resposta traz as 10 primeiras escolas em ordem alfabética, útil para abrir o seletor já com opções.

**Exemplo**

```
GET /api/escolas/sugestoes?nome=esc
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

O `idEscola` retornado é o valor a enviar no campo `idEscola` ao cadastrar ou atualizar um paciente.

---

## `GET /api/escolas/{idEscola}` — detalhes

**Resposta `200`:** um item no mesmo formato da listagem (com `quantidadePacientes`).

**Resposta `404`:** `Escola não encontrada com ID: {id}`.

---

## `POST /api/escolas` — cadastrar escola

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `nome` | string | sim | espaços nas pontas são removidos |
| `tipoEscola` | enum | sim | `PUBLICA` ou `PARTICULAR` |
| `telefone` | string | não | |

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

> Não é possível cadastrar a escola dentro do corpo do paciente. O fluxo é: buscar com `GET /api/escolas/sugestoes?nome=`; se não houver sugestão, cadastrar com `POST /api/escolas`, pegar o `idEscola` da resposta e então usá-lo no cadastro do paciente.

> A API não impede escolas com nome repetido. Antes de cadastrar, confira as sugestões do autocomplete.

---

## `PATCH /api/escolas/{idEscola}` — editar escola

Segue as regras comuns de PATCH (`00-GERAL.md`): envie só o que quer alterar.

| Campo | Observação |
|---|---|
| `nome` | não pode ser vazio nem `null`; espaços nas pontas são removidos |
| `tipoEscola` | não pode ser `null` |
| `telefone` | opcional; `null` limpa |

```json
{
  "telefone": "1533332222"
}
```

**Resposta `200`:** os detalhes atualizados (com `quantidadePacientes`).

---

## `DELETE /api/escolas/{idEscola}` — excluir escola

Escolas não têm status: o `DELETE` **exclui** a escola e retorna `204 No Content`.

Só é possível excluir uma escola **sem pacientes vinculados**, contando também os pacientes inativos. Antes, troque a escola desses pacientes (`PATCH /api/pacientes/{id}` com outro `idEscola`, ou `null`). Para encontrá-los: `GET /api/pacientes?idEscola={idEscola}`.

---

## Erros

| Situação | Status | Onde aparece | Mensagem |
|---|---|---|---|
| `nome` vazio no cadastro | `400` | `fields` | O nome da escola é obrigatório |
| `tipoEscola` ausente no cadastro | `400` | `fields` | O tipo da escola é obrigatório |
| `tipoEscola` com valor fora do enum (corpo) | `400` | `message` | Corpo da requisição inválido ou malformado. |
| `tipoEscola` com valor fora do enum (filtro) | `400` | `message` | Valor inválido para 'tipoEscola': {valor} |
| `nome` vazio ou `null` no PATCH | `400` | `message` | O nome da escola não pode ser vazio. |
| `tipoEscola` `null` no PATCH | `400` | `message` | O tipo da escola não pode ser nulo. |
| Excluir escola com pacientes | `400` | `message` | Não é possível excluir a escola: há pacientes vinculados a ela. |
| Escola inexistente na URL | `404` | `message` | Escola não encontrada com ID: {id} |
| Sem permissão | `403` | `message` | Você não tem permissão para realizar esta ação. |

Escola inexistente ao informar `idEscola` em um paciente: ver `PACIENTES.md`.

---

## Roteiro rápido de testes

Com o token do administrador, salvo onde indicado.

1. `POST /api/escolas` com o exemplo acima. Esperado: `201` e o `idEscola`.
2. `GET /api/escolas/sugestoes?nome=` com parte do nome cadastrado, em minúsculas. Esperado: a escola aparece na lista (array, até 10).
3. `GET /api/escolas?tipoEscola=PARTICULAR`. Esperado: `200` com `content` e `page`; a escola com `quantidadePacientes: 0`.
4. `PATCH /api/escolas/{id}` com `{"telefone": null}`. Esperado: `200` sem `telefone`. Com `{"nome": ""}`: `400` `O nome da escola não pode ser vazio.`
5. Cadastrar um paciente com esse `idEscola` (ver `PACIENTES.md`). `GET /api/escolas/{id}`. Esperado: `quantidadePacientes: 1`. `GET /api/pacientes?idEscola={id}`. Esperado: o paciente.
6. `DELETE /api/escolas/{id}`. Esperado: `400` `Não é possível excluir a escola: há pacientes vinculados a ela.` Inativar o paciente e repetir. Esperado: `400` (inativos também contam).
7. Cadastrar outra escola sem pacientes e `DELETE`. Esperado: `204`; o `GET` dela responde `404`.
8. Com o token de um usuário "Recepção": `GET /api/escolas/sugestoes` e `POST /api/escolas`. Esperado: `200` e `201`. `GET /api/escolas`, `PATCH` e `DELETE`. Esperado: `403`.
9. Com o token de um usuário "Assistente Social": `GET /api/escolas/sugestoes`. Esperado: `403`.
