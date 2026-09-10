# Nutau

Simulador de banco digital construído em torno de uma pergunta: **o que fazer
quando a regra de negócio depende de um sistema que você não controla?**

Emitir um cartão exige consultar bureaus de crédito. São chamadas externas,
lentas e sujeitas a falha. A forma ingênua — segurar a requisição HTTP até o
bureau responder — custa uma thread do servidor por cliente na fila e amarra a
disponibilidade da sua API à de um terceiro. O projeto existe para tratar esse
acoplamento direito.

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)

> **Status:** fluxo síncrono completo e testado. A mensageria (Fase 2) está com
> infraestrutura provisionada e o ponto de corte marcado no código — veja
> [Roadmap](#roadmap). Nada neste README descreve algo que ainda não roda.

---

## A decisão central

```
  POST /api/solicitacoes
         │
         ▼
  ┌──────────────┐   grava a solicitação
  │ CreditoService│──────────────────────▶  status: EM_ANALISE
  └──────┬───────┘
         │
         │  202 Accepted + id
         ▼
     cliente consulta /api/solicitacoes/{id} quando quiser


         ▲
         │  ponto de corte: aqui o fluxo síncrono termina.
         │  A Fase 2 publica CreditoSolicitadoEvent daqui.
```

O endpoint **não** devolve o veredito. Devolve `202 Accepted` e um identificador.
Isso é consistência eventual assumida de propósito: o cliente vê `EM_ANALISE`
antes de ver `APROVADO`, e em troca a API responde em milissegundos e não cai
junto com o bureau.

A entidade `ConsultaBureau` já é modelada para entrega **at least once** — se a
mesma mensagem for reentregue, o banco não duplica a consulta. Essa decisão foi
tomada agora, não depois, porque desfazer duplicata em produção é caro.

## O que está implementado

| Área | Detalhe |
|---|---|
| Autenticação | JWT (jjwt) com filtro próprio, `AccessDeniedHandler` e `AuthenticationEntryPoint` devolvendo JSON em vez de página de erro |
| Cadastro | Validação de CPF e de CEP, com endereço preenchido via **ViaCEP** |
| Solicitação de crédito | `EMISSAO_CARTAO` e `AUMENTO_LIMITE`, com status `EM_ANALISE · APROVADO · APROVADO_PARCIAL · REPROVADO · ERRO_ANALISE` |
| Bureau | `MockBureauService` simulando `APROVADO`, `SCORE_BAIXO` e `NOME_SUJO` — inclui falha proposital para exercitar retentativa |
| Cartões | Categorias `INICIAL · AMBAR · VIOLETA · INFINITO` conforme limite aprovado |
| Erros | `ErrorCode` centralizado + `GlobalExceptionHandler`, resposta padronizada em `ApiResponse` |
| Persistência | PostgreSQL com **Liquibase** versionado (`v0.1.0`, `v0.2.0`) |
| Mapeamento | MapStruct, sem conversão manual de DTO |
| Documentação | SpringDoc OpenAPI — Swagger UI em `/swagger-ui.html` |
| Testes | 8 classes cobrindo JWT, CPF, CEP, bureau, cartão e enums |

## Endpoints

```
POST   /api/auth/cadastro          cria conta
POST   /api/auth/login             devolve o token
GET    /api/auth/eu                dados do usuário autenticado

POST   /api/cartoes                solicita emissão
POST   /api/cartoes/aumento-limite solicita aumento
GET    /api/cartoes/meu            cartão do usuário

GET    /api/solicitacoes           histórico, mais recente primeiro
GET    /api/solicitacoes/{id}      acompanha uma solicitação
```

## Rodando

```bash
git clone https://github.com/gabrielbkx/nutau.git
cd nutau
cp .env.example .env          # ajuste JWT_SECRET
docker compose up -d          # postgres, kafka e rabbitmq
./mvnw spring-boot:run
```

Swagger em http://localhost:8080/swagger-ui.html · health em `/actuator/health`.

> Kafka e RabbitMQ sobem no compose porque a Fase 2 depende deles. A aplicação
> **não** os utiliza ainda e roda normalmente sem eles.

## Roadmap

- [x] **Fase 1 — fluxo síncrono.** Cadastro, autenticação, solicitação, cartão
- [ ] **Fase 2 — mensageria.** Publicar `CreditoSolicitadoEvent` no Kafka; consumer
      dedicado consulta os bureaus e atualiza o status
- [ ] **Fase 2 — notificação.** RabbitMQ com retentativa e dead-letter queue para
      avisar o cliente do veredito
- [ ] Testes de integração com Testcontainers

O ponto exato onde a Fase 2 entra está comentado em
[`CreditoService.registrar()`](src/main/java/br/com/nutau/services/CreditoService.java) — deixei explícito porque é
a fronteira mais importante do sistema.

## Estrutura

```
br.com.nutau
├── controllers      Auth · Cartao · Credito
├── services         regra de negócio + helpers de CPF/CEP
├── integrations     ViaCepClient · MockBureauService
├── security         JwtService · filtro · handlers JSON
├── models           entities · dtos · enums
├── mappers          MapStruct
├── exceptions       ErrorCode · NutauException · handler global
└── repositories     Spring Data JPA
```
