#!/usr/bin/python
#
#  Copyright (C) 2018 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#


import datetime, filecmp, math, os, shutil, subprocess, stat, sys
from collections import OrderedDict

def usage():
  print("""Usage: diff-filterer.py [--assume-no-side-effects] [--assume-input-states-are-correct] [--try-fail] [--work-path <workpath>] <passingPath> <failingPath> <shellCommand>

diff-filterer.py attempts to transform (a copy of) the contents of <passingPath> into the contents of <failingPath> subject to the constraint that when <shellCommand> is run in that directory, it returns 0

OPTIONS
  --assume-no-side-effects
    Assume that the given shell command does not make any (relevant) changes to the given directory, and therefore don't wipe and repopulate the directory before each invocation of the command
  --assume-input-states-are-correct
    Assume that <shellCommand> passes in <passingPath> and fails in <failingPath> rather than re-verifying this
  --try-fail
    Invert the success/fail status of <shellCommand> and swap <passingPath> and <failingPath>
    That is, instead of trying to transform <passingPath> into <failingPath>, try to transform <failingPath> into <passingPath>
  --work-path <filepath>
    File path to use as the work directory for testing the shell command
    This file path will be overwritten and modified as needed for testing purposes, and will also be the working directory of the shell command when it is run
""")
  sys.exit(1)

# Miscellaneous functions
def avg(x):
  if len(x) <= 0:
    return None
  return sum(x) / len(x)

# Miscellaneous file utilities
class FileIo(object):
  def __init__(self):
    return

  def ensureDirExists(self, filePath):
    if os.path.isfile(filePath):
      os.remove(filePath)
    if not os.path.isdir(filePath):
      os.makedirs(filePath)

  def copyFile(self, fromPath, toPath):
    self.ensureDirExists(os.path.dirname(toPath))
    self.removePath(toPath)
    shutil.copy2(fromPath, toPath)

  def writeFile(self, path, text):
    f = open(path, "w+")
    f.write(text)
    f.close()

  def writeScript(self, path, text):
    self.writeFile(path, text)
    os.chmod(path, 0755)

  def removePath(self, filePath):
    if len(os.path.split(filePath)) < 2:
      raise Exception("Will not remove path at " + filePath + "; is too close to the root of the filesystem")
    if os.path.isdir(filePath):
      shutil.rmtree(filePath)
    elif os.path.isfile(filePath):
      os.remove(filePath)

  def join(self, path1, path2):
    return os.path.normpath(os.path.join(path1, path2))

  # tells whether <parent> either contains <child> or is <child>
  def contains(self, parent, child):
    if parent == child:
      return True
    return child.startswith(parent + "/")

  # returns the common prefix of two paths. For example, commonPrefixOf2("a/b/c", "a/b/cat") returns "a/b"
  def commonPrefixOf2(self, path1, path2):
    prefix = path2
    while True:
      #print("commonPrefixOf2 path1 = " + str(path1) + " prefix = " + str(prefix))
      if self.contains(prefix, path1):
        return prefix
      parent = os.path.dirname(prefix)
      if parent == prefix:
        return None
      prefix = parent

  # returns the common prefix of multiple paths
  def commonPrefix(self, paths):
    if len(paths) < 1:
      return None
    result = paths[0]
    for path in paths:
      prev = result
      result = self.commonPrefixOf2(result, path)
      #print("commonPrefixOf2 of (" + str(prev) + ", " + str(path) + ") is " + str(result))
      if result is None:
        return result
    return result



fileIo = FileIo()

# Runs a shell command
class ShellScript(object):
  def __init__(self, commandText):
    self.commandText = commandText

  def process(self, cwd):
    print("Running '" + self.commandText + "' in " + cwd)
    try:
      subprocess.check_call(["bash", "-c", "cd " + cwd + " && " + self.commandText])
      return 0
    except subprocess.CalledProcessError as e:
      return e.returncode

# Base class that can hold the state of a file
class FileContent(object):
  def apply(self, filePath):
    pass

  def equals(self, other):
    pass

# A FileContent that refers to the content of a specific file
class FileBacked_FileContent(FileContent):
  def __init__(self, referencePath):
    super(FileBacked_FileContent, self).__init__()
    self.referencePath = referencePath

  def apply(self, filePath):
    fileIo.copyFile(self.referencePath, filePath)

  def equals(self, other):
    if not isinstance(other, FileBacked_FileContent):
      return False
    if self.referencePath == other.referencePath:
      return True
    return filecmp.cmp(self.referencePath, other.referencePath)

  def __str__(self):
    return self.referencePath

