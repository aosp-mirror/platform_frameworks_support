#!/bin/bash
set -e

function usage() {
  echo "usage: $0 <exploded dir> <source dir>"
  echo "This script concatenates files in <exploded dir> and puts the results in <source dir>"
  exit 1
}

explodedDir="$1"
sourceDir="$2"

if [ "$sourceDir" == "" ]; then
  usage
fi
mkdir -p "$sourceDir"
sourceDir="$(cd $sourceDir && pwd)"

if [ "$explodedDir" == "" ]; then
  usage
fi
explodedDir="$(cd $explodedDir && pwd)"

function joinPath() {
  explodedPath="$1"
  sourcePath="$2"
  #echo joining $explodedPath into $sourcePath

  mkdir -p "$(dirname $sourcePath)"

  cd $explodedPath
  ls | sort -n | xargs cat > "$sourcePath"
}


function main() {
  rm "$sourceDir" -rf
  mkdir -p "$sourceDir"

  cd $explodedDir
  echo finding everything in $explodedDir
  filePaths="$(find -type f | sed 's|/[^/]*$||' | sort | uniq)"
  echo joining all file paths under $explodedDir into $sourceDir
  for filePath in $filePaths; do
    joinPath "$explodedDir/$filePath" "$sourceDir/$filePath"
  done
  echo done joining all file paths under $explodedDir into $sourceDir
}


main
