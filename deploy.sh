#!/bin/bash
./gradlew clean build shadow bintrayUpload -PappVersion=$TRAVIS_BRANCH
./gradlew clean build shadow
docker build . -t bsycorp/kees/init:$TRAVIS_BRANCH -f docker/Dockerfile.init
docker build . -t bsycorp/kees/creator:$TRAVIS_BRANCH -f docker/Dockerfile.creator
docker push bsycorp/kees