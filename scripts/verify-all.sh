#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

./gradlew build
./gradlew integrationTest
./gradlew compileBrokenExamples

if grep -R -n " issue:" modules/*/broken-examples \
    --include='*.java' --include='*.kt' --include='Dockerfile' \
    --include='*.yml' --include='*.yaml' --include='*.json'; then
    echo "Categorized answer comment found in a clean review target." >&2
    exit 1
fi

if grep -R -n -E '^public class|^@Service|^@Transactional' \
    docs/topics docs/issues docs/scenarios --include='*.md'; then
    echo "Possible hand-copied Java source found in learner documentation." >&2
    exit 1
fi

if grep -R -n -E 'lombok|com\.h2database' modules build-logic gradle \
    --include='*.kts' --include='*.toml'; then
    echo "Banned baseline dependency found." >&2
    exit 1
fi

if compgen -G 'tracks/*/settings.gradle.kts' >/dev/null; then
    ./gradlew buildTracks
fi

if [[ -x .venv/bin/mkdocs ]]; then
    mkdocs_command=(.venv/bin/mkdocs)
else
    mkdocs_command=(mkdocs)
fi
"${mkdocs_command[@]}" build --strict
