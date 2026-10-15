# API GISA — Usuários: usuário logado, senha e administração de acessos

Autenticação, convenções e formato de erro: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

## Rotas

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| `GET` | `/api/usuarios/me` | autenticado | dados de quem está logado (nome, e-mail, tipo de cadastro, perfil e permissões) |
| `PATCH` | `/api/usuarios/minha-senha` | autenticado | altera a própria senha |
| `GET` | `/api/usuarios` | `GERENCIAR_USUARIOS` | lista os usuários de acesso (paginado) |
| `GET` | `/api/usuarios/{idCadastro}` | `GERENCIAR_USUARIOS` | detalhes do acesso de um cadastro |
| `PATCH` | `/api/usuarios/{idCadastro}/perfil` | `GERENCIAR_USUARIOS` | troca o perfil |
| `POST` | `/api/usuarios/{idCadastro}/senha-temporaria` | `GERENCIAR_USUARIOS` | gera uma nova senha temporária e a envia por e-mail (também serve para reenviar o acesso) |
| `DELETE` | `/api/usuarios/{idCadastro}` | `GERENCIAR_USUARIOS` | retira o acesso (apaga só o usuário) |

> `PATCH /api/usuarios/{idCadastro}/senha` (o administrador digitava a senha nova) **foi removida**: foi substituída por `POST /api/usuarios/{idCadastro}/senha-temporaria`. Login, "esqueci minha senha" e primeiro acesso ficam em `00-GERAL.md` ("Autenticação").

`ACESSO_TOTAL` também libera todas as rotas. Sem a permissão: `403` com `Você não tem permissão para realizar esta ação.`

O `{idCadastro}` é o ID da **pessoa** dona do usuário: o mesmo das rotas de profissional e especialista (e o que aparece em `usuarios` de `GET /api/perfis/{idPerfil}`). Conceder acesso a quem ainda não tem usuário continua nas rotas `POST .../{idCadastro}/usuario` de profissional e especialista (ver `00-GERAL.md`).

---

## `GET /api/usuarios/me` — usuário logado

O token só identifica o usuário. Esta rota diz ao front **quem está logado**, para mostrar o nome no cabeçalho e abrir o painel certo depois do login. O usuário é identificado pelo token, então nenhum ID é enviado.

**Resposta `200`**

```json
{
  "idCadastro": 9,
  "nome": "Mariana Costa",
  "cpf": "12345678062",
  "email": "mariana.costa@email.com",
  "tipoCadastro": "ESPECIALISTA",
  "perfil": { "idPerfil": 3, "nome": "Especialista" },
  "permissoes": [
    { "idPermissao": 6, "nome": "VISUALIZAR_PACIENTES", "descricao": "Buscar e ver todos os pacientes e responsáveis", "categoria": "Pacientes" },
    { "idPermissao": 10, "nome": "VISUALIZAR_HISTORICO_PACIENTE", "descricao": "Ver o histórico de atividades do paciente", "categoria": "Pacientes" },
    { "idPermissao": 19, "nome": "EMITIR_ATESTADO", "descricao": "Emitir atestados", "categoria": "Documentos" },
    { "idPermissao": 20, "nome": "EMITIR_RECEITA", "descricao": "Emitir receitas", "categoria": "Documentos" }
  ]
}
```

| `tipoCadastro` | Quem é | Rota de detalhes |
|---|---|---|
| `PROFISSIONAL` | profissional comum | `GET /api/profissionais/{idCadastro}` |
| `ESPECIALISTA` | especialista | `GET /api/especialistas/{idCadastro}` |
| `ESPECIALISTA_PJ` | especialista PJ | `GET /api/especialistas/pj/{idCadastro}` |
| `ASSISTENTE_SOCIAL` | assistente social | `GET /api/assistentes-sociais/{idCadastro}`; o painel dele usa `GET /api/assistentes-sociais/me/pacientes` |
| `PESSOA` | cadastro sem vínculo de profissional. Hoje, **só o administrador inicial** (criado pela migration `V6` direto na tabela de pessoas) | — |

- `email` é o login do usuário. O administrador inicial recebeu `admin@gisa.local` pela migration `V16`.
- `permissoes` vem ordenada por `idPermissao`; `[]` quando o perfil não tem permissões.
- Para decidir o que mostrar na tela, use as **permissões**, e não o nome do perfil, que pode ser renomeado.
- É uma consulta comum: **não** gera registro na auditoria.
- Token ausente, inválido ou expirado: `401`/`403`, como nas demais rotas.

---

A senha pode ser trocada destas formas:

