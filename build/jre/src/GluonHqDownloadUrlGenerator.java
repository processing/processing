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
 * URL generator for downloading JFX directly from OpenJFX's sponsor Gluon.
 */
public class GluonHqDownloadUrlGenerator extends DownloadUrlGenerator {

  private static final String TEMPLATE_URL = "http://gluonhq.com/download/javafx-%d-%d-%d-sdk-%s/";

  @Override
  public String buildUrl(String platform, String component, int train, int version, int update,
      int build, String flavor, String hash) {

      String platformLower = platform.toLowerCase();

      String platformShort;
      if (platformLower.contains("linux")) {
        platformShort = "linux";
      } else if (platformLower.contains("mac")) {
        platformShort = "mac";
      } else if (platformLower.contains("windows")) {
        platformShort = "windows";
      } else {
        throw new RuntimeException("Unsupported platform for JFX: " + platform);
      }

      return String.format(TEMPLATE_URL, train, version, update, platformShort);
  }

}
