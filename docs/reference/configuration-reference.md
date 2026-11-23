# Configuration reference

The packaged `application.yaml` holds the safe defaults. Environment-specific values come from environment variables; the environment profiles come from a mounted file, `config/application.yml` in the working directory (`/work/config/application.yml` in the container). Start from [`config/application.example.yml`](../../config/application.example.yml). Invalid configuration stops the service at boot with a message naming the key.

## ODM services

| Key | Default | Meaning |
| --- | --- | --- |
| `odm.product-plane.registry-service.active` | `true` | Must be `true`: the descriptor comes from Registry v2. |
| `odm.product-plane.registry-service.address` | `${ODM_REGISTRY_SERVICE_ADDRESS}` | Registry v2 base URL. Required. |
| `odm.product-plane.devops-service.active` | `${ODM_DEVOPS_SERVICE_ACTIVE:false}` | When `true`, the report is posted to the task's results before the terminal status. When `false`, it is in the task logs only. |
| `odm.product-plane.devops-service.address` | `${ODM_DEVOPS_SERVICE_ADDRESS}` | DevOps v2 base URL. Required when active. |

## Agent and rules

| Key | Default | Meaning |
| --- | --- | --- |
| `blindata.agent.http.connect-timeout` | `5s` | Connect timeout of the Registry, agent and DevOps calls. The request timeout is the template's (60 s). |
| `blindata.agent.http.read-retries` | `2` | Retries of reads (job history and logs, Registry) on 5xx or 429. The job start is never retried. |
| `blindata.agent.logs.max-agent-log-size` | `1M` | Upper bound of the agent job log copied into the task logs (head and tail kept). |
| `blindata.contract-rules.blocking-property` | `blocking` | ODCS `customProperties` key that makes a rule non-blocking with value `false`. |
| `blindata.contract-rules.connection-property` | `x-blindataConnectionName` | Port property holding the Blindata connection name (same as the observer). |

## Environment profiles

`blindata.environments.<name>`, where `<name>` is the `environment` pipeline parameter (`[A-Za-z0-9_-]+`). At least one profile is required.

| Key | Default | Meaning |
| --- | --- | --- |
| `agent-url` | — | Base URL of the Blindata agent. With one shared agent, the same in every profile. |
| `tenant-uuid` | — | Blindata tenant of the environment, sent as `X-BD-Tenant`. |
| `connection-name-template` | `{name}` | Connection name the agent resolves: `{name}` is the descriptor's `x-blindataConnectionName`, `{environment}` the profile key. Leave `{name}` when agent connections are scoped per tenant. |
| `platform` | `GENERIC` | `BIGQUERY` enables the BigQuery limits of the agent (see [Contract rules](contract-rules.md#what-the-agent-cannot-run)). |
| `auth.type` | — | `API_KEY` or `OAUTH2_CLIENT_CREDENTIALS`. |
| `auth.api-key.user`, `auth.api-key.key` | — | Technical Blindata user and API key (`X-BD-User`, `X-BD-ApiKey`). Observer keys: `blindata.user`, `blindata.password`. |
| `auth.oauth2.url` | — | Token endpoint of the identity provider. |
| `auth.oauth2.grant-type` | `client_credentials` | Grant type of the token request. |
| `auth.oauth2.scope` | — | Scope, e.g. `api://<Blindata API app id>/.default` on Entra ID. |
| `auth.oauth2.client-id` | — | Client id. |
| `auth.oauth2.client-secret` | — | Shared secret, or: |
| `auth.oauth2.client-certificate`, `auth.oauth2.client-certificate-private-key` | — | PEM certificate and PKCS#8 private key, sent as a signed client assertion (Entra ID certificate credentials). |

The OAuth2 keys are the observer's `blindata.oauth2.*`, and the token request is the same code (the template's authenticated client): one identity setup serves both services. The Blindata API accepts its own tokens and Entra ID v2 tokens; an Entra service principal is the Blindata user `msazure:<oid>@<tid>`, which needs `AGENTS_EDITOR` and `AGENTS_VIEWER` in the tenant. The token is cached in memory until it expires.

Secrets (API keys, client secrets, private keys) and tokens are never logged.
