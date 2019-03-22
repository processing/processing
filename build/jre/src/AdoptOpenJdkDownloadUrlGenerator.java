/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2019 The Processing Foundation

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
 * Utility to generate download URLs from AdoptOpenJDK.
 */
public class AdoptOpenJdkDownloadUrlGenerator extends DownloadUrlGenerator {

  private static final String BASE_URL = "https://github.com/AdoptOpenJDK/openjdk%d-binaries/releases/download/jdk-%d.%d.%d%%2B%d/OpenJDK%dU-%s_%d.%d.%d_%d.%s";

  @Override
  public String buildUrl(String platform, String component, int train, int version, int update,
      int build, String flavor, String hash) {

    if (!component.equalsIgnoreCase("jdk")) {
      throw new RuntimeException("Can only generate JDK download URLs for AdoptOpenJDK.");
    }

    String filename = buildDownloadRemoteFilename(platform);
    String fileExtension = buildFileExtension(platform);
    return String.format(
        BASE_URL,
        train,
        train,
        version,
        update,
        build,
        train,
        filename,
        train,
        version,
        update,
        build,
        fileExtension
    );
  }

  /**
   * Build a the filename (the "flavor") that is expected on AdoptOpenJDK.
   *
   * @param downloadPlatform The platform for which the download URL is being generated like
   *    "macos" or "linux64".
   * @return The artifact name without extension like "jdk_x64_mac_hotspot".
   */
  private String buildDownloadRemoteFilename(String downloadPlatform) {
    switch (downloadPlatform.toLowerCase()) {
      case "windows32": return "jdk_x86-32_windows_hotspot";
      case "windows64": return "jdk_x64_windows_hotspot";
      case "macosx64": return "jdk_x64_mac_hotspot";
      case "linux32": throw new RuntimeException("Linux32 not supported by AdoptOpenJDK.");
      case "linux64": return "jdk_x64_linux_hotspot";
      case "linuxarm": return "jdk_aarch64_linux_hotspot";
      default: throw new RuntimeException("Unknown platform: " + downloadPlatform);
    }
  }

  /**
   * Determine the download file extension.
   *
   * @param downloadPlatform The platform for which the download URL is being generated like
   *    "macos" or "linux64".
   * @return The file extension without leading period like "zip" or "tar.gz".
   */
  private String buildFileExtension(String downloadPlatform) {
    return downloadPlatform.startsWith("windows") ? "zip" : "tar.gz";
  }
}
