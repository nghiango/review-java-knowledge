#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! $1 =~ ^[a-z0-9-]+$ ]]; then
    echo "Usage: scripts/run-module.sh <slug>" >&2
    exit 64
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"
shopt -s nullglob
matches=(modules/[0-9][0-9]-"$1")

if [[ ${#matches[@]} -ne 1 ]]; then
    echo "Expected one module for slug '$1'; found ${#matches[@]}." >&2
    exit 1
fi

module_name="${matches[0]#modules/}"
project_path=":modules:${module_name}"
if ! ./gradlew "${project_path}:tasks" --all --quiet | grep -q '^bootRun '; then
    echo "Module '$1' is a library/demo module; run ./gradlew ${project_path}:test" >&2
    exit 2
fi

exec ./gradlew "${project_path}:bootRun"