# A FileContent describing the nonexistence of a file
class MissingFile_FileContent(FileContent):
  def __init__(self):
    super(MissingFile_FileContent, self).__init__()

  def apply(self, filePath):
    fileIo.removePath(filePath)

  def equals(self, other):
    return isinstance(other, MissingFile_FileContent)

  def __str__(self):
    return "Empty"

# A FileContent describing a directory
class Directory_FileContent(FileContent):
  def __init__(self):
    super(Directory_FileContent, self).__init__()

  def apply(self, filePath):
    fileIo.ensureDirExists(filePath)

  def equals(self, other):
    return isinstance(other, Directory_FileContent)

  def __str__(self):
    return "[empty dir]"

# A collection of many FileContent objects
class FilesState(object):
  def __init__(self):
    self.fileStates = OrderedDict()

  def apply(self, filePath):
    for relPath, state in self.fileStates.iteritems():
      state.apply(fileIo.join(filePath, relPath))

  def add(self, filePath, fileContent):
    self.fileStates[filePath] = fileContent

  def getContent(self, filePath):
    if filePath in self.fileStates:
      return self.fileStates[filePath]
    return None

  def containsAt(self, filePath, content):
    ourContent = self.getContent(filePath)
    if ourContent is None or content is None:
      return ourContent == content
    return ourContent.equals(content)

  # returns a FilesState resembling <self> but without the keys for which other[key] == self[key]
  def withoutDuplicatesFrom(self, other):
    result = FilesState()
    for filePath, fileState in self.fileStates.iteritems():
      if not fileState.equals(other.getContent(filePath)):
        result.add(filePath, fileState)
    return result

  # returns self[fromIndex:toIndex]
  def slice(self, fromIndex, toIndex):
    result = FilesState()
    for filePath in self.fileStates.keys()[fromIndex:toIndex]:
      result.fileStates[filePath] = self.fileStates[filePath]
    return result

  # returns a FilesState having the same keys as this FilesState, but with values taken from <other> when it has them, and <self> otherwise
  def withConflictsFrom(self, other):
    result = FilesState()
    for filePath, fileContent in self.fileStates.iteritems():
      if filePath in other.fileStates:
        result.add(filePath, other.fileStates[filePath])
      else:
        result.add(filePath, fileContent)
    return result

  # returns a set of paths to all of the dirs in <self> that are implied by any files in <self>
  def listImpliedDirs(self):
    dirs = set()
    keys = self.fileStates.keys()[:]
    i = 0
    while i < len(keys):
      path = keys[i]
      parent, child = os.path.split(path)
      if parent == "":
        parent = "."
      if not parent in dirs:
        dirs.add(parent)
        keys.append(parent)
      i += 1
    return dirs

  # returns a FilesState having all of the entries from <self>, plus empty entries for any keys in <other> not in <self>
  def expandedWithEmptyEntriesFor(self, other):
    impliedDirs = self.listImpliedDirs()
    # now look for entries in <other> not present in <self>
    result = self.clone()
    for filePath in other.fileStates:
      if filePath not in result.fileStates and filePath not in impliedDirs:
        result.fileStates[filePath] = MissingFile_FileContent()
    return result

  def clone(self):
    result = FilesState()
    for path, content in self.fileStates.iteritems():
      result.add(path, content)
    return result

  def withoutEmptyEntries(self):
    result = FilesState()
    empty = MissingFile_FileContent()
    for path, state in self.fileStates.iteritems():
      if not empty.equals(state):
        result.add(path, state)
    return result

  def getCommonDir(self):
    result = fileIo.commonPrefix(self.fileStates.keys())
    #print("getCommonDir of " + str(self.fileStates.keys()) + " is " + str(result))
    return result

  def groupByDirs(self):
    if len(self.fileStates) <= 1:
      if len(self.fileStates) == 1:
        return [self]
      return []

    commonDir = self.getCommonDir()
    #print("Common dir of " + str(self) + " is " + str(commonDir))
    if commonDir is None:
      prefixLength = 0
    else:
      prefixLength = len(commonDir) + 1 # skip the following '/'
    groupsByDir = {}

    for filePath, fileContent in self.fileStates.iteritems():
      subPath = filePath[prefixLength:]
      slashIndex = subPath.find("/")
      if slashIndex < 0:
        firstDir = subPath
      else:
        firstDir = subPath[:slashIndex]
      #print("FilesState considering creating substate. commonDir = " + str(commonDir) + ", prefixLength = " + str(prefixLength) + ", filePath = " + filePath + ", component = " + firstDir)
      if not firstDir in groupsByDir:
        #print("FilesState creating substate. commonDir = " + str(commonDir) + ", prefixLength = " + str(prefixLength) + ", filePath = " + filePath + ", component = " + firstDir)
        groupsByDir[firstDir] = FilesState()
      groupsByDir[firstDir].add(filePath, fileContent)
    return [group for group in groupsByDir.values()]

  # Returns a list of FilesState, each roughly of size <size>, that collectively have the same set of files as <self>.
  # Will try to keep directories together
  def splitDownToSize(self, targetSize):
    # First, find directories that are small enough such that each one is of size <size> or less
    #print("Splitting state (at " + str(self.getCommonDir()) + ", " + str(self.size()) + " entries)  down to size " + str(size))
    if self.size() <= 1 or self.size() <= targetSize:
      return [self]
    children = self.groupByDirs()
    if len(children) == 1:
      print("Error: grouped state of size 1 into 1 child. Self = " + str(self) + ", children = " + str(children) + ", commonDir = " + str(self.getCommonDir()))
      #sys.exit(1)
    #print("Grouped " + str(self) + " into " + str(len(children)) + " children")
    descendents = []
    for child in children:
      if child.size() > targetSize * 1.5:
        descendents += child.splitDownToSize(targetSize)
      else:
        descendents += [child]
    # Next, in case we found lots of tiny directories, recombine adjacent directories to make them approximately of size <size>
    results = []
    if targetSize < 1:
      targetSize = 1
    estimatedNumResults = self.size() / targetSize + 1
    descendents = sorted(descendents, key=FilesState.size, reverse=True)
    for descendent in descendents:
      added = False
      if len(results) >= estimatedNumResults:
        smallestObservedSize = min([result.size() for result in results])
        for i in range(len(results) - 1, -1, -1):
          if results[i].size() == smallestObservedSize:
            #if results[i].size() + descendent.size() <= targetSize:
            added = True
            results[i] = results[i].expandedWithEmptyEntriesFor(descendent).withConflictsFrom(descendent)
            break
      if not added:
        results.append(descendent)
    return results    

  def summarize(self):
    numFiles = self.size()
    commonDir = self.getCommonDir()
    if numFiles <= 3:
      return str(self)
    if commonDir is not None:
      return str(numFiles) + " files under " + str(commonDir)
    return str(numFiles) + " files"

  def size(self):
    return len(self.fileStates)

  def __str__(self):
    if len(self.fileStates) == 0:
      return "[empty fileState]"
    entries = []
    for filePath, state in self.fileStates.iteritems():
      entries.append(filePath + " -> " + str(state))
    if len(self.fileStates) > 1:
      prefix = str(len(entries)) + " entries:\n"
    else:
      prefix = "1 entry: "
    return prefix + "\n".join(entries)

