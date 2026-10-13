# API GISA — Perfis de acesso e Permissões

Autenticação, convenções, enums e formato de erro: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

- **Permissão** = uma funcionalidade do sistema (ex.: `CADASTRAR_PROFISSIONAL`). As permissões são definidas pelo próprio sistema, por migration; a API **só as consulta**.
- **Perfil** = um conjunto de permissões (ex.: "Administrador", "Recepção"). Cada usuário de acesso tem exatamente um perfil. O perfil do usuário é escolhido ao conceder acesso (ver `00-GERAL.md`) e pode ser trocado pelo `idPerfil` no PATCH de profissional/especialista.

> **Situação atual:** as permissões já são gravadas e retornadas, mas **ainda não restringem nenhuma rota**: qualquer usuário autenticado acessa todos os endpoints. A verificação por permissão entra quando a lista definitiva de permissões for criada.

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/perfis` | lista todos os perfis |
| `GET` | `/api/perfis/{idPerfil}` | detalhes: permissões e usuários do perfil |
| `POST` | `/api/perfis` | cadastra perfil |
| `PATCH` | `/api/perfis/{idPerfil}` | renomeia o perfil e/ou substitui suas permissões |
| `DELETE` | `/api/perfis/{idPerfil}` | exclui perfil sem usuários |
| `POST` | `/api/perfis/{idPerfil}/permissoes` | adiciona permissões ao perfil |
| `DELETE` | `/api/perfis/{idPerfil}/permissoes/{idPermissao}` | remove uma permissão do perfil |
| `GET` | `/api/permissoes` | lista todas as permissões do sistema |
| `GET` | `/api/permissoes/{idPermissao}` | detalhes de uma permissão |

**Fluxo das telas**

1. Tela de perfis: `GET /api/perfis`.
2. Ao abrir o perfil: `GET /api/perfis/{idPerfil}` mostra as permissões e os usuários (CPFs) do perfil.
3. Botão "Adicionar permissão": `GET /api/permissoes` para montar a lista (mostrando só as que o perfil ainda não tem) e `POST /api/perfis/{idPerfil}/permissoes` com as escolhidas.
4. Botão de remover em cada permissão: `DELETE /api/perfis/{idPerfil}/permissoes/{idPermissao}`.

As rotas que alteram o perfil respondem com os detalhes atualizados, então a tela pode ser redesenhada direto com a resposta.

---

## `GET /api/perfis` — listar perfis

Sem paginação, ordenado por nome.

**Resposta `200`**

```json
[
  {
    "idPerfil": 1,
    "nome": "Administrador",
    "quantidadePermissoes": 5,
    "quantidadeUsuarios": 3
  }
]
```

---

## `GET /api/perfis/{idPerfil}` — detalhes do perfil

**Resposta `200`**

```json
{
  "idPerfil": 1,
  "nome": "Administrador",
  "permissoes": [
    { "idPermissao": 1, "nome": "ACESSO_TOTAL", "descricao": "Acesso total aos recursos da API" },
    { "idPermissao": 3, "nome": "CADASTRAR_ESPECIALISTA", "descricao": "Cadastrar especialistas" }
  ],
  "usuarios": [
    { "idCadastro": 1, "nome": "Administrador do Sistema", "cpf": "99999999999", "statusCadastro": "ATIVO" },
    { "idCadastro": 9, "nome": "Mariana Costa", "cpf": "12345678902", "statusCadastro": "ATIVO" }
  ]
}
```

- `permissoes` vem ordenado pelo nome da permissão; `usuarios`, pelo nome da pessoa.
- `usuarios` inclui cadastros inativos (confira `statusCadastro`); eles continuam presos ao perfil, mas não conseguem fazer login.
- O `idCadastro` de cada usuário é o mesmo usado nas rotas de profissional e especialista.

**Resposta `404`:** `Perfil não encontrado com ID: {id}`.

---

## `POST /api/perfis` — cadastrar perfil

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `nome` | string | sim | único, sem diferenciar maiúsculas de minúsculas; espaços nas pontas são removidos |
| `permissoesIds` | lista de número | não | permissões iniciais; IDs repetidos são unificados. Sem a lista, o perfil nasce sem permissões |

```json
{
  "nome": "Recepção",
  "permissoesIds": [2]
}
```

**Resposta `201 Created`:** os detalhes do perfil criado (mesmo formato do `GET /api/perfis/{idPerfil}`, com `usuarios: []`).

---

## `PATCH /api/perfis/{idPerfil}` — atualizar perfil

Segue as regras comuns de PATCH (`00-GERAL.md`).

| Campo | Observação |
|---|---|
| `nome` | não pode ser vazio nem `null`; não pode repetir o nome de outro perfil |
| `permissoesIds` | **substitui** todas as permissões do perfil; `[]` ou `null` deixa o perfil sem permissões |

Para adicionar ou remover uma permissão de cada vez, prefira as rotas `/permissoes` abaixo, que não exigem reenviar a lista inteira.

```json
{
  "nome": "Recepção e Triagem"
}
```

**Resposta `200`:** os detalhes atualizados do perfil.

---

## `DELETE /api/perfis/{idPerfil}` — excluir perfil

Perfis não têm status: o `DELETE` **exclui** o perfil (e os vínculos dele com permissões) e retorna `204 No Content`.

Só é possível excluir um perfil **sem usuários**. Antes, troque o perfil desses usuários pelo PATCH de profissional/especialista (`idPerfil`).

---

## `POST /api/perfis/{idPerfil}/permissoes` — adicionar permissões

```json
{
  "permissoesIds": [3, 4]
}
```

- `permissoesIds` é obrigatório e precisa ter ao menos um ID.
- Permissões que o perfil já possui são ignoradas (não geram erro nem duplicidade).
- Se algum ID não existir, nada é adicionado e a resposta lista os IDs inválidos.

**Resposta `200`:** os detalhes atualizados do perfil.

---

## `DELETE /api/perfis/{idPerfil}/permissoes/{idPermissao}` — remover permissão

Remove uma permissão do perfil. A permissão continua existindo no sistema.

**Resposta `200`:** os detalhes atualizados do perfil (diferente dos outros DELETE, esta rota devolve corpo, para a tela ser atualizada).

**Resposta `404`:** `O perfil não possui a permissão com ID: {id}`.

---

## `GET /api/permissoes` — listar permissões

Lista todas as permissões do sistema, ordenadas pelo nome, sem paginação.

```json
[
  { "idPermissao": 1, "nome": "ACESSO_TOTAL", "descricao": "Acesso total aos recursos da API" },
  { "idPermissao": 2, "nome": "CADASTRAR_PROFISSIONAL", "descricao": "Cadastrar profissionais" },
  { "idPermissao": 3, "nome": "CADASTRAR_ESPECIALISTA", "descricao": "Cadastrar especialistas" },
  { "idPermissao": 4, "nome": "CADASTRAR_ESPECIALISTA_PJ", "descricao": "Cadastrar especialistas pessoa juridica" },
  { "idPermissao": 5, "nome": "CADASTRAR_USUARIO", "descricao": "Cadastrar usuarios" }
]
```

As permissões acima são as iniciais. A lista definitiva será criada por migration; não há rotas para cadastrar, alterar ou excluir permissões.

## `GET /api/permissoes/{idPermissao}` — detalhes da permissão

**Resposta `200`:** um item no mesmo formato da listagem. **`404`:** `Permissão não encontrada com ID: {id}`.

---

## Mensagens de erro

### Validação de campos (`400`, em `fields`)

| Campo | Mensagem |
|---|---|
| `nome` (POST) | O nome do perfil é obrigatório |
| `permissoesIds` (adicionar) | Informe ao menos uma permissão |
| `permissoesIds[]` (adicionar) | O ID da permissão não pode ser nulo |

### Regras de negócio (`message`)

| Situação | Status | Mensagem |
|---|---|---|
| Nome de perfil já existente | `400` | Já existe um perfil com esse nome. |
| `nome` vazio no PATCH | `400` | O nome do perfil não pode ser vazio. |
| ID de permissão inexistente | `400` | Permissão(ões) não encontrada(s): {ids} |
| Excluir perfil com usuários | `400` | Não é possível excluir o perfil: há usuários vinculados a ele. |
| Remover permissão que o perfil não tem | `404` | O perfil não possui a permissão com ID: {id} |
| Perfil inexistente | `404` | Perfil não encontrado com ID: {id} |
| Permissão inexistente (GET) | `404` | Permissão não encontrada com ID: {id} |

---

## Roteiro rápido de testes

1. `GET /api/perfis`. Esperado: "Administrador" com `quantidadeUsuarios` ≥ 1.
2. `GET /api/perfis/1`. Esperado: as permissões e o CPF `99999999999` em `usuarios`.
3. `POST /api/perfis` com `{"nome": "Recepção", "permissoesIds": [2]}`. Esperado: `201` com uma permissão.
4. Repetir o passo 3 com `"nome": "recepção"`. Esperado: `400` (nome já existe).
5. `POST /api/perfis/{id}/permissoes` com `{"permissoesIds": [2, 3]}`. Esperado: `200` com duas permissões (a `2` não duplica).
6. `POST /api/perfis/{id}/permissoes` com `{"permissoesIds": [999]}`. Esperado: `400` com `Permissão(ões) não encontrada(s): 999`.
7. `DELETE /api/perfis/{id}/permissoes/2`. Esperado: `200` com só a permissão `3`. Repetir: `404`.
8. Conceder acesso a um profissional com o novo `idPerfil` (ver `00-GERAL.md`) e `GET /api/perfis/{id}`. Esperado: o CPF dele em `usuarios`.
9. `DELETE /api/perfis/{id}`. Esperado: `400` (há usuário). Trocar o `idPerfil` do profissional para `1` e repetir. Esperado: `204`.
