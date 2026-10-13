# API GISA — Auditoria

Autenticação, convenções e formato de erro: ver `00-GERAL.md`.

A API registra automaticamente **quem fez o quê, quando e com qual resultado**. O registro é feito pelo back em toda requisição relevante, sem nenhuma ação do front. Serve para rastrear alterações, investigar acessos indevidos e atender à LGPD, já que o sistema guarda dados de saúde de menores.

## O que é registrado

| Ação (`acao`) | Quando |
|---|---|
| `LOGIN` | toda tentativa de login, **inclusive as que falham** (útil para detectar tentativas de adivinhar senha) |
| `CRIACAO` | todo `POST` (exceto login) |
| `ALTERACAO` | todo `PATCH` ou `PUT` |
| `EXCLUSAO` | todo `DELETE` (inativações e exclusões) |
| `CONSULTA` | consultas a dados sensíveis: detalhes de paciente (`GET /api/pacientes/{id}`), detalhes de responsável (`GET /api/responsaveis/{id}`) e a própria auditoria |

Listagens e consultas comuns (ex.: `GET /api/pacientes`, `GET /api/cargos`) **não** são registradas.

Cada registro guarda: data e hora, CPF do usuário, ação, método HTTP, caminho chamado (com o ID, ex.: `/api/pacientes/5`), status da resposta e IP de origem.

> **O corpo das requisições nunca é gravado**, porque pode conter senhas e dados de saúde. A auditoria mostra que a pessoa X alterou o paciente 5 às 14:32, mas não os valores enviados.

Se o registro da auditoria falhar, a requisição original **não** é afetada; a falha fica só no log do servidor.

---

## `GET /api/auditoria` — consultar a auditoria

Exige a permissão **`ACESSO_TOTAL`** (perfil "Administrador"). Sem ela: `403`. Consultar a auditoria também gera um registro.

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
      "ip": "177.10.20.30"
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
- `nomeUsuario`: o nome da pessoa dona do CPF, ou `null` se o CPF não pertence a ninguém cadastrado (ex.: tentativa de login com CPF inexistente).
- `sucesso`: `true` quando `statusHttp` é menor que 400. Um `LOGIN` com `sucesso: false` é uma senha errada ou um cadastro inativo.
- `cpfUsuario` pode ser `null` em tentativas de login sem CPF.

---

## Roteiro rápido de testes

1. `POST /api/auth/login` com a senha errada do administrador e depois com a certa.
2. `GET /api/auditoria?acao=LOGIN` com o token do administrador. Esperado: as duas tentativas, a mais recente primeiro; a errada com `statusHttp: 401` e `sucesso: false`.
3. `PATCH` em qualquer paciente e `GET /api/pacientes/{id}`. Esperado: registros `ALTERACAO` e `CONSULTA` com o caminho e o CPF do administrador.
4. `GET /api/pacientes` (listagem). Esperado: **nenhum** registro novo.
5. `GET /api/auditoria` com o token de um usuário sem `ACESSO_TOTAL`. Esperado: `403`.
