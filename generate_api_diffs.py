#!/usr/bin/python3

import os, sys, zipfile
import argparse
import subprocess

def print_e(*args, **kwargs):
	print(*args, file=sys.stderr, **kwargs)

def generate_api_diffs(args):
	print("Generating api diffs for library %s" % args.library)
	generate_local_diffs_cmd =  "./gradlew %s:generateLocalDiffs -PtoApi=%s" % (args.library, args.releaseVersion)
	if args.fromApiVersion:
		from_api_version = " -PfromApi=%s" % args.fromApiVersion
		generate_local_diffs_cmd = generate_local_diffs_cmd + from_api_version
	try:
		subprocess.run(generate_local_diffs_cmd, stderr=subprocess.STDOUT, shell=True, check=True)
	except subprocess.CalledProcessError:
		print_e('Failed to generate local diffs for library %s' % args.library)
		sys.exit(1)
	print("Diff succesfully generated")

def zip_diff(library, releaseVersion):
	diff_path = "./../../out//host/gradle/frameworks/support/build/javadoc/online/sdk/support_api_diff/%s/%s" % (library, releaseVersion)
	generate_local_diffs_cmd =  "zip -r %s-diffs.zip %s" % (args.library, diff_path)
	try:
		subprocess.run(generate_local_diffs_cmd, stderr=subprocess.STDOUT, shell=True, check=True)
	except subprocess.CalledProcessError:
		print_e('Failed to create zip file for diffs')
		sys.exit(1)
	print("%s/%s-diffs.zip successfully created" % (os.path.dirname(os.path.realpath(__file__)), library))



# Set up input arguments
parser = argparse.ArgumentParser(
	description=('Generates api diffs from one version to another for the specified project, if no versions are specified, api diffs will be generated from the last released project version to the current project version'))
parser.add_argument(
	'library',
	help='The library to generate the diffs for, as it appears in settings.gradle')
parser.add_argument(
	'releaseVersion',
	help='The version currently being released, api diffs will be generated up to this version')
parser.add_argument(
	'-fromApiVersion',
	help='The old library version we want to generate the diffs from, if not specified we use the version immediately preceding the one currenly being released')

args = parser.parse_args()

os.chdir(os.path.dirname(os.path.abspath(__file__)))
generate_api_diffs(args)
zip_diff(args.library, args.releaseVersion)