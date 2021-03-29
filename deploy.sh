#!/bin/bash
set -e

if [ -z "$TRAVIS_BRANCH" ]; then
  TRAVIS_BRANCH="${GITHUB_REF##*/}"
fi
./gradlew build bintrayUpload -i -PappVersion="$TRAVIS_BRANCH" --stacktrace
./gradlew nativeImage -i -PappVersion="$TRAVIS_BRANCH" --stacktrace
(cd create; docker build . -t bsycorp/kees-creator:"$TRAVIS_BRANCH")
docker push bsycorp/kees-creator:$TRAVIS_BRANCH
(cd init; docker build . -t bsycorp/kees-init:"$TRAVIS_BRANCH")
docker push bsycorp/kees-init:$TRAVIS_BRANCH
