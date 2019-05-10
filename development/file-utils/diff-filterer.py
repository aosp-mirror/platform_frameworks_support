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


import datetime, filecmp, math, multiprocessing, os, shutil, subprocess, stat, sys
from collections import OrderedDict

def usage():
  print("""Usage: diff-filterer.py [--assume-no-side-effects] [--assume-input-states-are-correct] [--try-fail] [--work-path <workpath>] [--num-jobs <count>] <passingPath> <failingPath> <shellCommand>

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
  --num-jobs <count>
    The maximum number of concurrent executions of <shellCommand> to spawn at once
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
      if result is None:
        return result
    return result

fileIo = FileIo()

# Runs a shell command
class ShellScript(object):
  def __init__(self, commandText, cwd):
    self.commandText = commandText
    self.cwd = cwd

  def process(self):
    cwd = self.cwd
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

  def hasContentAt(self, filePath, content):
    ourContent = self.getContent(filePath)
    if ourContent is None:
      return (content is None)
    return ourContent.equals(content)

  # returns a FilesState resembling <self> but without the keys for which other[key] == self[key]
  def withoutDuplicatesFrom(self, other):
    result = FilesState()
    for filePath, fileState in self.fileStates.iteritems():
      otherContent = other.getContent(filePath)
      if not fileState.equals(otherContent):
        result.add(filePath, fileState)
    return result

  # returns self[fromIndex:toIndex]
  def slice(self, fromIndex, toIndex):
    result = FilesState()
    for filePath in self.fileStates.keys()[fromIndex:toIndex]:
      result.fileStates[filePath] = self.fileStates[filePath]
    return result

  # returns a FilesState having the same keys as this FilesState, but with values taken from <other> when it has them, and <self> otherwise
  def withConflictsFrom(self, other, listEmptyDirs = False):
    result = FilesState()
    for filePath, fileContent in self.fileStates.iteritems():
      if filePath in other.fileStates:
        result.add(filePath, other.fileStates[filePath])
      else:
        result.add(filePath, fileContent)
    if listEmptyDirs:
      oldImpliedDirs = self.listImpliedDirs()
      newImpliedDirs = result.listImpliedDirs()
      for impliedDir in oldImpliedDirs:
        if impliedDir not in newImpliedDirs and impliedDir not in result.fileStates:
          result.add(impliedDir, MissingFile_FileContent())
    return result

  # returns a set of paths to all of the dirs in <self> that are implied by any files in <self>
  def listImpliedDirs(self):
    dirs = set()
    empty = MissingFile_FileContent()
    keys = [key for (key, value) in self.fileStates.iteritems() if not empty.equals(value)]
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
    return result

  # Returns a list of FilesState objects each containing a different subdirectory of <self>
  # If groupDirectFilesTogether == True, then all files directly under self.getCommonDir() will be assigned to the same group
  def groupByDirs(self, groupDirectFilesTogether = False):
    if len(self.fileStates) <= 1:
      if len(self.fileStates) == 1:
        return [self]
      return []

    commonDir = self.getCommonDir()
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
      if not firstDir in groupsByDir:
        groupsByDir[firstDir] = FilesState()
      groupsByDir[firstDir].add(filePath, fileContent)
    return [group for group in groupsByDir.values()]

  # splits into multiple, smaller, FilesState objects
  def splitOnce(self):
    if self.size() <= 1:
      return [self]
    children = self.groupByDirs(True)
    if len(children) == 1:
      return children[0].groupByDirs(False)
    return children

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

  def getNumChildren(self):
    return len(self.getChildren())

  def getAllFiles(self):
    files = FilesState()
    for child in self.getChildren():
      files = files.expandedWithEmptyEntriesFor(child).withConflictsFrom(child)
    return files

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

# runs a Job in this process
def runJobInSameProcess(shellCommand, workPath, baseState, full_resetTo_state, assumeNoSideEffects, candidateBox, twoWayPipe):
  job = Job(shellCommand, workPath, baseState, full_resetTo_state, assumeNoSideEffects, candidateBox, twoWayPipe)
  job.runAndReport()

# starts a Job in a new process
def runJobInOtherProcess(shellCommand, workPath, baseState, full_resetTo_state, assumeNoSideEffects, candidateBox, queue, identifier):
  parentWriter, childReader = multiprocessing.Pipe()
  childInfo = TwoWayPipe(childReader, queue, identifier)
  process = multiprocessing.Process(target=runJobInSameProcess, args=(shellCommand, workPath, baseState, full_resetTo_state, assumeNoSideEffects, candidateBox, childInfo,))
  process.start()
  return parentWriter

class TwoWayPipe(object):
  def __init__(self, readerConnection, writerQueue, identifier):
    self.readerConnection = readerConnection
    self.writerQueue = writerQueue
    self.identifier = identifier

# Stores a subprocess for running tests and some information about which tests to run
class Job(object):
  def __init__(self, shellCommand, workPath, baseState, full_resetTo_state, assumeNoSideEffects, candidateBox, twoWayPipe):
    self.shellCommand = shellCommand
    self.workPath = workPath
    self.resetTo_state = baseState
    self.full_resetTo_state = full_resetTo_state
    self.assumeNoSideEffects = assumeNoSideEffects
    # all of the files that we've found so far that we can add
    self.acceptedState = FilesState()
    # HyperBox of all of the possible changes we're considering
    self.candidateBox = candidateBox
    # FilesState telling the current set of files that we're testing modifying
    self.currentTestState = None
    self.busy = False
    self.complete = False
    self.pipe = twoWayPipe

  def onSuccess(self, acceptedState):
    self.acceptedState = self.acceptedState.expandedWithEmptyEntriesFor(acceptedState).withConflictsFrom(acceptedState)

  def runAndReport(self):
    try:
      self.run()
    finally:
      print("Child " + str(self.pipe.identifier) + " completed")
      print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^")
      self.pipe.writerQueue.put((self.pipe.identifier, self.acceptedState))

  def jobTest(self, testState, timeout = None):
    # reset state if needed
    if not self.assumeNoSideEffects:
      print("Resetting " + str(self.workPath))
      fileIo.removePath(self.workPath)
      self.full_resetTo_state.apply(self.workPath)
      testState.apply(self.workPath)
    else:
      delta = self.resetTo_state.withConflictsFrom(testState, True)
      delta.apply(self.workPath)

    start = datetime.datetime.now()
    returnCode = ShellScript(self.shellCommand, self.workPath).process()
    duration = (datetime.datetime.now() - start).total_seconds()
    print("shell command completed in " + str(duration))
    if returnCode == 0:
      # Success! Save these changes
      #self.resetTo_state = self.resetTo_state.expandedWithEmptyEntriesFor(testState).withConflictsFrom(testState)
      #self.full_resetTo_state = self.full_resetTo_state.expandedWithEmptyEntriesFor(testState).withConflictsFrom(testState).withoutEmptyEntries()
      return (True, duration)
    else:
      if self.assumeNoSideEffects:
        # unapply changes so that the contents of self.workPath should match self.resetTo_state
        delta = testState.withConflictsFrom(self.resetTo_state, True)
        delta.apply(self.workPath)
      return (False, duration)

  def run(self):
    box = self.candidateBox
    print("##############################################################################################################################################################################################")
    print("Checking candidateState id " + str(self.pipe.identifier) + " at " + str(self.workPath))
    if self.assumeNoSideEffects:
      # If the user told us that we don't have to worry about the possibility of the shell command generating files whose state matters, then
      # we reset the relevant contents of the work path right before we start running a bunch of tests.
      # Note that we don't remove any unrecognized generated files. If assumeNoSideEffects is true then those files are assumed to not affect the correctness of the test, and might even be caches that improve speed.
      self.full_resetTo_state.apply(self.workPath)
    # test each slice
    succeededDuringThisBox = False
    for dimension in range(box.getNumDimensions()):
      for index in range(box.getSize(dimension) - 1, -1, -1):
        if dimension > 0 and box.getSize(dimension) == 1:
          # We've narrowed the search space down to one row
          # Make a note that it could still be worth retesting this row after having removed some other entries from the box
          box.setSliceDuration(dimension, index, 0)
          # However, we know that this row must fail now, so skip re-testing it at the moment
          continue
        candidateState = box.getSlice(dimension, index)
        print("")
        if candidateState.size() < 1:
          print("Skipping slice of size 0")
          box.setSliceDuration(dimension, index, None)
          continue
        (testResults, duration) = self.jobTest(candidateState)
        if testResults:
          print("Job " + str(self.pipe.identifier) + " accepted slice: " + str(candidateState.summarize()))
          self.resetTo_state = self.resetTo_state.withConflictsFrom(candidateState)
          self.onSuccess(candidateState)
          succeededDuringThisBox = True
          succeededDuringThisScan = True
          box.removeSlice(dimension, index)
        else:
          print("Job " + str(self.pipe.identifier) + " rejected slice: " + str(candidateState.summarize()))
          box.setSliceDuration(dimension, index, duration)
    # make some guesses (based on duration) about which individual blocks are likely to contain failures, and run some tests without those failing blocks
    if box.getNumDimensions() >= 2:
      # Most likely, the command that we're testing will read files in a certain order
      # If that's true, then it's also likely that there's a certain file (or directory) that if modified will cause a faster failure than any other
      # Here we do a trinary and/or binary search to try to:
      #  1. Identify that file based on which tests fail most quickly
      #  2. Temporarily exclude that file (or directory) from the rest of the tests that we execute from within this method
      #  3. Hope that that improves the rate of passing tests
      #  4. If we encounter enough success, then continuing searching for the next-fastest failing file or directory
      print("//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////")
      print("Doing a timing analysis of box " + str(self.pipe.identifier))
      loopMax = box.getNumDimensions()
      numSuccesses = 0
      numFailures = 0
      busy = True
      while busy:
        print("")
        if numFailures > numSuccesses + loopMax:
          print("Too many failures in a row. Continuing to next iteration")
          break
        # find the row and column that fail most quickly
        coordinates = []
        for dimension in range(box.getNumDimensions()):
          index = box.getFastestIndex(dimension)
          if index is None:
            busy = False
            break
          coordinates.append(index)
        if not busy:
          break
        blockState = box.getFiles(coordinates)
        box.clearFiles(coordinates)
        if blockState.size() < 1:
          for dimension in range(len(coordinates)):
            box.setSliceDuration(dimension, coordinates[dimension], None)
          continue
        print("Testing shortened box: clearing block at " + str(coordinates) + " : " + str(blockState.summarize()))
        #nextCandidateStates.append(blockState)
        for dimension in range(box.getNumDimensions()):
          index = coordinates[dimension]
          sliceState = box.getSlice(dimension, index)
          if sliceState.size() < 1:
            print("Job skipping empty slice")
            box.setSliceDuration(dimension, index, None)
            continue
          (testResults, duration) = self.jobTest(sliceState)
          if testResults:
            print("Job accepted shortened slice: " + str(sliceState.summarize()))
            self.resetTo_state = self.resetTo_state.withConflictsFrom(sliceState)
            self.onSuccess(sliceState)
            numSuccesses += 1
            succeededDuringThisScan = True
            box.removeSlice(dimension, index)
          else:
            print("Job rejected shortened slice: " + str(sliceState.summarize()))
            numFailures += 1
            box.setSliceDuration(dimension, index, duration)

# Runner class that determines which diffs between two directories cause the given shell command to fail
class DiffRunner(object):
  def __init__(self, failingPath, passingPath, shellCommand, tempPath, workPath, assumeNoSideEffects, assumeInputStatesAreCorrect, tryFail, maxNumJobsAtOnce):
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
    print("Found " + self.originalPassingState.summarize() + " in " + str(passingPath))
    print("")
    print("Finding files in " + failingPath)
    self.originalFailingState = filesStateFromTree(failingPath)
    print("Found " + self.originalFailingState.summarize() + " in " + str(failingPath))
    print("")
    print("Identifying duplicates")
    # list of the files in the state to reset to after each test
    self.full_resetTo_state = self.originalPassingState
    # minimal description of only the files that are supposed to need to be reset after each test
    self.resetTo_state = self.originalPassingState.expandedWithEmptyEntriesFor(self.originalFailingState).withoutDuplicatesFrom(self.originalFailingState)
    self.targetState = self.originalFailingState.expandedWithEmptyEntriesFor(self.originalPassingState).withoutDuplicatesFrom(self.originalPassingState)
    self.originalNumDifferences = self.resetTo_state.size()
    print("Processing " + str(self.originalNumDifferences) + " file differences")
    self.maxNumJobsAtOnce = maxNumJobsAtOnce

  def runnerTest(self, testState, timeout = None):
    workPath = self.getWorkPath("main")
    # reset state if needed
    if not self.assumeNoSideEffects:
      print("Resetting " + str(workPath))
      fileIo.removePath(workPath)
      self.full_resetTo_state.apply(workPath)
      testState.apply(workPath)
    else:
      diff = self.resetTo_state.withConflictsFrom(testState, True)
      diff.apply(workPath)
    start = datetime.datetime.now()
    returnCode = ShellScript(self.testScript_path, workPath).process()
    duration = (datetime.datetime.now() - start).total_seconds()
    print("shell command completed in " + str(duration))
    if returnCode == 0:
      return (True, duration)
    else:
      if self.assumeNoSideEffects:
        # unapply changes so that the contents of workPath should match self.resetTo_state
        testState.withConflictsFrom(self.resetTo_state).apply(workPath)
      return (False, duration)

  def onSuccess(self, testState):
    print("Runner received success of testState: " + str(testState.summarize()))
    self.targetState = self.targetState.withoutDuplicatesFrom(testState)
    self.resetTo_state = self.resetTo_state.withConflictsFrom(testState).withoutDuplicatesFrom(testState)
    delta = self.full_resetTo_state.expandedWithEmptyEntriesFor(testState).withConflictsFrom(testState, True).withoutDuplicatesFrom(self.full_resetTo_state)
    delta.apply(self.bestState_path)
    self.full_resetTo_state = self.full_resetTo_state.withConflictsFrom(delta)

  def getWorkPath(self, jobId):
    return os.path.join(self.workPath, "job-" + str(jobId))

  def run(self):
    start = datetime.datetime.now()
    numIterationsCompleted = 0
    print("Clearing work directories")
    for jobId in range(self.maxNumJobsAtOnce):
      fileIo.removePath(self.getWorkPath(jobId))
    workPath = self.getWorkPath("main")
    if not self.assumeInputStatesAreCorrect:
      print("Testing that the given failing state actually fails")
      fileIo.removePath(workPath)
      fileIo.ensureDirExists(workPath)
      if self.runnerTest(self.targetState)[0]:
        print("\nGiven failing state at " + self.originalFailingPath + " does not actually fail!")
        return False

      print("Testing that the given passing state actually passes")
      if self.assumeNoSideEffects:
        self.resetTo_state.apply(self.workPath)
      else:
        fileIo.removePath(workPath)
        fileIo.ensureDirExists(workPath)
      if not self.runnerTest(self.resetTo_state)[0]:
        print("\nGiven passing state at " + self.originalPassingPath + " does not actually pass!")
        return False

    print("Saving best state found so far")
    fileIo.removePath(self.bestState_path)
    self.originalPassingState.apply(self.bestState_path)

    print("Starting")
    print("(You can inspect " + self.bestState_path + " while this process runs, to observe the best state discovered so far)")
    print("")
    # Now we search over groups of inodes (files or dirs) in the tree
    # Every time we encounter a group of inodes, we try replacing them and seeing if the replacement passes our test
    # If it does, we accept those changes and continue searching
    # If it doesn't, we split that group into smaller groups and continue
    numFailuresDuringCurrentWindowSize = 0
    # TODO: take advantage of parallelism in the first iteration too?
    box = boxFromList(self.targetState.splitOnce())
    jobId = 0
    workingDir = self.getWorkPath(jobId)
    queue = multiprocessing.Queue()
    activeJobs = {jobId:runJobInOtherProcess(self.testScript_path, workingDir, self.resetTo_state, self.full_resetTo_state, self.assumeNoSideEffects, box, queue, jobId)}
    boxesById = {jobId:box}
    pendingBoxes = []
    numConsecutiveFailures = 0
    invalidatedIds = set()
    probablyAcceptableState = FilesState()
    # continue until all files fail and no jobs are running
    while numConsecutiveFailures < self.resetTo_state.size() and len(activeJobs) > 0:
      # wait for a response from a worker
      message = "Elapsed duration: " + str(datetime.datetime.now() - start) + ". Waiting for " + str(len(activeJobs)) + " active subprocesses (" + str(len(pendingBoxes) + len(activeJobs)) + " total available jobs). " + str(self.resetTo_state.size()) + " changes left to test"
      print(message)
      response = queue.get()
      identifier = response[0]
      box = boxesById[identifier]
      acceptedState = response[1]
      if acceptedState.size() > 0:
        print("Runner received successful response from job " + str(identifier) + " : " + str(acceptedState.summarize()))
        if identifier in invalidatedIds:
          # queue a retesting of this box
          testedState = boxesById[identifier].getAllFiles()
          failingState = testedState.withoutDuplicatesFrom(acceptedState)
          print("Queuing a re-test of response from job " + str(identifier) + " due to previous cancellation. Successful state: " + str(acceptedState.summarize()) + ". Failing state: " + str(failingState.summarize()))
          probablyAcceptableState = probablyAcceptableState.expandedWithEmptyEntriesFor(acceptedState).withConflictsFrom(acceptedState)
          if failingState.size() > 0:
            split = failingState.splitOnce()
            failingBox = boxFromList(split)
            if len(split) > 1:
              pendingBoxes = [failingBox] + pendingBoxes
            else:
              pendingBoxes.append(failingBox)
          # Temporarily skip retesting the failing children of this state, because we already added the passing changes to probablyAcceptableState and added the failing changes to failingBox
          box = boxFromList([FilesState()])
        else:
          # A worker discovered a nonempty change that can be made successfully; update our best accepted state
          self.onSuccess(acceptedState)
          numConsecutiveFailures = 0
          # record that the results from any previously started process are no longer guaranteed to be valid
          for i in activeJobs.keys()[:]:
            connection = activeJobs[i]
            if i != identifier:
              invalidatedIds.add(i)
      else:
        print("Received termination response from job " + str(identifier))
        # count failures
        if box.getNumFiles() == 1:
          numConsecutiveFailures += 1
        else:
          numConsecutiveFailures = 0
      # find any children that failed and queue a re-test of those children
      for child in box.getChildren():
        updatedChild = child.withoutDuplicatesFrom(child.withConflictsFrom(self.resetTo_state))
        if updatedChild.size() > 0:
          split = updatedChild.splitOnce()
          print("Split box " + str(updatedChild.summarize()) + " into " + str(len(split)) + " children")
          split = updatedChild.splitOnce()
          box = boxFromList(split)
          if len(split) > 1:
            pendingBoxes = [box] + pendingBoxes
          else:
            pendingBoxes.append(box)

      # clear invalidation status
      if identifier in invalidatedIds:
        invalidatedIds.remove(identifier)
      del activeJobs[identifier]
      del boxesById[identifier]

      # if we haven't checked everything yet, then try to queue more jobs
      if numConsecutiveFailures < self.resetTo_state.size():
        # queue more jobs if there are any available
        maxRunningSize = max([0] + [boxesById[jobId].getNumFiles() for jobId in boxesById])
        if probablyAcceptableState.size() > 0 and probablyAcceptableState.size() >= maxRunningSize:
          print("Retesting previous likely successful states as a single test of size " + str(probablyAcceptableState.size()))
          pendingBoxes = [boxFromList([probablyAcceptableState])] + pendingBoxes
          probablyAcceptableState = FilesState()
        if len(pendingBoxes) < 1 and len(activeJobs) < 1:
          print("Error: no changes remain left to test. It was expected that applying all changes would fail")
          break
        pendingBoxes.sort(reverse=True, key=FilesState_HyperBox.getNumFiles)
        while len(activeJobs) < self.maxNumJobsAtOnce and len(activeJobs) < self.resetTo_state.size() and len(pendingBoxes) > 0:
          maxRunningSize = max([0] + [boxesById[jobId].getNumFiles() for jobId in boxesById])
          # find next pending job
          box = pendingBoxes[0]
          if box.getNumFiles() < maxRunningSize / self.maxNumJobsAtOnce:
            print("Waiting for existing job of size " + str(maxRunningSize) + " to complete before queuing job of size " + str(box.getNumFiles()))
            break
          # find next unused job id
          jobId = 0
          while jobId in activeJobs:
            jobId += 1
          # start job
          print("Starting process " + str(jobId) + " with " + str(box.getNumChildren()) + " child boxes to test and " + str(box.getNumFiles()) + " files to test")
          workingDir = self.getWorkPath(jobId)
          activeJobs[jobId] = runJobInOtherProcess(self.testScript_path, workingDir, self.resetTo_state, self.full_resetTo_state, self.assumeNoSideEffects, box, queue, jobId)
          boxesById[jobId] = box
          pendingBoxes = pendingBoxes[1:]

    print("double-checking results")
    wasSuccessful = True
    workPath = self.getWorkPath("main")
    fileIo.removePath(workPath)
    if not self.runnerTest(filesStateFromTree(self.bestState_path))[0]:
      message = "Error: expected best state at " + self.bestState_path + " did not pass the second time. Could the test be non-deterministic?"
      if self.assumeNoSideEffects:
        message += " (it may help to remove the --assume-no-side-effects flag)"
      if self.assumeInputStatesAreCorrect:
        message += " (it may help to remove the --assume-input-states-are-correct flag)"
      print(message)
      wasSuccessful = False

    print("")
    if self.targetState.size() < 1000:
      filesDescription = str(self.targetState)
    else:
      filesDescription = str(self.targetState.summarize())
    print("Done trying to transform the contents of passing path:\n " + self.originalPassingPath + "\ninto the contents of failing path:\n " + self.originalFailingPath)
    print("Of " + str(self.originalNumDifferences) + " differences, could not accept: " + filesDescription)
    print("The final accepted state can be seen at " + self.bestState_path)
    return wasSuccessful

def main(args):
  assumeNoSideEffects = False
  assumeInputStatesAreCorrect = False
  tryFail = False
  workPath = None
  maxNumJobsAtOnce = 1
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
    if arg == "--num-jobs":
      if len(args) < 2:
        usage()
      maxNumJobsAtOnce = int(args[1])
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
  success = DiffRunner(failingPath, passingPath, shellCommand, tempPath, workPath, assumeNoSideEffects, assumeInputStatesAreCorrect, tryFail, maxNumJobsAtOnce).run()
  endTime = datetime.datetime.now()
  duration = endTime - startTime
  if success:
    print("Succeeded in " + str(duration))
  else:
    print("Failed in " + str(duration))
    sys.exit(1)

main(sys.argv[1:])
