/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org

Copyright (c) 2012-19 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.mode.java.preproc.issue.strategy;

import processing.app.Language;
import processing.app.Platform;


/**
 * Convenience functions useful for generating simplified messages.
 */
public class MessageSimplifierUtil {

  /**
   * Get the snippet of "offending code" from an error message if given.
   *
   * @param area The area from which to extract the offending code.
   * @return The offending code described in the error message or the original message if the subset
   *    describing the offending code could not be found.
   */
  public static String getOffendingArea(String area) {
    return getOffendingArea(area, true);
  }

  /**
   * Get the snippet of "offending code" from an error message if given.
   *
   * @param area The area from which to extract the offending code.
   * @param removeNewline Flag indicating if newlines should be removed or not.
   * @return The offending code described in the error message or the original message if the subset
   *    describing the offending code could not be found.
   */
  public static String getOffendingArea(String area, boolean removeNewline) {
    if (!area.contains("viable alternative")) {
      return area;
    }

    String content = area.replace("no viable alternative at input \'", "");

    if (removeNewline) {
        content = content
            .replace("\n", "")
            .replace("\\n", "");
    }

    if (content.endsWith("'")) {
      return content.substring(0, content.length() - 1);
    } else {
      return content;
    }
  }

  /**
   * Generate an generic error message.
   *
   * @param unlocalized The unlocalized string. Will be included in resulting message but with
   *    surrounding localized text.
   * @return Semi-localized message.
   */
  public static String getLocalizedGenericError(String unlocalized) {
    String template = getLocalStr("editor.status.error_on");
    return String.format(template, unlocalized);
  }

  /**
   * Get a localized template string.
   *
   * @param stringName Name of the template.
   * @return The template's contents prior to rendering.
   */
  public static String getLocalStr(String stringName) {
    String errStr;
    String retStr;

    if (Platform.isInit()) {
      errStr = Language.text("editor.status.error.syntax");
      retStr = Language.text(stringName);
    } else {
      errStr = DefaultErrorLocalStrSet.get().get("editor.status.error.syntax").orElse("Error");
      retStr = DefaultErrorLocalStrSet.get().get(stringName).orElse(stringName);
    }

    return String.format(errStr, retStr);
  }
}
