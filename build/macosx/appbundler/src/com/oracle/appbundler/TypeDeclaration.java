/*
 * Copyright 2015, Quality First Software GmbH and/or its affiliates. All rights reserved.
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
 */

package com.oracle.appbundler;

import static com.oracle.appbundler.BundleDocument.getListFromCommaSeparatedString;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Class representing an UTExportedTypeDeclaration or UTImportedTypeDeclaration in Info.plist
 */
public class TypeDeclaration implements IconContainer {

    private boolean imported = false;
    private String identifier = null;
    private String referenceUrl = null;
    private String description = null;
    private String icon = null;
    private List<String> conformsTo = null;
    private List<String> osTypes = null;
    private List<String> mimeTypes = null;
    private List<String> extensions = null;

    public TypeDeclaration() {
        this.conformsTo = Arrays.asList(new String[]{"public.data"});
    }
    
    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getReferenceUrl() {
        return referenceUrl;
    }

    public void setReferenceUrl(String referenceUrl) {
        this.referenceUrl = referenceUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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
    
    public List<String> getConformsTo() {
        return conformsTo;
    }

    public void setConformsTo(String conformsToAsString) {
        this.conformsTo = getListFromCommaSeparatedString(conformsToAsString, "Conforms To");
    }

    public List<String> getOsTypes() {
        return osTypes;
    }

    public void setOsTypes(String osTypesAsString) {
        this.osTypes = getListFromCommaSeparatedString(osTypesAsString, "OS Types");
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(String mimeTypesAsString) {
        this.mimeTypes = getListFromCommaSeparatedString(mimeTypesAsString, "Mime Types", true);
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(String extensionsAsString) {
        this.extensions = getListFromCommaSeparatedString(extensionsAsString, "Extensions", true);
    }

    @Override
    public String toString() {
        return "" + imported;
    }
}
