/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.sync.workspace;

import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.base.bazel.BuildSystemProvider;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.primitives.ExecutionRootPath;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot;
import com.google.idea.blaze.base.settings.Blaze;
import com.google.idea.blaze.base.settings.BuildSystem;
import com.google.idea.blaze.base.sync.data.BlazeProjectDataManager;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Converts execution-root-relative paths to absolute files with a minimum of file system calls
 * (typically none).
 *
 * <p>Files which exist both underneath the execution root and within a workspace will be resolved
 * to paths within their workspace. This prevents those paths from being broken when a different
 * target is built.
 */
public class ExecutionRootPathResolver {

  private final ImmutableList<String> buildArtifactDirectories;
  private final File executionRoot;
  private final WorkspacePathResolver workspacePathResolver;

  public ExecutionRootPathResolver(
      BuildSystem buildSystem,
      WorkspaceRoot workspaceRoot,
      File executionRoot,
      WorkspacePathResolver workspacePathResolver) {
    this.buildArtifactDirectories = buildArtifactDirectories(buildSystem, workspaceRoot);
    this.executionRoot = executionRoot;
    this.workspacePathResolver = workspacePathResolver;
  }

  @Nullable
  public static ExecutionRootPathResolver fromProject(Project project) {
    BlazeProjectData projectData =
        BlazeProjectDataManager.getInstance(project).getBlazeProjectData();
    if (projectData == null) {
      return null;
    }
    return new ExecutionRootPathResolver(
        Blaze.getBuildSystem(project),
        WorkspaceRoot.fromProject(project),
        projectData.getBlazeInfo().getExecutionRoot(),
        projectData.getWorkspacePathResolver());
  }

  private static ImmutableList<String> buildArtifactDirectories(
      BuildSystem buildSystem, WorkspaceRoot workspaceRoot) {
    BuildSystemProvider provider = BuildSystemProvider.getBuildSystemProvider(buildSystem);
    if (provider == null) {
      provider = BuildSystemProvider.defaultBuildSystem();
    }
    return provider.buildArtifactDirectories(workspaceRoot);
  }

  public File resolveExecutionRootPath(ExecutionRootPath path) {
    if (path.isAbsolute()) {
      return path.getAbsoluteOrRelativeFile();
    }
    if (isInWorkspace(path)) {
      return workspacePathResolver.resolveToFile(path.getAbsoluteOrRelativeFile().getPath());
    }
    return convertExternalToStable(path.getFileRootedAt(executionRoot));
  }

  /**
   * This method should be used for directories. Returns all workspace files corresponding to the
   * given execution-root-relative path. If the file does not exist inside a workspace (e.g. for
   * blaze output files), returns the path rooted in the execution root.
   */
  public ImmutableList<File> resolveToIncludeDirectories(ExecutionRootPath path) {
    if (path.isAbsolute()) {
      return ImmutableList.of(path.getAbsoluteOrRelativeFile());
    }
    if (isInWorkspace(path)) {
      WorkspacePath workspacePath =
          WorkspacePath.createIfValid(path.getAbsoluteOrRelativeFile().getPath());
      if (workspacePath != null) {
        return workspacePathResolver.resolveToIncludeDirectories(workspacePath);
      } else {
        return ImmutableList.of();
      }
    }
    return ImmutableList.of(convertExternalToStable(path.getFileRootedAt(executionRoot)));
  }

  public File getExecutionRoot() {
    return executionRoot;
  }

  private boolean isInWorkspace(ExecutionRootPath path) {
    String firstPathComponent = getFirstPathComponent(path.getAbsoluteOrRelativeFile().getPath());
    return !buildArtifactDirectories.contains(firstPathComponent)
        && !isExternalWorkspacePath(firstPathComponent);
  }

  private static String getFirstPathComponent(String path) {
    int index = path.indexOf(File.separatorChar);
    return index == -1 ? path : path.substring(0, index);
  }

  private static boolean isExternalWorkspacePath(String firstPathComponent) {
    return firstPathComponent.equals("external");
  }

  /**
   * Converts external paths in unstable locations under execroot that may change on the next build
   * to stable ones under their external workspace.
   * That is, converts paths under <outputBase>/execroot/<workspace_name>/external/ to paths under
   * <outputBase>/external/.
   * Safe to call on all paths; non-external paths are left as they are.
   * See https://github.com/bazelbuild/intellij/issues/2766 for more details.
   */
  private static File convertExternalToStable(File f) {
    String regexSep = Pattern.quote(f.separator);
    // Workspace name defaults to __main__ per DEFAULT_REPOSITORY_DIRECTORY in https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/cmdline/LabelConstants.java
    // Valid sorkspace name regex copied from LEGAL_WORKSPACE_NAME in https://github.com/bazelbuild/bazel/blob/9302ebd906a2f5e9678f994efb2fbc8abab544c0/src/main/java/com/google/devtools/build/lib/packages/WorkspaceGlobals.java
    String regexUnstableExecrootSubpath = // ↓ matches workspace name.
      regexSep + "execroot" + regexSep + "(__main__|\\p{Alpha}[-.\\w]*)" + regexSep + "external" + regexSep;
    String stableExternalSubpath = f.separator + "external" + f.separator;
    return new File(f.getAbsolutePath().replaceFirst(regexUnstableExecrootSubpath, stableExternalSubpath));
  }
}
