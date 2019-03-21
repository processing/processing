/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2014-19 The Processing Foundation

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


import java.util.Optional;

/**
 * Utility to generate the download URL from Oracle.
 */
public class OracleDownloadUrlGenerator extends DownloadUrlGenerator {
  private static final String COOKIE =
      "oraclelicense=accept-securebackup-cookie";


  @Override
  public String buildUrl(String platform, String component, int train, int version, int update,
      int build, String flavor, String hash) {

    if (!component.equalsIgnoreCase("jdk")) {
      throw new RuntimeException("Can only generate JDK download URLs for Oracle.");
    }

    String filename = getLocalFilename(
        platform,
        component,
        train,
        version,
        update,
        build,
        flavor,
        hash
    );

    String url = "http://download.oracle.com/otn-pub/java/jdk/" +
        (update == 0 ?
            String.format("%d-b%02d/", version, build) :
            String.format("%du%d-b%02d/", version, update, build));

    // URL format changed starting with 8u121
    if (update >= 121) {
      url += hash + "/";
    }

    // Finally, add the filename to the end
    url += filename;

    return url;
  }

  public Optional<String> getCookie() {
    return Optional.of(COOKIE);
  }

}
