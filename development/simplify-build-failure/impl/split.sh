#!/bin/bash
set -e

function usage() {
  echo "usage: $0 <source dir> <exploded dir>"
  echo "This script splits files in <source dir> by line and puts the results in <exploded dir>"
  exit 1
}

sourceDir="$1"
explodedDir="$2"

if [ "$sourceDir" == "" ]; then
  usage
fi
sourceDir="$(cd $sourceDir && pwd)"

if [ "$explodedDir" == "" ]; then
  usage
fi
mkdir -p "$explodedDir"
explodedDir="$(cd $explodedDir && pwd)"

function explodePath() {
  sourcePath="$1"
  explodedPath="$2"
  #echo exploding $sourcePath into $explodedPath

  mkdir -p "$explodedPath"

  cat $sourcePath | sed 's/.*/./' | grep -o "^." -n | sed 's/:.//' | sed "s|\(.*\)|head -n \1 $sourcePath \| tail -n 1 > $explodedPath/\1|" | bash
}


function main() {
  rm "$explodedDir" -rf
  mkdir -p "$explodedDir"

  cd $sourceDir
  echo splitting everything in $(pwd) into $explodedDir
  for filePath in $(find -type f); do
    explodePath "$filePath" "$explodedDir/$filePath"
  done
  echo done splitting everything in $(pwd) into $explodedDir
}


main
