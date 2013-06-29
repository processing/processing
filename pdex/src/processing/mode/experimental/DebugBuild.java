/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package processing.mode.experimental;

import java.io.File;
import processing.app.Sketch;
import processing.app.SketchException;
import processing.mode.java.JavaBuild;

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
