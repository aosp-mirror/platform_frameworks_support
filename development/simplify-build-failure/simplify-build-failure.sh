#!/bin/bash
set -e

function usage() {
  echo 'NAME'
  echo '  simplify-build-failure.sh'
  echo
  echo 'SYNOPSIS'
  echo "  $0 (--task <gradle task> <error message> | --command <shell command> ) [<subfile path>]"
  echo
  echo DESCRIPTION
  echo '  Searches for a minimal set of files and/or lines required to reproduce a given build failure'
  echo
  echo OPTIONS
  echo '  --analyze-all'
  echo '    By default, certain files such as those in buildSrc/ are ignored. This flags reenables analysis of all files'
  echo
  echo '  --task <gradle task> <error message>`'
  echo '    Specifies that `./gradlew <gradle task>` must fail with error message <error message>'
  echo
  echo '  --command <shell command>'
  echo '    Specifies that <shell command> must succeed.'
  echo
  echo '<subfile path>'
  echo '  If <subfile path> is specified, then individual lines in files in <subfile path> will be considered for removal, too'
  exit 1
}

if [ "$1" == "--task" ]; then
  shift
  gradleTasks="$1"
  errorMessage="$2"

  if [ "$gradleTasks" == "" ]; then
    usage
  fi

  if [ "$errorMessage" == "" ]; then
    usage
  fi

  subfilePath="$3"
  testCommand="OUT_DIR=out ./gradlew $gradleTasks > log 2>&1; grep \"$errorMessage\" log"
else
  if [ "$1" == "--command" ]; then
    shift
    testCommand="$1"
    if [ "$testCommand" == "" ]; then
      usage
    fi
    if echo "$testCommand" | grep -v OUT_DIR 2>/dev/null; then
      echo "Error: must set OUT_DIR in the test command to prevent concurrent Gradle executions from interfering with each other"
      exit 1
    fi

    subfilePath="$2"
  else
    usage
  fi
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

tempDir="/tmp/simplify-build-failure"
referencePassingDir="$tempDir/base"

rm "$tempDir/base" -rf
mkdir "$tempDir/base"

if [ "$subfilePath" != "" ]; then
  if [ ! -e "$subfilePath" ]; then
    echo "$subfilePath" does not exist
    exit 1
  fi
fi

echo Running diff-filterer.py once to identify the minimal set of files needed to reproduce the error
./development/file-utils/diff-filterer.py --assume-no-side-effects --work-path .. --num-jobs 6 . "$referencePassingDir" "$testCommand"

if [ "$subfilePath" == "" ]; then
  echo Splitting files into individual lines was not enabled. Done. See results at /tmp/diff-filterer/bestResults
else
  fewestFilesPath="$tempDir/minFilesOutput"
  mkdir -p "$fewestFilesPath"
  echo Copying minimal set of files into $fewestFilesPath
  rm -rf "$fewestFilesPath"
  cp -rT "/tmp/diff-filterer/bestResults" "$fewestFilesPath"

  echo Creating working directory for identifying individually smallest files
  smallestFilesInput="$tempDir/smallestFilesInput"
  rm -rf "$smallestFilesInput"
  cp -rT "$fewestFilesPath" "$smallestFilesInput"

  echo Splitting files into individual lines
  cd "$smallestFilesInput"
  splitsPath="${subfilePath}.split"
  "${scriptPath}/impl/split.sh" "$subfilePath" "$splitsPath"
  rm "$subfilePath" -rf

  # TODO: maybe we should make diff-filterer.py directly support checking individual line differences within files rather than first running split.sh and asking diff-filterer.py to run join.sh
  # It would be harder to implement in diff-filterer.py though because diff-filterer.py would also need to support comparing against nonempty files too
  echo Running diff-filterer.py again to identify the minimal set of lines needed to reproduce the error
  "$supportRoot/development/file-utils/diff-filterer.py" --assume-no-side-effects --work-path "$(dirname $supportRoot)" --num-jobs 6 "$smallestFilesInput" "$referencePassingDir" "${scriptPath}/impl/join.sh ${splitsPath} ${subfilePath} && $testCommand"

  echo Re-joining the files
  smallestFilesOutput="$tempDir/smallestFilesOutput"
  rm -rf "${smallestFilesOutput}"
  cp -rT /tmp/diff-filterer/bestResults "${smallestFilesOutput}"
  cd "${smallestFilesOutput}"
  "${scriptPath}/impl/join.sh" "${splitsPath}" "${subfilePath}"

  echo "Done. See simplest discovered reproduction test case at ${smallestFilesOutput}"
fi
