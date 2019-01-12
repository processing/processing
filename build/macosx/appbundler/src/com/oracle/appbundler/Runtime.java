/*
 * Copyright 2012, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.appbundler;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

public class Runtime extends FileSet {

    /* Override to provide canonical path so that runtime can be specified
     * via a version-agnostic path (relative link, e.g. `current-jre`) while
     * still preserving the original runtime directory name, e.g. `jre1.8.0_45.jre`.
     */
    @Override
    public File getDir() {
        File dir = super.getDir();
        try {
            return dir.getCanonicalFile();
        } catch (IOException e) {
            return dir;
        }
    }

    private void detectType() {
        boolean isJDK = new File(getDir(), "jre").isDirectory();
        
        if (isJDK) {
            appendIncludes(new String[] {
                    "jre/lib/",
                    "jreCOPYRIGHT",
                    "jre/LICENSE",
                    "jre/README",
                    "jre/THIRDPARTYLICENSEREADME-JAVAFX.txt",
                    "jre/THIRDPARTYLICENSEREADME.txt",
                    "jre/Welcome.html"
            });
            appendExcludes(new String[] {
                    "jre/lib/deploy/",
                    "jre/lib/deploy.jar",
                    "jre/lib/javaws.jar",
                    "jre/lib/libdeploy.dylib",
                    "jre/lib/libnpjp2.dylib",
                    "jre/lib/plugin.jar",
                    "jre/lib/security/javaws.policy"
                });
        } else {
            appendIncludes(new String[] {
                    "lib/",
                    "COPYRIGHT",
                    "LICENSE",
                    "README",
                    "THIRDPARTYLICENSEREADME-JAVAFX.txt",
                    "THIRDPARTYLICENSEREADME.txt",
                    "Welcome.html"
            });
        }
    }

    void copyTo(File targetDir) throws IOException {
        detectType();
        
        File runtimeHomeDirectory = getDir();
        File runtimeContentsDirectory = runtimeHomeDirectory.getParentFile();
        File runtimeDirectory = runtimeContentsDirectory.getParentFile();

        // Create root plug-in directory
        File pluginDirectory = new File(targetDir, runtimeDirectory.getName());
        pluginDirectory.mkdir();

        // Create Contents directory
        File pluginContentsDirectory = new File(pluginDirectory, runtimeContentsDirectory.getName());
        pluginContentsDirectory.mkdir();

        // Copy MacOS directory
        File runtimeMacOSDirectory = new File(runtimeContentsDirectory, "MacOS");
        AppBundlerTask.copy(runtimeMacOSDirectory, new File(pluginContentsDirectory, runtimeMacOSDirectory.getName()));


        // Copy Info.plist file
        File runtimeInfoPlistFile = new File(runtimeContentsDirectory, "Info.plist");
        AppBundlerTask.copy(runtimeInfoPlistFile, new File(pluginContentsDirectory, runtimeInfoPlistFile.getName()));

        // Copy included contents of Home directory
        File pluginHomeDirectory = new File(pluginContentsDirectory, runtimeHomeDirectory.getName());

        DirectoryScanner directoryScanner = getDirectoryScanner(getProject());
        String[] includedFiles = directoryScanner.getIncludedFiles();

        for (int i = 0; i < includedFiles.length; i++) {
            String includedFile = includedFiles[i];
            File source = new File(runtimeHomeDirectory, includedFile);
            File destination = new File(pluginHomeDirectory, includedFile);
            AppBundlerTask.copy(source, destination);
        }
    }
}
