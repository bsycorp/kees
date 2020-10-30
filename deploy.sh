#!/bin/bash
set -e
export BINTRAY_USER=${{ secrets.BINTRAY_USER }}
export BINTRAY_KEY=${{ secrets.BINTRAY_KEY }}

if [ -z "$TRAVIS_BRANCH" ]; then
  TRAVIS_BRANCH="${GITHUB_REF##*/}"
fi
./gradlew build bintrayUpload -i -PappVersion="$TRAVIS_BRANCH" --stacktrace
./gradlew nativeImage -i -PappVersion="$TRAVIS_BRANCH" --stacktrace
(cd init; docker build . -t bsycorp/kees-init:"$TRAVIS_BRANCH")
(cd create; docker build . -t bsycorp/kees-create:"$TRAVIS_BRANCH")
echo ${{ secrets.DOCKERPASS }} | docker login -u ${{ secrets.DOCKERUSER }} --password-stdin
docker push bsycorp/kees-init
docker push bsycorp/kees-create
