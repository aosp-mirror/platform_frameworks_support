#!/usr/bin/env python3

import os, sys, zipfile
import argparse
import subprocess
from shutil import rmtree
from distutils.dir_util import copy_tree

# cd into directory of script
os.chdir(os.path.dirname(os.path.abspath(__file__)))

# See go/fetch_artifact for details on this script.
LIBRARYVERSIONS_REL = 'buildSrc/src/main/kotlin/androidx/build/LibraryVersions.kt'
FRAMEWORKS_SUPPORT_FP = os.path.abspath(os.path.join(os.getcwd(), '..'))
LIBRARYVERSIONS_FP = os.path.join(FRAMEWORKS_SUPPORT_FP, LIBRARYVERSIONS_REL)
GMAVEN_BASE_URL = 'https://dl.google.com/dl/android/maven2/androidx/'
summary_log = []
exclude_dirs = []


def print_e(*args, **kwargs):
	print(*args, file=sys.stderr, **kwargs)

def should_update_artifact(groupId, artifactId):
	# If a artifact or group list was specified and if the artifactId or groupId were NOT specified 
	# in either list on the command line, return false
	should_update = False
	if (args.groups) or (args.artifacts):
		if (args.groups) and (groupId in args.groups):
			should_update = True
		if (args.artifacts) and (artifactId in args.artifacts):
			should_update = True
	else:
		should_update = True
	return should_update

def print_change_summary():
	print("\n ---  SUMMARY --- ")
	for change in summary_log:
		print(change)

def get_artifactID_from_LibraryVersions_line(line):
	artifactId = line.split('val')[1]
	artifactId = artifactId.split('=')[0]
	artifactId = artifactId.strip(' ')
	artifactId = artifactId.lower()
	artifactId = artifactId.replace('_', '-')
	return artifactId

def get_version_from_LibraryVersions_line(line):
	if 'Version(' in line and '\"' in line:
		return line.split('\"')[1]
	else:
		return line.split('=')[-1].strip(' \n').lower()

def get_tot_version_map():
	tot_version_map = {}
	# Open LibraryVersions.kt file for reading and get all lines
	with open(LIBRARYVERSIONS_FP, 'r') as f:
		lv_lines = f.readlines()
	num_lines = len(lv_lines)
	for i in range(num_lines):
		cur_line = lv_lines[i]
		# Skip any line that doesn't declare a version
		if 'val' not in cur_line: continue
		# lines are of the format "    val CUSTOMVIEW = Version("1.0.0")"
		artifactId = get_artifactID_from_LibraryVersions_line(cur_line)
		version = get_version_from_LibraryVersions_line(cur_line)
		tot_version_map[artifactId] = version
	# For all versions that were dependent on other artifactId versions,
	# copy over versions to make map complete
	for key in tot_version_map:
		if not tot_version_map[key][0].isnumeric():
			tot_version_map[key] = tot_version_map[tot_version_map[key].lower().replace('_', '-')]
	return tot_version_map

def does_exist_on_gmaven(groupId, artifactId, version):
	print("Checking GMaven for %s-%s..." % (artifactId, version), end = '')
	# URLS are of the format: 
	# https://dl.google.com/dl/android/maven2/androidx/${groupId}/${artifactId}/${version}/${artifactId}-${version}.pom
	artifactUrl = GMAVEN_BASE_URL + groupId + '/' + artifactId + '/' + version + '/' + artifactId + '-' + version + '.pom'
	try:
		# Curl the url to see if artifact pom exists
		curl_output = subprocess.check_output('curl -s %s' % artifactUrl, shell=True)
	except subprocess.CalledProcessError:
		print_e('FAIL: Failed to curl url: ' %  artifactUrl)
		return None
	# Curl should return a 404 because the dev version should not match the release version
	if '404' in curl_output.decode():
		print("version is good")
		return False
	else:
		print("version is OUT OF DATE")
		return True

