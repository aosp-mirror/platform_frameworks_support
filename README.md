# AOSP Support Library Contribution Guide
## Accepted types of contributions
* Bug fixes (needs a corresponding bug report in b.android.com)
* Each bug fix is expected to come with tests
* Fixing spelling errors
* Updating documentation
* Adding new tests to the area that is not currently covered by tests

We **will not** currently be accepting new modules, features, or behavior changes

## Checking out the code
Follow the [“Downloading the Source”](https://source.android.com/source/downloading.html) guide to install and set up `repo` tool, but instead of running the listed `repo` commands to initialize the repository, run the folowing:

    repo init -u https://android.googlesource.com/platform/manifest -b ub-supportlib-master

Now your repository is step to pull only what you need for building and running support library. Download the code (and grab a coffee while we pull down 10GB):

    repo sync -j8 -c

You will use this command to sync your checkout in the future - it’s similar to `git fetch`


## Using Android Studio
Open `path/to/checkout/frameworks/support/` in Android Studio

If you get “Unregistered VCS root detected” click “Add root” to enable git integration for Android Studio.

If you see any warnings (red underlines) run `Build > Clean Project`.

## Building
You can do most of your work from Android Studio, however optionally you are also able to build the full support library from command line:

    cd path/to/checkout/frameworks/support/
    ./gradlew createArchive

## Running tests

### Running a class of tests or a single test
1. Open the desired test file in Android Studio.
2. Right-click on a class name or a test name and select `Run FooBarTest`

### Running all the tests in a single package
1. In the project side panel open the desired module.
2. Find the directory with the tests
3. Right-click on the directory and select `Run android.support.foobar`

## Running sample apps
Support library has a set of Android applications that exercise support library code. These applications can be useful when you are adding new APIs and want to make sure that they work in a real application.

These applications are named support-\*-demos (e.g. support-4v-demos or support-leanback-demos. You can run them by clicking `Run > Run ...` and choosing the desired application.

## Making a change
    cd path/to/checkout/frameworks/support/
    repo start my_branch_name .
    (make needed modifications)
    git commit -a
    repo upload --current-branch .

Choose `always` if you get the following prompt:

    Run hook scripts from https://android.googlesource.com/platform/manifest (yes/always/NO)?

## Getting reviewed
* After you run repo upload, open [r.android.com](http://r.android.com)
* Sign in into your account (or create one if you do not have one yet)
* Add an appropriate reviewer (use git log to find who did most modifications on the file you are fixing)