| Quem | Rota | Quando usar |
|---|---|---|
| O próprio usuário, logado | `PATCH /api/usuarios/minha-senha` | tela "Alterar minha senha"; exige a senha atual |
| O próprio usuário, sem login | `POST /api/auth/esqueci-senha` e `POST /api/auth/redefinir-senha` | esqueceu a senha: recebe um link por e-mail (ver `00-GERAL.md`) |
| O próprio usuário, no primeiro acesso | `POST /api/auth/redefinir-senha` | o login com a senha temporária devolve um `tokenRedefinicao` (ver `00-GERAL.md`) |
| Administrador | `POST /api/usuarios/{idCadastro}/senha-temporaria` | gera uma nova senha temporária e a envia ao e-mail do usuário; também serve para "reenviar acesso" quando o e-mail não chegou |

**Regra comum:** a nova senha tem de 8 a 30 caracteres.

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

## `POST /api/usuarios/{idCadastro}/senha-temporaria` — nova senha temporária (administrador)

Exige **`GERENCIAR_USUARIOS`** (ou `ACESSO_TOTAL`). Sem ela: `403`. **Sem corpo.**

O back gera uma nova senha temporária (12 caracteres, válida por **7 dias**) e a envia ao e-mail do usuário. Serve também como **"reenviar acesso"**, quando o e-mail da concessão não chegou (`emailEnviado: false`).

**Resposta `204 No Content`.**

- A senha anterior (temporária ou definitiva) deixa de valer, as sessões abertas são encerradas e os links de "esqueci minha senha" pendentes são invalidados.
- No próximo login, o usuário cai no fluxo de primeiro acesso e cadastra uma senha nova (ver `00-GERAL.md`).
- Se o e-mail **não puder ser enviado**, nada é alterado (a senha anterior continua valendo) e a resposta é **`503`** com `Não foi possível enviar o e-mail com a nova senha temporária. Nada foi alterado; tente novamente mais tarde.`

| Situação | Status | Mensagem |
|---|---|---|
| Cadastro sem e-mail | `400` | O cadastro precisa ter um e-mail para receber acesso ao sistema. |
| Cadastro sem usuário de acesso | `404` | Este cadastro não possui usuário de acesso. ID: {id} |
| Cadastro inexistente | `404` | Cadastro não encontrado com ID: {id} |
| Falha no envio do e-mail | `503` | Não foi possível enviar o e-mail com a nova senha temporária. Nada foi alterado; tente novamente mais tarde. |

---

## Administração de acessos

Tela de "Usuários do sistema": quem tem acesso, com qual perfil, e as ações de trocar perfil, gerar nova senha temporária e retirar acesso. Todas as rotas desta seção exigem **`GERENCIAR_USUARIOS`** (ou `ACESSO_TOTAL`). A senha nunca aparece nas respostas.

### `GET /api/usuarios` — listar usuários (paginado)

Lista só os cadastros **com** usuário de acesso.

