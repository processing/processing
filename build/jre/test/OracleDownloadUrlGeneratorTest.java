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
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;

public class OracleDownloadUrlGeneratorTest {

  private static final String EXPECTED_URL = "http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-macosx-x64.dmg";

  private OracleDownloadUrlGenerator urlGenerator;

  @Before
  public void setUp() throws Exception {
    urlGenerator = new OracleDownloadUrlGenerator();
  }

  @Test
  public void testBuildUrl() {
    String url = urlGenerator.buildUrl(
      "macos",
      "jdk",
      1,
      8,
      131,
      11,
      "macosx-x64.dmg",
      "d54c1d3a095b4ff2b6607d096fa80163"
    );

    assertEquals(
      EXPECTED_URL,
      url
    );
  }

}
