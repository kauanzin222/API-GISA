# API GISA — Cadastros de equipe (Profissionais e Especialistas)

Payloads e regras dos endpoints de **Profissionais**, **Especialistas** e **Especialistas PJ**.

> Este documento faz par com o `PACIENTES-ESCOLAS-CIDS-API.md`. As seções "Informações gerais", "Autenticação", "Convenções" e "Padrão de respostas e erros" são iguais nos dois.

## Informações gerais

- **URL base:** `https://api-gisa.onrender.com`
- **Formato:** JSON (`Content-Type: application/json`)
- **Autenticação:** token JWT em todos os endpoints, exceto o login (ver abaixo)
- **Atenção (hospedagem):** se a API estiver em plano gratuito do Render, a primeira requisição após um período sem uso pode demorar (a instância "acorda"). Se a primeira chamada demorar ou der timeout, tente de novo antes de investigar.

---

## Autenticação

Todos os endpoints exigem token JWT. Primeiro faça o login, depois envie o token em cada requisição.

### `POST /api/auth/login`

Use as credenciais administrativas iniciais (**somente para testes**; não use em produção e não publique este arquivo em repositório aberto com a senha):

```json
{
  "cpf": "99999999999",
  "senha": "Admin@12345"
}
```

**Resposta `200`:** JSON contendo o token JWT.

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

> ⚠️ Conferir com o back: nome do campo do token na resposta e o formato do cabeçalho abaixo.

**Uso nas demais requisições:**

```
Authorization: Bearer <token>
```

**Encerrar a sessão:** basta remover o token da aplicação.

---

## Convenções

| Item | Regra |
|---|---|
| Datas | formato ISO `yyyy-MM-dd` (ex.: `"1990-05-10"`) |
| Horários | formato `HH:mm` (ex.: `"08:00"`) |
| CPF | exatamente 11 dígitos, **sem máscara** |
| CPF único | o CPF não pode coincidir com o de nenhuma pessoa já cadastrada no sistema. Ao testar, **não repita CPFs entre os exemplos** (os exemplos deste documento e do de pacientes já usam CPFs diferentes entre si). O CPF `99999999999` é do administrador inicial |
| Campos opcionais | podem ser omitidos ou enviados como `null` |

### Enums

| Campo | Valores aceitos |
|---|---|
| `estadoCivil` | `SOLTEIRO`, `CASADO`, `VIUVO`, `SEPARADO`, `DIVORCIADO` |
| `sexo` | um único caractere: `F`, `M` ou `O`; outros valores são rejeitados com `400` |
| `diaSemana` | `1` = Segunda, `2` = Terça, `3` = Quarta, `4` = Quinta, `5` = Sexta, `6` = Sábado, `7` = Domingo |

---

## Padrão de respostas e erros

### Status HTTP

| Status | Quando |
|---|---|
| `200 OK` | consulta realizada / login realizado |
| `201 Created` | cadastro realizado |
| `400 Bad Request` | validação de campos ou regra de negócio violada |
| `401` / `403` | token ausente, inválido ou expirado |
| `404 Not Found` | recurso não encontrado (usado em `GET /api/cids/{codigoCID}`) |

### Corpo de erro

```json
{
  "timestamp": "2026-09-22T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Erro de validacao dos dados.",
  "path": "/api/profissionais",
  "fields": {
    "cpf": "O CPF deve conter 11 dígitos numéricos"
  }
}
```

- Em erros de **validação de campos**, `fields` traz um item por campo inválido (nome do campo → mensagem). Exiba cada mensagem junto ao campo correspondente.
- Em erros de **regra de negócio** (ex.: CPF já cadastrado), o corpo segue o mesmo formato e a mensagem da regra vem em `message`.

---

## 1. Profissional

`POST /api/profissionais`

```json
{
  "nome": "Maria Silva",
  "cpf": "12345678901",
  "dataNascimento": "1990-05-10",
  "sexo": "F",
  "celular": "11999999999",
  "numCNS": "123456789012345",
  "estadoCivil": "SOLTEIRO",
  "enderecos": [
    {
      "cep": "01001000",
      "rua": "Praça da Sé",
      "numero": "100",
      "complemento": "Apto 12",
      "bairro": "Sé",
      "cidade": "São Paulo",
      "estado": "SP"
    }
  ],
  "email": "maria.silva@email.com",
  "idCargo": 1
}
```

## 2. Profissional com acesso

`POST /api/profissionais/com-usuario`

Cadastra o profissional e já cria o usuário de acesso ao sistema.