| Param | Padrão | Descrição |
|---|---|---|
| `cpf` | — | trecho do CPF; máscara ignorada |
| `nome` | — | trecho do nome, sem diferenciar maiúsculas |
| `idPerfil` | — | só usuários deste perfil |
| `tipoCadastro` | todos | `PESSOA`, `PROFISSIONAL`, `ESPECIALISTA`, `ESPECIALISTA_PJ`, `ASSISTENTE_SOCIAL`; aceita mais de um (`?tipoCadastro=ESPECIALISTA&tipoCadastro=ESPECIALISTA_PJ`) |
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
      "cpf": "12345678062",
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
  "cpf": "12345678062",
  "email": "mariana.costa@email.com",
  "tipoCadastro": "ESPECIALISTA",
  "statusCadastro": "ATIVO",
  "cargo": { "idCargo": 1, "nome": "Psicóloga" },
  "perfil": { "idPerfil": 3, "nome": "Especialista" },
  "permissoes": [
    { "idPermissao": 6, "nome": "VISUALIZAR_PACIENTES", "descricao": "Buscar e ver todos os pacientes e responsáveis", "categoria": "Pacientes" }
  ]
}
```

- `email` é o login. Para o tipo `PESSOA` (administrador inicial), vem o e-mail dado pela migration `V16` (`admin@gisa.local`) e `cargo` vem `null`.
- `permissoes` são as do perfil, ordenadas por `idPermissao` (como em `/me`).
- Cadastro que existe mas não tem usuário: `404` `Este cadastro não possui usuário de acesso. ID: {id}`. Cadastro inexistente: `404` `Cadastro não encontrado com ID: {id}`. Valem também para trocar perfil, gerar senha temporária e retirar acesso.

### `PATCH /api/usuarios/{idCadastro}/perfil` — trocar o perfil

```json
{
  "idPerfil": 2
}
```

**Resposta `200`:** os detalhes atualizados (mesmo formato do `GET /api/usuarios/{idCadastro}`).

- Funciona para qualquer tipo de cadastro, inclusive `PESSOA` (que não tem PATCH próprio).
- O novo perfil vale na **próxima requisição** do usuário, sem novo login e sem invalidar o token.
- O `idPerfil` no PATCH de profissional, especialista e assistente social continua funcionando, com as mesmas regras (e também exige `GERENCIAR_USUARIOS`; ver `PROFISSIONAIS.md`).

### `DELETE /api/usuarios/{idCadastro}` — retirar o acesso

Apaga **só o usuário** (login, senha e perfil). O cadastro da pessoa continua como está: o profissional segue ativo, nas terapias, na listagem de profissionais etc.

**Resposta `204 No Content`.**

- Os tokens já emitidos para esse usuário deixam de valer **na hora** (`401`/`403` na próxima requisição), em qualquer dispositivo.
- O acesso pode ser concedido de novo pelo `POST .../{idCadastro}/usuario` (ver `00-GERAL.md`), com perfil novo e nova senha temporária enviada por e-mail. Os tokens antigos **continuam recusados** depois disso.
- O usuário some de `usuarios` em `GET /api/perfis/{idPerfil}`, o que permite excluir um perfil que só ele usava.
- O histórico da auditoria é mantido (ela guarda o CPF, não o usuário).

### Travas de segurança (trocar perfil e retirar acesso)

| Situação | Status | Mensagem |
|---|---|---|
| Retirar o próprio acesso | `400` | Você não pode retirar o seu próprio acesso. |
| Deixar o sistema sem nenhum usuário **ativo** cujo perfil tenha `GERENCIAR_PERFIS` ou `ACESSO_TOTAL` | `409` | Operação bloqueada: o sistema precisa de ao menos um usuário ativo com permissão para gerenciar perfis (GERENCIAR_PERFIS ou ACESSO_TOTAL). |
| Deixar o sistema sem nenhum usuário **ativo** com `ACESSO_TOTAL` | `400` | O sistema precisa de ao menos um usuário ativo com acesso total. |
| Perfil inexistente | `400` | Perfil não encontrado com ID: {id} |
| Cadastro inexistente | `404` | Cadastro não encontrado com ID: {id} |

- **Proteção contra trancar o sistema (`409`)**: conta os usuários com cadastro **ativo** cujo perfil tem `GERENCIAR_PERFIS` ou `ACESSO_TOTAL`. Vale ao retirar o acesso e ao trocar para um perfil sem nenhuma das duas (por esta rota ou pelo `idPerfil` do PATCH de profissional), e também ao editar as permissões de um perfil, excluir um perfil e inativar um profissional (ver `00-GERAL.md` e `PERFIS.md`). É verificada **antes** da trava do administrador abaixo.
- A trava do administrador (`400`, que já existia) conta só usuários com cadastro **ativo** cujo perfil tem `ACESSO_TOTAL`. Ela vale ao retirar o acesso e ao trocar para um perfil **sem** `ACESSO_TOTAL`.
- Trocar o **próprio** perfil é permitido, desde que não quebre nenhuma das travas.
- Duas operações simultâneas esperam uma pela outra, para não removerem juntas os dois últimos administradores ou gestores de perfis.

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
| Último usuário ativo que gerencia perfis | `409` | Operação bloqueada: o sistema precisa de ao menos um usuário ativo com permissão para gerenciar perfis (GERENCIAR_PERFIS ou ACESSO_TOTAL). |
| Perfil inexistente (trocar perfil) | `400` | Perfil não encontrado com ID: {id} |
| Cadastro sem e-mail (senha temporária) | `400` | O cadastro precisa ter um e-mail para receber acesso ao sistema. |
| Sem `GERENCIAR_USUARIOS` | `403` | Você não tem permissão para realizar esta ação. |
| Cadastro sem usuário de acesso | `404` | Este cadastro não possui usuário de acesso. ID: {id} |
| Cadastro inexistente (detalhes, perfil, senha temporária, retirar acesso) | `404` | Cadastro não encontrado com ID: {id} |
| E-mail da nova senha temporária não enviado | `503` | Não foi possível enviar o e-mail com a nova senha temporária. Nada foi alterado; tente novamente mais tarde. |

---

## Roteiro rápido de testes

Pré-requisito: um profissional com acesso no perfil "Recepção" (crie com `POST /api/profissionais` e conceda com `POST /api/profissionais/{id}/usuario`), com o `{idCadastro}` dele. Sem SMTP configurado, a senha temporária aparece no log do servidor. Faça o primeiro acesso dele (login com o e-mail e a senha temporária, depois `POST /api/auth/redefinir-senha`). Tokens: `ADM` (administrador) e `REC` (recepção).

**Usuário logado**

- `GET /api/usuarios/me` com `ADM`. Esperado: `200`, `nome: "Administrador do Sistema"`, `email: "admin@gisa.local"`, `tipoCadastro: "PESSOA"`, perfil `Administrador` e só a permissão `ACESSO_TOTAL`.
- O mesmo com `REC`. Esperado: `tipoCadastro: "PROFISSIONAL"`, o `idCadastro` e o `email` dele e as 9 permissões da recepção.
- `GET /api/auditoria`. Esperado: nenhum registro de `/api/usuarios/me`.

**Senha**

1. `PATCH /api/usuarios/minha-senha` com `REC` e o exemplo acima (usando a senha real como `senhaAtual`). Esperado: `200` com um token novo `REC2`.
2. `GET /api/usuarios/me` com `REC`. Esperado: `401`/`403` (token antigo invalidado). Com `REC2`: `200`.
3. Login com a senha antiga. Esperado: `401`. Com a nova: `200` com `senhaTemporaria: false`.
4. `PATCH /api/usuarios/minha-senha` com `senhaAtual` errada. Esperado: `400` `A senha atual está incorreta.`
5. `POST /api/usuarios/{idCadastro}/senha-temporaria` com `REC2`. Esperado: `403`.
6. O mesmo com `ADM`. Esperado: `204`; o `REC2` deixa de valer, a senha anterior deixa de funcionar e o login com a nova senha temporária (no log) responde `senhaTemporaria: true` com um `tokenRedefinicao`. Cadastre a senha nova (`POST /api/auth/redefinir-senha`): token `REC3`.

**Administração de acessos**

7. `GET /api/usuarios` com `REC3`. Esperado: `403`. Com `ADM`: `200`, com o administrador (`PESSOA`) e o profissional.
8. `GET /api/usuarios?tipoCadastro=PESSOA`. Esperado: só o administrador. `GET /api/usuarios/{idAdministrador}`: `email: "admin@gisa.local"` e `cargo` nulo.
9. `PATCH /api/usuarios/{idCadastro}/perfil` com `{"idPerfil": 999}`. Esperado: `400` `Perfil não encontrado com ID: 999`.
10. `GET /api/usuarios/{id}` de um paciente. Esperado: `404` `Este cadastro não possui usuário de acesso. ID: {id}`.

**Travas de segurança** (supondo que o administrador inicial é o único usuário ativo com `ACESSO_TOTAL` ou `GERENCIAR_PERFIS`)

11. `DELETE /api/usuarios/{idAdministrador}` com `ADM`. Esperado: `400` `Você não pode retirar o seu próprio acesso.`
12. Com `ADM`, criar o perfil `{"nome": "Gestor de acessos", "permissoesIds": [5]}` (`POST /api/perfis`; o ID 5 é `GERENCIAR_USUARIOS`) e trocar o perfil do profissional para ele (`PATCH /api/usuarios/{idCadastro}/perfil`). Login com o profissional: token `GES`.
13. Com `GES`, `PATCH /api/usuarios/{idAdministrador}/perfil` com o ID da "Recepção". Esperado: `409` `Operação bloqueada: ...` (o administrador é o único que gerencia perfis). O mesmo para `DELETE /api/usuarios/{idAdministrador}`.
14. Com `GES`, `DELETE /api/usuarios/{idCadastro}` (o próprio). Esperado: `400` `Você não pode retirar o seu próprio acesso.`

**Retirar acesso e token antigo**

15. Com `ADM`, `DELETE /api/usuarios/{idCadastro}` do profissional. Esperado: `204`.
16. `GET /api/usuarios/me` com `GES`. Esperado: `401`/`403`. Login com o e-mail dele. Esperado: `401`.
17. `GET /api/profissionais/{idCadastro}`. Esperado: `200`, cadastro `ATIVO` e `usuario: null`.
18. `POST /api/profissionais/{idCadastro}/usuario` com `{"idPerfil": 2}` (ID da "Recepção" em `GET /api/perfis`). Esperado: `201` com `emailEnviado: true`. O token `GES` continua recusado; o login com a nova senha temporária leva ao primeiro acesso.
