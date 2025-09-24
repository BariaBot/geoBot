# Telegram Mini App Dating Platform Roadmap

## 1. Mission and Target Outcomes
- Deliver a WOW-level dating experience inside Telegram Mini Apps.
- Start with a capital-efficient MVP that runs on Railway alongside existing services.
- Prepare a smooth migration path to Russian cloud providers (Selectel or Yandex Cloud) once load and compliance require it.
- Maintain existing Java/Spring domain logic while adopting a Mini App-friendly edge layer.

## 2. Operating Constraints
- Railway free tier: up to 3 services per project, build/runtime hours shared.* Plan to co-locate Mini App services with the current project until scale demands migration.
- Russian sanctions: public clouds AWS/Azure/GCP unavailable for legal entities. Long-term hosting must use domestic providers or own hardware.
- Telegram Mini App policies: mandatory TON Connect for any blockchain payments by 21 Feb 2025; digital goods must use Telegram Stars. DeviceStorage/SecureStorage available from Bot API 9.0 for client-side persistence.
- Budget guardrail for MVP: ≤ $30 per month (≈ 3 000 ₽) excluding domain and media storage.
- Compliance: readiness for Telegram audits, GDPR-inspired data deletion, and audit trails for payments.

## 3. Target Architecture Overview
```
Telegram Client → React Mini App (Vite + @telegram-apps/sdk + tma.js)
           ↘         ↘
      Railway CDN     |
                       ↓
                 TypeScript Gateway (NestJS/Fastify)
                 • initData validation & auth
                 • Stars/TON webhooks & billing
                 • WebSocket/SSE fan-out
                 • Rate limiting & Redis/BullMQ tasks
                       ↓
                 Spring Boot Core (Java 17)
                 • Profiles, Preferences, Matching
                 • Payment ledger, Analytics hooks
                       ↓
                 PostGIS (geo data) + Redis (cache/queues)
                 Object Storage (media) + Monitoring stack
```

### Frontend Mini App
- React + Vite, TypeScript strict mode, Zustand store, shadcn/ui for components.
- Integrate `@telegram-apps/sdk` and `tma.js` for safe area, theme, lifecycle, init data mocks.
- DeviceStorage for offline profile drafts; SecureStorage for TON Connect session data.
- UX specifics: fullscreen mode, swipe gesture opt-out (`web_app_setup_swipe_behavior`), haptic feedback, theme sync, localization (RU/EN).

### Mini App Gateway (TypeScript)
- NestJS (Fastify adapter) with modular structure (`auth`, `profiles`, `matches`, `payments`, `realtime`).
- Validates `initData` signature on every request, enforces TTL and replay protection.
- Handles Stars payment flows, TON Connect state, Telegram webhook callbacks.
- Provides WebSocket/SSE endpoints for live matches, typing indicators, premium feature triggers.
- Uses Redis for rate limits, session cache, match queues; BullMQ for background jobs (match re-evaluation, notification fan-out).

### Core Services (Java 17 / Spring Boot)
- Reuse existing domain logic for profile management, Geo matching, recommendations.
- Expose REST/GraphQL APIs to the gateway; adopt gRPC later for heavy compute.
- Liquibase continues to manage schema; PostGIS supplies geo queries (GIST indexes, geohash buckets).
- Dedicated modules for payments ledger, analytics events, compliance logging.

### Data & Observability
- PostGIS: tables `profiles`, `locations`, `matches`, `interactions` with spatial indexes (SRID 4326).
- Redis: `session:{user}`, `queue:likes`, geosets for proximity lookups.
- Object storage (Railway plugin or external S3) for media assets.
- OpenTelemetry instrumentation in gateway/core; metrics exported to Prometheus/Grafana (Railway plug-in initially, migrate to YC/Selectel stack later).

## 4. Deployment Strategy
### MVP on Railway
- Services: `miniapp-frontend` (static), `miniapp-gateway` (NestJS), `matching-core` (Spring Boot), shared Postgres (with PostGIS extension) and Redis add-ons.
- CI/CD: GitHub Actions building Docker images, deploy via Railway CLI.
- Secrets management via Railway environment variables; rotate TON keys manually during MVP.

### Scale-Out Migration (Selectel or Yandex Cloud)
- Provision VMs or managed Kubernetes clusters; mirror infrastructure with Terraform modules.
- Move PostGIS and Redis to managed offerings (Selectel Managed DB, Yandex Managed PostgreSQL/Redis).
- Host static frontend on Object Storage + CDN; configure custom domain (miniapp.tld) proxied through Telegram.
- Implement blue/green or canary deployments for gateway and core.

## 5. Product Roadmap
### Phase 0 – Foundations (Week 0–1)
- Audit existing Spring Boot modules and DB schema.
- Write API contracts (OpenAPI/GraphQL) for gateway ↔ core communication.
- Set up Railway environments and shared secrets.

