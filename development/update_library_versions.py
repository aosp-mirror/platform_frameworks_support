#!/usr/bin/env python3

import os, sys, zipfile
import argparse
import subprocess

#### ####
# This scripts updates $LIBRARYVERSIONS_REL based on the artifacts 
# in $GMAVEN_BASE_URL.  It will only numerically increment alpha or beta versions.
# It will NOT change stability suffixes and it will NOT increment the version of a RC 
# or stable library.  These changes should be done manually and purposefully.
#### ####

LIBRARYVERSIONS_REL = 'buildSrc/src/main/kotlin/androidx/build/LibraryVersions.kt'
FRAMEWORKS_SUPPORT_FP = os.path.abspath(os.path.join(os.getcwd(), '..'))
LIBRARYVERSIONS_FP = os.path.join(FRAMEWORKS_SUPPORT_FP, LIBRARYVERSIONS_REL)
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

def get_artifactId_from_LibraryVersions_line(line):
	artifactId = line.split('val')[1]
	artifactId = artifactId.split('=')[0]
	artifactId = artifactId.strip(' ')
	artifactId = artifactId.lower()
	artifactId = artifactId.replace('_', '-')
	return artifactId

def get_version_from_LibraryVersions_line(line):
	## Sample input:	'val ACTIVITY = Version("1.0.0-alpha04")'
	## Sample output: 	'1.0.0-alpha04'
	if 'Version(' in line and '\"' in line:
		return line.split('\"')[1]
	else:
		return line.split('=')[-1].strip(' \n').lower()

def get_tot_version_map():
	tot_version_map = {}
	version_needs_resolving_map = {}
	# Open LibraryVersions.kt file for reading and get all lines
	with open(LIBRARYVERSIONS_FP, 'r') as f:
		for cur_line in f.readlines():
			# Skip any line that doesn't declare a version
			if 'val' not in cur_line: continue
			artifactId = get_artifactId_from_LibraryVersions_line(cur_line)
			version = get_version_from_LibraryVersions_line(cur_line)
			if version[0].isnumeric():
				tot_version_map[artifactId] = version
			else:
				# Artifact version is the MACRO for another artifactId
				version_needs_resolving_map[artifactId] = version
	# For all versions that were dependent on other artifactId versions, resolve
	# their versions to make the map complete.
	# This is needed because version MACROs can be a copy another version MACRO.  For example:
	#    val ARCH_CORE = Version("2.0.0")
	#    val ARCH_CORE_TESTING = ARCH_CORE
	for key in version_needs_resolving_map:
		artifactId_to_copy = version_needs_resolving_map[key].lower().replace('_', '-')
		tot_version_map[key] = tot_version_map[artifactId_to_copy]
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
	if '404' in curl_output.decode():
		print("version is good")
		return False
	else:
		print("version is OUT OF DATE")
		return True

def get_groupId_from_artifactId(artifactId):
	# By convention, androidx namespace is declared as:
	# androidx.${groupId}:${groupId}-${optionalArtifactIdSuffix}:${version}
	# However, macros use an '_' instead of a '-'.  So sometimes, sometimes inside this 
	# script, artifactId may use '_' instead of '-' in their artifact Ids because the 
	# artifactId was generated from the macro.
	# So, artifactId == "${groupId}-${optionalArtifactIdSuffix}"
	# or artifactId == "${groupId}_${optionalArtifactIdSuffix}"
	if '-' in artifactId:
		return artifactId.split('-')[0]
	else:
		return artifactId.split('_')[0]

def increment_alpha_beta_version(version):
	# Only increment alpha and beta versions.
	# rc and stable should never need to be incremented in the androidx-master-dev branch
	# Suffix changes should be done manually.
	if 'alpha' in version or 'beta' in version:
		new_version = int(version[-2:]) + 1
		formatted_version = "%02d" % (new_version,)
		return version[:-2] + formatted_version
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
		artifactId = get_artifactId_from_LibraryVersions_line(cur_line)
		groupId = get_groupId_from_artifactId(artifactId)
		version = get_version_from_LibraryVersions_line(cur_line)
		if artifactId.lower() == libraryId.lower():
			lv_lines[i] ="    val " + artifactId.upper() + " = Version(\"" + increment_alpha_beta_version(version) + "\")\n"
			summary_log.append("Updated %s to FROM %s TO %s" % (artifactId.upper(), version, increment_alpha_beta_version(version)))
	# Open file for writing and update all lines
	with open(LIBRARYVERSIONS_FP, 'w') as f:
		f.writelines(lv_lines)

def update_api():
	try:
		os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
		curl_output = subprocess.check_output('./gradlew updateApi', shell=True)
		os.chdir(os.path.dirname(os.path.abspath(__file__)))
	except subprocess.CalledProcessError:
		print_e('FAIL: Failed gradle task updateApi!')
		return None

def update_libary_versions(args):
	tot_version_map = get_tot_version_map()
	# Loop through every library version and update the version, if necessary
	update_api_needed = False
	for artifactId in tot_version_map:
		version = tot_version_map[artifactId]
		groupId = get_groupId_from_artifactId(artifactId)
		if should_update_artifact(args, groupId, artifactId):
			print("Updating %s " % artifactId)
			if does_exist_on_gmaven(groupId, artifactId, version):
				update_artifact_version(artifactId)
				update_api_needed = True
	if update_api_needed:
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

