#!/usr/bin/env python3

import os, sys, zipfile
import argparse
import subprocess

#### ####
# This scripts updates LibraryVersions.k (see $LIBRARYVERSIONS_REL_PATH) based on the artifacts 
# in Google Maven (see $GMAVEN_BASE_URL).  It will only numerically increment alpha or beta versions.
# It will NOT change stability suffixes and it will NOT increment the version of a RC
# or stable library.  These changes should be done manually and purposefully.
#### ####

LIBRARYVERSIONS_REL_PATH = 'buildSrc/src/main/kotlin/androidx/build/LibraryVersions.kt'
FRAMEWORKS_SUPPORT_FULL_PATH = os.path.abspath(os.path.join(os.getcwd(), '..'))
LIBRARYVERSIONS_FULL_PATH = os.path.join(FRAMEWORKS_SUPPORT_FULL_PATH, LIBRARYVERSIONS_REL_PATH)
GMAVEN_BASE_URL = 'https://dl.google.com/dl/android/maven2/androidx/'
summary_log = []
exclude_dirs = []

def print_e(*args, **kwargs):
	print(*args, file=sys.stderr, **kwargs)

def should_update_artifact(commlineArgs, groupId, artifactId):
	# Tells whether to update the given artifact based on the command-line arguments
	should_update = False
	if (commlineArgs.groups) or (commlineArgs.artifacts):
		if (commlineArgs.groups) and (groupId in commlineArgs.groups):
			should_update = True
		if (commlineArgs.artifacts) and (artifactId in commlineArgs.artifacts):
			should_update = True
	else:
		should_update = True
	return should_update

def print_change_summary():
	print("\n ---  SUMMARY --- ")
	for change in summary_log:
		print(change)

def read_in_lines_from_file(file_path):
	if not os.path.exists(file_path):
		print_e("File path does not exist: %s" % file_path)
		exit(1)
	else:
		with open(file_path, 'r') as f:
			lv_lines = f.readlines()
		return lv_lines

def write_lines_to_file(file_path, lines):
	if not os.path.exists(file_path):
		print_e("File path does not exist: %s" % file_path)
		exit(1)
	# Open file for writing and update all lines
	with open(file_path, 'w') as f:
		f.writelines(lines)

def get_artifactId_from_LibraryVersions_line(line):
	artifactId = line.split('val')[1]
	artifactId = artifactId.split('=')[0]
	artifactId = artifactId.strip(' ')
	artifactId = artifactId.lower()
	artifactId = artifactId.replace('_', '-')
	return artifactId

def get_version_or_macro_from_LibraryVersions_line(line):
	## Sample input:	'val ACTIVITY = Version("1.0.0-alpha04")'
	## Sample output: 	'1.0.0-alpha04', True
	## Sample input:	'val ACTIVITY = FRAGMENT'
	## Sample output: 	'fragment', False
	is_version = True
	version = ""
	if 'Version(' in line and '\"' in line:
		is_version = True
		version = line.split('\"')[1]
	else:
		is_version = False
		version = line.split('=')[-1].strip(' \n').lower()
	return version, is_version

def get_tot_version_map(lv_lines):
	resolved_versions_map = {}
	specified_versions_map = {}
	for cur_line in lv_lines:
		# Skip any line that doesn't declare a version
		if 'val' not in cur_line: continue
		artifactId = get_artifactId_from_LibraryVersions_line(cur_line)
		version, is_version = get_version_or_macro_from_LibraryVersions_line(cur_line)
		if is_version:
			resolved_versions_map[artifactId] = version
		else:
			# Artifact version specification is a reference to another artifact's version
			specified_versions_map[artifactId] = version
	# Resolve variable references in specified_versions_map to complete resolved_versions_map.
	# This is needed because version MACROs can be a copy another version MACRO.  For example:
	#    val ARCH_CORE = Version("2.0.0")
	#    val ARCH_CORE_TESTING = ARCH_CORE
	for key in specified_versions_map:
		artifactId_to_copy = specified_versions_map[key].lower().replace('_', '-')
		resolved_versions_map[key] = resolved_versions_map[artifactId_to_copy]
	return resolved_versions_map

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
	if '404' in curl_output.decode():
		print("version is good")
		return False
	else:
		print("version is OUT OF DATE")
		return True

def get_groupId_from_artifactId(artifactId):
	# By convention, androidx namespace is declared as:
	# androidx.${groupId}:${groupId}-${optionalArtifactIdSuffix}:${version}
	# So, artifactId == "${groupId}-${optionalArtifactIdSuffix}"
	return artifactId.split('-')[0]

def increment_alpha_beta_version(version):
	# Only increment alpha and beta versions.
	# rc and stable should never need to be incremented in the androidx-master-dev branch
	# Suffix changes should be done manually.
	if 'alpha' in version or 'beta' in version:
		if version[-2:].isdigit():
			new_version = int(version[-2:]) + 1
			formatted_version = "%02d" % (new_version,)
			return version[:-2] + formatted_version
		else:
			# Account for single digit versions with no preceeding 0 (the leading 0 will be added)
			new_version = int(version[-1:]) + 1
			formatted_version = "%02d" % (new_version,)
			return version[:-1] + formatted_version
	else:
		return version

def update_artifact_version(lv_lines, libraryId):
	num_lines = len(lv_lines)
	for i in range(num_lines):
		cur_line = lv_lines[i]
		# Skip any line that doesn't declare a version
		if 'val' not in cur_line: continue
		artifactId = get_artifactId_from_LibraryVersions_line(cur_line)
		groupId = get_groupId_from_artifactId(artifactId)
		version, _ = get_version_or_macro_from_LibraryVersions_line(cur_line)
		if artifactId.lower() == libraryId.lower():
			lv_lines[i] ="    val " + artifactId.upper() + " = Version(\"" + increment_alpha_beta_version(version) + "\")\n"
			summary_log.append("Updated %s to FROM %s TO %s" % (artifactId.upper(), version, increment_alpha_beta_version(version)))

def update_api():
	try:
		os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
		curl_output = subprocess.check_output('./gradlew updateApi', shell=True)
		os.chdir(os.path.dirname(os.path.abspath(__file__)))
	except subprocess.CalledProcessError:
		print_e('FAIL: Failed gradle task updateApi!')
		return None

def update_libary_versions(args):
	# Open LibraryVersions.kt file for reading and get all lines
	libraryversions_lines = read_in_lines_from_file(LIBRARYVERSIONS_FULL_PATH)
	tot_version_map = get_tot_version_map(libraryversions_lines)
	# Loop through every library version and update the version, if necessary
	versions_changed = False
	for artifactId in tot_version_map:
		version = tot_version_map[artifactId]
		groupId = get_groupId_from_artifactId(artifactId)
		if should_update_artifact(args, groupId, artifactId):
			print("Updating %s " % artifactId)
			if does_exist_on_gmaven(groupId, artifactId, version):
				update_artifact_version(libraryversions_lines, artifactId)
				versions_changed = True
	if versions_changed:
		write_lines_to_file(LIBRARYVERSIONS_FULL_PATH, libraryversions_lines)
		update_api()
		summary_log.append("These changes have not been commited.  Please double check before uploading.")
	else:
		summary_log.append("No changes needed.  All versions are update to date :)")


if __name__ == '__main__':
	# cd into directory of script
	os.chdir(os.path.dirname(os.path.abspath(__file__)))

	# Set up input arguments
	parser = argparse.ArgumentParser(
		description=('This script increments versions in LibraryVersions.kt based on artifacts released to Google Maven.'))
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
	args = parser.parse_args()
	update_libary_versions(args)
	print_change_summary()

