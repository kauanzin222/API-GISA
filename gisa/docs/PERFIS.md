# API GISA — Perfis de acesso e Permissões

Autenticação, convenções, enums e formato de erro: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

- **Permissão** = uma funcionalidade do sistema (ex.: `CADASTRAR_PROFISSIONAL`). As permissões são definidas pelo próprio sistema, por migration; a API **só as consulta**.
- **Perfil** = um conjunto de permissões (ex.: "Administrador", "Recepção"). Cada usuário de acesso tem exatamente um perfil. O perfil do usuário é escolhido ao conceder acesso (ver `00-GERAL.md`) e pode ser trocado por `PATCH /api/usuarios/{idCadastro}/perfil` (ver `USUARIOS.md`) ou pelo `idPerfil` no PATCH de profissional/especialista.
- Toda rota da API exige uma permissão (tabela completa em `00-GERAL.md`, seção "Permissões por rota"). Mudanças no perfil valem na próxima requisição do usuário, sem novo login.

**Permissão exigida neste módulo:** `GERENCIAR_PERFIS` em todas as rotas, exceto `GET /api/perfis`, que aceita também `GERENCIAR_USUARIOS` (a tela de conceder acesso precisa listar os perfis). Sem ela: `403`.

---

## Permissões do sistema

Criadas pelas migrations `V6`, `V15` e `V18` (os IDs 1 a 5 vêm da `V6`; o 5 foi renomeado de `CADASTRAR_USUARIO`). Quem tem `ACESSO_TOTAL` pode tudo.

Cada permissão tem uma **`categoria`** (para agrupar na tela) e uma **`descricao`** (frase curta do que ela libera), preenchidas pela migration `V18`. A coluna "Categoria" abaixo é exatamente o valor de `categoria`.

| Categoria | Permissão | O que libera |
|---|---|---|
| Segurança | `ACESSO_TOTAL` | todas as rotas |
| | `GERENCIAR_USUARIOS` | conceder acesso, trocar perfil, gerar senha temporária e retirar acesso |
| | `GERENCIAR_PERFIS` | perfis e permissões (este módulo) |
| | `VISUALIZAR_AUDITORIA` | consultar a auditoria |
| Profissionais | `CADASTRAR_PROFISSIONAL` | cadastrar profissionais e assistentes sociais |
| | `CADASTRAR_ESPECIALISTA` | cadastrar especialistas |
| | `CADASTRAR_ESPECIALISTA_PJ` | cadastrar especialistas PJ |
| | `VISUALIZAR_PROFISSIONAIS` | listar e ver profissionais, especialistas e assistentes sociais |
| | `EDITAR_PROFISSIONAL` | editar profissionais, especialistas e assistentes sociais, inclusive os vínculos de pacientes do assistente social (trocar o perfil exige também `GERENCIAR_USUARIOS`) |
| | `INATIVAR_PROFISSIONAL` | inativar profissionais, especialistas e assistentes sociais |
| Pacientes | `VISUALIZAR_PACIENTES` | buscar e ver **todos** os pacientes e os responsáveis |
| | `VISUALIZAR_PACIENTES_VINCULADOS` | buscar e ver **só os pacientes vinculados** ao próprio usuário (assistente social; ver `PACIENTES.md`). Não dá acesso às rotas de responsáveis |
| | `CADASTRAR_PACIENTE` | cadastrar pacientes |
| | `EDITAR_PACIENTE` | editar pacientes e responsáveis, incluir responsáveis |
| | `INATIVAR_PACIENTE` | inativar pacientes e responsáveis |
| | `VISUALIZAR_HISTORICO_PACIENTE` | ver o histórico de atividades do paciente (com só `VISUALIZAR_PACIENTES_VINCULADOS`, apenas dos vinculados) |
| Terapias | `VISUALIZAR_TERAPIAS` | listar e ver todas as terapias |
| | `AGENDAR_TERAPIA` | agendar terapias e verificar conflitos |
| | `EDITAR_TERAPIA` | remarcar e trocar participantes |
| | `ALTERAR_STATUS_TERAPIA` | mudar o status, inclusive cancelar |
| | `REGISTRAR_PRESENCA` | registrar presença e falta |
| Documentos | `EMITIR_ATESTADO`, `EMITIR_RECEITA`, `EMITIR_RECUSA` | **só o front usa**; a API não tem rotas de documentos |
| Tabelas de apoio | `GERENCIAR_CARGOS` | cargos e CBOs |
| | `GERENCIAR_ESCOLAS` | listagem, edição e exclusão de escolas |
| | `GERENCIAR_ESPECIALIDADES` | detalhes, cadastro, edição e exclusão de especialidades |

> Mudança de agrupamento: `GERENCIAR_PERFIS` e `VISUALIZAR_AUDITORIA` agora ficam em "Segurança", com `ACESSO_TOTAL` e `GERENCIAR_USUARIOS`.

## Perfis padrão

