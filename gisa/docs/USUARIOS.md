# API GISA — Usuários: usuário logado, senha e administração de acessos

Autenticação, convenções e formato de erro: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

## Rotas

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| `GET` | `/api/usuarios/me` | autenticado | dados de quem está logado (nome, tipo de cadastro, perfil e permissões) |
| `PATCH` | `/api/usuarios/minha-senha` | autenticado | altera a própria senha |
| `GET` | `/api/usuarios` | `GERENCIAR_USUARIOS` | lista os usuários de acesso (paginado) |
| `GET` | `/api/usuarios/{idCadastro}` | `GERENCIAR_USUARIOS` | detalhes do acesso de um cadastro |
| `PATCH` | `/api/usuarios/{idCadastro}/perfil` | `GERENCIAR_USUARIOS` | troca o perfil |
| `PATCH` | `/api/usuarios/{idCadastro}/senha` | `GERENCIAR_USUARIOS` | redefine a senha de outro usuário |
| `DELETE` | `/api/usuarios/{idCadastro}` | `GERENCIAR_USUARIOS` | retira o acesso (apaga só o usuário) |

`ACESSO_TOTAL` também libera todas as rotas. Sem a permissão: `403` com `Você não tem permissão para realizar esta ação.`

O `{idCadastro}` é o ID da **pessoa** dona do usuário: o mesmo das rotas de profissional e especialista (e o que aparece em `usuarios` de `GET /api/perfis/{idPerfil}`). Conceder acesso a quem ainda não tem usuário continua nas rotas `POST .../{idCadastro}/usuario` de profissional e especialista (ver `00-GERAL.md`).

---

## `GET /api/usuarios/me` — usuário logado

O token só carrega o CPF. Esta rota diz ao front **quem está logado**, para mostrar o nome no cabeçalho e abrir o painel certo depois do login. O usuário é identificado pelo token, então nenhum ID é enviado.

**Resposta `200`**

```json
{
  "idCadastro": 9,
  "nome": "Mariana Costa",
  "cpf": "12345678902",
  "tipoCadastro": "ESPECIALISTA",
  "perfil": { "idPerfil": 3, "nome": "Especialista" },
  "permissoes": [
    { "idPermissao": 6, "nome": "VISUALIZAR_PACIENTES", "descricao": "Visualizar pacientes e responsáveis" },
    { "idPermissao": 10, "nome": "VISUALIZAR_HISTORICO_PACIENTE", "descricao": "Visualizar o histórico de atividades do paciente" },
    { "idPermissao": 19, "nome": "EMITIR_ATESTADO", "descricao": "Emitir atestados" },
    { "idPermissao": 20, "nome": "EMITIR_RECEITA", "descricao": "Emitir receitas" }
  ]
}
```

| `tipoCadastro` | Quem é | Rota de detalhes |
|---|---|---|
| `PROFISSIONAL` | profissional comum | `GET /api/profissionais/{idCadastro}` |
| `ESPECIALISTA` | especialista | `GET /api/especialistas/{idCadastro}` |
| `ESPECIALISTA_PJ` | especialista PJ | `GET /api/especialistas/pj/{idCadastro}` |
| `PESSOA` | cadastro sem vínculo de profissional. Hoje, **só o administrador inicial** (criado pela migration `V6` direto na tabela de pessoas) | — |

- `permissoes` vem ordenada por `idPermissao`; `[]` quando o perfil não tem permissões.
- Para decidir o que mostrar na tela, use as **permissões**, e não o nome do perfil, que pode ser renomeado.
- É uma consulta comum: **não** gera registro na auditoria.
- Token ausente, inválido ou expirado: `401`/`403`, como nas demais rotas.

---

A senha pode ser trocada de duas formas:

| Quem | Rota | Quando usar |
|---|---|---|
| O próprio usuário | `PATCH /api/usuarios/minha-senha` | tela "Alterar minha senha"; exige a senha atual |
| Administrador | `PATCH /api/usuarios/{idCadastro}/senha` | usuário esqueceu a senha ou a conta precisa ser protegida; não exige a senha atual |

**Regra comum:** a nova senha tem de 8 a 30 caracteres (a mesma regra do cadastro de usuário).

> **Toda troca de senha encerra as sessões abertas.** Os tokens emitidos antes da troca deixam de valer imediatamente, em qualquer dispositivo. Se a senha foi redefinida porque alguém descobriu a antiga, quem estava logado com ela perde o acesso na hora.

