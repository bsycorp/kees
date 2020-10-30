#!/bin/bash
if [ -z "$TRAVIS_BRANCH" ]; then
  TRAVIS_BRANCH="${GITHUB_REF##*/}"
fi
./gradlew clean build bintrayUpload -PappVersion="$TRAVIS_BRANCH"
./gradlew clean nativeImage -PappVersion="$TRAVIS_BRANCH"
(cd init; docker build . -t bsycorp/kees-init:"$TRAVIS_BRANCH")
(cd creator; docker build . -t bsycorp/kees-creator:"$TRAVIS_BRANCH")
docker login -u "$DOCKERUSER" -p "$DOCKERPASS"
docker push bsycorp/kees-init
docker push bsycorp/kees-creator
