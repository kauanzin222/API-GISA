# API GISA — CIDs

Autenticação, convenções, enums e formato de erro: ver `00-GERAL.md`. Todos os endpoints exigem `Authorization: Bearer <token>`.

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/cids/{codigoCID}` | busca um CID pelo código |

> **Não existe endpoint nem tela separada para cadastrar CID.** Todo CID novo é cadastrado pelo campo `novosCids` do cadastro de paciente (`POST /api/pacientes`, ver `PACIENTES.md`).

---

## `GET /api/cids/{codigoCID}` — buscar CID pelo código

**Exemplo**

```
GET /api/cids/F84.0
```

**Resposta `200`** (CID encontrado)

```json
{
  "codigoCID": "F84.0",
  "descricao": "Autismo infantil"
}
```

**Resposta `404`** (CID não encontrado): corpo de erro padrão com `message` = `CID não encontrado: {código}`. Significa que o CID ainda não existe na base e deve seguir o fluxo da próxima seção.

> **Importante:** a busca compara o código **exatamente como recebido**, sem ignorar maiúsculas/minúsculas nem formatação. `F84.0`, `f84.0` e `F840` são códigos diferentes. Padronize o código no front (sugestão: maiúsculas, sem espaços e sempre no mesmo formato) tanto ao buscar quanto ao cadastrar.

---

## Fluxo de busca e cadastro de CID

O front trabalha com o código CID digitado pelo usuário:

```
Usuário digita o código do CID
          │
          ▼
GET /api/cids/{codigo}
          │
   ┌──────┴───────┐
 200 (achou)     404 (não achou)
   │                  │
   │                  ▼
   │        Consulta a API pública de CID
   │        (a definir)
   │                  │
   │        ┌─────────┴──────────┐
   │     Encontrou           Não encontrou
   │        │                    │
   │        │            Código inválido: avisar o usuário
   │        ▼            e não adicionar à lista
   │   Usa a DESCRIÇÃO retornada pela API pública
   │   e adiciona o CID à lista "novosCids"
   │
   ▼
Adiciona o código à lista "codigosCid"

          │
          ▼
No POST do paciente, enviar as duas listas
(no PATCH, somente "codigosCid")
```

> **PATCH do paciente:** o `PATCH /api/pacientes/{idCadastro}` aceita apenas `codigosCid` (CIDs que já existem). O campo `novosCids` é ignorado no PATCH. Na tela de edição, um CID que retornar `404` ainda não pode ser vinculado; ele só passa a existir depois de ser enviado em `novosCids` no cadastro de algum paciente.

**Como a descrição é gravada**

Quando o CID não existe na API do GISA e é encontrado na API pública, a **descrição retornada pela API pública é usada no campo `descricao`** e enviada em `novosCids`. Ao receber o cadastro do paciente, o back **cadastra e armazena esse CID** (código + descrição) e o vincula ao paciente. A partir daí, o CID passa a ser encontrado em `GET /api/cids/{codigo}` (retorna `200`).

> A API pública de CID ainda **não foi definida**. Quando for escolhida, este documento será atualizado com a URL e o formato da resposta.

**Exemplo de uso no front (JavaScript)**

```js
const BASE_URL = "https://api-gisa.onrender.com";

async function resolverCid(codigoDigitado, token) {
  const codigo = codigoDigitado.trim().toUpperCase();

  // 1. Busca na API do GISA (envia o token JWT)
  const resposta = await fetch(`${BASE_URL}/api/cids/${encodeURIComponent(codigo)}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (resposta.ok) {
    return { tipo: "existente", codigoCID: codigo };
  }

  // 2. Não achou (404): busca na API pública
  //    (a definir: trocar buscarCidNaApiPublica pela chamada real)
  const publico = await buscarCidNaApiPublica(codigo);
  if (!publico) {
    return { tipo: "invalido" };
  }

  // 3. Usa a descrição da API pública como "descricao" do novo CID
  return { tipo: "novo", codigoCID: codigo, descricao: publico.descricao };
}

// Ao montar o corpo do paciente:
//   existentes -> codigosCid: ["F84.0"]
//   novos      -> novosCids:  [{ codigoCID: "Q90.9", descricao: "..." }]
```

> A chamada à API **pública** não deve levar o token do GISA no cabeçalho.

---

## Regras do back ao receber os CIDs

- Códigos repetidos na mesma requisição são unificados.
- Código em `codigosCid` que não existe na base: erro `400` com a mensagem `CID(s) não encontrado(s): X, Y`.
- Código em `novosCids` que **já existe** na base: o CID existente é reaproveitado e a descrição enviada é **ignorada** (a descrição já cadastrada não é alterada).
- Em `novosCids`, `codigoCID` e `descricao` são obrigatórios (`fields`: "O código CID é obrigatório" / "A descrição do CID é obrigatória").
- Em `codigosCid`, itens vazios são recusados ("O código CID não pode ser vazio") no POST.
- O código CID tem no máximo 20 caracteres e a descrição no máximo 255.

---

## Roteiro rápido de testes

1. `GET /api/cids/F84.0` antes de cadastrar. Esperado: `404`.
2. `POST /api/pacientes` com o CID em `novosCids` (exemplo em `PACIENTES.md`). Esperado: `201`.
3. `GET /api/cids/F84.0` de novo. Esperado: `200` com a descrição enviada.
4. `GET /api/cids/f84.0` (minúsculas). Esperado: `404` (a busca diferencia maiúsculas).
5. `POST /api/pacientes` com um código inexistente em `codigosCid`. Esperado: `400` (CID não encontrado).
6. `POST /api/pacientes` com `novosCids` repetindo um CID já cadastrado e uma descrição diferente. Esperado: `201`, e `GET /api/cids/{codigo}` continua com a descrição original.
7. `PATCH /api/pacientes/{id}` com `"codigosCid": ["F84.0"]`. Esperado: `200` com o CID vinculado.
