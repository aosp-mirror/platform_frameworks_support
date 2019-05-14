#!/bin/bash
set -e

function usage() {
  echo "usage: $0 <gradle task> <error message> [<subfile path>]"
  echo 'Searches for a minimal set of files such that `./gradlew <gradle task>` fails with message <error message>'
  echo 'Then, if <subfile path> is specified, then individual lines in files in <subfile path> will be considered for removal too'
  exit 1
}

gradleTasks="$1"
errorMessage="$2"
subfilePath="$3"

if [ "$gradleTasks" == "" ]; then
  usage
fi

if [ "$errorMessage" == "" ]; then
  usage
fi

if [ "$subfilePath" == "." ]; then
  echo A subfilePath of '.' is not supported. You should specify a specific file or directory to make the search run more quickly.
  # Also, there's some string manipulation below that assumes that "${subfilePath}.split" is not a subdirectory of "${subfilePath}"
  exit 1
fi

cd "$(dirname $0)"
scriptPath="$(pwd)"
cd ../..
supportRoot="$(pwd)"

rm /tmp/empty -rf
mkdir /tmp/empty

tempDir="/tmp/simplify-build-failure"

echo Running diff-filterer.py once to identify the minimal set of files needed to reproduce the error
./development/file-utils/diff-filterer.py --assume-no-side-effects --work-path .. --num-jobs 6 . /tmp/empty/ "OUT_DIR=./out ./gradlew $gradleTasks > log 2>& 1; grep \"$errorMessage\" log"

if [ "$subfilePath" == "" ]; then
  echo Splitting files into individual lines was not enabled. Done. See results at /tmp/diff-filterer/bestResults
else
  fewestFilesPath="$tempDir/minFilesOutput"
  mkdir -p "$fewestFilesPath"
  echo Copying minimal set of files into $fewestFilesPath
  rm -rf "$fewestFilesPath"
  #cp -rT "/tmp/diff-filterer/bestResults" "$fewestFilesPath"
  cp -rT "/tmp/diff-filterer/roomResults" "$fewestFilesPath"

  echo Creating working directory for identifying individually smallest files
  smallestFilesPath="$tempDir/smallestFilesInput"
  rm -rf "$smallestFilesPath"
  cp -rT "$fewestFilesPath" "$smallestFilesPath"

  echo Splitting files into individual lines
  cd "$smallestFilesPath"
  splitsPath="${subfilePath}.split"
  "${scriptPath}/impl/split.sh" "$subfilePath" "$splitsPath"
  rm "$subfilePath" -rf

  # TODO: maybe we should make diff-filterer.py directly support checking individual line differences within files rather than first running split.sh and asking diff-filterer.py to run join.sh
  # It would be harder to implement in diff-filterer.py though because diff-filterer.py would also need to support comparing against nonempty files too
  echo Running diff-filterer.py again to identify the minimal set of lines needed to reproduce the error
  "$supportRoot/development/file-utils/diff-filterer.py" --assume-no-side-effects --work-path "$(dirname $supportRoot)" --num-jobs 6 "$smallestFilesPath" /tmp/empty/ "${scriptPath}/impl/join.sh ${splitsPath} ${subfilePath} && OUT_DIR=./out ./gradlew $gradleTasks > log 2>& 1; grep '$errorMessage' log"

  echo Re-joining the files
  "${scriptPath}/impl/join.sh" "/tmp/diff-filterer/bestResults/${splitsPath}" "${smallestFilesPath}/${subfilePath}"
  echo "Done. See simplest discovered reproduction test case at ${smallestFilesPath}"
fi
