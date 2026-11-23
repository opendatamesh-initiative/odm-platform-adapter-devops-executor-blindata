#!/bin/sh
# Fallback F1 (BDMD-5755 decision 11): run the contract verification from the last stage of the provisioning
# pipeline (GitLab job or Cloud Build step) through this executor's own API. Prints the report; exits 1 when the
# verification FAILED or was CANCELED, so the pipeline (and the provisioning task) fails.
#
# Required: EXECUTOR_URL, DATA_PRODUCT_FQN, ENVIRONMENT, RELEASE_TAG
# Optional: ACTIVITY_NAME, TASK_NAME (post the report to that task's results), POLL_SECONDS (default 10),
#           TIMEOUT_SECONDS (default 1800). Needs curl and jq.
set -eu

: "${EXECUTOR_URL:?}" "${DATA_PRODUCT_FQN:?}" "${ENVIRONMENT:?}" "${RELEASE_TAG:?}"
POLL_SECONDS="${POLL_SECONDS:-10}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-1800}"
API="${EXECUTOR_URL%/}/api/v2/up/executor/tasks"

call() {
  curl -sS --fail-with-body -H 'Content-Type: application/json' -X POST "$API/$1" --data "$2"
}

params=$(jq -n --arg fqn "$DATA_PRODUCT_FQN" --arg env "$ENVIRONMENT" \
  --arg activity "${ACTIVITY_NAME:-}" --arg task "${TASK_NAME:-}" \
  '{dataProductFqn: $fqn, environment: $env} + (if $activity != "" then {activityName: $activity, taskName: $task} else {} end)')
start_body=$(jq -n --arg tag "$RELEASE_TAG" --argjson params "$params" \
  '{executorParameters: {ref: {name: $tag, type: "TAG"}}, pipelineParameters: $params}')

run_id=$(call start "$start_body" | jq -r '.providerRunId')
echo "Contract verification started: $run_id"
run_body=$(jq -n --arg tag "$RELEASE_TAG" --arg id "$run_id" '{executorParameters: {ref: {name: $tag}}, providerRunId: $id}')

waited=0
status=RUNNING
while [ "$status" = RUNNING ]; do
  if [ "$waited" -ge "$TIMEOUT_SECONDS" ]; then
    echo "Contract verification $run_id still running after ${TIMEOUT_SECONDS}s" >&2
    exit 1
  fi
  sleep "$POLL_SECONDS"
  waited=$((waited + POLL_SECONDS))
  status=$(call status "$run_body" | jq -r '.status')
done

call logs "$run_body" | jq -r '.content'
echo "Contract verification $run_id: $status"
[ "$status" = SUCCEEDED ]
