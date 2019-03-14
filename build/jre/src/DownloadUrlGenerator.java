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


/**
 * Abstract base class for strategy to generate download URLs.
 */
public abstract class DownloadUrlGenerator {

  /**
   * Determine the URL at which the artifact can be downloaded.
   *
   * @param downloadPlatform The platform for which the download URL is being generated like
   *    "macos" or "linux64".
   * @param jdk Flag indicating if the JDK or JRE is being downloaded. True indicates that the
   *    JDK is being downloaded and false indicates JRE.
   * @param train The JDK train (like 1 or 11).
   * @param version The JDK version (like 8 or 1).
   * @param update The update (like 13).
   * @param build The build number (like 133).
   * @param flavor The flavor like "macosx-x64.dmg".
   * @param hash The hash like "d54c1d3a095b4ff2b6607d096fa80163".
   */
  public abstract String buildUrl(String platform, boolean jdk, int train, int version,
      int update, int build, String flavor, String hash);

  /**
   * Determine the name of the file to which the remote file should be saved.
   *
   * @param downloadPlatform The platform for which the download URL is being generated like
   *    "macos" or "linux64".
   * @param jdk Flag indicating if the JDK or JRE is being downloaded. True indicates that the
   *    JDK is being downloaded and false indicates JRE.
   * @param train The JDK train (like 1 or 11).
   * @param version The JDK version (like 8 or 1).
   * @param update The update (like 13).
   * @param build The build number (like 133).
   * @param flavor The flavor like "macosx-x64.dmg".
   * @param hash The hash like "d54c1d3a095b4ff2b6607d096fa80163".
   */
  public String getLocalFilename(String downloadPlatform, boolean jdk, int train, int version,
      int update, int build, String flavor, String hash) {

    String baseFilename = (jdk ? "jdk" : "jre");

    String versionStr;
    if (update == 0) {
      versionStr = String.format("-%d-%s", version, flavor);
    } else {
      versionStr = String.format("-%du%d-%s", version, update, flavor);
    }

    return baseFilename + versionStr;
  }

}
