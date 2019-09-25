# Android Jetpack

Jetpack is a suite of libraries, tools, and guidance to help developers write high-quality apps easier. These components help you follow best practices, free you from writing boilerplate code, and simplify complex tasks, so you can focus on the code you care about.

Jetpack comprises the `androidx.*` package libraries, unbundled from the platform APIs. This means that it offers backward compatibility and is updated more frequently than the Android platform, making sure you always have access to the latest and greatest versions of the Jetpack components.

Our official AARs and JARs binaries are distributed through [Google Maven](https://dl.google.com/dl/android/maven2/index.html).

You can learn more about using it from [Android Jetpack landing page](https://developer.android.com/jetpack).

# Contribution Guide
## Accepted Types of Contributions
* Bug fixes - needs a corresponding bug report in the [Android Issue Tracker](https://issuetracker.google.com/issues/new?component=192731&template=842428)
* Each bug fix is expected to come with tests
* Fixing spelling errors
* Updating documentation
* Adding new tests to the area that is not currently covered by tests
* New features to existing libraries if the feature request bug has been approved by an AndroidX team member.

We **are not** currently accepting new modules.

## Checking Out the Code
**NOTE: You will need to use Linux or Mac OS. Building under Windows is not currently supported.**

1. Install `repo` (Repo is a tool that makes it easier to work with Git in the context of Android. For more information about Repo, see the [Repo Command Reference](https://source.android.com/setup/develop/repo))

```bash
    mkdir ~/bin
    PATH=~/bin:$PATH
    curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo
    chmod a+x ~/bin/repo
```

2. Configure Git with your real name and email address.

```bash
    git config --global user.name "Your Name"
    git config --global user.email "you@example.com"
```

3. Create a directory for your checkout (it can be any name)

```bash
    mkdir androidx-master-dev
    cd androidx-master-dev
```

4. Use `repo` command to initialize the repository.

```bash
    repo init -u https://android.googlesource.com/platform/manifest -b androidx-master-dev
```

5. Now your repository is set to pull only what you need for building and running AndroidX libraries. Download the code (and grab a coffee while we pull down 6GB):

```bash
    repo sync -j8 -c
```

You will use this command to sync your checkout in the future - it’s similar to `git fetch`


## Using Android Studio
To open the project with the specific version of Android Studio recommended for developing:

    cd path/to/checkout/frameworks/support/
    ./studiow

and accept the license agreement when prompted. Now you're ready edit, run, and test!

If you get “Unregistered VCS root detected” click “Add root” to enable git integration for Android Studio.

If you see any warnings (red underlines) run `Build > Clean Project`.

## Builds
### Full Build (Optional)
You can do most of your work from Android Studio, however you can also build the full AndroidX library from command line:

    cd path/to/checkout/frameworks/support/
    ./gradlew createArchive

### Testing modified AndroidX Libraries to in your App
You can build maven artifacts locally, and test them directly in your app:

    ./gradlew createArchive

And put in your **project** `build.gradle` file:

    handler.maven { url '/path/to/checkout/out/androidx/build/support_repo/' }

### Continuous integration
[Our continuous integration system](https://ci.android.com/builds/branches/aosp-androidx-master-dev/grid?) builds all in progress (and potentially unstable) libraries as new changes are merged. You can manually download these AARs and JARs for your experimentation.

## Running Tests

### Single Test Class or Method
1. Open the desired test file in Android Studio.
2. Right-click on a test class or @Test method name and select `Run FooBarTest`

### Full Test Package
1. In the project side panel open the desired module.
2. Find the directory with the tests
3. Right-click on the directory and select `Run androidx.foobar`

## Running Sample Apps
The AndroidX repository has a set of Android applications that exercise AndroidX code. These applications can be useful when you want to debug a real running application, or reproduce a problem interactively, before writing test code.

These applications are named either `<libraryname>-integration-tests-testapp`, or `support-\*-demos` (e.g. `support-4v-demos` or `support-leanback-demos`). You can run them by clicking `Run > Run ...` and choosing the desired application.

## Password and Contributor Agreement before making a change
Before uploading your first contribution, you will need setup a password and agree to the contribution agreement:

Generate a HTTPS password:
https://android-review.googlesource.com/new-password

Agree to the Google Contributor Licenses Agreement:
https://android-review.googlesource.com/settings/new-agreement

## Making a change
    cd path/to/checkout/frameworks/support/
    repo start my_branch_name .
    (make needed modifications)
    git commit -a
    repo upload --current-branch .

If you see the following prompt, choose `always`:

    Run hook scripts from https://android.googlesource.com/platform/manifest (yes/always/NO)?

If the upload succeeds, you'll see output like:

    remote:
    remote: New Changes:
    remote:   https://android-review.googlesource.com/c/platform/frameworks/support/+/720062 Further README updates
    remote:

To edit your change, use `git commit --amend`, and re-upload.

## Getting reviewed
* After you run repo upload, open [r.android.com](http://r.android.com)
* Sign in into your account (or create one if you do not have one yet)
* Add an appropriate reviewer (use git log to find who did most modifications on the file you are fixing or check the OWNERS file in the project's directory)

## Handling binary dependencies
AndroidX uses git to store all the binary Gradle dependencies. They are stored in `prebuilts/androidx/internal` and `prebuilts/androidx/external` directories in your checkout. All the dependencies in these directories are also available from `google()`, `jcenter()`, or `mavenCentral()`. We store copies of these dependencies to have hermetic builds. You can pull in [a new dependency using our importMaven tool](development/importMaven/README.md).


Apache License
                           Version 2.0, January 2004
                        https://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [2019] [Rolando Gopez Lacuata]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