---

## `PATCH /api/usuarios/minha-senha` — alterar minha senha

O usuário é identificado pelo token, então nenhum ID é enviado.

| Campo | Obrigatório | Observação |
|---|---|---|
| `senhaAtual` | sim | precisa estar correta |
| `novaSenha` | sim | 8 a 30 caracteres; diferente da atual |
| `confirmacaoNovaSenha` | sim | igual a `novaSenha` |

```json
{
  "senhaAtual": "SenhaAntiga123",
  "novaSenha": "SenhaNova456",
  "confirmacaoNovaSenha": "SenhaNova456"
}
```

**Resposta `200`:** um **token novo**, no mesmo formato do login.

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

> **Importante para o front:** o token usado nesta requisição deixa de valer. Substitua o token salvo pelo da resposta; assim o usuário continua logado sem precisar entrar de novo. Se o token antigo continuar sendo enviado, a API responderá `401`/`403`.

Senha atual incorreta responde **`400`** (e não `401`), para o front não confundir com sessão expirada e deslogar o usuário.

---

## `PATCH /api/usuarios/{idCadastro}/senha` — redefinir senha (administrador)

Exige **`GERENCIAR_USUARIOS`** (ou `ACESSO_TOTAL`). Sem ela: `403`.

```json
{
  "novaSenha": "Temporaria2026"
}
```

**Resposta `204 No Content`.** O usuário perde as sessões abertas e passa a entrar com a nova senha.

**Sugestão de fluxo:** o administrador define uma senha temporária, informa a pessoa pessoalmente, e ela troca em "Alterar minha senha" no primeiro acesso. A API ainda **não obriga** essa troca.

---

## Administração de acessos

Tela de "Usuários do sistema": quem tem acesso, com qual perfil, e as ações de trocar perfil, redefinir senha e retirar acesso. Todas as rotas desta seção exigem **`GERENCIAR_USUARIOS`** (ou `ACESSO_TOTAL`). A senha nunca aparece nas respostas.

### `GET /api/usuarios` — listar usuários (paginado)

Lista só os cadastros **com** usuário de acesso.

| Param | Padrão | Descrição |
|---|---|---|
| `cpf` | — | trecho do CPF; máscara ignorada |
| `nome` | — | trecho do nome, sem diferenciar maiúsculas |
| `idPerfil` | — | só usuários deste perfil |
| `tipoCadastro` | todos | `PESSOA`, `PROFISSIONAL`, `ESPECIALISTA`, `ESPECIALISTA_PJ`; aceita mais de um (`?tipoCadastro=ESPECIALISTA&tipoCadastro=ESPECIALISTA_PJ`) |
| `statusCadastro` | todos | `ATIVO` ou `INATIVO` |
| `page` / `size` / `sort` | `0` / `9` / `nome,asc` | `sort` aceita `nome`, `cpf` e `statusCadastro` |

```
GET /api/usuarios?tipoCadastro=ESPECIALISTA&statusCadastro=ATIVO
```

**Resposta `200`** (paginação no objeto `page`, como nas demais listagens):

