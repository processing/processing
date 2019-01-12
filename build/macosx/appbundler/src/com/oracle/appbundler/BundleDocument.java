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
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;


/**
 * Represent a CFBundleDocument.
 */
public class BundleDocument implements IconContainer {
    private String name = null;
    private String role = "Editor";
    private String icon = null;
    private String handlerRank = null;
    private List<String> extensions;
    private List<String> contentTypes;
    private List<String> exportableTypes;
    private boolean isPackage = false;

    private String capitalizeFirst(String string) {
        char[] stringArray = string.toCharArray();
        stringArray[0] = Character.toUpperCase(stringArray[0]);
        return new String(stringArray);
    }
    
    public void setExtensions(String extensionsString) {
        extensions = getListFromCommaSeparatedString(extensionsString, "Extensions", true);
    }

    public void setContentTypes(String contentTypesString) {
        contentTypes = getListFromCommaSeparatedString(contentTypesString, "Content Types");
    }

    public void setExportableTypes(String exportableTypesString) {
        exportableTypes = getListFromCommaSeparatedString(exportableTypesString, "Exportable Types");
    }

    public static List<String> getListFromCommaSeparatedString(String listAsString,
            final String attributeName) {
        return getListFromCommaSeparatedString(listAsString, attributeName, false);
    }
            
    public static List<String> getListFromCommaSeparatedString(String listAsString,
            final String attributeName, final boolean lowercase) {
        if(listAsString == null) {
            throw new BuildException(attributeName + " can't be null");
        }
        
        String[] splittedListAsString = listAsString.split(",");
        List<String> stringList = new ArrayList<String>();
        
        for (String extension : splittedListAsString) {
            String cleanExtension = extension.trim();
            if (lowercase) {
                cleanExtension = cleanExtension.toLowerCase();
            }
            if (cleanExtension.length() > 0) {
                stringList.add(cleanExtension);
            }
        }
        
        if (stringList.size() == 0) {
            throw new BuildException(attributeName + " list must not be empty");
        }
        return stringList;
    }
    
    public void setIcon(String icon) {
      this.icon = icon;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setRole(String role) {
      this.role = capitalizeFirst(role);
    }
    
    public void setHandlerRank(String handlerRank) {
      this.handlerRank = capitalizeFirst(handlerRank);
    } 
      
    public void setIsPackage(String isPackageString) {
        if(isPackageString.trim().equalsIgnoreCase("true")) {
            this.isPackage = true;
        } else {
            this.isPackage = false;
        }
    }
    
    public String getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getHandlerRank() {
        return handlerRank;
    }
    
    public List<String> getExtensions() {
        return extensions;
    }
    
    public List<String> getContentTypes() {
        return contentTypes;
    }
    
    public List<String> getExportableTypes() {
        return exportableTypes;
    }
    
    public File getIconFile() {
        if (icon == null) { return null; }

        File ifile = new File (icon);
        
        if (! ifile.exists ( ) || ifile.isDirectory ( )) { return null; }

        return ifile;
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
        s.append(" ").append(getRole())
        .append(" ").append(getIcon())
        .append(" ").append(getHandlerRank())
        .append(" ");
        if (contentTypes != null) {
            for(String contentType : contentTypes) {
                s.append(contentType).append(" ");
            }
        }
        if (extensions != null) {
            for(String extension : extensions) {
                s.append(extension).append(" ");
            }
        }
        if (exportableTypes != null) {
            for(String exportableType : exportableTypes) {
                s.append(exportableType).append(" ");
            }
        }
        
        return s.toString();
    }
}
