#!/bin/bash
set -e

# save script directory
script_dir=$PWD

# cd into framework/support
cd ../..

function usage() {
	echo "usage: ./add_release_commit_tags.sh <release-list-file>"
	echo "Adds git tags to last commit for a given release on release date YYYY-MM-DD (ISO date format).  Expects release date YYYY-MM-DD as first list of commit list."
	echo "Expected format for each artifactId in the <release-list-file> is:<SHA:artifactId:version>"
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
		echo "Date is either missing, incorrect, or in an invalid format.  Expected format is: YYYY-MM-DD"
		echo "Received: \"$1\""
		exit 1
	fi
}

function addGitTag() {
	exists="$(git tag -l $1 | wc -l)"
	if [ $exists == 0 ]; then
		git tag -a "$1" "$2" -m "$3"
		echo "Added tag!"
	else
		echo "Tag already exists"
	fi
}	

# Iterate through commit log files and create tags
commit_list="$script_dir/$1"
first_line=1
date="0000-00-00"
while read -r line; do
	if [ $first_line -eq 1 ]; then
		date_in_release_file="$line"
		validateDate $date_in_release_file
		date=$date_in_release_file
		first_line=0
		continue
	fi
	sha="$(echo "$line" | cut -d':' -f1)"
	numberColons="$(echo "$line" | tr -cd ':' | wc -c)"
	if [ $numberColons == 3 ]; then
		line_has_groupId_and_artifactId=1
	elif [ $numberColons == 2 ]; then
		line_has_groupId_and_artifactId=0
	else
		echo "The following line is malformatted: "
		echo "$line"
		echo "Cannot resolve line, exiting"
		exit 1
	fi
	if [ $line_has_groupId_and_artifactId == 1 ]; then
		artifactId="$(echo "$line" | cut -d':' -f3)"
		version="$(echo "$line" | cut -d':' -f4)"
	else
		artifactId="$(echo "$line" | cut -d':' -f2)"
		version="$(echo "$line" | cut -d':' -f3)"
	fi
	# Add tag for the release
	echo "Adding tag $date-release-$artifactId to SHA: $sha..."
	addGitTag "$date-release-$artifactId" "$sha" "Inclusive cutoff commit for $artifactId for the $date release"
	# Add tag for the artifact version
	echo "Adding tag $artifactId-$version to SHA: $sha.."
	addGitTag "$artifactId-$version" "$sha" "Inclusive cutoff commit for $artifactId-$version"
done < "$commit_list"

exit