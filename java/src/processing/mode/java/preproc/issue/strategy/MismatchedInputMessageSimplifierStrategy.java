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
 * Strategy to explain a mismatched input issue.
 */
public class MismatchedInputMessageSimplifierStrategy implements PreprocIssueMessageSimplifierStrategy {

  private static final String PARSER_STR = "mismatched input '(.*)' expecting ";
  private final Pattern parser;

  /**
   * Create a new strategy for mismatched input.
   */
  public MismatchedInputMessageSimplifierStrategy() {
    parser = Pattern.compile(PARSER_STR);
  }

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    if (message.toLowerCase().contains("mismatched input")) {
      Matcher matcher = parser.matcher(message);

      String newMessage = String.format(
          MessageSimplifierUtil.getLocalStr("editor.status.mismatched"),
          matcher.find() ? matcher.group(1) : message
      );

      return Optional.of(
          new IssueMessageSimplification(
              newMessage,
              true
          )
      );
    } else {
      return Optional.empty();
    }
  }

}
