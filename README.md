# API GISA — Sistema Clínico da APAE Sorocaba

API REST do sistema clínico da **APAE Sorocaba**, desenvolvida também como **trabalho de graduação do curso de Análise e Desenvolvimento de Sistemas (ADS)**.

> ⚠️ **Projeto em desenvolvimento.** Funcionalidades, contratos de requisição/resposta e regras podem mudar ao longo do trabalho.

---

## Sumário

- [Contexto e objetivo](#contexto-e-objetivo)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Configuração e execução local](#configuração-e-execução-local)
- [Testes](#testes)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Documentação da API](#documentação-da-api)
- [Estado do projeto e limitações conhecidas](#estado-do-projeto-e-limitações-conhecidas)
- [Licença e uso](#licença-e-uso)

---

## Contexto e objetivo

A APAE Sorocaba atende pessoas com deficiência intelectual e múltipla, e o dia a dia de um atendimento clínico envolve muitas informações: dados de pacientes e de seus responsáveis, escola, diagnósticos (CID), a equipe de profissionais e especialistas e a agenda de terapias.

Esta API **centraliza o cadastro e a consulta dessas informações em um único serviço**, consumido por outras aplicações (como a interface web) por meio de requisições HTTP. Ela recebe os dados, valida, aplica as regras de negócio e os armazena, de modo que as telas não precisem repetir essa lógica.

Como trabalho de graduação em ADS, o projeto também tem finalidade acadêmica: aplicar, em um problema real, práticas de modelagem, desenvolvimento back-end, segurança e documentação de software.

---

## Funcionalidades

| Módulo | Principais recursos |
|---|---|
| **Autenticação e usuários** | login por CPF e senha com token JWT; dados do usuário logado (nome, tipo, perfil e permissões); troca da própria senha e redefinição pelo administrador, com encerramento das sessões abertas |
| **Pacientes e responsáveis** | cadastro completo em uma requisição (prontuário, endereços, responsáveis, CIDs); busca por CPF ou nome; inclusão de responsável em paciente existente; inativação; histórico de atividades do paciente (linha do tempo sem dados de saúde) |
| **Escolas e CIDs** | busca de escolas para autocomplete; consulta de CID por código; cadastro de CIDs novos junto com o paciente |
| **Equipe** | profissionais, especialistas e especialistas PJ, com especialidades e jornadas de trabalho; listagem com filtro por tipo e status; concessão de acesso ao sistema |
| **Cargos e CBOs** | cadastro e manutenção de cargos ligados ao CBO; detalhes com os profissionais de cada cargo |
| **Perfis e permissões** | perfis de acesso com suas permissões e usuários |
| **Terapias** | agendamento individual ou coletivo, com verificação de conflito de horário (inclusive para cadastros e edições simultâneos); edição de terapia agendada; mudança de status e cancelamento com motivo; listagem geral e do especialista logado, com filtros por dia, intervalo, paciente e especialista; registro de presença e falta, com liberação do horário em falta justificada |
| **Auditoria** | registro de logins, alterações e consultas a dados sensíveis, com consulta filtrada para o administrador |

Comum a todos os módulos:

- validação dos dados de entrada;
- respostas de erro em um formato único;
- listagens paginadas;
- buscas com índices no banco.

O detalhamento de cada módulo está na [documentação da API](#documentação-da-api).

---

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 | linguagem |
| Spring Boot 4 | framework da aplicação |
| Spring Web MVC | API REST |
| Spring Data JPA (Hibernate) | persistência |
| Spring Security + JWT (jjwt) | autenticação e autorização |
| Bean Validation | validação dos dados de entrada |
| Flyway | versionamento do esquema do banco (`src/main/resources/db/migration`) |
| PostgreSQL | banco de dados (com a extensão `pg_trgm` para buscas por trecho) |
| Maven | build e dependências |
| JUnit 5, MockMvc, H2 e embedded-postgres | testes |
| Docker | imagem para o deploy (`Dockerfile`) |

Hospedagem atual: **Render** (`https://api-gisa.onrender.com`).

---

## Configuração e execução local

**Pré-requisitos:** JDK 21 e um PostgreSQL acessível. O Maven não precisa estar instalado: use o wrapper `./mvnw`.

1. Clone o repositório e entre na pasta `gisa`.
2. Defina as variáveis de ambiente lidas pelo `application.properties`, **sem versionar valores reais**:

   | Variável | Conteúdo |
   |---|---|
   | `DB_HOST` | host e porta do PostgreSQL (ex.: `localhost:5432`) |
   | `DB_NAME` | nome do banco |
   | `DB_USER` | usuário do banco |
   | `DB_PASSWORD` | senha do banco |
   | `JWT_SECRET` | chave de assinatura dos tokens: texto aleatório com **pelo menos 32 caracteres** |
   | `CORS_ORIGINS` | *(opcional)* origens do front liberadas no CORS, **separadas por vírgula** e sem barra no final (ex.: `https://gisa.exemplo.com,http://localhost:4200`). Sem a variável, vale só `http://localhost:4200`. Quando definida, **substitui** o padrão: inclua o `localhost` se ainda precisar dele. Não use `*` (a API envia credenciais) |

   ```bash
   export DB_HOST="localhost:5432"
   export DB_NAME="gisa"
   export DB_USER="<usuario>"
   export DB_PASSWORD="<senha>"
   export JWT_SECRET="<chave_longa_e_aleatoria>"
   export CORS_ORIGINS="https://<front-publicado>,http://localhost:4200"   # opcional
   ```

   Para gerar uma chave: `openssl rand -base64 48`.

3. Rode a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Na inicialização, o Flyway cria ou atualiza o esquema do banco. O usuário do banco precisa de permissão para `CREATE EXTENSION pg_trgm` (migration `V11`).

### Usuário administrador inicial

A migration `V6` cria um administrador para o primeiro acesso. As credenciais estão em `gisa/docs/00-GERAL.md`. **Troque a senha dele logo após o primeiro login** em qualquer ambiente publicado (`PATCH /api/usuarios/minha-senha`, ver `USUARIOS.md`), pois a senha inicial é conhecida por quem tem acesso ao repositório.

---

## Testes

```bash
./mvnw test
```

Não é preciso configurar nenhum banco:

- **Testes de regra de negócio** usam o H2 em memória.
- **`ApiPostgresTest`** sobe um **PostgreSQL real embarcado**, sem Docker, aplica todas as migrations e valida o esquema contra as entidades. Depois chama a API por HTTP com o administrador inicial: listagens e filtros, detalhes, erros, conflitos de horário, edição e status de terapias, presença, histórico do paciente, usuário logado, CORS e auditoria.
- **`TerapiaConcorrenciaTest`** dispara cadastros e edições simultâneos de terapias para o mesmo horário e confirma que só um é gravado.

---

## Estrutura do projeto

```
.
├── README.md                     # este arquivo
└── gisa/
    ├── docs/                     # documentação da API, um arquivo por módulo
    ├── Dockerfile                # imagem usada no deploy
    ├── pom.xml
    └── src/
        ├── main/java/com/fatec/gisa/
        │   ├── config/           # segurança, JWT, interceptor de auditoria
        │   ├── controllers/      # rotas REST, por módulo
        │   ├── dtos/             # contratos de requisição e resposta
        │   ├── entities/         # entidades JPA
        │   ├── enums/
        │   ├── exceptions/       # exceções e formato padrão de erro
        │   ├── repositories/     # acesso ao banco (Spring Data)
        │   ├── services/         # regras de negócio
        │   └── validation/       # validações customizadas (ex.: sexo)
        ├── main/resources/
        │   ├── application.properties
        │   └── db/migration/     # migrations do Flyway (V1 a V14)
        └── test/java/            # testes automatizados
```

---

## Documentação da API

Os documentos ficam em [`gisa/docs/`](./gisa/docs/). Comece pelo **`00-GERAL.md`**, que traz autenticação, convenções, enums, formato de erro e as regras comuns de PATCH e DELETE.

| Documento | Conteúdo |
|---|---|
| [00-GERAL.md](./gisa/docs/00-GERAL.md) | autenticação, convenções, status HTTP, formato de erro, concessão de acesso |
| [PACIENTES.md](./gisa/docs/PACIENTES.md) | pacientes, responsáveis e histórico do paciente |
| [ESCOLAS.md](./gisa/docs/ESCOLAS.md) | busca e cadastro de escolas |
| [CIDS.md](./gisa/docs/CIDS.md) | consulta de CID e fluxo de cadastro |
| [PROFISSIONAIS.md](./gisa/docs/PROFISSIONAIS.md) | profissionais |
| [ESPECIALISTAS.md](./gisa/docs/ESPECIALISTAS.md) | especialistas, especialistas PJ e especialidades |
| [CARGOS.md](./gisa/docs/CARGOS.md) | cargos e CBOs |
| [PERFIS.md](./gisa/docs/PERFIS.md) | perfis de acesso e permissões |
| [TERAPIAS.md](./gisa/docs/TERAPIAS.md) | agendamento, edição, status e filtros de terapias, conflitos de horário e presença |
| [USUARIOS.md](./gisa/docs/USUARIOS.md) | usuário logado, troca e redefinição de senha |
| [AUDITORIA.md](./gisa/docs/AUDITORIA.md) | trilha de auditoria |

Cada documento termina com um **roteiro rápido de testes** para Postman ou similar.

---

## Estado do projeto e limitações conhecidas

- **Permissões por rota:** os perfis e permissões já existem, mas só a redefinição de senha e a auditoria exigem permissão. Nas demais rotas, qualquer usuário autenticado acessa tudo (inclusive mudar status, editar e cancelar terapias). A verificação por permissão entra com a migration que definirá a lista definitiva de permissões.
- **CORS:** configurável pela variável `CORS_ORIGINS` (ver [Configuração](#configuração-e-execução-local)); sem ela, só `http://localhost:4200`. A origem do front publicado precisa ser definida no ambiente do Render antes do deploy dele.
- **Login sem limite de tentativas:** as tentativas que falham ficam na auditoria, mas não são bloqueadas.
- **"Esqueci minha senha":** não há recuperação por e-mail. O usuário troca a própria senha (`/api/usuarios/minha-senha`) ou o administrador redefine.
- **Terapias:** já há mudança de status (iniciar, concluir, cancelar com motivo), edição de terapia agendada (remarcar e trocar participantes) e filtros por dia, intervalo, paciente e especialista. Ainda não há contadores prontos da visão por dia ("Consultas Diárias"): o front calcula a partir de `GET /api/terapias/minhas?data=`. A API não confere a data ao iniciar ou concluir um atendimento, nem a jornada de trabalho do especialista ao agendar.
- **Histórico do paciente:** começa nesta versão, sem carga retroativa: pacientes e terapias anteriores não têm eventos. Não registra início e conclusão de atendimento, nem alterações feitas direto no banco.
- **Receita e atestado:** gerados e impressos só no front; a API não guarda esses documentos.
- **Integração com API pública de CID:** não definida. Quando um CID não existe na base, o front precisa obter a descrição e enviá-la em `novosCids` no cadastro do paciente.
- **CBOs:** a base vem só com o CBO de teste `999999`; os oficiais podem ser cadastrados por `POST /api/cbos` ou carregados por migration.

---

## Licença e uso

Projeto acadêmico e institucional. Defina aqui a licença e as condições de uso, se aplicável.
