# Payloads de cadastro da API

Todos os endpoints exigem autenticacao JWT Token.

## Autenticacao

Use as credenciais administrativas iniciais:

JSON: 
```json
{
    "cpf": "99999999999",
    "senha": "Admin@12345"
}
```

O endpoint: `POST https://api-gisa.onrender.com/api/auth/login` retornará um token válido.

Para encerrar a sessão, remova o token da aplicação.

## 1. Profissional

`POST https://api-gisa.onrender.com/api/profissionais`

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
      "rua": "Praca da Se",
      "numero": "100",
      "complemento": "Apto 12",
      "bairro": "Se",
      "cidade": "Sao Paulo",
      "estado": "SP"
    }
  ],
  "email": "maria.silva@email.com",
  "idCargo": 1
}
```

## 2. Profissional com acesso

`POST https://api-gisa.onrender.com/api/profissionais/com-usuario`

```json
{
  "profissional": {
    "nome": "Maria Silva",
    "cpf": "12345678901",
    "dataNascimento": "1990-05-10",
    "sexo": "F",
    "celular": "11999999999",
    "numCNS": "123456789012345",
    "estadoCivil": "SOLTEIRO",
    "enderecos": [],
    "email": "maria.silva@email.com",
    "idCargo": 1
  },
  "usuario": {
    "senha": "SenhaSegura123",
    "idPerfil": 1
  }
}
```

## 3. Especialista

`POST https://api-gisa.onrender.com/api/especialistas`

```json
{
  "nome": "Joao Santos",
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
  "descricao": "Atendimento terapeutico ocupacional"
}
```

O especialista precisa possuir pelo menos uma especialidade existente ou nova.

## 4. Especialista com acesso

`POST https://api-gisa.onrender.com/api/especialistas/com-usuario`

```json
{
  "especialista": {
    "nome": "Joao Santos",
    "cpf": "23456789012",
    "dataNascimento": "1985-08-20",
    "sexo": "M",
    "celular": "11988888888",
    "estadoCivil": "CASADO",
    "enderecos": [],
    "email": "joao.santos@email.com",
    "idCargo": 1,
    "registroConselho": "CRP-12345",
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

`POST https://api-gisa.onrender.com/api/especialistas/pj`

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
  "razaoSocial": "Clinica Exemplo Ltda",
  "nomeFantasia": "Clinica Exemplo",
  "inscricaoEstadual": "110042490114"
}
```

## 6. Especialista PJ com acesso

`POST https://api-gisa.onrender.com/api/especialistas/pj/com-usuario`

```json
{
  "especialistaPJ": {
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
    "razaoSocial": "Clinica Exemplo Ltda",
    "nomeFantasia": "Clinica Exemplo",
    "inscricaoEstadual": "110042490114"
  },
  "usuario": {
    "senha": "SenhaSegura123",
    "idPerfil": 1
  }
}
```

## Enums aceitos

### `estadoCivil`

```text
SOLTEIRO, CASADO, VIUVO, SEPARADO, DIVORCIADO
```

### `sexo`

Use um caractere, por exemplo:

```text
F, M, O
```

### `diaSemana`

```text
1 = Segunda-feira
2 = Terca-feira
3 = Quarta-feira
4 = Quinta-feira
5 = Sexta-feira
6 = Sabado
7 = Domingo
```

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

## Resposta de erro

Exemplo de validacao:

```json
{
  "timestamp": "2026-09-22T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Erro de validacao dos dados.",
  "path": "/api/profissionais",
  "fields": {
    "cpf": "O CPF deve conter 11 digitos numericos"
  }
}
```
