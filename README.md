# Blindata executor

A stateless full-control DevOps executor that **verifies the data contract** of a data product version in one environment. The DevOps server calls it as the "verify contract" task of a deploy activity, after the provisioning task. The executor reads the version's descriptor from the ODM Registry, runs its executable contract rules (ODCS `quality`, types `library` and `sql`) on the Blindata agent of the environment, in dry run, and answers `SUCCEEDED` or `FAILED` with the per-rule detail.

The service is a **Quarkus 3** application on port **9080**. Analysis: [`spdd/analysis/BDMD-5755-…-blindata-devops-executor.md`](spdd/analysis).

## What it does

1. **Start.** Finds the version in Registry v2 by `dataProductFqn` and the release tag (`executorParameters.ref.name`), or by `dataProductVersion` when given. Reads the rules of every port with `x-blindataConnectionName`, like the Blindata observer: check code `<domain> - <name> - <rule id>`, physical binding from `databaseSchemaName`, table and column. Launches one `QUALITY_PROBES` job of `CONTRACT_RULE` checks on the agent, always with `dryRun: true`, and returns `providerRunId = <environment>:<sequenceId>`.
2. **Status.** Reads the job history. While the job runs: `RUNNING`. When it ends, computes the verdict from the rules, not from the agent's run status (a failed rule does not fail the agent job): `FAILED` when a **blocking** rule fails or errors, `SUCCEEDED` otherwise. Before answering the terminal status it posts the report to the task's results in DevOps (when configured).
3. **Logs.** The full report as a table (outcome, blocking flag, rule, object, metric, detail; skipped rules with their reason), followed by the agent job log.
4. **Cancel.** The agent cannot stop an ad-hoc job: a running job is accepted (200) and ends on its own; an ended job is 409; an unknown job 404.

Nothing is exported to Blindata at deploy time. **The ODM task (results and logs) is the record of the run**, including failed deploys. Blindata gets tables, checks and probes from each environment's observer, after the deploy activity of its stage succeeds.

### Rules

| Rule | Outcome |
| --- | --- |
| `library` or `sql` with an `id`, on a port with `x-blindataConnectionName` | sent to the agent |
| `text`, `custom`, no `id`, no connection name, check code declared twice | `SKIPPED`, with the reason |
| quality reference (`customProperties.refName`), legacy rule (`scoreStrategy`) | ignored |
| agent row with score > 0 / score 0 / error row, no score or no row | `PASSED` / `FAILED` / `ERROR` |

A rule is **blocking** unless its `customProperties` set `blocking` to `false` (boolean or string; ODCS list form `[{property, value}]` or object form).

### Rules the agent cannot run

The agent builds library SQL with no dialect switch and emits identifiers unquoted. At start, each rule is checked against the profile's `platform`:

- `invalidValues` with `arguments.pattern` cannot run on `BIGQUERY` (`REGEXP_LIKE`, `CAST … AS VARCHAR`): write it as a `sql` rule with `REGEXP_CONTAINS`.
- Schema, table and column names must match `[a-zA-Z0-9_]+` on any platform. On BigQuery the binding schema is the dataset only; the project comes from the connection's default project.
- Unknown library metrics are refused (`nullValues`, `missingValues`, `invalidValues`, `duplicateValues`, `rowCount` are supported).

A **blocking** rule that cannot run **rejects the start** (400, with the reason and the `sql` to write); a non-blocking one is `SKIPPED`. Also note that `invalidValues` counts `NULL` as invalid.

There is no time window: library metrics scan the whole table on every deploy. On high-frequency tables, write the rules as `sql` rules on the latest partition.

## Requirements

- Java 21
- Maven 3.9+ or the included `./mvnw` wrapper
- ODM Registry v2, ODM DevOps v2 (`odm-platform-pp-devops-server`), a Blindata agent with `CONTRACT_RULE` probes

## Run

```bash
./mvnw quarkus:dev
```

The environment profiles are not packaged: put them in `config/application.yml` (copy [`config/application.example.yml`](config/application.example.yml)). Quarkus reads that file from the working directory; in the container it is `/work/config/application.yml`.

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

Native image and Docker:

```bash
./mvnw package -DskipTests -Dnative -Dquarkus.native.container-build=true
docker build -t odm-platform-adapter-devops-executor-blindata .
docker run --rm -p 9080:9080 -v "$PWD/config:/work/config:ro" -e ODM_REGISTRY_SERVICE_ADDRESS=http://registry:8080 odm-platform-adapter-devops-executor-blindata
```

Tests (unit tests in `test`, integration tests with the application running in `verify`):

```bash
./mvnw verify
```

## Endpoints

All four calls are `POST` under `/api/v2/up/executor/tasks/`. Every body carries `executorParameters`. OpenAPI at `/api-docs`, Swagger UI at `/swagger-ui`.

| Path | Request body | Response |
| --- | --- | --- |
| `/start` | `{ "executorParameters", "pipelineParameters" }` | `{ "providerRunId" }` (`<environment>:<sequenceId>`) |
| `/status` | `{ "executorParameters", "providerRunId" }` | `{ "providerRunId", "status" }` |
| `/logs` | `{ "executorParameters", "providerRunId" }` | `{ "content", "generatedAt" }` |
| `/cancel` | `{ "executorParameters", "providerRunId" }` | empty **200** |

`pipelineParameters` (from the descriptor task's `configurations.params`):

| Parameter | Required | Meaning |
| --- | --- | --- |
| `dataProductFqn` | yes | Data product to verify. |
| `environment` | yes | Key of the environment profile (e.g. `lab`, `prd`). |
| `dataProductVersion` | no | Version number; wins over the release tag. |
| `activityName`, `taskName` | no | Needed to post the report to the task's results in DevOps. Without them the report is in the logs only. |

Errors are plain text: **400** missing parameter, unknown environment, version not found, blocking rule the agent cannot run, malformed `providerRunId`; **404** unknown agent job; **409** cancel of an ended job; **502** the Registry or the agent failed or refused the credentials. Messages never contain credentials.

Descriptor task:

```yaml
lifecycleInfo:
  deploy-lab:
    - name: provisioning
      service: { $href: gitlab }
    - name: verify-contract
      service: { $href: blindata }
      template: { definition: {} }
      configurations:
        params:
          dataProductFqn: urn:dpds:acme:dataproducts:orders:1
          environment: lab
          activityName: deploy-lab
          taskName: verify-contract
```

## Status mapping

| Agent `runStatus` | Executor status |
| --- | --- |
| `RUNNING`, unknown value | `RUNNING` (the DevOps poll cap bounds the wait) |
| `SUCCESS`, `WARNING` | `FAILED` if a blocking rule is `FAILED` or `ERROR`, else `SUCCEEDED` |
| `ERROR` | `FAILED` |
| `STOPPED` | `CANCELED` |

## Configuration

ODM services follow the ODM pattern `odm.product-plane.<name>-service` (`active`, `address`); Blindata settings live under `blindata`. Full reference: [`docs/reference/configuration-reference.md`](docs/reference/configuration-reference.md).

Authentication to Blindata supports the same options as the Blindata observer, per environment: an API key (`X-BD-User` + `X-BD-ApiKey`) or OAuth2 client credentials with a shared secret or a certificate (e.g. a Microsoft Entra ID service principal, which the Blindata API maps to the user `msazure:<oid>@<tid>`; that user needs `AGENTS_EDITOR` and `AGENTS_VIEWER` in the tenant).

## DevOps configuration

On the DevOps server:

```yaml
odm:
  utility-plane:
    executors:
      polling-interval: 30s
      services:
        blindata:
          address: http://odm-platform-adapter-devops-executor-blindata:9080
          execution-mode: full-control
```

## Blindata agent configuration

Declare the executor `blindata` in the agent's DevOps configuration with **no secrets**: the executor uses the technical identity of each environment profile, not the user's credentials.

## Fallback without the executor

[`scripts/verify-contract.sh`](scripts/verify-contract.sh) runs the same check from the last stage of the provisioning pipeline (GitLab job or Cloud Build step): it calls this service's start, status and logs, prints the report and exits 1 on `FAILED`, so the provisioning task fails.

## Statelessness

No database and no run store. The environment travels in the provider run id; the rules, their blocking flags and the verification context travel in the agent job (`configs.odm`, `configs.checks[].odm`, `configs.skipped`), which the agent keeps in its history. The only cache is the OAuth2 token, a performance cache. Any number of replicas can run behind one address.