```json
{
  "profissional": {
    "nome": "Mariana Costa",
    "cpf": "12345678902",
    "dataNascimento": "1990-05-10",
    "sexo": "F",
    "celular": "11999999998",
    "numCNS": "123456789012346",
    "estadoCivil": "SOLTEIRO",
    "enderecos": [],
    "email": "mariana.costa@email.com",
    "idCargo": 1
  },
  "usuario": {
    "senha": "SenhaSegura123",
    "idPerfil": 1
  }
}
```

## 3. Especialista

`POST /api/especialistas`

```json
{
  "nome": "João Santos",
  "cpf": "23456789012",
  "dataNascimento": "1985-08-20",
  "sexo": "M",
  "celular": "11988888888",
  "numCNS": "234567890123456",
  "estadoCivil": "CASADO",
  "enderecos": [],
  "email": "joao.santos@email.com",
  "idCargo": 1,
  "registroConselho": "CRP-12345",
  "especialidadesIds": [1, 2],
  "novasEspecialidades": [],
  "jornadas": [
    {
      "diaSemana": 1,
      "horaInicio": "08:00",
      "horaTermino": "17:00"
    },
    {
      "diaSemana": 3,
      "horaInicio": "08:00",
      "horaTermino": "12:00"
    }
  ]
}
```

Para criar uma especialidade nova, use `novasEspecialidades`:

```json
{
  "nome": "Terapia ocupacional",
  "descricao": "Atendimento terapêutico ocupacional"
}
```

O especialista precisa possuir pelo menos uma especialidade existente (`especialidadesIds`) ou nova (`novasEspecialidades`).

## 4. Especialista com acesso

`POST /api/especialistas/com-usuario`

```json
{
  "especialista": {
    "nome": "Pedro Almeida",
    "cpf": "23456789013",
    "dataNascimento": "1985-08-20",
    "sexo": "M",
    "celular": "11988888887",
    "estadoCivil": "CASADO",
    "enderecos": [],
    "email": "pedro.almeida@email.com",
    "idCargo": 1,
    "registroConselho": "CRP-12346",
    "especialidadesIds": [1],
    "novasEspecialidades": [],
    "jornadas": [
      {
        "diaSemana": 2,
        "horaInicio": "09:00",
        "horaTermino": "18:00"
      }
    ]
  },
  "usuario": {
    "senha": "SenhaSegura123",
    "idPerfil": 1
  }
}
```

## 5. Especialista PJ

`POST /api/especialistas/pj`

```json
{
  "nome": "Ana Oliveira",
  "cpf": "34567890123",
  "dataNascimento": "1982-03-15",
  "sexo": "F",
  "celular": "11977777777",
  "estadoCivil": "DIVORCIADO",
  "enderecos": [],
  "email": "ana.oliveira@clinica.com",
  "idCargo": 1,
  "registroConselho": "CRM-67890",
  "especialidadesIds": [1],
  "novasEspecialidades": [],
  "jornadas": [
    {
      "diaSemana": 5,
      "horaInicio": "08:00",
      "horaTermino": "16:00"
    }
  ],
  "cnpj": "12345678000195",
  "razaoSocial": "Clínica Exemplo Ltda",
  "nomeFantasia": "Clínica Exemplo",
  "inscricaoEstadual": "110042490114"
}
```

## 6. Especialista PJ com acesso

`POST /api/especialistas/pj/com-usuario`

```json
{
  "especialistaPJ": {
    "nome": "Beatriz Lima",
    "cpf": "34567890124",
    "dataNascimento": "1982-03-15",
    "sexo": "F",
    "celular": "11977777776",
    "estadoCivil": "DIVORCIADO",
    "enderecos": [],
    "email": "beatriz.lima@clinica.com",
    "idCargo": 1,
    "registroConselho": "CRM-67891",
    "especialidadesIds": [1],
    "novasEspecialidades": [],
    "jornadas": [
      {
        "diaSemana": 5,
        "horaInicio": "08:00",
        "horaTermino": "16:00"
      }
    ],
    "cnpj": "12345678000195",
    "razaoSocial": "Clínica Exemplo Ltda",
    "nomeFantasia": "Clínica Exemplo",
    "inscricaoEstadual": "110042490114"
  },
  "usuario": {
    "senha": "SenhaSegura123",
    "idPerfil": 1
  }
}
```

> Os exemplos 5 e 6 usam o mesmo CNPJ. Se o CNPJ já tiver sido cadastrado, use outro válido ao testar o segundo.

---

## Resposta de sucesso

Os cadastros retornam `201 Created`.

Exemplo de profissional:

```json
{
  "idCadastro": 10,
  "nome": "Maria Silva",
  "cpf": "12345678901",
  "email": "maria.silva@email.com",
  "idCargo": 1,
  "acessoCriado": true
}
```
