# API GISA — Auditoria

Autenticação, convenções e formato de erro: ver `00-GERAL.md`.

A API registra automaticamente **quem fez o quê, quando e com qual resultado**. O registro é feito pelo back em toda requisição relevante, sem nenhuma ação do front. Serve para rastrear alterações, investigar acessos indevidos e atender à LGPD, já que o sistema guarda dados de saúde de menores.

> A auditoria não é a linha do tempo exibida na tela do paciente. Essa é o **histórico do paciente** (`GET /api/pacientes/{idCadastro}/historico`, ver `PACIENTES.md`), com eventos de negócio legíveis e sem dado de saúde.

## O que é registrado

| Ação (`acao`) | Quando |
|---|---|
| `LOGIN` | **só as tentativas de login que falharam** (útil para detectar tentativas de adivinhar senha). Logins bem-sucedidos **não** são registrados |
| `CRIACAO` | todo `POST` (exceto login e `POST /api/terapias/verificar-conflitos`, que só consulta), inclusive `POST /api/auth/esqueci-senha` e `POST /api/auth/redefinir-senha` |
| `ALTERACAO` | todo `PATCH` ou `PUT` |
| `EXCLUSAO` | todo `DELETE` (inativações e exclusões) |
| `CONSULTA` | **só** `GET /api/pacientes/{id}` (acesso aos dados de saúde de um paciente) |

**Nenhum outro `GET` é registrado** (ex.: busca de pacientes, detalhes de responsável, consulta à própria auditoria, `GET /api/cargos`).

Cada registro guarda: data e hora, CPF do usuário, ação, método HTTP, caminho chamado (com o ID, ex.: `/api/pacientes/5`), status da resposta, IP de origem e, nas tentativas de login, o **e-mail digitado** (`emailInformado`).

> Registros gravados antes desta versão continuam como estavam (inclusive logins bem-sucedidos e consultas que hoje não são mais registradas); neles, `emailInformado` vem `null`.

> **O corpo das requisições nunca é gravado**, porque pode conter senhas e dados de saúde. A auditoria mostra que a pessoa X alterou o paciente 5 às 14:32, mas não os valores enviados.

Se o registro da auditoria falhar, a requisição original **não** é afetada; a falha fica só no log do servidor.

---

## `GET /api/auditoria` — consultar a auditoria

Exige a permissão **`VISUALIZAR_AUDITORIA`** (ou `ACESSO_TOTAL`, do perfil "Administrador"). Sem ela: `403`. Consultar a auditoria **não** gera registro.

> Tentativas sem permissão (`403`) de alterar dados ou de abrir os detalhes de um paciente também são registradas: o `statusHttp` mostra a recusa.

| Param | Padrão | Descrição |
|---|---|---|
| `cpf` | — | **início** do CPF do usuário; a máscara é ignorada |
| `acao` | todas | uma ou mais ações: `?acao=LOGIN&acao=EXCLUSAO` |
| `inicio` | — | data inicial (`yyyy-MM-dd`), horário de Brasília |
| `fim` | — | data final (`yyyy-MM-dd`), **dia inteiro incluído** |
| `page` | `0` | número da página |
| `size` | `20` | itens por página |
| `sort` | `dataHora,desc` | mais recentes primeiro |

```
GET /api/auditoria?acao=LOGIN&inicio=2026-10-01&fim=2026-10-13
```

**Resposta `200`**

```json
{
  "content": [
    {
      "idLog": 42,
      "dataHora": "2026-10-13T17:32:10.512Z",
      "cpfUsuario": "99999999999",
      "nomeUsuario": "Administrador do Sistema",
      "acao": "LOGIN",
      "metodo": "POST",
      "caminho": "/api/auth/login",
      "statusHttp": 401,
      "sucesso": false,
      "ip": "177.10.20.30",
      "emailInformado": "admin@gisa.local"
    }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

- `dataHora` vem em UTC (o `Z` no final); converta para o horário local na tela.
- `nomeUsuario`: o nome da pessoa dona do CPF, ou `null` se o CPF não pertence a ninguém cadastrado.
- `sucesso`: `true` quando `statusHttp` é menor que 400. Os novos registros de `LOGIN` são sempre `sucesso: false`: e-mail não cadastrado, senha errada, cadastro inativo ou senha temporária expirada.
- `emailInformado`: o e-mail digitado na tentativa de login (como veio, mesmo que não exista); `null` nos demais registros e nos registros antigos.
- `cpfUsuario`: nas tentativas de login, o CPF do dono do e-mail digitado; `null` se o e-mail não pertence a nenhum usuário. O filtro `cpf` não encontra essas tentativas sem CPF: use `acao=LOGIN` e procure pelo `emailInformado`.

---

## Roteiro rápido de testes

1. `POST /api/auth/login` com o e-mail do administrador e a senha errada; depois com um e-mail inexistente; depois com a senha certa.
2. `GET /api/auditoria?acao=LOGIN` com o token do administrador. Esperado: só as duas tentativas que falharam, a mais recente primeiro, com `statusHttp: 401`, `sucesso: false` e o `emailInformado` de cada uma; a do e-mail inexistente com `cpfUsuario: null`. O login certo **não** aparece.
3. `PATCH` em qualquer paciente e `GET /api/pacientes/{id}`. Esperado: registros `ALTERACAO` e `CONSULTA` com o caminho e o CPF do administrador, e `emailInformado: null`.
4. `GET /api/pacientes?termo=lucas`, `GET /api/responsaveis/{id}` e `GET /api/auditoria`. Esperado: **nenhum** registro novo.
5. `GET /api/auditoria` com o token de um usuário sem `VISUALIZAR_AUDITORIA` (ex.: perfil "Recepção"). Esperado: `403`.
6. Com esse mesmo usuário, `DELETE /api/pacientes/{id}`. Esperado: `403`. Com o administrador, `GET /api/auditoria?acao=EXCLUSAO`. Esperado: o registro dessa tentativa, com `statusHttp: 403`.
