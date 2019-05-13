#!/bin/bash
set -e

# save script directory
script_dir=$PWD

# cd into framework/support
cd ../..

function usage() {
	echo "usage: ./add_release_commit_tags.sh <release-list-file>"
	echo "Adds git tags to last commit for a given release on release date YYYY-MM-DD (ISO date format).  Expects release date YYYY-MM-DD as first list of commit list."
	echo "Expected format for each artifactId in the <release-list-file> is:"
	echo "<SHA:artifactId:version> OR <SHA:grouId:artifactId:version>"
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
	tagName="$1"
	sha="$2"
	message="$3"
	exists="$(git tag -l $tagName | wc -l)"
	if [ $exists == 0 ]; then
		git tag -a "$tagName" "$sha" -m "$message"
		echo "Added tag!"
	else
		echo "Tag already exists"
	fi
}

function getGroupIdFromArtifactId() {
	groupId="$(echo "$1" | cut -d'-' -f1)"
	echo "androidx.$groupId"
}

function getReleaseNotesURL() {
	groupId="$1"
	version="$2"
	groupIdWithoutAndroidx="$(echo "$groupId" | cut -d'.' -f2)"
	## Account for androidx.camera having a camerax path
	if [ "$groupIdWithoutAndroidx" == "camera" ]; then
		groupIdWithoutAndroidx="camerax"
	fi
	releaseNoteBaseURL="https://developer.android.com/jetpack/androidx/releases/"
	groupReleaseNotesURL="$releaseNoteBaseURL$groupIdWithoutAndroidx#$version"
	echo $groupReleaseNotesURL
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
		# format: <SHA:grouId:artifactId:version>
		line_has_groupId_and_artifactId=true
	elif [ $numberColons == 2 ]; then
		# format: <SHA:artifactId:version>
		line_has_groupId_and_artifactId=false
	else
		echo "The following line is malformatted: "
		echo "$line"
		echo "Cannot resolve line, exiting"
		exit 1
	fi
	if [ $line_has_groupId_and_artifactId == true ]; then
		# format: <SHA:grouId:artifactId:version>
		artifactId="$(echo "$line" | cut -d':' -f3)"
		groupId="$(getGroupIdFromArtifactId $artifactId)"
		version="$(echo "$line" | cut -d':' -f4)"
	else
		# format: <SHA:artifactId:version>
		artifactId="$(echo "$line" | cut -d':' -f2)"
		groupId="$(getGroupIdFromArtifactId $artifactId)"
		version="$(echo "$line" | cut -d':' -f3)"
	fi
	# Add tag for the artifact version
	tagName="androidx-$artifactId-$version"
	releaseNotesURL="$(getReleaseNotesURL "$groupId" "$version")"
	echo "Adding tag $tagName to SHA: $sha..."
	addGitTag "$tagName" "$sha" "Release $groupId:$artifactId:$version, released on $date. Release notes for this artifact can be found here: $releaseNotesURL"
done < "$commit_list"

exit