# Creates a FilesState matching the state of a directory on disk
def filesStateFromTree(rootPath):
  rootPath = os.path.abspath(rootPath)

  paths = []
  states = {}

  for root, dirPaths, filePaths in os.walk(rootPath):
    if len(filePaths) == 0 and len(dirPaths) == 0:
      relPath = os.path.relpath(root, rootPath)
      paths.append(relPath)
      states[relPath] = Directory_FileContent()
    for filePath in filePaths:
      fullPath = fileIo.join(root, filePath)
      relPath = os.path.relpath(fullPath, rootPath)
      paths.append(relPath)
      states[relPath] = FileBacked_FileContent(fullPath)

  paths = sorted(paths)
  state = FilesState()
  for path in paths:
    state.add(path, states[path])
  return state

# Runner class that determines which diffs between two directories cause the given shell command to fail
class DiffRunner(object):
  def __init__(self, failingPath, passingPath, shellCommand, tempPath, workPath, assumeNoSideEffects, assumeInputStatesAreCorrect, tryFail):
    # some simple params
    if workPath is None:
      self.workPath = fileIo.join(tempPath, "work")
    else:
      self.workPath = os.path.abspath(workPath)
    self.bestState_path = fileIo.join(tempPath, "bestResults")
    self.testScript_path = fileIo.join(tempPath, "test.sh")
    fileIo.writeScript(self.testScript_path, shellCommand)
    self.originalPassingPath = os.path.abspath(passingPath)
    self.originalFailingPath = os.path.abspath(failingPath)
    self.assumeNoSideEffects = assumeNoSideEffects
    self.assumeInputStatesAreCorrect = assumeInputStatesAreCorrect
    self.tryFail = tryFail

    # lists of all the files under the two dirs
    print("Finding files in " + passingPath)
    self.originalPassingState = filesStateFromTree(passingPath)
    print("Finding files in " + failingPath)
    self.originalFailingState = filesStateFromTree(failingPath)

    print("Identifying duplicates")
    # list of the files in the state to reset to after each test
    self.full_resetTo_state = self.originalPassingState
    # minimal description of only the files that are supposed to need to be reset after each test
    self.resetTo_state = self.originalPassingState.expandedWithEmptyEntriesFor(self.originalFailingState).withoutDuplicatesFrom(self.originalFailingState)
    self.originalNumDifferences = self.resetTo_state.size()
    print("Processing " + str(self.originalNumDifferences) + " file differences")
    # state we're trying to reach
    self.targetState = self.resetTo_state.withConflictsFrom(self.originalFailingState.expandedWithEmptyEntriesFor(self.resetTo_state))
    self.windowSize = self.resetTo_state.size()

  def test(self, filesState, timeout = None):
    if timeout is not None:
      shellCommand = "timeout -s SIGHUP " + str(timeout) + " " + self.testScript_path
    else:
      shellCommand = self.testScript_path
    
    #print("Applying state: " + str(filesState) + " to " + self.workPath)
    filesState.apply(self.workPath)
    start = datetime.datetime.now()
    returnCode = ShellScript(shellCommand).process(self.workPath)
    duration = (datetime.datetime.now() - start).total_seconds()
    print("shell command completed in " + str(duration))
    if returnCode == 124:
      return "timeout"
    else:
      if returnCode == 0:
        return "success"
      else:
        return "failure"

  def run(self):
    start = datetime.datetime.now()
    numIterationsCompleted = 0
    if not self.assumeInputStatesAreCorrect:
      print("Testing that the given failing state actually fails")
      fileIo.removePath(self.workPath)
      fileIo.ensureDirExists(self.workPath)
      if self.test(self.originalFailingState) != "failure":
        print("\nGiven failing state at " + self.originalFailingPath + " does not actually fail!")
        return False

      print("Testing that the given passing state actually passes")
      if self.assumeNoSideEffects:
        self.resetTo_state.apply(self.workPath)
      else:
        fileIo.removePath(self.workPath)
        fileIo.ensureDirExists(self.workPath)
      if self.test(self.originalPassingState) != "success":
        print("\nGiven passing state at " + self.originalPassingPath + " does not actually pass!")
        return False
    else:
      fileIo.removePath(self.workPath)
      fileIo.ensureDirExists(self.workPath)
      self.originalPassingState.apply(self.workPath)

    print("Saving best state found so far")
    fileIo.removePath(self.bestState_path)
    self.originalPassingState.apply(self.bestState_path)

    print("Starting")
    print("(You can inspect " + self.bestState_path + " while this process runs, to observe the best state discovered so far)")
    print("")
    numFailuresDuringCurrentWindowSize = 0
    # We essentially do a breadth-first search over the inodes (files or dirs) in the tree
    # Every time we encounter an inode, we try replacing it (and any descendents if it has any) and seeing if that passes our given test
    candidateStates = [[self.targetState]]
    while True:
      # scan the state until reaching the end
      succeededDuringThisScan = False
      # if we encounter only successes for this window size, then check all windows except the last (because if all other windows pass, then the last must fail)
      # if we encounter any failure for this window size, then check all windows
      numFailuresDuringPreviousWindowSize = numFailuresDuringCurrentWindowSize
      numFailuresDuringCurrentWindowSize = 0
      displayIndex = 0
      displayMax = sum([len(stateGroup) for stateGroup in candidateStates])
      #averageWindowSize = self.resetTo_state.size() / displayMax
      for j in range(len(candidateStates) - 1, -1, -1):
        stateGroup = candidateStates[j]
        print("Testing window group having " + str(len(stateGroup)) + " windows")
        if len(stateGroup) > 1:
          testLastStateInGroup = False
        else:
          testLastStateInGroup = True
        for i in range(len(stateGroup) - 1, -1, -1):
          displayIndex += 1
          print("")
          if i == 0 and not testLastStateInGroup:
            # All of the other tests in this window group passed
            # Before we created this window group, we had previously tested all of the changes in this window group at once, and that failed
            # So, we suspect that reapplying the rest of the changes in this window group won't cause the test to pass
            # So, we skip retesting these changes, unless we're in the double-checking phase
            print("Skipping window #" + str(displayIndex) + " because previous windows already passed")
            break
          currentWindow = stateGroup[i]
          print("Testing window #" + str(displayIndex) + " of " + str(displayMax) + ": " + str(currentWindow.size()) + " changes (" + str(self.resetTo_state.size()) + " total changes remaining)")

          # determine which changes to test
          testState = self.resetTo_state.withConflictsFrom(currentWindow)
          # reset state if needed
          if not self.assumeNoSideEffects:
            print("Resetting " + str(self.workPath))
            fileIo.removePath(self.workPath)
            self.full_resetTo_state.apply(self.workPath)
  
          # estimate time remaining
          currentTime = datetime.datetime.now()
          elapsedDuration = currentTime - start
          estimatedNumInvalidFiles = numFailuresDuringPreviousWindowSize
          if estimatedNumInvalidFiles < 1:
            estimatedNumInvalidFiles = 1
          estimatedNumValidFilesPerInvalidFile = self.resetTo_state.size() / estimatedNumInvalidFiles
          # In each iteration we generally split each window into pieces of size no more than one half of its current size
          # So in practice we generally split each window into 3 pieces
          estimatedNumWindowShrinkages = math.log(max(estimatedNumValidFilesPerInvalidFile, 1), 3)
          estimatedNumTestsPerWindow = estimatedNumWindowShrinkages * 3 + 1
          estimatedNumIterationsRemaining = estimatedNumTestsPerWindow * estimatedNumInvalidFiles
          numIterationsCompleted += 1
          estimatedRemainingDuration = datetime.timedelta(seconds=(elapsedDuration.total_seconds() * estimatedNumIterationsRemaining / numIterationsCompleted))
          print("Estimated remaining duration = " + str(estimatedRemainingDuration) + " and remaining num iterations = "  + str(estimatedNumIterationsRemaining) + " (elapsed duration = " + str(elapsedDuration) + ")")

          # test the state
          testResults = self.test(testState)
          if testResults == "success":
            print("Accepted updated state: " + str(currentWindow.summarize()))
            # success! keep these changes
            testState.apply(self.bestState_path)
            self.full_resetTo_state = self.full_resetTo_state.expandedWithEmptyEntriesFor(testState).withConflictsFrom(testState).withoutEmptyEntries()
            # remove these changes from the set of changes to reconsider
            self.targetState = self.targetState.withoutDuplicatesFrom(testState)
            del stateGroup[i]
            self.resetTo_state = self.targetState.withConflictsFrom(self.resetTo_state)
            succeededDuringThisScan = True
          elif testResults == "failure":
            print("Rejected updated state: " + currentWindow.summarize())
            testLastStateInGroup = True
            numFailuresDuringCurrentWindowSize += 1
          else:
            raise Exception("Internal error: unrecognized test status " + str(testResults))
      # we checked every file once; now descend deeper into each directory and repeat
      newCandidateStates = []
      numWindows = sum([len(stateGroup) for stateGroup in candidateStates])
      targetNumWindows = numWindows * 2
      #targetAverageWindowSize = (self.resetTo_state.size() - 1) / targetNumWindows + 1
      targetAverageWindowSize = self.resetTo_state.size() / targetNumWindows
      #averageWindowSize = self.resetTo_state.size() / len(candidateStates)
      print("############################################################################################################################################")
      splitDuringThisScan = False
      for windowGroup in candidateStates:
        for window in windowGroup:
          # If a specific window is large, we want to shrink it more quickly so it will catch up to the sizes of the other windows
          # (it would be really bad if most windows have reached size 1 but there's one straggler window with more size,
          # because then we'd be re-testing most of these size 1 windows every time we shrink the remaining window)
          # If a specific window is small, we don't need to shrink it as quickly
          #thisRawTargetSize = min(targetAverageWindowSize, (window.size() + 1) / 2)
          #if thisRawTargetSize < 1:
          #  thisRawTargetSize = 1
          #numSubwindows = int(float(window.size()) / float(thisRawTargetSize) + 0.5)
          #numSubwindows = int(float(window.size()) / float(targetAverageWindowSize) + 0.5)
          #thisTargetSize = window.size() / numSubwindows
          #thisTargetSize = min(targetAverageWindowSize, (window.size() + 1) / 2)
          thisTargetSize = min(targetAverageWindowSize, window.size() / 2)
          #thisTargetSize = targetAverageWindowSize
          #if window.size() > averageWindowSize / 2:
          #  thisTargetSize = int(window.size() / 2.2)
          #else:
          #  thisTargetSize = window.size()
          #print("Trying to split window of size " + str(window.size()) + " into windows of size " + str(thisRawTargetSize) + " but renormalized target size to " + str(thisTargetSize))
          print("Trying to split window of size " + str(window.size()) + " into windows of size " + str(thisTargetSize))
          #currentSplit = window.splitDownToSize(thisTargetSize * 2 / 3, thisTargetSize * 4 / 3)
          currentSplit = window.splitDownToSize(thisTargetSize)
          
          print("Split window: " + window.summarize() + " into " + str(len(currentSplit)) + " sub windows:")
          for subWindow in currentSplit:
            print(str(subWindow.size()))
          if len(currentSplit) > 1:
            splitDuringThisScan = True
          newCandidateStates += [currentSplit]
      #print("splitDuringThisScan = " + str(splitDuringThisScan))
      if not splitDuringThisScan:
        #print("succeededDuringThisScan = " + str(succeededDuringThisScan))
        if not succeededDuringThisScan:
          # only stop if we confirmed that no files could be reverted (if one file was reverted, it's possible that that unblocks another file)
          break
      print("Split " + str(numWindows) + " window into " + str(sum([len(stateGroup) for stateGroup in newCandidateStates])))
      #for state in newCandidateStates:
      #  print(state.getCommonDir())
      print("############################################################################################################################################")
      candidateStates = newCandidateStates


    print("double-checking results")
    fileIo.removePath(self.workPath)
    wasSuccessful = True
    if not self.test(filesStateFromTree(self.bestState_path)):
      message = "Error: expected best state at " + self.bestState_path + " did not pass the second time. Could the test be non-deterministic?"
      if self.assumeNoSideEffects:
        message += " (it may help to remove the --assume-no-side-effects flag)"
      if self.assumeInputStatesAreCorrect:
        message += " (it may help to remove the --assume-input-states-are-correct flag)"
      print(message)
      wasSuccessful = False

    print("")
    print("Done trying to transform the contents of passing path:\n " + self.originalPassingPath + "\ninto the contents of failing path:\n " + self.originalFailingPath)
    print("Of " + str(self.originalNumDifferences) + " differences, could not accept: " + str(self.targetState))
    print("The final accepted state can be seen at " + self.bestState_path)
    return wasSuccessful

