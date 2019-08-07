/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GluonHqDownloadUrlGeneratorTest {

  private static final String EXPECTED_WIN64_URL = "http://gluonhq.com/download/javafx-11-0-2-sdk-windows/";
  private static final String EXPECTED_MAC_URL = "http://gluonhq.com/download/javafx-11-0-2-sdk-mac";
  private static final String EXPECTED_LINUX_URL = "http://gluonhq.com/download/javafx-11-0-2-sdk-linux/";

  private static final String COMPONENT = "jfx";
  private static final int TRAIN = 11;
  private static final int VERSION = 0;
  private static final int UPDATE = 2;
  private static final int BUILD = 0;
  private static final String FLAVOR_SUFFIX = ".zip";
  private static final String HASH = "";

  private AdoptOpenJdkDownloadUrlGenerator urlGenerator;

  @Before
  public void setUp() throws Exception {
    urlGenerator = new AdoptOpenJdkDownloadUrlGenerator();
  }

  @Test
  public void testBuildUrlWindows() {
    String url = urlGenerator.buildUrl(
        "windows64",
        COMPONENT,
        TRAIN,
        VERSION,
        UPDATE,
        BUILD,
        "windows" + FLAVOR_SUFFIX,
        HASH
    );

    assertEquals(
        EXPECTED_WIN64_URL,
        url
    );
  }

  @Test
  public void testBuildUrlMac() {
    String url = urlGenerator.buildUrl(
        "macosx64",
        COMPONENT,
        TRAIN,
        VERSION,
        UPDATE,
        BUILD,
        "mac" + FLAVOR_SUFFIX,
        HASH
    );

    assertEquals(
        EXPECTED_MAC_URL,
        url
    );
  }

  @Test
  public void testBuildUrlLinux() {
    String url = urlGenerator.buildUrl(
        "linux64",
        COMPONENT,
        TRAIN,
        VERSION,
        UPDATE,
        BUILD,
        "linux64" + FLAVOR_SUFFIX,
        HASH
    );

    assertEquals(
        EXPECTED_LINUX_URL,
        url
    );
  }

}
