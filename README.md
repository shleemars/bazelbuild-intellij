# About the Fork

**Goal: Have a working, external version of the Bazel Android Studio plugin. Share it with anyone else who might want it!**

We've forked only because we found we needed to apply bug fixes to get the plugin to work. Hopefully that's temporary; we'd be delighted to get these fixes into the main repo instead! Indeed, we tried to propose solutions there first, but haven't heard anything from the maintainers in a while--nor have most users, issues, or PRs, it seems. Bazel intellij developers, if you're reading this, thank you for the great software you've shared with the world. We're appreciative and would be delighted to delete this repo when the main one is back in working order.

In the meantime, we'll try to maintain this repo as a version of the plugin that works outside of Google. We'll have it automatically pull in the latest from the official repo. If you spot good fixes that unbreak key issues in the main repo but aren't being merged, we'd love it if you'd help keep the fork working by filing a PR or issue here. But new issues should go in the main repo!

## Key Issues Fixed:

- Compiler wrapper breaks C++/NDK autocomplete: https://github.com/bazelbuild/intellij/issues/2285
- External include paths break when you run a build: https://github.com/bazelbuild/intellij/issues/2766
- Blaze icon sneaks through for non-incremental sync: https://github.com/bazelbuild/intellij/issues/2762

# An IntelliJ plugin for [Bazel](http://bazel.build) projects

## Installation

To use the fork, you'll have to build it from source--but that's easy and fast. See "Building the plugin," below. (Skip the GitHub releases and Jetbrains marketplace builds--those are from the main repo.)

Load it into the IDE via Preferences->Plugins->Gear->Install Plugin from Disk.

As a fast path, [here's my build for Android Studio](https://github.com/hedronvision/bazelbuild-intellij/releases/download/v2021.07.17/aswb_bazel.zip) (From July 17, 2021. Fixes things for me as of Android Studio 4.2.2).

[If anyone wants to help with better distribution (binaries, CI, etc.) we'd love it!]

## Usage

To import an existing Bazel project, choose `Import Bazel Project`,
and follow the instructions in the project import wizard.

Detailed docs are available [here](http://ij.bazel.build).


## Building the plugin

Install Bazel, then build the target `*:*_bazel_zip` for your desired product:

* `bazel build //ijwb:ijwb_bazel_zip --define=ij_product=intellij-ue-oss-stable`
* `bazel build //clwb:clwb_bazel_zip --define=ij_product=clion-oss-stable`
* `bazel build //aswb:aswb_bazel_zip --define=ij_product=android-studio-oss-stable`

from the project root. This will create a plugin zip file at
`bazel-bin/<PRODUCT>/<PRODUCT>_bazel.zip`, which can be installed directly
from the IDE. `<PRODUCT>` can be one of `ijwb, clwb, aswb`.

If the IDE refuses to load the plugin because of version issues, specify the
correct `ij_product`. These are in the form `<IDE>-oss-<VERSION>` with 
  * `<IDE>` being one of `intellij-ue, intellij, clion, android-studio`, 
  * `<VERSION>` being one of `stable, beta, under-dev`.

Note that there is a difference between `intellij` and `intellij-ue`.
`ue` stands for IntelliJ Ultimate Edition and contains additional 
features for JavaScript as well as Go.

If you are using the most recent version of your IDE, you likely want
`--define=ij_product=<IDE>-oss-beta` which will be the next version after
`<IDE>-oss-stable`. Additionally, `under-dev` can be a largely untested `alpha` 
build of an upcoming version. A complete mapping of all currently defined 
versions can be found in  `intellij_platform_sdk/build_defs.bzl`.

You can import the project into IntelliJ (with the Bazel plugin)
via importing the `ijwb/ijwb.bazelproject` file.

## Compatibility with IDE Versions

You can build the plugin for different IDE versions by adjusting the `ij_product` 
option either from command line or by updating the `.bazelproject` file to specify
the correct value for `ij_product` under `build_flags`. 

We have three aliases for product versions;
  * `stable` is the IDE version supported by the Bazel plugin released to 
  the JetBrains stable channel.
  * `beta` is the IDE version supported by the Bazel plugin released to
  the JetBrains Beta channel.
  * `under-dev` is the IDE version we are currently working towards supporting.

The current corresponding IDE versions of these aliases can be found [here](./intellij_platform_sdk/build_defs.bzl#L31).
