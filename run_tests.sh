#!/bin/bash
set -e

cargo test -- --list --format=terse | grep ': test' | sed 's/: test//' | while read test_name; do
    echo "Running test $test_name"
    timeout 0.5s cargo test -- --nocapture --test-threads=1 "$test_name"
done