```json
{
  "content": [
    {
      "idCadastro": 9,
      "nome": "Mariana Costa",
      "cpf": "12345678902",
      "tipoCadastro": "ESPECIALISTA",
      "perfil": { "idPerfil": 3, "nome": "Especialista" },
      "statusCadastro": "ATIVO"
    }
  ],
  "page": { "size": 9, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

- Cadastros **inativos** continuam com usuário (e aparecem aqui), mas não conseguem fazer login.
- `idPerfil` inexistente não é erro: a lista vem vazia.
- `PESSOA` é o administrador inicial (ver tabela de `tipoCadastro` acima).

### `GET /api/usuarios/{idCadastro}` — detalhes do acesso

**Resposta `200`**

```json
{
  "idCadastro": 9,
  "nome": "Mariana Costa",
  "cpf": "12345678902",
  "email": "mariana.costa@email.com",
  "tipoCadastro": "ESPECIALISTA",
  "statusCadastro": "ATIVO",
  "cargo": { "idCargo": 1, "nome": "Psicóloga" },
  "perfil": { "idPerfil": 3, "nome": "Especialista" },
  "permissoes": [
    { "idPermissao": 6, "nome": "VISUALIZAR_PACIENTES", "descricao": "Visualizar pacientes e responsáveis" }
  ]
}
```

- `email` e `cargo` vêm `null` para o tipo `PESSOA` (administrador inicial).
- `permissoes` são as do perfil, ordenadas por `idPermissao` (como em `/me`).
- Cadastro que existe mas não tem usuário: `404` `Este cadastro não possui usuário de acesso. ID: {id}`. Cadastro inexistente: `404` `Cadastro não encontrado com ID: {id}`. Valem também para trocar perfil e retirar acesso.

### `PATCH /api/usuarios/{idCadastro}/perfil` — trocar o perfil

```json
{
  "idPerfil": 2
}
```

**Resposta `200`:** os detalhes atualizados (mesmo formato do `GET /api/usuarios/{idCadastro}`).

- Funciona para qualquer tipo de cadastro, inclusive `PESSOA` (que não tem PATCH próprio).
- O novo perfil vale na **próxima requisição** do usuário, sem novo login e sem invalidar o token.
- O `idPerfil` no PATCH de profissional/especialista continua funcionando, com as mesmas regras (e também exige `GERENCIAR_USUARIOS`; ver `PROFISSIONAIS.md`).

### `DELETE /api/usuarios/{idCadastro}` — retirar o acesso

Apaga **só o usuário** (login, senha e perfil). O cadastro da pessoa continua como está: o profissional segue ativo, nas terapias, na listagem de profissionais etc.

**Resposta `204 No Content`.**

- Os tokens já emitidos para esse usuário deixam de valer **na hora** (`401`/`403` na próxima requisição), em qualquer dispositivo.
- O acesso pode ser concedido de novo pelo `POST .../{idCadastro}/usuario` de profissional/especialista (ver `00-GERAL.md`), com perfil e senha novos. Os tokens antigos **continuam recusados** depois disso.
- O usuário some de `usuarios` em `GET /api/perfis/{idPerfil}`, o que permite excluir um perfil que só ele usava.
- O histórico da auditoria é mantido (ela guarda o CPF, não o usuário).

### Travas de segurança (trocar perfil e retirar acesso)

| Situação | Status | Mensagem |
|---|---|---|
| Retirar o próprio acesso | `400` | Você não pode retirar o seu próprio acesso. |
| Deixar o sistema sem nenhum usuário **ativo** com `ACESSO_TOTAL` | `400` | O sistema precisa de ao menos um usuário ativo com acesso total. |
| Perfil inexistente | `400` | Perfil não encontrado com ID: {id} |
| Cadastro inexistente | `404` | Cadastro não encontrado com ID: {id} |

- A trava do administrador conta só usuários com cadastro **ativo** cujo perfil tem `ACESSO_TOTAL`. Ela vale ao retirar o acesso e ao trocar para um perfil **sem** `ACESSO_TOTAL` (por esta rota ou pelo `idPerfil` do PATCH de profissional).
- Trocar o **próprio** perfil é permitido, desde que não deixe o sistema sem administrador ativo.
- Duas retiradas simultâneas esperam uma pela outra, para não removerem juntas os dois últimos administradores.

---

## Mensagens de erro

### Validação de campos (`400`, em `fields`)

| Campo | Mensagem |
|---|---|
| `senhaAtual` | A senha atual é obrigatória. |
| `novaSenha` | A nova senha é obrigatória. / A senha deve conter entre 8 e 30 caracteres. |
| `confirmacaoNovaSenha` | A confirmação da nova senha é obrigatória. |
| `idPerfil` (trocar perfil) | O ID do perfil é obrigatório. |

### Regras de negócio (`message`)

| Situação | Status | Mensagem |
|---|---|---|
| Senha atual incorreta | `400` | A senha atual está incorreta. |
| Confirmação diferente da nova senha | `400` | A confirmação não corresponde à nova senha. |
| Nova senha igual à atual | `400` | A nova senha deve ser diferente da senha atual. |
| Retirar o próprio acesso | `400` | Você não pode retirar o seu próprio acesso. |
| Último administrador ativo | `400` | O sistema precisa de ao menos um usuário ativo com acesso total. |
| Perfil inexistente (trocar perfil) | `400` | Perfil não encontrado com ID: {id} |
| Sem `GERENCIAR_USUARIOS` | `403` | Você não tem permissão para realizar esta ação. |
| Cadastro sem usuário de acesso | `404` | Este cadastro não possui usuário de acesso. ID: {id} |
| Cadastro inexistente (detalhes, perfil, retirar acesso) | `404` | Cadastro não encontrado com ID: {id} |

> A redefinição de senha responde `Este cadastro não possui usuário de acesso. ID: {id}` também para cadastro inexistente (comportamento anterior, mantido).

---

## Roteiro rápido de testes

Pré-requisito: um profissional com acesso no perfil "Recepção" (crie com `POST /api/profissionais` e conceda com `POST /api/profissionais/{id}/usuario`), com o `{idCadastro}` dele. Tokens: `ADM` (administrador) e `REC` (recepção).

**Usuário logado**

- `GET /api/usuarios/me` com `ADM`. Esperado: `200`, `nome: "Administrador do Sistema"`, `tipoCadastro: "PESSOA"`, perfil `Administrador` e só a permissão `ACESSO_TOTAL`.
- O mesmo com `REC`. Esperado: `tipoCadastro: "PROFISSIONAL"`, o `idCadastro` dele e as 9 permissões da recepção.
- `GET /api/auditoria`. Esperado: nenhum registro de `/api/usuarios/me`.

**Senha**

1. `PATCH /api/usuarios/minha-senha` com `REC` e o exemplo acima (usando a senha real como `senhaAtual`). Esperado: `200` com um token novo `REC2`.
2. `GET /api/usuarios/me` com `REC`. Esperado: `401`/`403` (token antigo invalidado). Com `REC2`: `200`.
3. Login com a senha antiga. Esperado: `401`. Com a nova: `200`.
4. `PATCH /api/usuarios/minha-senha` com `senhaAtual` errada. Esperado: `400` `A senha atual está incorreta.`
5. `PATCH /api/usuarios/{idCadastro}/senha` com `REC2`. Esperado: `403`.
6. O mesmo com `ADM` e `{"novaSenha": "Temporaria2026"}`. Esperado: `204`; o `REC2` deixa de valer e o login com `Temporaria2026` funciona (novo token `REC3`).

**Administração de acessos**

7. `GET /api/usuarios` com `REC3`. Esperado: `403`. Com `ADM`: `200`, com o administrador (`PESSOA`) e o profissional.
8. `GET /api/usuarios?tipoCadastro=PESSOA`. Esperado: só o administrador. `GET /api/usuarios/{idAdministrador}`: `email` e `cargo` nulos.
9. `PATCH /api/usuarios/{idCadastro}/perfil` com `{"idPerfil": 999}`. Esperado: `400` `Perfil não encontrado com ID: 999`.
10. `GET /api/usuarios/{id}` de um paciente. Esperado: `404` `Este cadastro não possui usuário de acesso. ID: {id}`.

**Travas de segurança** (supondo que o administrador inicial é o único usuário ativo com `ACESSO_TOTAL`)

11. `DELETE /api/usuarios/{idAdministrador}` com `ADM`. Esperado: `400` `Você não pode retirar o seu próprio acesso.`
12. Com `ADM`, criar o perfil `{"nome": "Gestor de acessos", "permissoesIds": [5]}` (`POST /api/perfis`; o ID 5 é `GERENCIAR_USUARIOS`) e trocar o perfil do profissional para ele (`PATCH /api/usuarios/{idCadastro}/perfil`). Login com o profissional: token `GES`.
13. Com `GES`, `PATCH /api/usuarios/{idAdministrador}/perfil` com o ID da "Recepção". Esperado: `400` `O sistema precisa de ao menos um usuário ativo com acesso total.` O mesmo para `DELETE /api/usuarios/{idAdministrador}`.
14. Com `GES`, `DELETE /api/usuarios/{idCadastro}` (o próprio). Esperado: `400` `Você não pode retirar o seu próprio acesso.`

**Retirar acesso e token antigo**

15. Com `ADM`, `DELETE /api/usuarios/{idCadastro}` do profissional. Esperado: `204`.
16. `GET /api/usuarios/me` com `GES`. Esperado: `401`/`403`. Login com o CPF dele. Esperado: `401`.
17. `GET /api/profissionais/{idCadastro}`. Esperado: `200`, cadastro `ATIVO` e `usuario: null`.
18. `POST /api/profissionais/{idCadastro}/usuario` com `{"senha": "SenhaTeste123", "idPerfil": 2}` (ID da "Recepção" em `GET /api/perfis`). Esperado: `201`. O token `GES` continua recusado; o login com a senha nova funciona.
