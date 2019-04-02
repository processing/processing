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


/**
 * Strategy to describe an issue in an assignment.
 */
public class AssignmentMessageSimplifierStrategy extends RegexTemplateMessageSimplifierStrategy {

  @Override
  public String getRegexPattern() {
    return "\\s*[0-9a-zA-Z\\_<>]+\\s*=\\s*.*";
  }

  @Override
  public String getHintTemplate() {
    return MessageSimplifierUtil.getLocalStr("editor.status.bad.assignment");
  }

}
