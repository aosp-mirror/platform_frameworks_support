#!/usr/bin/python

import argparse
import os
import subprocess

NAME_HELP = '''
  The name of the artifact you want to add to the prebuilts folder.
  E.g. android.arch.work:work-runtime-ktx:1.0.0-alpha07
'''


def main():
    parser = argparse.ArgumentParser(
        description='Helps download maven artifacts to prebuilts.')
    parser.add_argument('-n', '--name', help=NAME_HELP,
                        required=True, dest='name')
    parse_result = parser.parse_args()
    artifact_name = parse_result.name
    command = './gradlew --build-file build.gradle.kts -PartifactName=%s' % (
        artifact_name)
    process = subprocess.Popen(command,
                               shell=True,
                               stdin=subprocess.PIPE,
                               stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE)
    stdout, stderr = process.communicate()

    if stderr is not None:
        lines = stderr.split('\n')
        for line in lines:
            print line

    if stdout is not None:
        lines = stdout.split('\n')
        for line in lines:
            print line


if __name__ == '__main__':
    main()
