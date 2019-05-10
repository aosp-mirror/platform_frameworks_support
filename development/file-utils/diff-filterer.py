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
    if not os.path.isfile(toPath):
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

  def groupByDirs(self, groupDirectFilesTogether = False):
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
        if groupDirectFilesTogether:
          firstDir = ""
        else:
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
  def splitDownToApproximatelySize(self, targetSize):
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
        descendents += child.splitDownToApproximatelySize(targetSize)
      else:
        descendents += [child]
    # Next, in case we found lots of tiny directories, recombine adjacent directories to make them approximately of size <size>
    results = []
    if targetSize < 1:
      targetSize = 1
    estimatedNumResults = self.size() / targetSize + 1
    for descendent in descendents:
      if len(results) < 1 or results[-1].size() + descendent.size() > targetSize:
        results.append(descendent)
      else:
        results[-1] = results[-1].expandedWithEmptyEntriesFor(descendent).withConflictsFrom(descendent)
    return results    

  def splitDepth(self, depth):
    if self.size() <= 1 or depth <= 0:
      return [self]
    groupDirectFilesTogether = (depth <= 1)
    children = self.groupByDirs(groupDirectFilesTogether)
    if len(children) == 1 and groupDirectFilesTogether:
      return children[0].groupByDirs(False)
    descendents = []
    for child in children:
      descendents += child.splitDepth(depth - 1)
    return descendents

  def splitOnce(self):
    return self.splitDepth(1)

  def summarize(self):
    numFiles = self.size()
    commonDir = self.getCommonDir()
    if numFiles <= 4:
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

class FilesState_HyperBoxNode(object):
  def __init__(self, dimensions):
    self.dimensions = dimensions
    self.children = []
    if len(dimensions) > 1:
      nextDimensions = dimensions[1:]
      for i in range(dimensions[0]):
        self.children.append(FilesState_HyperBoxNode(nextDimensions))
    else:
      for i in range(dimensions[0]):
         self.children.append(FilesState_LeafBox())

  def getFiles(self, coordinates):
    child = self.children[coordinates[0]]
    return child.getFiles(coordinates[1:])

  def setFiles(self, coordinates, files):
    self.children[coordinates[0]].setFiles(coordinates[1:], files)

  def clearFiles(self, coordinates):
    self.children[coordinates[0]].clearFiles(coordinates[1:])

  def removeSlice(self, dimension, index):
    if dimension == 0:
      del self.children[index]
    else:
      for child in self.children:
        child.removeSlice(dimension - 1, index)

  def getSlice(self, dimension, index):
    result = FilesState()
    for i in range(len(self.children)):
      if dimension != 0 or i == index:
        child = self.children[i]
        childResult = child.getSlice(dimension - 1, index)
        result = result.expandedWithEmptyEntriesFor(childResult).withConflictsFrom(childResult)
    return result

# Essentially a state machine that tests a FilesState_HyperBoxNode
class BoxRunner(object):
  def __init__(self, box):
    self.box = box
    self.dimension = 0
    self.index = box.getSize(0) - 1
    self.timingDimension = None

  def isDoingTimingAnalysis(self):
    return self.timingDimension is not None

  def getNextFiles(self):
    if not self.isDoingTimingAnalysis():
      return self.getFullSliceFiles()
    return self.getTimingFiles()

  def getFullSliceFiles(self):
    sliceState = self.box.getSlice(self.dimension, self.index)
    self.index += 1
    if self.index < 0:
      self.dimension += 1
      if self.dimension < self.box.getNumDimensions():
        self.index = self.box.getSize(self.dimension) - 1
      else:
        self.dimension = None
        self.timingDimension = 0
    return sliceState
    


class FilesState_LeafBox(object):
  def __init__(self):
    self.files = FilesState()

  def getFiles(self, coordinates):
    return self.files

  def setFiles(self, coordinates, files):
    self.files = files

  def clearFiles(self, coordinates):
    self.files = FilesState()

  def removeSlice(self, dimensions, index):
    return

  def getSlice(self, dimension, index):
    return self.getFiles([])

