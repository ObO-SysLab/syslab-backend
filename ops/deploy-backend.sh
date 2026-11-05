#!/usr/bin/env bash
# Runs as root on the API host. Provision schema, secrets and infrastructure before main merge.
set -euo pipefail
umask 077
test "$(id -u)" = 0
image=${1:?immutable image URI required}
revision=${2:?source revision required}
[[ "$revision" =~ ^[0-9a-f]{40}$ ]]
[[ "$image" =~ ^290373175858\.dkr\.ecr\.ap-northeast-2\.amazonaws\.com/[a-z0-9][a-z0-9._/-]*@sha256:[0-9a-f]{64}$ ]]
exec 9>/var/lock/diveon-backend-deploy.lock
flock -n 9
config=/etc/diveon/application-secret.yml
runtime_env=/etc/diveon/backend.env
protected_file() {
  [[ -s "$1" && ! -L "$1" ]]
  test "$(stat -c %u "$1")" = 0
  [[ "$(stat -c %a "$1")" == 600 || "$(stat -c %a "$1")" == 400 ]]
}
protected_file "$config"
env_args=()
if [[ -e "$runtime_env" ]]; then
  protected_file "$runtime_env"
  env_args=(--env-file "$runtime_env")
fi
test -s /home/ec2-user/syslab-key.pem
test "$(docker exec redis redis-cli ping)" = PONG
release_dir="/var/lib/diveon/releases/$revision/backend-$(date -u +%Y%m%dT%H%M%SZ)-$$"
mkdir -p -- "$release_dir"
chmod 700 -- "$release_dir"
export DOCKER_CONFIG
DOCKER_CONFIG=$(mktemp -d /run/diveon-docker-login.XXXXXX)
previous=''
candidate_created=false
cleanup() {
  status=$?
  trap - EXIT
  set +e
  if [[ "$status" != 0 ]] && "$candidate_created" && \
      [[ "$(docker inspect --format '{{index .Config.Labels "net.diveon.source"}}' syslab-backend 2>/dev/null)" == "$revision" ]]; then
    docker logs syslab-backend > "$release_dir/failed-container.log" 2>&1
    docker update --restart=no syslab-backend > /dev/null
    docker stop --time 20 syslab-backend > /dev/null
    docker rename syslab-backend "syslab-backend-failed-${revision:0:12}-$$"
    echo "Candidate failed and was stopped; previous container remains preserved: ${previous:-none}. Protected diagnostics: $release_dir" >&2
  fi
  rm -f -- "$DOCKER_CONFIG/config.json"
  rmdir -- "$DOCKER_CONFIG"
  exit "$status"
}
trap cleanup EXIT
trap 'exit 130' HUP INT TERM
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 290373175858.dkr.ecr.ap-northeast-2.amazonaws.com > /dev/null
docker pull "$image"
test "$(docker image inspect --format '{{index .RepoDigests 0}}' "$image" | cut -d@ -f2)" = "${image##*@}"
if docker container inspect syslab-backend > "$release_dir/previous-container.json" 2>/dev/null; then
  previous="syslab-backend-prev-$(date -u +%Y%m%dT%H%M%SZ)-$$"
  docker stop --time 30 syslab-backend > /dev/null
  # A preserved stopped container must not revive on a later Docker daemon restart.
  # The original restart policy is retained in previous-container.json for reviewed rollback.
  docker update --restart=no syslab-backend > /dev/null
  docker rename syslab-backend "$previous"
fi
candidate_created=true
docker run -d --name syslab-backend --restart always --network host \
  --label "net.diveon.source=$revision" \
  "${env_args[@]}" \
  --env JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Seoul \
  --env SPRING_PROFILES_ACTIVE=prod \
  --env SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
  --env SPRING_CONFIG_ADDITIONAL_LOCATION=file:/run/diveon/ \
  --mount "type=bind,src=$config,dst=/run/diveon/application-secret.yml,readonly" \
  --mount type=bind,src=/home/ec2-user/syslab-key.pem,dst=/app/syslab-key.pem,readonly \
  "$image" > "$release_dir/container-id"
healthy=false
for attempt in {1..40}; do
  test "$(docker inspect --format '{{.State.Running}}' syslab-backend)" = true || break
  # Existing public DB-backed controller; no assumed actuator endpoint or production write.
  status=$(curl --silent --output /dev/null --write-out '%{http_code}' --connect-timeout 2 --max-time 3 'http://127.0.0.1:8080/api/ad?placement=main' || true)
  if [[ "$status" == 200 ]]; then healthy=true; break; fi
  sleep 3
done
"$healthy"
denied=$(curl --silent --output /dev/null --write-out '%{http_code}' --connect-timeout 2 --max-time 3 http://127.0.0.1:8080/api/profile/show)
[[ "$denied" == 401 || "$denied" == 403 ]]
test "$(docker inspect --format '{{.Config.Image}}' syslab-backend)" = "$image"
printf '{"revision":"%s","image":"%s","previousContainer":"%s","databaseReadiness":true,"anonymousProtectedRouteDenied":true}\n' "$revision" "$image" "$previous" > "$release_dir/deployment.json"
echo "Deployed revision=$revision image=$image previous=${previous:-none}"