### Phase 1 – MVP Baseline (Week 2–4)
- Scaffold React Mini App with Telegram template, implement onboarding/profile editing.
- Build NestJS gateway: initData validation, profile CRUD proxy, swipe queue (Redis), mocked matches.
- Connect to Spring core for profile persistence and PostGIS geo queries.
- Implement DeviceStorage draft saving, basic analytics events to logs.

### Phase 2 – Monetized MVP (Week 5–6)
- Enable Stars purchases for boosts + superlikes; persist ledger in core.
- Integrate TON Connect for premium users (secure storage, manifest hosting).
- Add anti-abuse rate limits, fraud detection hooks (suspicious swipe bursts).

### Phase 3 – Engagement & Feedback (Week 7–9)
- Launch WebSocket/SSE live updates (new matches, likes back, message prompts).
- Integrate Telegram Bot push campaigns for re-engagement flows.
- Expand geo matching (geohash buckets, daily discovery quotas), add analytics dashboard (Grafana).

### Phase 4 – Scale Readiness (Week 10–14)
- Migrate database and Redis to Selectel/YC; test under 20k RPS using k6.
- Implement A/B experimentation framework and feature flags.
- Harden observability (alerts, tracing sampling), document disaster recovery.

### Phase 5 – Full Product Evolution (Week 15+)
- Add premium filters, Spotlight carousel, advanced recommendations (ML pipeline).
- Security audits, penetration testing, compliance certification.
- Prepare global roll-out playbook and marketing integrations (Telegram Ads, influencers).

## 6. MVP Functional Specification
- Authorization: Telegram `initData`, automatic account creation, device-bound session tokens.
- Profile management: name, age, bio, interests, media upload, location sharing toggle.
- Discovery feed: swipe left/right, superlike, undo (premium), distance & interest filtering.
- Matching: mutual likes produce match entity, optional bot-introduced chat (MVP uses Telegram bot redirect).
- Monetization: paywall for boosts, superlikes; Stars balance tracking; TON Connect for wallet linking.
- Analytics: events `profile_complete`, `swipe`, `match`, `boost_purchase`, `ton_connect_success`.

## 7. Non-Functional Requirements
- Performance: gateway p95 ≤150 ms, core p95 ≤250 ms at 5k RPS; ready to scale to 20k RPS post-migration.
- Availability: 99.5 % uptime monthly; MTTR ≤30 min.
- Security: TLS everywhere, initData signature checks, request signing to core, encrypted PII at rest, RBAC for ops.
- Privacy: GDPR-style deletion via support request, data retention policy (90-day inactivity purge for media).
- Budget: Railway usage capped at $30 monthly, monitor build/runtime to avoid throttling.

## 8. Observability & Operations
- Logging: structured JSON logs (gateway/core) shipped to Railway logging; prepare Logstash/Vector for future move.
- Metrics: Prometheus exporters, Grafana dashboards for swipe rate, match conversion, payment success.
- Tracing: OpenTelemetry spans across gateway ↔ core with context propagation.
- Incident response: pager via Telegram channel; runbooks stored in `docs/runbooks/` (to be added).

## 9. Risk Register & Mitigations
- Railway resource ceilings → monitor usage weekly, prepare Selectel/YC Terraform modules before Phase 4.
- Regulatory shifts (TON/Stars) → subscribe to official Telegram & TON channels, schedule quarterly policy reviews.
- Payment volatility (Stars↔TON) → maintain adjustable pricing tables stored in core DB.
- Abuse/spam → rate limit swipes, add phone/email verification in future iterations, monitor anomaly metrics.
- Vendor lock-in → avoid proprietary Railway features; deploy same Docker images to future target clouds.

## 10. Next Immediate Actions
1. ✅ *Готово* — созданы сервисы `miniapp-frontend`, `miniapp-gateway`, заведены базовые конфиги и README.
2. ✅ *Готово* — подготовлен OpenAPI драфт (`docs/api/miniapp-gateway.openapi.yaml`).
3. ✅ *Готово* — развёрнут NestJS gateway с валидацией `initData` и прокси на core моки.
4. ✅ *Готово* — добавлены стартовые Liquibase изменения (`profile_media`, `star_transactions`, `match_events`).
5. ⏳ Назначить дизайн-спринт Mini App UI и зафиксировать таймлайн.

## 11. Progress Snapshot (обновлено 23.09.2025)
- Frontend scaffold (`apps/miniapp-frontend`) собран на Vite + React с Telegram SDK и Zustand, включает базовый launcher экран.
- Mini App Gateway (`apps/miniapp-gateway`) реализует модули `auth`, `profiles`, `swipes`, `payments`, использует общий HTTP-клиент к Spring core и задокументирован openapi.
- Spring Boot core получил REST-заглушки для профилей/свайпов/платежей, конфиг `application.yml` и расширенные схемы в Liquibase.
- Maven и Temurin JDK 17 установлены (через Homebrew), проверены `java -version` и `mvn -version`; сборка требует локального репозитория (`-Dmaven.repo.local=.m2`).

---
*Railway limits current as of September 23, 2025; re-confirm before launch.*
