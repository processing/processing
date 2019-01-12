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

/**
 * Class representing an option that will be passed to the JVM at startup.
 * The class can optionally be named, which allows the bundled Java program
 * itself to override the option. Changes will take effect upon restart of the
 * application.<p>
 * Assuming your {@code CFBundleIdentifier} (settable via {@link AppBundlerTask#setIdentifier(String)})
 * is {@code com.oracle.appbundler}. Then you can override a named option by calling
 * <xmp>
 *     import java.util.prefs.Preferences;
 *     [...]
 *     Preferences jvmOptions = Preferences.userRoot().node("/com/oracle/appbundler/JVMOptions");
 *     jvmOptions.put("name", "value");
 *     jvmOptions.flush();
 * </xmp>
 * The corresponding entries will be stored in a file called
 * {@code ~/Library/Preferences/com.oracle.appbundler.plist}.
 * To manipulate the file without Java's {@link java.util.prefs.Preferences} from the command line,
 * you should use the tool
 * <a href="https://developer.apple.com/library/mac/documentation/Darwin/Reference/ManPages/man1/defaults.1.html">defaults</a>.
 * For example, to add an entry via the command line, use:
 * <xmp>
 *     defaults write com.oracle.appbundler /com/oracle/appbundler/ -dict-add JVMOptions/ '{"name"="value";}'
 * </xmp>
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a> (preference related code only)
 */
public class Option {
    private String value = null;
    private String name = null;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name == null ? value : name + "=" + value;
    }
}