class FilesState_HyperBox(object):
  def __init__(self, dimensions):
    self.dimensions = dimensions
    self.durations = []
    self.numFiles = 0
    if len(dimensions) < 1:
      raise Exception("dimensions must be nonempty: " + str(dimensions))
    for length in dimensions:
      if length < 1:
        raise Exception("Illegal dimension " + str(length) + " in " + str(dimensions))
      self.durations.append([None] * length)
    self.root = FilesState_HyperBoxNode(dimensions)

  def getNumDimensions(self):
    return len(self.dimensions)

  def getSize(self, dimension):
    return self.dimensions[dimension]

  def getDimensions(self):
    return self.dimensions

  def getSliceDuration(self, dimension, index):
    return self.durations[dimension][index]

  def setSliceDuration(self, dimension, index, value):
    durations = self.durations[dimension]
    if index >= len(durations):
      raise Exception("Index " + str(index) + " too large for durations " + str(durations) + " of length " + str(len(durations)) + ". All durations: " + str(self.durations))
    durations[index] = value

  def removeSlice(self, dimension, index):
    durations = self.durations[dimension]
    del durations[index]
    self.root.removeSlice(dimension, index)
    self.dimensions[dimension] -= 1

  def getFastestIndex(self, dimension):
    durations = self.durations[dimension]
    fastestValue = None
    fastestIndex = None
    for i in range(len(durations)):
      value = durations[i]
      if value is not None:
        if fastestValue is None or value < fastestValue:
          fastestValue = value
          fastestIndex = i
    return fastestIndex

  def getFastestIndices(self):
    return [self.getFastestIndex(dimension) for dimension in range(self.getNumDimensions())]

  def getFiles(self, coordinates):
    #print("Box calling getFiles(" + str(coordinates) + ")")
    return self.root.getFiles(coordinates)

  def setFiles(self, dimensions, files):
    self.root.setFiles(dimensions, files)
    self.numFiles = None

  def clearFiles(self, dimensions):
    self.setFiles(dimensions, FilesState())

  def getNumFiles(self):
    if self.numFiles is None:
      numFiles = 0
      for child in self.getChildren():
        numFiles += child.size()
      self.numFiles = numFiles
    return self.numFiles

  def getSlice(self, dimension, index):
    return self.root.getSlice(dimension, index)

  def incrementCoordinates(self, coordinates):
    coordinates = coordinates[:]
    for i in range(len(coordinates)):
      coordinates[i] += 1
      if coordinates[i] >= self.dimensions[i]:
        coordinates[i] = 0
      else:
        return coordinates
    return None

  def getChildren(self):
    if len(self.dimensions) < 1 or self.dimensions[0] < 1:
      return []
    coordinates = [0] * len(self.dimensions)
    children = []
    while coordinates is not None:
      child = self.getFiles(coordinates)
      if child is not None and child.size() > 0:
        children.append(child)
      coordinates = self.incrementCoordinates(coordinates)
    return children

def boxFromList(fileStates):
  numStates = len(fileStates)
  if numStates == 1:
    dimensions = [1]
  else:
    dimensions = []
    while numStates > 1:
      if numStates == 4:
        # if there are 4 states we want to make it a 2x2
        nextDimension = 2
      else:
        nextDimension = min(3, numStates)
      dimensions.append(nextDimension)
      numStates = int(math.ceil(float(numStates) / float(nextDimension)))
  tree = FilesState_HyperBox(dimensions)
  coordinates = [0] * len(dimensions)
  for state in fileStates:
    tree.setFiles(coordinates, state)
    coordinates = tree.incrementCoordinates(coordinates)
  return tree