A migration `V15` garante os perfis abaixo (cria os que não existem, comparando o nome sem diferenciar maiúsculas) e **substitui** as permissões de cada um por exatamente estas. Depois disso, os perfis podem ser alterados pelas rotas deste módulo; a migration não roda de novo.

| Perfil | Permissões | Ideia |
|---|---|---|
| Administrador | `ACESSO_TOTAL` | tudo |
| Recepção | `VISUALIZAR_PACIENTES`, `EDITAR_PACIENTE`, `VISUALIZAR_HISTORICO_PACIENTE`, `VISUALIZAR_TERAPIAS`, `AGENDAR_TERAPIA`, `EDITAR_TERAPIA`, `ALTERAR_STATUS_TERAPIA`, `REGISTRAR_PRESENCA`, `EMITIR_RECUSA` | edita pacientes (não cadastra nem inativa); agenda, edita e muda status de terapias; registra presença |
| Especialista | `VISUALIZAR_PACIENTES`, `VISUALIZAR_HISTORICO_PACIENTE`, `EMITIR_ATESTADO`, `EMITIR_RECEITA` | lê pacientes; vê só as próprias terapias (`/api/terapias/minhas` e o detalhe das terapias de que participa), sem alterar nada |
| Assistente Social | `VISUALIZAR_PACIENTES_VINCULADOS`, `VISUALIZAR_HISTORICO_PACIENTE` | lê só os pacientes vinculados a ele (e o histórico deles) |

> A migration `V18` ajustou o perfil "Assistente Social": tirou `VISUALIZAR_PACIENTES` e incluiu `VISUALIZAR_PACIENTES_VINCULADOS` (e `VISUALIZAR_HISTORICO_PACIENTE`, se faltasse). Outras permissões que o perfil já tinha foram mantidas. Se o perfil tinha sido excluído, ele foi criado de novo.

> O especialista não tem `VISUALIZAR_TERAPIAS`. Ele vê as terapias de que participa por `GET /api/terapias/minhas` e `GET /api/terapias/{id}` (ver `TERAPIAS.md`).

