#!/bin/bash
set -e

outDir=../../out/host/gradle/frameworks/support/build

function runDokka() {
  ./gradlew --no-daemon dokka --stacktrace
}

rm -rf $outDir/dokka*
runDokka
mv $outDir/dokka $outDir/dokka_1
runDokka
mv $outDir/dokka $outDir/dokka_2

if diff -r $outDir/dokka_1 $outDir/dokka_2; then
  echo no diff
  exit 0
else
  echo found a diff
  exit 1
fi
