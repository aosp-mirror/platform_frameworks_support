#!/bin/bash
set -e

# cd into framework/support
cd ../..

function usage() {
	echo "usage: ./add_release_commit_tags.sh <commit-list>"
  	echo "Adds git tags to last commit for a given release on date YYYY-MM-DD.  Expects ISO date format YYYY-MM-DD as first list of commit list."
  	echo "Expected format for each artifactId in the list is:'SHA:artifactId:version'"
  	exit 1
}

# # Check for invalid usage
if [ $# -ne 1 ]; then
	usage
fi

function validateDate() {
	if [[ $1 =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
		echo "Release date: $1"
	else
		echo "Date is incorrectly or in an invalid format.  Expected format is: YYYY-MM-DD"
		exit 1
	fi
}

# Iterate through commit log files and create tags
commit_list="$1"
date="0000-00-00"
first_line=1
while read -r line; do
	if [ $first_line -eq 1 ]; then
		date="$line"
		validateDate $date
		first_line=0
		continue
	fi
    sha="$(echo "$line" | cut -d':' -f1)"
    artifactId="$(echo "$line" | cut -d':' -f2)"
    version="$(echo "$line" | cut -d':' -f3)"
    # Add tag for the release
    echo "Adding tag $date-release-$artifactId to SHA: $sha"
    git tag -a "$date-release-$artifactId" "$sha" -m "Inclusive cutoff commit for $artifactId for the $date release"
    # Add tag for the artifact version
    echo "Adding tag $artifactId-$version to SHA: $sha"
    git tag -a "$artifactId-$version" "$sha" -m "Inclusive cutoff commit for $artifactId-$version"
done < "$commit_list"

exit