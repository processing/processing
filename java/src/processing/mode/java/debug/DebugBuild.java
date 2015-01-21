/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.debug;

import java.io.File;

import processing.app.Sketch;
import processing.app.SketchException;
import processing.mode.java.JavaBuild;
import processing.mode.java.pdex.ExperimentalMode;

/**
 * Copied from processing.mode.java.JavaBuild, just changed compiler.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class DebugBuild extends JavaBuild {

    public DebugBuild(Sketch sketch) {
        super(sketch);
    }

    /**
     * Preprocess and compile sketch. Copied from
     * processing.mode.java.JavaBuild, just changed compiler.
     *
     * @param srcFolder
     * @param binFolder
     * @param sizeWarning
     * @return main class name or null on compile failure
     * @throws SketchException
     */
    @Override
    public String build(File srcFolder, File binFolder, boolean sizeWarning) throws SketchException {
        this.srcFolder = srcFolder;
        this.binFolder = binFolder;

//    Base.openFolder(srcFolder);
//    Base.openFolder(binFolder);

        // run the preprocessor
        String classNameFound = preprocess(srcFolder, sizeWarning);

        // compile the program. errors will happen as a RunnerException
        // that will bubble up to whomever called build().
//    Compiler compiler = new Compiler(this);
//    String bootClasses = System.getProperty("sun.boot.class.path");
//    if (compiler.compile(this, srcFolder, binFolder, primaryClassName, getClassPath(), bootClasses)) {

        if (Compiler.compile(this)) { // use compiler with debug info enabled (-g switch flicked)
            sketchClassName = classNameFound;
            return classNameFound;
        }
        return null;
    }

    public ExperimentalMode getMode() {
        return (ExperimentalMode)mode;
    }
}
