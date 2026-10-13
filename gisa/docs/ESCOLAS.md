# API GISA — Escolas

Autenticação, convenções, enums e formato de erro: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/escolas?nome=` | busca de escolas por nome (sugestões para autocomplete) |
| `POST` | `/api/escolas` | cadastro de escola |

---

## `GET /api/escolas?nome=` — buscar escolas por nome

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

O `idEscola` retornado é o valor a enviar no campo `idEscola` ao cadastrar ou atualizar um paciente.

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

> Não é possível cadastrar a escola dentro do corpo do paciente. O fluxo é: buscar com `GET /api/escolas?nome=`; se não houver sugestão, cadastrar com `POST /api/escolas`, pegar o `idEscola` da resposta e então usá-lo no cadastro do paciente.

> A API não impede escolas com nome repetido. Antes de cadastrar, confira as sugestões do autocomplete.

---

## Erros

| Situação | Onde aparece | Mensagem |
|---|---|---|
| `nome` vazio no cadastro | `fields` | O nome da escola é obrigatório |
| `tipoEscola` ausente | `fields` | O tipo da escola é obrigatório |
| `tipoEscola` com valor fora do enum | `message` | Corpo da requisição inválido ou malformado. |
| `GET` sem o parâmetro `nome` | `400` | parâmetro obrigatório ausente |

Escola inexistente ao informar `idEscola` em um paciente: ver `PACIENTES.md`.

---

## Roteiro rápido de testes

1. `GET /api/escolas?nome=` com um trecho de nome. Esperado: lista (vazia se não houver escolas).
2. `GET /api/escolas?nome=` (em branco). Esperado: `[]`.
3. `POST /api/escolas` com o exemplo acima. Esperado: `201` e o `idEscola`.
4. `GET /api/escolas?nome=` com parte do nome cadastrado, em minúsculas. Esperado: a escola aparece na lista.
5. `POST /api/escolas` sem `tipoEscola`. Esperado: `400` com a mensagem em `fields`.
