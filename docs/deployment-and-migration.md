# Deployment & Migration Plan

## 1. Railway (MVP) Environment
- **Services:**
  - `miniapp-frontend`: static hosting (Vite build) served via Railway static site.
  - `miniapp-gateway`: NestJS container (Fastify, Node 20).
  - `matching-core`: Spring Boot container (Java 17).
- **Data Stores:** Railway Postgres + PostGIS extension; Railway Redis for sessions/rate-limits.
- **CI/CD:** GitHub Actions → build Docker images → push to registry → `railway up` with environment variables.
- **Secrets:** Managed via Railway UI/CLI (`RAILWAY_ENVIRONMENT` per stage). Store TON keys, Stars credentials, Postgres DSN.
- **Monitoring:** Railway logs + basic HTTP health-checks; set up log drain to external storage when available.
- **Budget Tracking:** Weekly review of runtime/build minutes; auto-sleep services during off-peak (if acceptable).

## 2. Pre-Migration Checklist
1. Terraform modules drafted for Selectel and Yandex Cloud (network, compute, managed DB, object storage).
2. Container images parameterized (registry URL via env). Avoid Railway-specific features.
3. Database migrations (Liquibase) tested on vanilla Postgres/PostGIS.
4. Observability stack (Prometheus/Grafana/OTel collector) containerized for portability.
5. Backups: automatic daily dump of Postgres to S3-compatible storage (MinIO/Yandex Object Storage).

## 3. Migration Targets
### 3.1 Selectel
- Managed Kubernetes + Managed PostgreSQL/PostGIS; optional managed Redis.
- Object Storage for media/CDN; custom domains with LetsEncrypt.
- Recommended for lower entry deposit (from 1 000 ₽) and local support.

### 3.2 Yandex Cloud
- Managed Service for Kubernetes (MSK8S) или Compute Cloud (VMs) + Managed PostgreSQL/PostGIS + Managed Redis.
- Object Storage + Cloud CDN for frontend.
- Free grant 4 000 ₽ для новых аккаунтов.

## 4. Migration Steps (Post-MVP)
1. **Infra Provisioning:** apply Terraform in target provider, create VPC, subnets, security groups, managed databases.
2. **Data Transfer:** snapshot Railway Postgres, restore into target PostGIS; sync Redis data (if necessary) via persistence or rehydration scripts.
3. **Deploy Core Services:** run gateway/core images in Kubernetes (or Compose on VMs), update environment variables to point to new DB/Redis.
4. **Cutover:**
   - Set traffic splitting (e.g., 10% to new infra) using DNS or Telegram deep-link parameters.
   - Monitor latency, error rates, payment success.
   - Gradually increase to 100%; decommission Railway services once stable.
5. **Post-Migration:** configure autoscaling, extend observability, update runbooks, enable disaster recovery backups.

## 5. Disaster Recovery
- Daily database snapshots stored in geo-redundant storage.
- Redis RDB dumps every hour.
- Runbook for restoring services within 30 minutes (to be added to `docs/runbooks/`).

## 6. Compliance & Security
- Store secrets in provider-managed vaults (Selectel KMS / Yandex Lockbox).
- Enforce TLS certificates via managed ingress.
- Periodic security scans (trivy, snyk) integrated into CI.

## 7. Future Enhancements
- Multi-region deployment for latency (EU/Asia) once санкционные ограничения сняты или появится white-label партнёр.
- Infrastructure cost dashboards (Grafana + budgets API) для контроля расходов.
- Blue/green deployments для major релизов после миграции.
