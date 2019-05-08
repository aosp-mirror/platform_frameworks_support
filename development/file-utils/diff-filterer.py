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

class FilesState_Grid(object):
  def __init__(self, numColumns, numRows):
    self.rows = []
    for i in range(numRows):
      self.rows.append([])
      for j in range(numColumns):
         self.rows[-1].append(FilesState())
    self.rowDurations = [None] * numRows
    self.columnDurations = [None] * numColumns

  def removeRow(self, index):
    del self.rows[index]
    del self.rowDurations[index]

  def removeColumn(self, index):
    for row in self.rows:
      del row[index]
    del self.columnDurations[index]

  def getFiles(self, xIndex, yIndex):
    return self.rows[yIndex][xIndex]

  def clearFiles(self, xIndex, yIndex):
    self.rows[yIndex][xIndex] = FilesState()

  def getRow(self, index):
    result = FilesState()
    row = self.rows[index]
    for block in row:
      result = result.expandedWithEmptyEntriesFor(block).withConflictsFrom(block)
    return result

  def getColumn(self, index):
    result = FilesState()
    for row in self.rows:
      block = row[index]
      result = result.expandedWithEmptyEntriesFor(block).withConflictsFrom(block)
    return result

  def getNumColumns(self):
    return len(self.columnDurations)

  def getColumnDuration(self, index):
    return self.columnDurations[index]

  def setColumnDuration(self, index, duration):
    self.columnDurations[index] = duration

  def getFastestColumn(self):
    return self.getFastest(self.columnDurations)

  def getNumRows(self):
    return len(self.rowDurations)

  def getRowDuration(self, index):
    return self.rowDurations[index]

  def setRowDuration(self, index, duration):
    self.rowDurations[index] = duration

  def getFastestRow(self):
    return self.getFastest(self.rowDurations)

  # returns the index of the smallest value in the given list, ignoring None values
  def getFastest(self, values):
    bestValue = None
    bestIndex = None
    for i in range(len(values)):
      value = values[i]
      if value is not None:
        if bestValue is None or value < bestValue:
          bestIndex = i
          bestValue = value
    return bestIndex


  def putFiles(self, filesState, xIndex, yIndex):
    self.rows[yIndex][xIndex] = filesState

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
    # reset state if needed
    if not self.assumeNoSideEffects:
      print("Resetting " + str(self.workPath))
      fileIo.removePath(self.workPath)
      self.full_resetTo_state.apply(self.workPath)
      filesState.apply(self.workPath)
    else:
      self.resetTo_state.withConflictsFrom(filesState).apply(self.workPath)
    start = datetime.datetime.now()
    returnCode = ShellScript(self.testScript_path).process(self.workPath)
    duration = (datetime.datetime.now() - start).total_seconds()
    print("shell command completed in " + str(duration))
    if returnCode == 0:
      return (True, duration)
    else:
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
    candidateStates = FilesState_Grid(1, 1)
    candidateStates.putFiles(self.targetState , 0, 0)
    splitDepth = 0
    while True:
      numFailuresDuringPreviousWindowSize = numFailuresDuringCurrentWindowSize
      numFailuresDuringCurrentWindowSize = 0
      displayIndex = 0
      succeededDuringThisScan = False
      # test each row
      for rowIndex in range(candidateStates.getNumRows() - 1, -1, -1):
        row = candidateStates.getRow(rowIndex)
        print("")
        print("Elapsed duration = " + str(datetime.datetime.now() - start) + ". " + str(self.resetTo_state.size()) + " changes left to test")
        print("Testing row " + str(rowIndex))
        (testResults, duration) = self.test(row)
        if testResults:
          print("Accepted row: " + str(row.summarize()))
          # success! keep these changes
          row.apply(self.bestState_path)
          self.full_resetTo_state = self.full_resetTo_state.expandedWithEmptyEntriesFor(row).withConflictsFrom(row).withoutEmptyEntries()
          # remove these changes from the set of changes to reconsider
          self.targetState = self.targetState.withoutDuplicatesFrom(row)
          self.resetTo_state = self.targetState.withConflictsFrom(self.resetTo_state)
          succeededDuringThisScan = True
          candidateStates.removeRow(rowIndex)
        else:
          print("Rejected row: " + str(row.summarize()))
          candidateStates.setRowDuration(rowIndex, duration)
      # test each column
      if candidateStates.getNumRows() > 1 and candidateStates.getNumColumns() > 1:
        for columnIndex in range(candidateStates.getNumColumns() - 1, -1, -1):
          column = candidateStates.getColumn(columnIndex)
          print("")
          print("Elapsed duration = " + str(datetime.datetime.now() - start) + ". " + str(self.resetTo_state.size()) + " changes left to test")
          print("Testing column " + str(columnIndex))
          (testResults, duration) = self.test(column)
          if testResults:
            print("Accepted column: " + str(column.summarize()))
            # success! keep these changes
            column.apply(self.bestState_path)
            self.full_resetTo_state = self.full_resetTo_state.expandedWithEmptyEntriesFor(column).withConflictsFrom(column).withoutEmptyEntries()
            # remove these changes from the set of changes to reconsider
            self.targetState = self.targetState.withoutDuplicatesFrom(column)
            self.resetTo_state = self.targetState.withConflictsFrom(self.resetTo_state)
            succeededDuringThisScan = True
            candidateStates.removeColumn(columnIndex)
          else:
            print("Rejected column: " + str(column.summarize()))
            candidateStates.setColumnDuration(columnIndex, duration)
        # make some guesses (based on duration) about which individual blocks are likely to contain failures, and run some tests without those failing blocks
        if candidateStates.getNumColumns() + candidateStates.getNumRows() >= 4:
          print("////////////////////////////////////////////////////////////////////////////////////////////////////")
          loopMax = min(candidateStates.getNumColumns(), candidateStates.getNumRows())
          numSuccesses = 0
          numFailures = 0
          while True:
            print("Elapsed duration = " + str(datetime.datetime.now() - start) + ". " + str(self.resetTo_state.size()) + " changes left to test (" + str(candidateStates.getNumColumns()) + "x" + str(candidateStates.getNumRows()) + " grid)")
            if numFailures >= numSuccesses + loopMax:
              print("Too many failures in a row. Continuing to next iteration")
              break
            # find the row and column that fail most quickly
            columnIndex = candidateStates.getFastestColumn()
            if candidateStates.getNumColumns() <= 1 or columnIndex is None:
              print("Removed all columns other than one. Continuing to next iteration")
              break
            rowIndex = candidateStates.getFastestRow()
            if candidateStates.getNumRows() <= 1 or rowIndex is None:
              print("Removed all rows other than one. Continuing to next iteration")
              break
            existingBlock = candidateStates.getFiles(columnIndex, rowIndex)
            print("Testing shortened row and column: clearing block at (" + str(columnIndex) + "," + str(rowIndex) + ") (row previously failed in " + str(candidateStates.getRowDuration(rowIndex)) + ", column previously failed in " + str(candidateStates.getColumnDuration(columnIndex)) + "). Block: " + str(existingBlock.summarize()))
            if existingBlock.size() < 1:
              candidateStates.setRowDuration(rowIndex, None)
              candidateStates.setColumnDuration(columnIndex, None)
              if candidateStates.getNumRows() * candidateStates.getNumColumns() >= self.resetTo_state.size():
                print("Returned to a previously cleared block at (" + str(columnIndex) + ", " + str(rowIndex) + "). Skipping to next iteration")
              else:
                print("Returned to a previously cleared block at (" + str(columnIndex) + ", " + str(rowIndex) + "). Skipping it and continuing")
              continue
            # this block probably has an error in it, so clear it
            candidateStates.clearFiles(columnIndex, rowIndex)
            # retest the row
            row = candidateStates.getRow(rowIndex)
            if row.size() < 1:
              print("Skipping empty row " + str(rowIndex))
              candidateStates.setRowDuration(rowIndex, None)
              continue
            print("Testing shortened row " + str(rowIndex))
            (testResults, duration) = self.test(row)
            if testResults:
              print("Accepted shortened row: " + str(row.summarize()))
              numSuccesses += 1
              row.apply(self.bestState_path)
              self.full_resetTo_state = self.full_resetTo_state.expandedWithEmptyEntriesFor(row).withConflictsFrom(row).withoutEmptyEntries()
              # remove these changes from the set of changes to reconsider
              self.targetState = self.targetState.withoutDuplicatesFrom(row)
              self.resetTo_state = self.targetState.withConflictsFrom(self.resetTo_state)
              succeededDuringThisScan = True
              candidateStates.removeRow(rowIndex)
            else:
              print("Rejected shortened row: " + str(row.summarize()))
              numFailures += 1
              candidateStates.setRowDuration(rowIndex, duration)
            # retest the column
            column = candidateStates.getColumn(columnIndex)
            if column.size() < 1:
              print("Skipping empty column " + str(columnIndex))
              candidateStates.setColumnDuration(columnIndex, None)
              continue
            print("")
            print("Testing shortened column " + str(columnIndex))
            (testResults, duration) = self.test(column)
            if testResults:
              print("Accepted shortened column: " + str(column.summarize()))
              numSuccesses += 1
              column.apply(self.bestState_path)
              self.full_resetTo_state = self.full_resetTo_state.expandedWithEmptyEntriesFor(column).withConflictsFrom(column).withoutEmptyEntries()
              # remove these changes from the set of changes to reconsider
              self.targetState = self.targetState.withoutDuplicatesFrom(column)
              self.resetTo_state = self.targetState.withConflictsFrom(self.resetTo_state)
              succeededDuringThisScan = True
              candidateStates.removeColumn(columnIndex)
            else:
              print("Rejected shortened column: " + str(column.summarize()))
              numFailures += 1
              candidateStates.setColumnDuration(columnIndex, duration)
            print("")
      # split the window into smaller pieces and continue
      targetHeight = candidateStates.getNumRows() * 2
      targetWidth = candidateStates.getNumColumns() * 2
      if targetWidth * targetHeight > self.resetTo_state.size():
        print("targetWidth (" + str(targetWidth) + ") * targetHeight (" + str(targetHeight) + ") > resetTo_state.size() (" + str(self.resetTo_state.size()) + ")")
        targetHeight = candidateStates.getNumRows() * 3
        targetWidth = self.resetTo_state.size() / targetHeight
        if targetWidth < 1:
          print("Keeping target width to at least 1")
          targetWidth = 1
      print("Trying to split a " + str(candidateStates.getNumColumns()) + "x" + str(candidateStates.getNumRows()) + " grid into " + str(targetWidth) + "x" + str(targetHeight))
      targetNumBlocks = targetWidth * targetHeight
      sizePerBlock = self.targetState.size() / targetNumBlocks
      splitDepth += 1
      newBlocks = self.targetState.splitDepth(splitDepth)
      #newBlocks = self.targetState.splitDownToApproximatelySize(sizePerBlock)
      #print("Called splitDownToApproximatelySize(" + str(sizePerBlock) + "). Got:")
      print("Called splitDepth(" + str(splitDepth) + "). Got " + str(len(newBlocks)) + " blocks:")
      for block in newBlocks:
        print(block.size())
      
      newHeight = max(targetHeight, int(math.ceil(math.sqrt(len(newBlocks)))))
      if newHeight < 1:
        newHeight = 1
      if newHeight > len(newBlocks):
        newHeight = len(newBlocks)
      newWidth = int(math.ceil(float(len(newBlocks)) / float(newHeight)))
      print("#####################################################################################################")
      print("Splitting " + str(len(newBlocks)) + " blocks into a " + str(newWidth) + "x" + str(newHeight) + " grid")
      newCandidateStates = FilesState_Grid(newWidth, newHeight)
      x = 0
      y = 0
      numExtraBoxes = newWidth * newHeight - len(newBlocks)
      for i in range(len(newBlocks)):
        #print("Saving block " + str(i) + " at (" + str(x) + ", " + str(y) + ")")
        newCandidateStates.putFiles(newBlocks[i], x, y)
        x += 1
        if x + y + 1 == numExtraBoxes:
          x += 1
        if x >= newWidth:
          x = 0
          y += 1

      splitDuringThisScan = newCandidateStates.getNumRows() != candidateStates.getNumRows() or newCandidateStates.getNumColumns() != candidateStates.getNumColumns()
      candidateStates = newCandidateStates
      if not splitDuringThisScan and not succeededDuringThisScan:
        break

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
