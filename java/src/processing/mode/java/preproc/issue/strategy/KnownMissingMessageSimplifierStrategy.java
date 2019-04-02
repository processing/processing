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

import processing.mode.java.preproc.issue.IssueMessageSimplification;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Strategy to handle missing token messages.
 */
public class KnownMissingMessageSimplifierStrategy implements PreprocIssueMessageSimplifierStrategy {

  private static final String PARSE_PATTERN_STR = ".*missing '(.*)' at .*";

  private final Pattern parsePattern;

  public KnownMissingMessageSimplifierStrategy() {
    parsePattern = Pattern.compile(PARSE_PATTERN_STR);
  }

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    if (message.toLowerCase().contains("missing")) {
      String missingPiece;
      Matcher matcher = parsePattern.matcher(message);
      if (matcher.find()) {
        missingPiece = matcher.group(1);
      } else {
        missingPiece = "character";
      }

      String langTemplate = MessageSimplifierUtil.getLocalStr("editor.status.missing.default")
          .replace("%c", "%s");

      String newMessage = String.format(langTemplate, missingPiece);

      return Optional.of(
          new IssueMessageSimplification(newMessage, true)
      );
    } else {
      return Optional.empty();
    }
  }

}
