/*
 * Copyright 2012, The Infinite Kind and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  The Infinite Kind designates this
 * particular file as subject to the "Classpath" exception as provided
 * by The Infinite Kind in the LICENSE file that accompanied this code.
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
 */

package com.oracle.appbundler;

import java.io.File;

import org.apache.tools.ant.BuildException;


/**
 * Represent a CFBundleDocument.
 */
public class BundleDocument {
    private String name = "editor";
    private String role = "";
    private File icon = null;
    private String[] extensions;
    private boolean isPackage = false;

    static private String capitalizeFirst(String string) {
        char[] stringArray = string.toCharArray();
        stringArray[0] = Character.toUpperCase(stringArray[0]);
        return new String(stringArray);
    }
    
    public void setExtensions(String extensionsList) {
        if(extensionsList == null) {
            throw new BuildException("Extensions can't be null");
        }
        
        extensions = extensionsList.split(",");
        for (String extension : extensions) {
            extension = extension.trim().toLowerCase();
        }
    }
    
    public void setIcon(File icon) {
      this.icon = icon;
    }
    
    public void setName(String name) {
      this.name = name;
    }

    public void setRole(String role) {
      this.role = capitalizeFirst(role);
    }
    
    public void setIsPackage(String isPackageString) {
        if(isPackageString.trim().equalsIgnoreCase("true")) {
            this.isPackage = true;
        } else {
            this.isPackage = false;
        }
    }
    
//    public String getIcon() {
//        return icon;
//    }
    
    public String getIconName() {
        return icon.getName();
    }
    
    
    public File getIconFile() {
        return icon;
    }
    
    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
    
    public String[] getExtensions() {
        return extensions;
    }
    
    public boolean hasIcon() {
        return icon != null;
    }
    
    public boolean isPackage() {
        return isPackage;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(getName());
        s.append(" ").append(getRole()).append(" ").append(getIconName()). append(" ");
        for(String extension : extensions) {
            s.append(extension).append(" ");
        }
        
        return s.toString();
    }
}
