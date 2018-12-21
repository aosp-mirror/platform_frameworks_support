#!/usr/bin/python3

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

def should_check_artifact(groupId, artifactId):
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
	return line.split('val')[1].split('=')[0].strip(' ')

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
		tot_version_map[artifactId.lower()] = version
	# For all versions that were dependent on other artifactId versions,
	# copy over versions to make map complete
	for key in tot_version_map:
		if not tot_version_map[key][0].isnumeric():
			tot_version_map[key] = tot_version_map[tot_version_map[key]]
	return tot_version_map

def does_exist_on_gmaven(groupId, artifactId, version):
	# example upl: https://dl.google.com/dl/android/maven2/androidx/core/core-ktx/1.1.0-alpha03/core-ktx-1.1.0-alpha03.pom
	# URLS are of the format: .../dl/android/maven2/androidx/${groupId}/${artifactId}/${version}/${artifactId}-${version}.pom
	artifactUrl = GMAVEN_BASE_URL + groupId + '/' + artifactId + '/' + version + '/' + artifactId + '-' + version + '.pom'
	# print('Attempting to download: %s' % artifactUrl)
	try:
		# Curl the url to see if artifact pom exists
		curl_output = subprocess.check_output('curl -s %s' % artifactUrl, shell=True)
	except subprocess.CalledProcessError:
		print_e('FAIL: Failed to curl url: ' %  artifactUrl)
		return None
	# Curl should return a 404 because the dev version should not match the release version
	if '404' in curl_output.decode():
		return False
	else:
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



def incremened_version(version):
	# Only update alpha and beta versions.  rc and stable should never need to be incremented
	# in the androidx-master-dev branch
	if 'alpha' in version or 'beta' in version:
		return version[:-1] + str(int(version[-1]) + 1)
	else:
		return version

def update_artifact_version(artifact):
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
		# If artifacts or groups were specified on the commaned line, only update those artifacts or groups
		formatted_artifactId = artifactId.lower().replace('_','-')
		groupId = get_groupId_from_artifactId(formatted_artifactId)
		if not should_check_artifact(groupId, artifactId): continue
		version = get_version_from_LibraryVersions_line(cur_line)
		if artifactId.lower() == artifact.lower():
			lv_lines[i] ="    val " + artifactId.upper() + " = Version(\"" + incremened_version(version) + "\")\n"
			summary_log.append("Updated %s to FROM %s TO %s" % (artifactId.upper(), version, incremened_version(version)))
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
	for artifact in tot_version_map:
		version = tot_version_map[artifact]
		artifactId = artifact.replace('_', '-')
		groupId = get_groupId_from_artifactId(artifact)
		if does_exist_on_gmaven(groupId, artifactId, version):
			print("%s-%s has already been released!" % (artifactId, version))
			update_artifact_version(artifact)
	update_api()
	summary_log.append("These changes have not been commited.  Please double check before uploading.")


# Set up input arguments
parser = argparse.ArgumentParser(
	description=('Import AndroidX prebuilts from the Android Build Server and if necessary, update PublishDocsRules.kt.  By default, uses gmaven-diff-all-<BUILDID>.zip to get artifacts.'))
parser.add_argument(
	'--groups', metavar='groupId', nargs='+',
	help="""If specified, only check libraries whose groupId contains the listed text.
	For example, if you specify \"--groups paging slice lifecycle\", then this
	script will check each library with groupId beginning with \"androidx.paging\", \"androidx.slice\",
	or \"androidx.lifecycle\"""")
parser.add_argument(
	'--artifacts', metavar='artifactId', nargs='+',
	help="""If specified, only check libraries whose artifactId contains the listed text.
	For example, if you specify \"--artifacts core slice-view lifecycle-common\", then this
	script will check specific artifacts \"androidx.core:core\", \"androidx.slice:slice-view\",
	and \"androidx.lifecycle:lifecycle-common\"""")

# Parse arguments and check for existence of build ID or file
args = parser.parse_args()

update_libary_versions()
print_change_summary()