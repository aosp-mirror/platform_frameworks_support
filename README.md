# AOSP Support Library Contribution Guide
## Accepted types of contributions
* Bug fixes (needs a corresponding bug report in b.android.com)
* Each bug fix is expected to come with tests
* Fixing spelling errors
* Updating documentation
* Adding new tests to the area that is not currently covered by tests

We **will not** be accepting new modules, features, or behavior changes

##Checking out the code
Follow the [“Downloading the Source”](https://source.android.com/source/downloading.html) guide to install and set up `repo` tool. Once you get to `repo init` step use the following command instead (this will initialize to only get the components needed for building and testing support library):

    repo init -u https://android.googlesource.com/platform/manifest -b ub-supportlib-master

Now run this command to actually download the code (grab a coffee - it will take a while as it is ~10GB):

    repo sync -j8 -c

You will use this command to sync your checkout in the future - it’s similar to `git fetch`

## Building
You should just be able to use Android Studio, but for command line use:

    cd path/to/checkout/frameworks/support/
    git checkout aosp/master
    ./gradlew assemble


## Using Android Studio
Open `path/to/checkout/frameworks/support/` in Android Studio

If you get “Unregistered VCS root detected” click “Add root”. This will enable git integration for Android Studio.

Build > Clean Project
Build > Make Project

## Making a change
    cd path/to/checkout/frameworks/support/
    repo start my_branch_name .
    (make needed modifications)
    git commit -a
    repo upload .

Choose `always` if you get the following prompt:

    Run hook scripts from https://android.googlesource.com/platform/manifest (yes/always/NO)?

## Getting reviewed
* After you run repo upload, open [r.android.com](http://r.android.com)
* Sign in into your account (or create one if you do not have one yet)
* Add an appropriate reviewer (use git log to find who did most modifications on the file you are fixing)

