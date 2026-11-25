# Build, run and deploy

The executor is a Quarkus 3 service on port **9080**, Java 21, no database.

## Build and test

```bash
./mvnw verify
```

Unit tests (`*Test`) run without the application; integration tests (`*IT`) start it against a stub of the Registry, the agent, an OAuth2 token endpoint and DevOps.

## Run

The environment profiles are never packaged: put them in `config/application.yml` in the working directory (copy [`config/application.example.yml`](../../config/application.example.yml)), and set the service addresses as environment variables.

```bash
./mvnw package
ODM_REGISTRY_SERVICE_ADDRESS=http://localhost:8086 \
ODM_DEVOPS_SERVICE_ACTIVE=true ODM_DEVOPS_SERVICE_ADDRESS=http://localhost:8002 \
java -jar target/quarkus-app/quarkus-run.jar
```

Live reload: `./mvnw quarkus:dev`.

## Container

```bash
./mvnw package -DskipTests -Dnative -Dquarkus.native.container-build=true
docker build -t odm-platform-adapter-devops-executor-blindata .
docker run --rm -p 9080:9080 \
  -v "$PWD/config:/work/config:ro" \
  -e ODM_REGISTRY_SERVICE_ADDRESS=http://registry:8080 \
  -e ODM_DEVOPS_SERVICE_ACTIVE=true -e ODM_DEVOPS_SERVICE_ADDRESS=http://devops:8080 \
  odm-platform-adapter-devops-executor-blindata
```

In the container the profiles file is `/work/config/application.yml`. Pass secrets as environment variables referenced by the profiles file.

## End-to-end run on a local stack

1. Start Postgres, the Blindata API and a Blindata agent, the ODM Registry v2, Notification and DevOps v2. Register the executor in DevOps ([DevOps setup](devops-setup.md)).
2. In the agent, use a connection that reaches a local database, and create a table with a few rows.
3. Create a data product and a version in the Registry whose descriptor declares rules on that table and the connection name in `x-blindataConnectionName`.
4. Call `POST /api/v2/pp/devops/activities/execute` with one task whose `executorName` is `blindata`, then read the activity: the task carries the report in its results and logs.

Use `platform: GENERIC` for a Postgres connection; set `BIGQUERY` to see which rules would be refused or skipped on BigQuery.
