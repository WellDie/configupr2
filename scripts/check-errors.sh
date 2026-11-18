#!/usr/bin/env bash
set -u
cd "$(dirname "$0")/.."
check_error() {
  label="$1"
  shift
  if ./run.sh "$@" >/dev/null 2>&1; then
    echo "[FAIL] $label: expected non-zero exit code"
    exit 1
  else
    code=$?
    if [ "$code" -eq 2 ]; then
      echo "[OK] $label"
    else
      echo "[FAIL] $label: exit code $code"
      exit 1
    fi
  fi
}
check_error "missing package" --repository https://example.org
check_error "invalid package name" --package "bad name" --repository https://example.org
check_error "missing repository" --package A
check_error "invalid repository URL" --package A --repository invalid-url
check_error "missing test file" --package A --test --repo-file missing.txt
check_error "invalid output format" --package A --test --repo-file test-repo/graph.txt --output graph.png
check_error "unknown option" --package A --unknown
