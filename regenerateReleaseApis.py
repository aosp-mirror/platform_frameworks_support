#!/usr/bin/python
import subprocess
import os
import shutil

def runShell(text):
  print("Running '" + text + "'")
  try:
    subprocess.check_call(["bash", "-c", text])
    return 0
  except subprocess.CalledProcessError as e:
    return e.returncode

def runShellAndGetOutput(text):
  print("Running '" + text + "'")
  return subprocess.check_output(["bash", "-c", text + " 2>&1 || true"])


def findMissingApiFilePaths():
  print("Checking for missing api file paths:")
  missingApiFilePaths = []
  output = runShellAndGetOutput("./gradlew checkApi")
  lines = output.split("\n")
  for line in lines:
    marker = " is not a file"
    if line.endswith(marker):
      prefix = line[:-len(marker)]
      if prefix[0] == "/":
        missingApiFilePaths.append(prefix)
      else:
        print(output)
        raise Exception("Could not parse line '" + line + "'")
  print("Missing api file paths: " + str(missingApiFilePaths))
  return missingApiFilePaths

def getSourceDirForApiFilepath(apiFilePath):
  if not "/api/" in apiFilePath:
    raise Exception("Illegal api filePath: " + apiFilePath)
  apiDir = os.path.dirname(apiFilePath)
  projectDir = os.path.dirname(apiDir)
  sourceDir = os.path.join(projectDir, "src")
  return sourceDir

def checkoutOldCode(apiFilePath):
  sourceDir = getSourceDirForApiFilepath(apiFilePath)
  refCommit = "3478ef7cbc6b27d8c1497d76c3ffff688771380e"
  runShell("git checkout " + refCommit + " -- " + sourceDir)

def checkoutNewCode(apiFilePath):
  sourceDir = getSourceDirForApiFilepath(apiFilePath)
  runShell("git reset")
  runShell("git checkout HEAD -- " + sourceDir)
  runShell("cd " + sourceDir + " && git clean -fd")

def regenerateOldApiFilePathsInParallel(apiFilePaths):
  for apiFilePath in apiFilePaths:
    checkoutOldCode(apiFilePath)
  runShell("./gradlew updateApi --continue || true")
  for desiredApiFilePath in apiFilePaths:
    apiDir = os.path.dirname(desiredApiFilePath)
    createdApiFilePath = os.path.join(apiDir, "system_current.txt")
    shutil.copyfile(createdApiFilePath, desiredApiFilePath)
  for apiFilePath in apiFilePaths:
    checkoutNewCode(apiFilePath)

def regenerateOldApiFilePathsInSeries(apiFilePaths):
  for path in apiFilePaths:
    regenerateOldApiFilePathsInParallel([path])

def updateOldApiFilesRepeatedly():
  even = True
  while True:
    missingApiFilePaths = findMissingApiFilePaths()
    if len(missingApiFilePaths) < 1:
      return
    if even:
      regenerateOldApiFilePathsInParallel(missingApiFilePaths)
    else:
      regenerateOldApiFilePathsInSeries(missingApiFilePaths)
    even = not even

def main():
  updateOldApiFilesRepeatedly()
  runShell("./gradlew updateApi")
  print("Done")

main()