def main(args):
  assumeNoSideEffects = False
  assumeInputStatesAreCorrect = False
  tryFail = False
  workPath = None
  while len(args) > 0:
    arg = args[0]
    if arg == "--assume-no-side-effects":
      assumeNoSideEffects = True
      args = args[1:]
      continue
    if arg == "--assume-input-states-are-correct":
      assumeInputStatesAreCorrect = True
      args = args[1:]
      continue
    if arg == "--try-fail":
      tryFail = True
      args = args[1:]
      continue
    if arg == "--work-path":
      if len(args) < 2:
        usage()
      workPath = args[1]
      args = args[2:]
      continue
    if len(arg) > 0 and arg[0] == "-":
      print("Unrecognized argument: '" + arg + "'")
      usage()
    break
  if len(args) != 3:
    usage()
  passingPath = args[0]
  failingPath = args[1]
  shellCommand = args[2]
  tempPath = "/tmp/diff-filterer"
  startTime = datetime.datetime.now()
  if tryFail:
    temp = passingPath
    passingPath = failingPath
    failingPath = temp
  success = DiffRunner(failingPath, passingPath, shellCommand, tempPath, workPath, assumeNoSideEffects, assumeInputStatesAreCorrect, tryFail).run()
  endTime = datetime.datetime.now()
  duration = endTime - startTime
  if success:
    print("Succeeded in " + str(duration))
  else:
    print("Failed in " + str(duration))
    sys.exit(1)

main(sys.argv[1:])
