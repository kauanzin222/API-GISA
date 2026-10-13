# API GISA — Usuários: senha

Autenticação, convenções e formato de erro: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

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

Exige que o usuário logado tenha a permissão **`ACESSO_TOTAL`** ou **`CADASTRAR_USUARIO`** (o perfil "Administrador" tem as duas). Sem ela: `403`.

`idCadastro` é o mesmo ID usado nas rotas de profissional e especialista (aparece em `usuarios` de `GET /api/perfis/{idPerfil}`).

```json
{
  "novaSenha": "Temporaria2026"
}
```

**Resposta `204 No Content`.** O usuário perde as sessões abertas e passa a entrar com a nova senha.

**Sugestão de fluxo:** o administrador define uma senha temporária, informa a pessoa pessoalmente, e ela troca em "Alterar minha senha" no primeiro acesso. A API ainda **não obriga** essa troca.

---

## Mensagens de erro

### Validação de campos (`400`, em `fields`)

| Campo | Mensagem |
|---|---|
| `senhaAtual` | A senha atual é obrigatória. |
| `novaSenha` | A nova senha é obrigatória. / A senha deve conter entre 8 e 30 caracteres. |
| `confirmacaoNovaSenha` | A confirmação da nova senha é obrigatória. |

### Regras de negócio (`message`)

| Situação | Status | Mensagem |
|---|---|---|
| Senha atual incorreta | `400` | A senha atual está incorreta. |
| Confirmação diferente da nova senha | `400` | A confirmação não corresponde à nova senha. |
| Nova senha igual à atual | `400` | A nova senha deve ser diferente da senha atual. |
| Redefinir sem permissão | `403` | Você não tem permissão para realizar esta ação. |
| Cadastro sem usuário de acesso (redefinir) | `404` | Este cadastro não possui usuário de acesso. ID: {id} |

---

## Roteiro rápido de testes

Pré-requisito: um profissional com acesso (ex.: `POST /api/profissionais/com-usuario`), com o `{idCadastro}` dele.

1. Login com esse profissional e guardar o token `T1`.
2. `PATCH /api/usuarios/minha-senha` com `T1` e o exemplo acima (usando a senha real como `senhaAtual`). Esperado: `200` com token `T2`.
3. `GET /api/permissoes` com `T1`. Esperado: `401`/`403` (token antigo invalidado).
4. `GET /api/permissoes` com `T2`. Esperado: `200`.
5. Login com a senha antiga. Esperado: `401`. Com a nova: `200`.
6. `PATCH /api/usuarios/minha-senha` com `senhaAtual` errada. Esperado: `400` `A senha atual está incorreta.`
7. Com o token do **profissional** (perfil sem `ACESSO_TOTAL`/`CADASTRAR_USUARIO`), `PATCH /api/usuarios/{idCadastro}/senha`. Esperado: `403`. Se o perfil dele for "Administrador", este passo responde `204`.
8. Com o token do **administrador**, `PATCH /api/usuarios/{idCadastro}/senha` com `{"novaSenha": "Temporaria2026"}`. Esperado: `204`; o `T2` deixa de valer e o login com `Temporaria2026` funciona.
