# API GISA — Sistema Clínico da APAE Sorocaba

API REST voltada ao sistema clínico da **APAE Sorocaba**, desenvolvida também como **trabalho de graduação do curso de Análise e Desenvolvimento de Sistemas (ADS)**.

> ⚠️ **Projeto em desenvolvimento.** Funcionalidades, contratos de requisição/resposta e regras podem mudar ao longo do trabalho.

---

## Sumário

- [API GISA — Sistema Clínico da APAE Sorocaba](#api-gisa--sistema-clínico-da-apae-sorocaba)
  - [Sumário](#sumário)
  - [Contexto e objetivo](#contexto-e-objetivo)
  - [Funcionalidades documentadas](#funcionalidades-documentadas)
    - [Autenticação e autorização](#autenticação-e-autorização)
    - [Pacientes](#pacientes)
    - [Escolas](#escolas)
    - [CIDs](#cids)
    - [Equipe: profissionais e especialistas](#equipe-profissionais-e-especialistas)
    - [Validação e padrão de erros](#validação-e-padrão-de-erros)
  - [Tecnologias](#tecnologias)
  - [Configuração e execução local](#configuração-e-execução-local)
    - [Usuário administrador inicial](#usuário-administrador-inicial)
  - [Autenticação](#autenticação)
  - [Estrutura do projeto](#estrutura-do-projeto)
  - [Documentação da API](#documentação-da-api)
  - [Estado do projeto e limitações conhecidas](#estado-do-projeto-e-limitações-conhecidas)
  - [Licença e uso](#licença-e-uso)

---

## Contexto e objetivo

A APAE Sorocaba atende pessoas com deficiência intelectual e múltipla, e o dia a dia de um atendimento clínico envolve muitas informações: dados de pacientes e de seus responsáveis, escola, diagnósticos (CID) e a equipe de profissionais e especialistas.

Esta API existe para **centralizar o cadastro e a consulta dessas informações em um único serviço**, que outras aplicações (como uma interface web) podem consumir por meio de requisições HTTP. Em termos simples: a API é a "camada de dados e regras" do sistema. Ela recebe os dados, valida, aplica as regras de negócio e os armazena, de modo que as telas do sistema não precisem repetir essa lógica.

Como trabalho de graduação em ADS, o projeto também tem finalidade acadêmica: aplicar, em um problema real, práticas de modelagem, desenvolvimento back-end, segurança e documentação de software.

> Este README descreve o que está documentado no projeto. Não há, aqui, qualquer afirmação sobre uso em produção ou sobre resultados obtidos na instituição.

---

## Funcionalidades documentadas

As funcionalidades abaixo constam nos documentos [`CADASTROS-API.md`](./CADASTROS-API.md) e [`PACIENTES-ESCOLAS-CIDS-API.md`](./PACIENTES-ESCOLAS-CIDS-API.md). Os detalhes de payloads, regras e mensagens de erro estão nesses arquivos.

### Autenticação e autorização
- Login por CPF e senha (`POST /api/auth/login`), com retorno de token JWT.
- Todos os demais endpoints exigem o token no cabeçalho `Authorization: Bearer <token>`.
- Respostas `401`/`403` para token ausente, inválido ou expirado.

### Pacientes
- Listagem paginada e ordenável (`GET /api/pacientes`), sem filtros. A resposta inclui responsáveis, CIDs e idade calculada pela API.
- Cadastro (`POST /api/pacientes`) em uma única requisição, contemplando:
  - dados pessoais e endereços;
  - prontuário (alergias, comorbidade e mobilidade);
  - vínculo opcional com uma escola;
  - responsáveis, com regras de endereço, reaproveitamento por CPF e validação de duplicidade;
  - CIDs já existentes e CIDs novos.
- Status do paciente definido automaticamente no cadastro (`MATRICULADO`), assim como a data de cadastro.

### Escolas
- Busca por trecho do nome (`GET /api/escolas?nome=`), pensada para autocomplete, com no máximo 10 resultados.
- Cadastro de escola (`POST /api/escolas`), com tipo `PUBLICA` ou `PARTICULAR`.

### CIDs
- Consulta pelo código (`GET /api/cids/{codigoCID}`), com `404` quando o código não existe na base.
- Não há endpoint separado para cadastrar CID: novos CIDs são cadastrados junto com o paciente, pelo campo `novosCids`.
- A busca compara o código exatamente como recebido (sem normalizar maiúsculas/minúsculas ou formatação).

### Equipe: profissionais e especialistas
- Cadastro de profissional (`POST /api/profissionais`).
- Cadastro de especialista (`POST /api/especialistas`), com especialidades existentes ou novas e jornadas semanais.
- Cadastro de especialista pessoa jurídica (`POST /api/especialistas/pj`), com CNPJ, razão social, nome fantasia e inscrição estadual.
- Variantes **com usuário de acesso**, que criam o cadastro e o usuário de acesso ao sistema na mesma requisição:
  - `POST /api/profissionais/com-usuario`
  - `POST /api/especialistas/com-usuario`
  - `POST /api/especialistas/pj/com-usuario`

### Validação e padrão de erros
- Validação de campos (por exemplo, CPF com 11 dígitos numéricos, sexo `F`/`M`/`O`, datas no formato `yyyy-MM-dd`).
- Resposta de erro padronizada (`timestamp`, `status`, `error`, `message`, `path` e, em erros de validação, `fields`).

---

## Tecnologias

Stack indicada para o projeto (gerenciada via `pom.xml`):

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem |
| Spring Boot | Framework da aplicação |
| Maven | Build e gerenciamento de dependências |
| Spring Web MVC | API REST |
| Spring Data JPA | Persistência |
| Spring Security | Autenticação e autorização (JWT) |
| Bean Validation | Validação dos dados de entrada |
| Flyway | Versionamento do esquema do banco |
| PostgreSQL | Banco de dados relacional |
| H2 | Banco em memória (testes/desenvolvimento) |

> Os documentos de API mencionam o uso de token JWT, paginação no padrão `Page` do Spring e hospedagem em plano gratuito do Render, o que é coerente com a stack acima. A presença de uma dependência no `pom.xml` não implica, por si só, que ela esteja configurada para uso (por exemplo, qual banco é utilizado em cada perfil de execução).

---

## Configuração e execução local

Pré-requisitos indicados pela stack:

- JDK 21
- Maven
- Um banco PostgreSQL acessível (ou o H2, caso o projeto esteja configurado para isso)

Passos gerais:

1. Clone o repositório e acesse a pasta do projeto.
2. Configure a conexão com o banco e o segredo de assinatura do JWT por **variáveis de ambiente** ou pelo arquivo de configuração da aplicação, **sem versionar valores reais**. Exemplo de nomes ilustrativos:

   ```bash
   export DB_URL="jdbc:postgresql://localhost:5432/<nome_do_banco>"
   export DB_USERNAME="<usuario>"
   export DB_PASSWORD="<senha>"
   export JWT_SECRET="<segredo_longo_e_aleatorio>"
   ```

   > Os nomes acima são apenas exemplos. Use os nomes realmente lidos pelo `application.properties`/`application.yml` do projeto.

3. Execute a aplicação com Maven:

   ```bash
   mvn spring-boot:run
   ```

4. O esquema do banco é controlado pelo Flyway (migrations), aplicadas na inicialização quando configurado.

### Usuário administrador inicial
A documentação informa a existência de um usuário administrador inicial, destinado **somente a testes**. As credenciais não estão neste README: consulte-as com a equipe do projeto e **não as utilize em ambientes reais**.

---

## Autenticação

Fluxo resumido:

1. Faça login:

   ```http
   POST /api/auth/login
   Content-Type: application/json

   {
     "cpf": "<cpf_sem_mascara>",
     "senha": "<senha>"
   }
   ```

2. Use o token retornado nas demais requisições:

   ```http
   Authorization: Bearer <token>
   ```

3. Para encerrar a sessão, basta descartar o token no cliente.

> A documentação sinaliza que o nome do campo do token na resposta e o formato do cabeçalho ainda deviam ser conferidos com o back-end.

---

## Estrutura do projeto

Os arquivos Markdown de documentação ficam na raiz do projeto:

```
.
├── README.md                        # Este arquivo
├── CADASTROS-API.md                 # Profissionais, especialistas e especialistas PJ
├── PACIENTES-ESCOLAS-CIDS-API.md    # Pacientes, escolas e CIDs
└── pom.xml                          # Dependências e build (Maven)
```

> A organização interna do código-fonte (pacotes, camadas, migrations) ainda não está descrita aqui. Esta seção deve ser complementada com a estrutura real de `src/main/java` e `src/main/resources`.

---

## Documentação da API

| Documento | Conteúdo |
|---|---|
| [CADASTROS-API.md](./CADASTROS-API.md) | Autenticação, convenções, padrão de erros e cadastros de profissionais, especialistas e especialistas PJ (com e sem usuário de acesso) |
| [PACIENTES-ESCOLAS-CIDS-API.md](./PACIENTES-ESCOLAS-CIDS-API.md) | Listagem e cadastro de pacientes, busca e cadastro de escolas, consulta de CIDs, fluxo de CID, roteiro de testes e mensagens de erro |

---

## Estado do projeto e limitações conhecidas

- A API está **em desenvolvimento**.
- **Integração com API pública de CID:** ainda **não definida**. Hoje, quando um CID não existe na base, o front-end precisa obter a descrição por outro meio e enviá-la em `novosCids` no cadastro do paciente. Não há integração pronta com serviço externo de CID.
- Não há endpoint dedicado para cadastrar ou editar CIDs.
- A listagem de pacientes não oferece filtros, apenas paginação e ordenação.
- O reaproveitamento de responsáveis por CPF não atualiza os dados já cadastrados.

---

## Licença e uso

Projeto acadêmico e institucional. Defina aqui a licença e as condições de uso, se aplicável.