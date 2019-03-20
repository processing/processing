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
 * Data structure describing an item to be downloaded as part the Processing build.
 */
public class DownloadItem {

  private String url;
  private String localPath;
  private Optional<String> cookie;

  /**
   * Create a new download item.
   *
   * @param newUrl The remote URL at which the download can be found (via GET).
   * @param newLocalPath The local path to which the file should be written.
   * @param newCookie Optional cookie that, if present, should be given to the server along with the
   *    GET request for the download contents.
   */
  public DownloadItem(String newUrl, String newLocalPath, Optional<String> newCookie) {
    url = newUrl;
    localPath = newLocalPath;
    cookie = newCookie;
  }

  /**
   * Determine where the download can be requested.
   *
   * @return The remote URL at which the download can be found (via GET).
   */
  public String getUrl() {
    return url;
  }

  /**
   * Determine to where the download should be saved.
   *
   * @return The local path to which the file should be written.
   */
  public String getLocalPath() {
    return localPath;
  }

  /**
   * Determine if and what cookie should be given as part of the download request.
   *
   * @return Optional cookie that, if present, should be given to the server along with the
   *    GET request for the download contents. Should be ignored otherwise.
   */
  public Optional<String> getCookie() {
    return cookie;
  }


}
