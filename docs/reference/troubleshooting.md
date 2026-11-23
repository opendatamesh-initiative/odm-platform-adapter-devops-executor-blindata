# Troubleshooting

| Symptom | Cause and fix |
|:--|:--|
| The service does not start: `<key>: required` or `must be ...` | Invalid configuration; the message names the key. See the [Configuration reference](configuration-reference.md). |
| Start fails with 400 `Unknown environment` | The task's `environment` parameter has no profile. Add `blindata.environments.<name>`. |
| Start fails with 400 `Data product version not found in the Registry` | The release tag sent by DevOps does not match the version's tag in the Registry. Set `dataProductVersion` in the task parameters. |
| Start fails with 400 `blocking rule(s) cannot run on the Blindata agent` | The descriptor has blocking rules the agent cannot run on the profile's platform. Rewrite them as suggested, or mark them non-blocking. See [Contract rules](contract-rules.md#what-the-agent-cannot-run). |
| 502 `refused the credentials of the environment profile` | The identity is not a Blindata user of the tenant, lacks `AGENTS_EDITOR` / `AGENTS_VIEWER`, or the API key is wrong. With OAuth2, check the Entra user `msazure:<oid>@<tid>` ([Blindata setup](blindata-setup.md)). |
| 502 `Cannot authenticate to the Blindata agent` | The OAuth2 token request failed: token URL, client id, scope, secret or certificate. |
| A rule is `ERROR` with `Table ... not found` or a permission error | The table is not deployed, or the agent's service account cannot read it. A blocking rule in `ERROR` fails the task. |
| Every rule is `ERROR` with a connection error | The connection name does not resolve in the agent for this tenant. Check `x-blindataConnectionName` and `connection-name-template`. |
| A rule is `FAILED` on a column with NULL values | `invalidValues` counts NULL as invalid; write a `sql` rule if NULL is allowed. |
| The task succeeded but its Results tab is empty | The DevOps service is not active in the executor, or the task has no `activityName` / `taskName`. The report is in the logs. |
| The DevOps activity stays `PENDING` | DevOps approves executions through the Notification service; start it and make sure DevOps is subscribed. |
| DevOps refuses the activity: `repository provider type is required` | DevOps requires `dataProductRepo.providerType` on every task; the executor ignores it. |
| Start fails with 400 `The descriptor of ... cannot be parsed` | The descriptor in the Registry does not match the DPDS model the parser library reads. Check the version's content. |
| The report says `the rules of table ... are not read` | The table's definition does not declare `specification: json-schema` with a 1.x version; the ODCS plugin only reads those. |
