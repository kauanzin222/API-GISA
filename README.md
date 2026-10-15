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
| **Autenticação e usuários** | login por e-mail e senha com token JWT; senha temporária gerada pelo back e enviada por e-mail ao conceder acesso, com troca obrigatória no primeiro acesso; "esqueci minha senha" por e-mail; dados do usuário logado (nome, e-mail, tipo, perfil e permissões); troca da própria senha, com encerramento das sessões abertas; administração de acessos (listar usuários, trocar perfil, gerar nova senha temporária, retirar acesso), com travas para não ficar sem administrador nem sem quem gerencie perfis |
| **Pacientes e responsáveis** | cadastro completo em uma requisição (prontuário, endereços, responsáveis, CIDs), com validação de CPF; busca por CPF exato ou nome (sem listagem completa, LGPD); inclusão de responsável em paciente existente; inativação, que cancela ou ajusta as terapias futuras; histórico de atividades do paciente (linha do tempo sem dados de saúde) |
| **Escolas e CIDs** | gestão de escolas (listagem, edição, exclusão sem pacientes) e autocomplete; consulta de CID por código; cadastro de CIDs novos junto com o paciente |
| **Equipe** | profissionais, especialistas e especialistas PJ, com especialidades e jornadas de trabalho; assistentes sociais, com os pacientes vinculados a eles; listagem com filtro por tipo e status; concessão de acesso ao sistema; gestão de especialidades |
| **Cargos e CBOs** | cadastro e manutenção de cargos ligados ao CBO; detalhes com os profissionais de cada cargo |
| **Perfis e permissões** | toda rota exige permissão; permissões com categoria e descrição; perfis padrão (Administrador, Recepção, Especialista, Assistente Social, que vê só os pacientes vinculados); perfis de acesso com suas permissões e usuários |
| **Terapias** | agendamento individual ou coletivo, só para horários futuros (fuso de São Paulo), com verificação de conflito de horário (inclusive para cadastros e edições simultâneos); edição de terapia agendada; mudança de status e cancelamento com motivo; listagem geral e do especialista logado, com filtros por dia, intervalo, paciente e especialista; registro de presença e falta, com liberação do horário em falta justificada |
| **Auditoria** | registro de alterações, logins que falharam (com o e-mail digitado) e consultas aos detalhes de paciente, com consulta filtrada para o administrador |

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
   | `CORS_ORIGINS` | *(opcional)* origens do front liberadas no CORS, **separadas por vírgula** e sem barra no final (ex.: `https://gisa.exemplo.com,http://localhost:4200`). Sem a variável, valem `http://localhost:4200` e `https://eduardofproenca.github.io` (front publicado no GitHub Pages). Quando definida, **substitui** o padrão: inclua o `localhost` se ainda precisar dele. Não use `*` (a API envia credenciais) |
   | `FRONT_URL` | *(opcional)* endereço do front usado nos links enviados por e-mail (ex.: `https://gisa.exemplo.com`, sem barra no final). Padrão: `http://localhost:4200` |
   | `BREVO_API_KEY` | *(opcional, recomendada no Render)* chave da API do Brevo para os e-mails de senha temporária e de redefinição de senha. Usa HTTPS, então funciona onde as portas de SMTP são bloqueadas (plano gratuito do Render). Tem prioridade sobre o SMTP. **Sem ela e sem `SMTP_HOST`, nenhum e-mail é enviado**: o conteúdo (com a senha ou o link) vai para o log do servidor, o que serve para desenvolvimento local |
   | `EMAIL_REMETENTE_NOME` | *(opcional)* nome do remetente no Brevo. Padrão: `GISA` |
   | `SMTP_HOST` | *(opcional)* servidor SMTP, alternativa ao Brevo |
   | `SMTP_PORT` | *(opcional)* porta do SMTP. Padrão: `587` |
   | `SMTP_USERNAME` / `SMTP_PASSWORD` | *(opcionais)* usuário e senha do SMTP |
   | `SMTP_AUTH` / `SMTP_STARTTLS` | *(opcionais)* autenticação e STARTTLS do SMTP. Padrão: `true` nos dois |
   | `EMAIL_REMETENTE` | *(opcional)* remetente dos e-mails. Padrão: `nao-responda@gisa.local`. Com o Brevo, precisa ser um remetente verificado na conta |

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

A migration `V6` cria um administrador para o primeiro acesso, e a `V16` dá a ele o e-mail de login `admin@gisa.local`. As credenciais estão em `gisa/docs/00-GERAL.md`. **Troque a senha dele logo após o primeiro login** em qualquer ambiente publicado (`PATCH /api/usuarios/minha-senha`, ver `USUARIOS.md`), pois a senha inicial é conhecida por quem tem acesso ao repositório.

