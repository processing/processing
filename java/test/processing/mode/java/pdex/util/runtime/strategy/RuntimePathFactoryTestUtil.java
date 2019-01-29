/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2019 The Processing Foundation

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

package processing.mode.java.pdex.util.runtime.strategy;

import org.mockito.Mockito;
import processing.app.Library;
import processing.app.Sketch;
import processing.app.SketchException;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.ImportStatement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;


public class RuntimePathFactoryTestUtil {

  public static JavaMode createTestJavaMode() throws SketchException {
    JavaMode mode = Mockito.mock(JavaMode.class);

    List<Library> fakeCoreLibraries = new ArrayList<>();
    Library testLib1 = createTestLibrary("library1");
    Library testLib2 = createTestLibrary("library2");
    fakeCoreLibraries.add(testLib1);
    fakeCoreLibraries.add(testLib2);
    mode.coreLibraries = fakeCoreLibraries;

    List<Library> fakeContribLibraries = new ArrayList<>();
    Library testLib3 = createTestLibrary("library3");
    Library testLib4 = createTestLibrary("java.library4");
    Library testLib5 = createTestLibrary("library5");
    fakeContribLibraries.add(testLib3);
    fakeContribLibraries.add(testLib4);
    fakeContribLibraries.add(testLib5);
    mode.contribLibraries = fakeContribLibraries;

    Library testLib6 = createTestLibrary("library6");
    Library testLib7 = createTestLibrary("javax.library7");
    Library testLib8 = createTestLibrary("library8");
    StringJoiner searchPathBuilder = new StringJoiner(File.pathSeparator);
    searchPathBuilder.add("library6");
    searchPathBuilder.add("javax.library7");
    searchPathBuilder.add("library8");
    Mockito.when(mode.getSearchPath()).thenReturn(searchPathBuilder.toString());

    Mockito.when(mode.getLibrary("library1")).thenReturn(testLib1);
    Mockito.when(mode.getLibrary("library2")).thenReturn(testLib2);
    Mockito.when(mode.getLibrary("library3")).thenReturn(testLib3);
    Mockito.when(mode.getLibrary("java.library4")).thenReturn(testLib4);
    Mockito.when(mode.getLibrary("library5")).thenReturn(testLib5);
    Mockito.when(mode.getLibrary("library6")).thenReturn(testLib6);
    Mockito.when(mode.getLibrary("javax.library7")).thenReturn(testLib7);
    Mockito.when(mode.getLibrary("library8")).thenReturn(testLib8);

    return mode;
  }

  public static List<ImportStatement> createTestImports() {
    List<ImportStatement> importStatements = new ArrayList<>();

    importStatements.add(createTestImportStatement("library3"));
    importStatements.add(createTestImportStatement("java.library4"));
    importStatements.add(createTestImportStatement("library5"));

    return importStatements;
  }

  public static Sketch createTestSketch() throws IOException {
    Sketch retSketch = Mockito.mock(Sketch.class);

    File fakeCodeFolder = createFakeCodeFolder();

    Mockito.when(retSketch.hasCodeFolder()).thenReturn(true);
    Mockito.when(retSketch.getCodeFolder()).thenReturn(fakeCodeFolder);

    return retSketch;
  }

  private static File createFakeCodeFolder() throws IOException {
    File fakeCodeFolder = Mockito.mock(File.class);

    Mockito.when(fakeCodeFolder.getCanonicalPath()).thenReturn("testdir");
    Mockito.when(fakeCodeFolder.list()).thenReturn(
        new String[]{"file1.jar", "file2.txt", "file3.zip"}
    );

    return fakeCodeFolder;
  }

  private static Library createTestLibrary(String libraryClassPath) {
    Library fakeLibrary = Mockito.mock(Library.class);
    Mockito.when(fakeLibrary.getClassPath()).thenReturn(libraryClassPath);
    return fakeLibrary;
  }

  private static ImportStatement createTestImportStatement(String packageName) {
    ImportStatement retStatement = Mockito.mock(ImportStatement.class);
    Mockito.when(retStatement.getPackageName()).thenReturn(packageName);
    return retStatement;
  }

}