# runs a ShellScript in this process and write the result to <pipe> when done
def runScriptInSameProcess(shellScript, pipe):
  start = datetime.datetime.now()
  returnCode = shellScript.process()
  duration = datetime.datetime.now() - start
  pipe.write((identifier, returnCode, duration))

# runs a ShellScript in another process and returns a Connection that will store the results when done
def runScriptInOtherProcess(shellScript, identifier):
  parentConnection, childConnection = Pipe()
  process = Process(target=runScriptInSameProcess, args=(shellScript, childConnection, identifier,))
  process.start()
  return parentConnection

# Stores a subprocess for running tests and some information about which tests to run
class Job(object):
  def __init__(self, testScript, baseState, candidateBox, identifier):
    self.testScript = testScript
    self.resetTo_state = baseState
    # all of the files that we've found so far that we can add
    self.acceptedState = FilesState()
    # HyperBox of all of the possible changes we're considering
    self.candidateBox = candidateBox
    # FilesState telling the current set of files that we're testing modifying
    self.currentTestState = None
    self.busy = False
    self.complete = False
    self.pipe = None
    self.identifier = identifier

  # tells whether this job is actively using CPU
  # the job will periodically pause itself and need to be resumed, to make it easy to terminate it early if needed
  def isBusy(self):
    return self.pipe is not None

  def start(self):
    if self.isBusy():
      return None
    self.pipe = runScriptInOtherProcess(self.testScript, self.identifier)
    return self.pipe

  def madeProgress(self):
    return self.acceptedState.size() > 0

  # tells whether this job
  def isComplete(self):
    return self.complete

  def getAcceptedState(self):
    return self.acceptedState

  # specifies that this job should stop (once its children have stopped)
  def cancel(self):
    self.complete = True

  def onStatus(self, status):
    self.stepState(status)
    self.pipe = None

  def stepState(self, status)
    if status.returnCode == 0:
      self.acceptedState = self.acceptedState.expandedWithEmptyEntriesFor(self.currentTestState).withConflictsFrom(self.currentTestState)

  


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

  def test(self, testState, timeout = None):
    # reset state if needed
    if not self.assumeNoSideEffects:
      print("Resetting " + str(self.workPath))
      fileIo.removePath(self.workPath)
      self.full_resetTo_state.apply(self.workPath)
      testState.apply(self.workPath)
    else:
      self.resetTo_state.withConflictsFrom(testState).apply(self.workPath)
    start = datetime.datetime.now()
    returnCode = ShellScript(self.testScript_path).process(self.workPath)
    duration = (datetime.datetime.now() - start).total_seconds()
    print("shell command completed in " + str(duration))
    if returnCode == 0:
      # Success! Save these changes
      self.targetState = self.targetState.withoutDuplicatesFrom(testState)
      self.resetTo_state = self.targetState.withConflictsFrom(self.resetTo_state)
      self.full_resetTo_state = self.full_resetTo_state.expandedWithEmptyEntriesFor(testState).withConflictsFrom(testState).withoutEmptyEntries()
      testState.apply(self.bestState_path)
      return (True, duration)
    else:
      if self.assumeNoSideEffects:
        # unapply changes so that the contents of self.workPath should match self.resetTo_state
        testState.withConflictsFrom(self.resetTo_state).apply(self.workPath)
      return (False, duration)

  def run(self):
    start = datetime.datetime.now()
    numIterationsCompleted = 0
    if not self.assumeInputStatesAreCorrect:
      print("Testing that the given failing state actually fails")
      fileIo.removePath(self.workPath)
      fileIo.ensureDirExists(self.workPath)
      if self.test(self.originalFailingState)[0]:
        print("\nGiven failing state at " + self.originalFailingPath + " does not actually fail!")
        return False

      print("Testing that the given passing state actually passes")
      if self.assumeNoSideEffects:
        self.resetTo_state.apply(self.workPath)
      else:
        fileIo.removePath(self.workPath)
        fileIo.ensureDirExists(self.workPath)
      if not self.test(self.originalPassingState)[0]:
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
    candidateStates = [boxFromList(self.targetState.splitOnce())]
    numConsecutivelyFailingRows = 0
    while True:
      succeededDuringThisScan = False
      stateIndex = 0
      maxObservedStateIndex = 0
      while stateIndex < len(candidateStates):
        nextCandidateStates = []
        box = candidateStates[stateIndex]
        print("##############################################################################################################################################################################################")
        print("Checking candidateState index " + str(stateIndex) + " of " + str(len(candidateStates)) + ": ")
        if numConsecutivelyFailingRows >= self.resetTo_state.size():
          print("Checked all " + str(numConsecutivelyFailingRows) + " files with no successes. Done.")
          candidateStates = []
          nextCandidateStates = []
          break
        if len(candidateStates) == self.resetTo_state.size():
          print("At least " + str(self.resetTo_state.size() - numConsecutivelyFailingRows) + " iterations remaining")
        # test each slice
        succeededDuringThisBox = False
        for dimension in range(box.getNumDimensions()):
          for index in range(box.getSize(dimension) - 1, -1, -1):
            if box.getSize(dimension) == 1 and len(candidateStates) == 1:
              # We've narrowed the search space down to one row
              # Make a note that it could still be worth retesting this row after having removed some other entries from the box
              box.setSliceDuration(dimension, index, 0)
              # However, we know that this row must fail now, so skip re-testing it at the moment
              continue
            candidateState = box.getSlice(dimension, index)
            print("")
            print("Elapsed duration = " + str(datetime.datetime.now() - start) + ". " + str(self.resetTo_state.size()) + " changes left to test")
            print("Testing dimension " + str(dimension) + "/" + str(box.getNumDimensions()) + ", index " + str(index) + "/" + str(box.getSize(dimension)))
            if candidateState.size() < 1:
              print("Skipping slice of size 0")
              box.setSliceDuration(dimension, index, None)
              continue
            (testResults, duration) = self.test(candidateState)
            if testResults:
              print("Accepted slice: " + str(candidateState.summarize()))
              succeededDuringThisBox = True
              succeededDuringThisScan = True
              numConsecutivelyFailingRows = 0
              box.removeSlice(dimension, index)
            else:
              print("Rejected slice: " + str(candidateState.summarize()))
              box.setSliceDuration(dimension, index, duration)
              if dimension == 0:
                numConsecutivelyFailingRows += 1
        # make some guesses (based on duration) about which individual blocks are likely to contain failures, and run some tests without those failing blocks
        if box.getNumDimensions() >= 2:
          print("//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////")
          print("Doing a timing analysis of box " + str(stateIndex) + " of " + str(len(candidateStates)))
          loopMax = box.getNumDimensions()
          numSuccesses = 0
          numFailures = 0
          busy = True
          while busy:
            print("")
            print("Elapsed duration = " + str(datetime.datetime.now() - start) + ". " + str(self.resetTo_state.size()) + " changes left to test (current box dimensions: " + str(box.getDimensions()) + ")")
            if numFailures > numSuccesses + loopMax:
              print("Too many failures in a row. Continuing to next iteration")
              break
            # find the row and column that fail most quickly
            coordinates = []
            for dimension in range(box.getNumDimensions()):
              index = box.getFastestIndex(dimension)
              if index is None:
                print("Removed all slices in dimension " + str(dimension) + ". Continuing to next iteration")
                busy = False
                break
              coordinates.append(index)
              print("Previous failure duration was " + str(box.getSliceDuration(dimension, index)) + " for dimension " + str(dimension) + " and index " + str(index))
            if not busy:
              break
            blockState = box.getFiles(coordinates)
            print("Testing shortened box: clearing block at " + str(coordinates) + " : " + str(blockState.summarize()))
            box.clearFiles(coordinates)
            if blockState.size() < 1:
              print("Returned to a previously cleared block at (" + str(coordinates) + "). Ignoring it and continuing")
              for dimension in range(len(coordinates)):
                box.setSliceDuration(dimension, coordinates[dimension], None)
              continue
            nextCandidateStates.append(blockState)

            for dimension in range(box.getNumDimensions()):
              index = coordinates[dimension]
              sliceState = box.getSlice(dimension, index)
              print("Testing slice at dimension " + str(dimension) + ", index " + str(index))
              if sliceState.size() < 1:
                print("Skipping empty slice")
                box.setSliceDuration(dimension, index, None)
                continue
              (testResults, duration) = self.test(sliceState)
              if testResults:
                print("Accepted shortened slice: " + str(sliceState.summarize()))
                numSuccesses += 1
                succeededDuringThisScan = True
                box.removeSlice(dimension, index)
              else:
                print("Rejected shortened slice: " + str(sliceState.summarize()))
                numFailures += 1
                box.setSliceDuration(dimension, index, duration)

        if stateIndex >= maxObservedStateIndex:
          # If removing files from this box was successful, then removing other files from this box will probably be successful too
          #if succeededDuringThisBox:
          #  nextCandidateStates = box.getChildren() + nextCandidateStates
          #else:
          #  nextCandidateStates = nextCandidateStates + box.getChildren()
          nextCandidateStates += box.getChildren()
          #maxObservedStateIndex = stateIndex

        print("nextCandidateStates has " + str(len(nextCandidateStates)) + " states:")
        for state in nextCandidateStates:
          print("nextCandidateStates has state " + str(state.summarize()))
          box = boxFromList(state.splitOnce())
          candidateStates.append(box)
        smallCandidateStates = []
        largeCandidateStates = []
        for box in candidateStates[1:]:
          children = box.getChildren()
          if len(children) < 2:
            smallCandidateStates.append(box)
          else:
            largeCandidateStates.append(box)
            numConsecutivelyFailingRows = 0
        candidateStates = largeCandidateStates + smallCandidateStates

        #smallCandidateStates = []
        #largeCandidateStates = []
        #for state in nextCandidateStates:
        #  children = state.splitOnce()
        #  box = boxFromList(children)
        #  if len(children) < 2:
        #    smallCandidateStates.append(box)
        #  else:
        #    largeCandidateStates.append(box)
        #candidateStates = largeCandidateStates + candidateStates[1:] + smallCandidateStates
        

        #stateIndex += 1
      newBoxes = []
      nextCandidateStates = [block for block in nextCandidateStates if block.size() > 0]
      for block in nextCandidateStates:
        subBlocks = block.splitOnce()
        box = boxFromList(subBlocks)
        print("Split candidate " + str(block.summarize()) + " into box " + str(box.getDimensions()) + ". Contents: ")
        for block in subBlocks:
          print(block.summarize())
        newBoxes.append(box)
      print("Split " + str(len(nextCandidateStates)) + " blocks into " + str(len(newBoxes)) + " blocks:")
      targetNumBlocks = len(candidateStates) * 2
      if targetNumBlocks > self.resetTo_state.size() / 2:
        targetNumBlocks = self.resetTo_state.size()
      while len(newBoxes) < targetNumBlocks and len(newBoxes) < self.resetTo_state.size():
        print("Not enough blocks (" + str(len(newBoxes)) + " < " + str(targetNumBlocks) + "), splitting directories again")
        splitBlocks = []
        for box in newBoxes:
          for block in box.getChildren():
            splitBlocks.append(boxFromList(block.splitOnce()))
        newBoxes = splitBlocks
      newBoxes = sorted(newBoxes, reverse=True, key=FilesState_HyperBox.getNumFiles)
      if len(newBoxes) <= len(candidateStates) and not succeededDuringThisScan:
        break
      candidateStates = newBoxes
        
    print("double-checking results")
    fileIo.removePath(self.workPath)
    wasSuccessful = True
    if not self.test(filesStateFromTree(self.bestState_path))[0]:
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
