#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
build_dir=$(mktemp -d)
trap 'rm -rf "$build_dir"' EXIT HUP INT TERM
java com.sun.tools.javac.Main -d "$build_dir" \
  app/src/main/java/com/veilreader/app/domain/ReadingPolicy.java \
  tools/tests/ReadingPolicyTest.java
java -cp "$build_dir" ReadingPolicyTest
