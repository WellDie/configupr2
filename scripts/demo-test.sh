#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./run.sh --package A --test --repo-file test-repo/graph.txt --output examples/A.svg --reverse
./run.sh --package D --test --repo-file test-repo/graph.txt --output examples/D.svg
./run.sh --package G --test --repo-file test-repo/graph.txt --output examples/G.svg
