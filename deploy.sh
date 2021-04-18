#!/bin/bash
set -e

if [ -z "$TRAVIS_BRANCH" ]; then
  TRAVIS_BRANCH="${GITHUB_REF##*/}"
fi
./gradlew build publish -i -PappVersion="$TRAVIS_BRANCH" --stacktrace