# API reference

The executor implements the DevOps v2 executor API. All calls are `POST` with a JSON body under `/api/v2/up/executor/tasks/`, and every body carries `executorParameters`. OpenAPI: `/api-docs`; Swagger UI: `/swagger-ui`.

| Path | Request body | Response |
|:--|:--|:--|
| `/start` | `{ "executorParameters", "pipelineParameters" }` | `{ "providerRunId" }` |
| `/status` | `{ "executorParameters", "providerRunId" }` | `{ "providerRunId", "status" }` |
| `/logs` | `{ "executorParameters", "providerRunId" }` | `{ "content", "generatedAt" }` |
| `/cancel` | `{ "executorParameters", "providerRunId" }` | empty, 200 |

## start

```json
{
  "executorParameters": { "ref": { "name": "v1.2.0", "type": "TAG" } },
  "pipelineParameters": {
    "dataProductFqn": "urn:dpds:acme:dataproducts:orders:1",
    "environment": "dev",
    "activityName": "deploy-dev",
    "taskName": "verify-contract"
  }
}
```

`executorParameters.ref.name` is the release tag of the version; `pipelineParameters` are described in [DevOps setup](devops-setup.md#declare-the-task). The answer is `{ "providerRunId": "dev:1234" }`: the environment and the agent job's sequence id.

## status

`status` is `RUNNING`, `SUCCEEDED`, `FAILED` or `CANCELED` (see [Concepts](concepts.md#how-the-verdict-is-decided)). On the first call that sees the job ended, the report is posted to the task's results before the answer.

## logs

`content` is plain text: a header with the verdict and the counts, one line per rule, the skipped rules with their reason, notes, then the agent job log.

```text
=== Contract verification: FAILED ===
Data product: urn:dpds:acme:dataproducts:orders:1  version: 1.2.0  environment: dev
Agent job: 1234 (SUCCESS)
Rules: 9 total, 4 passed, 2 failed, 0 error, 3 skipped; 1 blocking failure(s)

OUTCOME  BLOCKING  RULE                                    OBJECT               METRIC  TOTAL  DETAIL
PASSED   yes       sales - orders - orders_not_empty       orders               4       -
FAILED   yes       sales - orders - order_id_not_null      orders.order_id      1       -
FAILED   no        sales - orders - country_code_pattern   orders.country_code  1       -      invalidValues counts NULL values as invalid

Skipped rules:
  - orders_documentation on orders (blocking): type text is not executable

=== Agent job log ===
…
```

## Errors

Errors are plain-text messages that DevOps shows on the task. They never contain credentials.

| Status | When |
|:--|:--|
| 400 | Missing `executorParameters`, `dataProductFqn`, `environment` or release tag; unknown environment; version not found in the Registry; blocking rules the agent cannot run in this environment; malformed `providerRunId` |
| 404 | No such job on the agent, or the job is not a contract verification |
| 409 | Cancel of a job that has already ended |
| 502 | The Registry or the agent failed, or the agent refused the profile's credentials |
