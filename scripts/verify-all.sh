#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

./gradlew build
./gradlew integrationTest
./gradlew compileBrokenExamples
./gradlew compileExamples

if grep -R -n " issue:" modules/*/broken-examples tracks/*/modules/*/broken-examples \
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

python3 - <<'EOF'
import os, sys

topics_dir = "docs/topics"
for slug in os.listdir(topics_dir):
    q_file = os.path.join(topics_dir, slug, "questions.md")
    if os.path.isfile(q_file):
        with open(q_file) as f:
            text = f.read()
        questions = len([l for l in text.splitlines() if l.startswith("### ")])
        reveals = len([l for l in text.splitlines() if '??? question "Reveal answer"' in l])
        examples = len([l for l in text.splitlines() if '??? example "Example"' in l])
        if not (questions == reveals == examples):
            print(f"Error in {q_file}: count mismatch! Questions: {questions}, Reveals: {reveals}, Examples: {examples}", file=sys.stderr)
            sys.exit(1)
print("All topic question files match structural counts.")
EOF