---

## Testes

```bash
./mvnw test
```

Não é preciso configurar nenhum banco:

- **Testes de regra de negócio** usam o H2 em memória.
- **`ApiPostgresTest`** sobe um **PostgreSQL real embarcado**, sem Docker, aplica todas as migrations e valida o esquema contra as entidades. Depois chama a API por HTTP com o administrador inicial: listagens e filtros, detalhes, erros, conflitos de horário, edição e status de terapias, presença, histórico do paciente, usuário logado, CORS e auditoria. Também confere as permissões e os perfis padrão criados pela migration `V15`, entrando com usuários de cada perfil.
- **`PermissoesPorRotaTest`** percorre todas as rotas da API e falha se alguma ficar sem `@PreAuthorize` (fora as que só pedem login), além de confirmar o `403` para um usuário sem permissões.
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
| [ESCOLAS.md](./gisa/docs/ESCOLAS.md) | escolas: gestão e autocomplete |
| [CIDS.md](./gisa/docs/CIDS.md) | consulta de CID e fluxo de cadastro |
| [PROFISSIONAIS.md](./gisa/docs/PROFISSIONAIS.md) | profissionais |
| [ESPECIALISTAS.md](./gisa/docs/ESPECIALISTAS.md) | especialistas, especialistas PJ e especialidades |
| [ASSISTENTES-SOCIAIS.md](./gisa/docs/ASSISTENTES-SOCIAIS.md) | assistentes sociais e pacientes vinculados |
| [CARGOS.md](./gisa/docs/CARGOS.md) | cargos e CBOs |
| [PERFIS.md](./gisa/docs/PERFIS.md) | perfis de acesso, lista de permissões e perfis padrão |
| [TERAPIAS.md](./gisa/docs/TERAPIAS.md) | agendamento, edição, status e filtros de terapias, conflitos de horário e presença |
| [USUARIOS.md](./gisa/docs/USUARIOS.md) | usuário logado, senha e administração de acessos |
| [AUDITORIA.md](./gisa/docs/AUDITORIA.md) | trilha de auditoria |

Cada documento termina com um **roteiro rápido de testes** para Postman ou similar.

---

## Estado do projeto e limitações conhecidas

- **Permissões por rota:** toda rota exige permissão (tabela em `00-GERAL.md`). O corpo da requisição é validado antes da permissão: sem permissão e com corpo inválido, a resposta é `400`, e não `403`. O assistente social vê só os pacientes vinculados a ele (`VISUALIZAR_PACIENTES_VINCULADOS`). Documentos (atestado, receita, recusa) têm permissões, mas só o front as usa.
- **CORS:** configurável pela variável `CORS_ORIGINS` (ver [Configuração](#configuração-e-execução-local)); sem ela, valem `http://localhost:4200` e `https://eduardofproenca.github.io` (front no GitHub Pages).
- **Login sem limite de tentativas:** as tentativas que falham ficam na auditoria, mas não são bloqueadas.
- **E-mails:** sem `BREVO_API_KEY` nem `SMTP_HOST`, os e-mails de senha temporária e de "esqueci minha senha" não são enviados, só registrados no log do servidor.
- **Terapias:** já há mudança de status (iniciar, concluir, cancelar com motivo), edição de terapia agendada (remarcar e trocar participantes) e filtros por dia, intervalo, paciente e especialista. Ainda não há contadores prontos da visão por dia ("Consultas Diárias"): o front calcula a partir de `GET /api/terapias/minhas?data=`. A API não confere a data ao iniciar ou concluir um atendimento. O agendamento confere horários ocupados e a jornada de trabalho dos especialistas (`409` com a lista de conflitos).
- **Histórico do paciente:** começa nesta versão, sem carga retroativa: pacientes e terapias anteriores não têm eventos. Não registra início e conclusão de atendimento, nem alterações feitas direto no banco.
- **Receita e atestado:** gerados e impressos só no front; a API não guarda esses documentos.
- **Integração com API pública de CID:** não definida. Quando um CID não existe na base, o front precisa obter a descrição e enviá-la em `novosCids` no cadastro do paciente.
- **CBOs:** a base vem só com o CBO de teste `999999`; os oficiais podem ser cadastrados por `POST /api/cbos` ou carregados por migration.

---

## Licença e uso

Projeto acadêmico e institucional. Defina aqui a licença e as condições de uso, se aplicável.
