#!/bin/bash
set -e

supportRoot="$(cd $(dirname $0)/.. && pwd)"
checkoutRoot="$(cd ${supportRoot}/../.. && pwd)"

function usage() {
  echo "usage: $0 <git treeish>"
  echo
  echo "For example, $0 HEAD^"
  echo
  echo "Validates that libraries built from <git treeish> are the same as the build outputs at HEAD"
  echo "This can be used to validate that a refactor did not change the outputs"
  return 1
}

oldCommit="$1"
if [ "$oldCommit" == "" ]; then
  usage
fi
newCommit="$(git log -1 --format=%H)"

oldOutPath="${checkoutRoot}/out-old"
newOutPath="${checkoutRoot}/out-new"
tempOutPath="${checkoutRoot}/out"

function echoAndDo() {
  echo "$*"
  eval "$*"
}

function doBuild() {
  ./gradlew createArchive
  unzip "${tempOutPath}/dist/top-of-tree-m2repository-all-0.zip" -d "${tempOutPath}/dist/top-of-tree-m2repository-all-0.unzipped"
}

rm -rf "$oldOutPath" "$newOutPath" "$tempOutPath"

echo building new commit
echoAndDo git checkout "$newCommit"
doBuild
mv "$tempOutPath" "$newOutPath"


echo building previous commit
echoAndDo git checkout "$oldCommit"
if doBuild; then
  echo previous build succeeded
else
  echo previous build failed
  git checkout "$newCommit"
  exit 1
fi
git checkout "$newCommit"
mv "$tempOutPath" "$oldOutPath"

echo
echo diffing results
# Don't care about maven-metadata files because they have timestamps in them
# We might care to know whether .sha1 or .md5 files have changed, but changes in those files will always be accompanied by more meaningful changes in other files, so we don't need to show changes in .sha1 or .md5 files
echoAndDo diff -r -x "maven-metadata*" -x "*.sha1" -x "*.md5" "$oldOutPath/dist/top-of-tree-m2repository-all-0.unzipped" "$newOutPath/dist/top-of-tree-m2repository-all-0.unzipped"
echo end of difference