> **Atenção ao deploy:** a `V15` reduz o perfil "Administrador" a `ACESSO_TOTAL` (antes ele tinha as 5 permissões iniciais). Usuários de outros perfis criados antes desta versão passam a ver só o que as permissões do perfil deles liberam.

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
    "quantidadePermissoes": 1,
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
    { "idPermissao": 1, "nome": "ACESSO_TOTAL", "descricao": "Acesso total: libera todas as funcionalidades do sistema", "categoria": "Segurança" },
    { "idPermissao": 3, "nome": "CADASTRAR_ESPECIALISTA", "descricao": "Cadastrar especialistas", "categoria": "Profissionais" }
  ],
  "usuarios": [
    { "idCadastro": 1, "nome": "Administrador do Sistema", "cpf": "99999999999", "statusCadastro": "ATIVO" },
    { "idCadastro": 9, "nome": "Mariana Costa", "cpf": "12345678062", "statusCadastro": "ATIVO" }
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

> **Proteção contra trancar o sistema.** Se as novas permissões tirarem do perfil `GERENCIAR_PERFIS` e `ACESSO_TOTAL` e, com isso, não sobrar nenhum usuário **ativo** que gerencie perfis (em qualquer perfil), a resposta é `409` com `Operação bloqueada: o sistema precisa de ao menos um usuário ativo com permissão para gerenciar perfis (GERENCIAR_PERFIS ou ACESSO_TOTAL).` e nada é gravado. Vale também para `DELETE /api/perfis/{idPerfil}/permissoes/{idPermissao}`.

---

## `DELETE /api/perfis/{idPerfil}` — excluir perfil

Perfis não têm status: o `DELETE` **exclui** o perfil (e os vínculos dele com permissões) e retorna `204 No Content`.

Só é possível excluir um perfil **sem usuários**. Antes, troque o perfil desses usuários (`PATCH /api/usuarios/{idCadastro}/perfil`) ou retire o acesso deles (`DELETE /api/usuarios/{idCadastro}`); ver `USUARIOS.md`. Como o perfil excluído não tem usuários, a exclusão nunca quebra a proteção contra trancar o sistema; a verificação (`409`) existe por garantia.

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

**Resposta `404`:** `O perfil não possui a permissão com ID: {id}`. **`409`:** a remoção deixaria o sistema sem usuário ativo que gerencie perfis (ver o PATCH acima).

---

## `GET /api/permissoes` — listar permissões

Lista todas as permissões do sistema, ordenadas pelo nome, sem paginação.

```json
[
  { "idPermissao": 1, "nome": "ACESSO_TOTAL", "descricao": "Acesso total: libera todas as funcionalidades do sistema", "categoria": "Segurança" },
  { "idPermissao": 15, "nome": "AGENDAR_TERAPIA", "descricao": "Agendar terapias e verificar conflitos de horário", "categoria": "Terapias" },
  { "idPermissao": 17, "nome": "ALTERAR_STATUS_TERAPIA", "descricao": "Iniciar, concluir e cancelar terapias", "categoria": "Terapias" }
]
```

- `categoria` e `descricao` vêm também em todas as outras respostas que listam permissões (detalhes do perfil, `GET /api/usuarios/me`, detalhes do usuário e o bloco `usuario` dos profissionais).
- A ordem continua por nome; para agrupar na tela, use `categoria`.

São as 27 permissões da tabela [Permissões do sistema](#permissões-do-sistema) (trecho acima). Não há rotas para cadastrar, alterar ou excluir permissões; mudanças na lista vêm por migration. Os IDs a partir do 6 dependem da ordem de criação: o front deve usar o **nome** da permissão, e não o ID.

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
| Mudança nas permissões (PATCH ou remoção) ou exclusão que deixaria o sistema sem usuário ativo com `GERENCIAR_PERFIS` ou `ACESSO_TOTAL` | `409` | Operação bloqueada: o sistema precisa de ao menos um usuário ativo com permissão para gerenciar perfis (GERENCIAR_PERFIS ou ACESSO_TOTAL). |
| Perfil inexistente | `404` | Perfil não encontrado com ID: {id} |
| Permissão inexistente (GET) | `404` | Permissão não encontrada com ID: {id} |

---

## Roteiro rápido de testes

**Lista de permissões e perfis padrão (migrations `V15` e `V18`)**

1. `GET /api/permissoes`. Esperado: 27 permissões, todas com `categoria` e `descricao`, com `GERENCIAR_USUARIOS` no ID 5, `VISUALIZAR_PACIENTES_VINCULADOS` na categoria "Pacientes" e sem `CADASTRAR_USUARIO`.
2. `GET /api/perfis`. Esperado: "Administrador" (1 permissão), "Recepção" (9), "Especialista" (4) e "Assistente Social" (2: `VISUALIZAR_PACIENTES_VINCULADOS` e `VISUALIZAR_HISTORICO_PACIENTE`).
3. `GET /api/perfis/{id}` da "Recepção". Esperado: as 9 permissões da tabela de perfis padrão, sem `CADASTRAR_PACIENTE` nem `INATIVAR_PACIENTE`.

**Gestão de perfis** (com o token do administrador)

4. `POST /api/perfis` com `{"nome": "Triagem", "permissoesIds": [2]}`. Esperado: `201` com uma permissão.
5. Repetir o passo 4 com `"nome": "triagem"`. Esperado: `400` (nome já existe).
6. `POST /api/perfis/{id}/permissoes` com `{"permissoesIds": [2, 3]}`. Esperado: `200` com duas permissões (a `2` não duplica).
7. `POST /api/perfis/{id}/permissoes` com `{"permissoesIds": [999]}`. Esperado: `400` com `Permissão(ões) não encontrada(s): 999`.
8. `DELETE /api/perfis/{id}/permissoes/2`. Esperado: `200` com só a permissão `3`. Repetir: `404`.
9. Conceder acesso a um profissional com o novo `idPerfil` (ver `00-GERAL.md`) e `GET /api/perfis/{id}`. Esperado: o CPF dele em `usuarios`.
10. `DELETE /api/perfis/{id}`. Esperado: `400` (há usuário). Trocar o perfil do profissional (`PATCH /api/usuarios/{idCadastro}/perfil` com `{"idPerfil": 1}`) e repetir. Esperado: `204`.

**Sem permissão**

11. Com o token de um usuário "Recepção": `GET /api/perfis`, `GET /api/permissoes` e `POST /api/perfis`. Esperado: `403`.
12. Com o token de um usuário que tem `GERENCIAR_USUARIOS` mas não `GERENCIAR_PERFIS`: `GET /api/perfis`. Esperado: `200`. `GET /api/perfis/1`. Esperado: `403`.
13. Com o administrador, tirar uma permissão de um perfil (ex.: `DELETE /api/perfis/{idRecepcao}/permissoes/{idVisualizarPacientes}`) e, **sem novo login**, chamar `GET /api/pacientes?termo=lucas` com um usuário desse perfil. Esperado: `403` (vale na próxima requisição). Devolva a permissão depois (`POST /api/perfis/{idRecepcao}/permissoes`).

**Proteção contra trancar o sistema** (supondo que só o administrador inicial gerencia perfis)

14. `DELETE /api/perfis/{idAdministrador}/permissoes/1` (tirar `ACESSO_TOTAL` do perfil "Administrador"). Esperado: `409` `Operação bloqueada: ...`; o perfil continua com a permissão. O mesmo com `PATCH /api/perfis/{idAdministrador}` e `{"permissoesIds": []}`.
15. Criar um perfil `{"nome": "Gestor de perfis", "permissoesIds": [<id de GERENCIAR_PERFIS>]}`, dar esse perfil a um profissional ativo e repetir o passo 14. Esperado: `200` (sobrou um gestor de perfis). O administrador perde o acesso total na próxima requisição: devolva `ACESSO_TOTAL` ao "Administrador" logado com o usuário "Gestor de perfis" (`POST /api/perfis/{idAdministrador}/permissoes` com `{"permissoesIds": [1]}`).