def get_groupId_from_artifactId(artifactId):
	# By convention, androidx namespace is declared as:
	# androidx.${groupId}:${groupId}-${optionalArtifactIdSuffix}:${version}
	# Here, artifactId == "${groupId}-${optionalArtifactIdSuffix}"
	# or artifactId == "${groupId}_${optionalArtifactIdSuffix}"
	if '-' in artifactId:
		return artifactId.split('-')[0]
	else:
		return artifactId.split('_')[0]

def increment_version(version):
	# Only increment alpha and beta versions.
	# rc and stable should never need to be incremented in the androidx-master-dev branch
	# Suffix changes should be done manually.
	if 'alpha' in version or 'beta' in version:
		return version[:-1] + str(int(version[-1]) + 1)
	else:
		return version

def update_artifact_version(libraryId):
	# Open LibraryVersions.kt file for reading and get all lines
	with open(LIBRARYVERSIONS_FP, 'r') as f:
		lv_lines = f.readlines()
	num_lines = len(lv_lines)
	for i in range(num_lines):
		cur_line = lv_lines[i]
		# Skip any line that doesn't declare a version
		if 'val' not in cur_line: continue
		artifactId = get_artifactID_from_LibraryVersions_line(cur_line)
		groupId = get_groupId_from_artifactId(artifactId)
		version = get_version_from_LibraryVersions_line(cur_line)
		if artifactId.lower() == libraryId.lower():
			lv_lines[i] ="    val " + artifactId.upper() + " = Version(\"" + increment_version(version) + "\")\n"
			summary_log.append("Updated %s to FROM %s TO %s" % (artifactId.upper(), version, increment_version(version)))
	# Open file for writing and update all lines
	with open(LIBRARYVERSIONS_FP, 'w') as f:
		f.writelines(lv_lines)

def update_api():
	try:
		# Curl the url to see if artifact pom exists
		os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
		curl_output = subprocess.check_output('./gradlew updateApi', shell=True)
		os.chdir(os.path.dirname(os.path.abspath(__file__)))
	except subprocess.CalledProcessError:
		print_e('FAIL: Failed gradle task updateApi!')
		return None

def update_libary_versions():
	tot_version_map = get_tot_version_map()
	# Loop through every library version and update the version, if necessary
	update_api_needed = False
	for artifactId in tot_version_map:
		version = tot_version_map[artifactId]
		groupId = get_groupId_from_artifactId(artifactId)
		if should_update_artifact(groupId, artifactId):
			print("Updating %s " % artifactId)
			if does_exist_on_gmaven(groupId, artifactId, version):
				update_artifact_version(artifactId)
				update_api_needed = True
	if update_api_needed:
		update_api()
		summary_log.append("These changes have not been commited.  Please double check before uploading.")
	else:
		summary_log.append("No changes needed.  All versions are update to date :)")

# Set up input arguments
parser = argparse.ArgumentParser(
	description=('Update LibraryVersions.kt versions to update a library to the next developement version. \
		Checks Google Maven to determine which versions have been released.'))
parser.add_argument(
	'--groups', metavar='groupId', nargs='+',
	help="""If specified, only increments the version for libraries whose groupId contains the listed text.
	For example, if you specify \"--groups paging slice lifecycle\", then this
	script will increment the version of each library with groupId beginning with \"androidx.paging\", \"androidx.slice\",
	or \"androidx.lifecycle\"""")
parser.add_argument(
	'--artifacts', metavar='artifactId', nargs='+',
	help="""If specified, only increments the version for libraries whose artifactId contains the listed text.
	For example, if you specify \"--artifacts core slice-view lifecycle-common\", then this
	script will increment the version for specific artifacts \"androidx.core:core\", \"androidx.slice:slice-view\",
	and \"androidx.lifecycle:lifecycle-common\"""")

# Parse arguments and check for existence of build ID or file
args = parser.parse_args()

update_libary_versions()
print_change_summary()