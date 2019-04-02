#!/bin/bash
set -e
set -u

scriptName="$(basename $0)"

function usage() {
  echo "usage: $0 <tasks>"
  echo "Attempts to diagnose why "'`'"./gradlew <tasks>"'`'" fails"
  echo
  echo "For example:"
  echo
  echo "  $0 assembleDebug # or any other arguments you would normally give to ./gradlew"
  echo
  echo "These are the types of diagnoses that $scriptName can make:"
  echo
  echo "  A) Some state saved in memory by the Gradle daemon is triggering an error"
  echo "  B) Your source files have been changed"
  echo "  C) Some file in the out/ dir is triggering an error"
  echo "     If this happens, $scriptName will identify which file(s) specifically"
  echo "  D) The build is nondeterministic and/or affected by timestamps"
  echo "  E) The build via gradlew actually passes"
  exit 1
}

gradleArgs="$*"
if [ "$gradleArgs" == "" ]; then
  usage
fi

scriptPath="$(cd $(dirname $0) && pwd)"
supportRoot="$(cd $scriptPath/../.. && pwd)"
checkoutRoot="$(cd $supportRoot/../.. && pwd)"
tempDir="/tmp/diagnose-build-failure"
if [ "${GRADLE_USER_HOME:-}" == "" ]; then
  GRADLE_USER_HOME="$(cd ~ && pwd)/.gradle"
fi

function echoAndDo() {
  echo $*
  eval $*
}

function checkStatusRepo() {
  repo status
}

function checkStatusGit() {
  git status
  git log -1
}

function checkStatus() {
  cd "$checkoutRoot"
  if [ "-e" .repo ]; then
    checkStatusRepo
  else
    checkStatusGit
  fi
}

function runBuild() {
  args="$*"
  cd "$supportRoot"
  if eval $args; then
    echo
    echo '`'$args'`' succeeded
    return 0
  else
    echo
    echo '`'$args'`' failed
    return 1
  fi
}

# makes the contents of $2 match the contents of $1
function copy() {
  from="$1"
  to="$2"
  rm "$to" -rf
  if [ -e "$from" ]; then
    mkdir -p "$(dirname $to)"
    cp --preserve=all -rT "$from" "$to"
  fi
}

function backupState() {
  backupDir="$1"
  echo "Saving state into $backupDir"
  copy "$checkoutRoot/out"             "$backupDir/out"
  copy "$supportRoot/.gradle"          "$backupDir/support/.gradle"
  copy "$supportRoot/buildSrc/.gradle" "$backupDir/buildSrc/.gradle"
  copy "$supportRoot/local.properties" "$backupDir/local.properties"
  copy "$GRADLE_USER_HOME"             "$backupDir/gradleUserHome"
}

function restoreState() {
  cd "$scriptPath"
  backupDir="$1"
  ./impl/restore-state.sh "$backupDir"
}

function clearState() {
  restoreState /dev/null
}


if runBuild ./gradlew $gradleArgs; then
  echo
  echo "This script failed to reproduce the build failure."
  echo "If the build failure you were observing was in Android Studio, then:"
  echo '  Were you launching Android Studio by running `./studiow`?'
  echo "  Try asking a team member why Android Studio is failing but gradlew is succeeding"
  echo "If you previously observed a build failure, then this means one of:"
  echo "  The state of your build is different than when you started your previous build"
  echo "    You could ask a team member if they've seen this error."
  echo "  The build is nondeterministic"
  echo "    If this seems likely to you, then please open a bug."
  exit 1
else
  echo
  echo "Reproduced build failure"
fi

echo
echo "Stopping the Gradle Daemon"
cd "$supportRoot"
./gradlew --stop || true

if runBuild ./gradlew --no-daemon $gradleArgs; then
  echo
  echo "The build passed when disabling the Gradle Daemon"
  echo "This suggests that there is some state saved in the Gradle Daemon that is causing a failure."
  echo "Unfortunately, this script does not know how to diagnose this further."
  echo "You could ask a team member if they've seen this error."
  exit 1
else
  echo
  echo "The build failed even with the Gradle Daemon disabled."
  echo "This may mean that there is state stored in a file somewhere, triggering the build to fail."
  echo "We will investigate the possibility of saved state next."
  echo
  backupState "$tempDir/prev"
fi

clearState
if runBuild ./gradlew --no-daemon $gradleArgs; then
  echo
  echo "The clean build passed, so we can now investigate what cached state is triggering this build to fail."
  # Technically, it's not guaranteed that the second build from this previously clean state will also pass,
  # but diff-filterer.py will check for that so we don't have to re-check it here.
  backupState "$tempDir/clean"
else
  echo
  echo "The clean build also failed."
  echo "This may mean that the build is failing for everyone"
  echo "This may mean that something about your checkout is different from others'"
  echo "Checking the status of your checkout:"
  checkStatus
  exit 1
fi

echo "Next we'll double-check that after restoring the failing state, that the build fails"
echo
restoreState "$tempDir/prev"
if runBuild ./gradlew --no-daemon $gradleArgs; then
  echo
  echo "After restoring the saved state, the build passed."
  echo "This might mean that there is additional state being saved somewhere else that this script does not know about"
  echo "This might mean that the success or failure status of the build is dependent on timestamps."
  echo "This might mean that the build is nondeterministic."
  echo "Unfortunately, this script does not know how to diagnose this further."
  echo "You could:"
  echo "  Ask a team member if they know where the state may be stored"
  echo "  Ask a team member if they recognize the build error"
  exit 1
else
  echo
  echo "After restoring the saved state, the build failed. This confirms that this script is successfully saving and restoring the relevant state"
fi

# Now ask diff-filterer.py to run a binary search to determine what the relevant differences are between "$tempDir/prev" and "$tempDir/clean"
echo
echo "Binary-searching the contents of the two output directories until the relevant differences are identified."
echo "This may take a while."
echo
echoAndDo "$supportRoot/development/file-utils/diff-filterer.py --assume-no-side-effects $tempDir/clean $tempDir/prev \"$scriptPath/impl/restore-state.sh . && cd $supportRoot && ./gradlew --no-daemon $gradleArgs\""

echo
echo "There should be something wrong with the above file state"
echo "Hopefully the output from diff-filterer.py above is enough information for you to figure out what is wrong"
echo "If not, you could ask a team member about your original error message and see if they have any ideas"
