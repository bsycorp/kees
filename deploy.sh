#!/bin/bash
set -e

if [ -z "$TRAVIS_BRANCH" ]; then
  TRAVIS_BRANCH="${GITHUB_REF##*/}"
fi
./gradlew build publish -i -PappVersion="$TRAVIS_BRANCH" --stacktrace
./gradlew nativeImage -i -PappVersion="$TRAVIS_BRANCH" --stacktrace
./gradlew distTar -i --stacktrace
(cd create; docker build . -t bsycorp/kees-creator:"$TRAVIS_BRANCH" -t bsycorp/kees-creator:"${TRAVIS_BRANCH}-java")
docker push bsycorp/kees-creator:"$TRAVIS_BRANCH"
docker push bsycorp/kees-creator:"${TRAVIS_BRANCH}-java"
(cd init; docker build . -f Dockerfile.native -t bsycorp/kees-init:"$TRAVIS_BRANCH")
docker push bsycorp/kees-init:"$TRAVIS_BRANCH"
(cd init; docker build . -t bsycorp/kees-init:"${TRAVIS_BRANCH}-java")
docker push bsycorp/kees-init:"${TRAVIS_BRANCH}-java"
