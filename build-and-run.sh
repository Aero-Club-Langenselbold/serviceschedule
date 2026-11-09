#!/usr/bin/env bash
# Builds schedule-api and schedule-ui and runs the whole stack in Podman.
set -euo pipefail

cd "$(dirname "$(readlink -f "$0")")"

API_HEALTH_URL="http://localhost:8080/api/v1/actuator/health"
UI_URL="http://localhost:8081/"
START_TIMEOUT_SECONDS=180

usage() {
    cat <<USAGE
Usage: ./build-and-run.sh [command] [options]

Commands:
  up       Build everything and start the stack (default)
  stop     Stop the containers, the database content is kept
  logs     Follow the container logs
  status   Show the container status

Options for up:
  --no-build   Start the stack from the last build
  --tests      Run the backend tests during the build
  --prod       Build the UI against the API URL from .env.production
               (default: .env, which points to the local API)
  -h, --help   Show this help
USAGE
}

log() { printf '\n==> %s\n' "$*"; }
die() { printf 'Error: %s\n' "$*" >&2; exit 1; }

require_command() {
    command -v "$1" >/dev/null || die "$1 is not installed."
}

compose() {
    podman compose "$@"
}

ensure_podman_socket() {
    # podman compose delegates to docker-compose, which talks to the Podman API socket.
    if [[ "$(podman info --format '{{.Host.RemoteSocket.Exists}}')" != "true" ]]; then
        systemctl --user start podman.socket \
            || die "Could not start podman.socket, run: systemctl --user enable --now podman.socket"
    fi
}

check_config() {
    [[ -f .env ]] || die ".env is missing, it has to define CORS_ALLOWEDORIGINS (e.g. http://localhost:8081)."
    [[ -f schedule-api/src/main/resources/application-docker.properties ]] \
        || die "schedule-api/src/main/resources/application-docker.properties is missing (not in git)."
}

build_api() {
    log "Building schedule-api"
    local args=(clean package)
    [[ "$run_tests" == true ]] || args+=(-DskipTests)
    (cd schedule-api && mvn -q "${args[@]}")
}

build_ui() {
    log "Building schedule-ui (API URL from $ui_env_file)"
    (
        cd schedule-ui
        pnpm install --frozen-lockfile
        pnpm exec nuxt generate --dotenv "$ui_env_file"
    )
}

wait_for() {
    local name="$1" url="$2" deadline=$((SECONDS + START_TIMEOUT_SECONDS))
    printf 'Waiting for %s ' "$name"
    until curl -fsS -o /dev/null --max-time 3 "$url" 2>/dev/null; do
        if ((SECONDS >= deadline)); then
            printf '\n'
            compose ps -a || true
            compose logs --tail 40 schedule-api || true
            die "$name did not become available within ${START_TIMEOUT_SECONDS}s."
        fi
        printf '.'
        sleep 3
    done
    printf ' ok\n'
}

cmd_up() {
    require_command podman
    require_command curl
    check_config
    ensure_podman_socket

    if [[ "$build" == true ]]; then
        require_command mvn
        require_command pnpm
        build_api
        build_ui
        log "Building container images"
        compose build
    fi

    log "Starting containers"
    compose up -d
    # nuxt generate replaces the bind-mounted output directory, a running nginx would keep serving the old one.
    compose up -d --no-deps --force-recreate schedule-ui

    wait_for "the API" "$API_HEALTH_URL"
    wait_for "the UI" "$UI_URL"

    log "Stack is running"
    printf 'UI:  %s\nAPI: %s\n' "$UI_URL" "${API_HEALTH_URL%/actuator/health}"
}

command_name="up"
build=true
run_tests=false
ui_env_file=".env"

while (($# > 0)); do
    case "$1" in
        up | stop | logs | status) command_name="$1" ;;
        --no-build) build=false ;;
        --tests) run_tests=true ;;
        --prod) ui_env_file=".env.production" ;;
        -h | --help) usage; exit 0 ;;
        *) usage >&2; exit 2 ;;
    esac
    shift
done

case "$command_name" in
    up) cmd_up ;;
    stop) compose stop ;;
    logs) compose logs -f ;;
    status) compose ps -a ;;
esac
