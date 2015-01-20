/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.tweak;

import processing.app.Base;
import processing.app.Editor;
import processing.mode.java.JavaToolbar;

public class TweakToolbar extends JavaToolbar {

	static protected final int RUN    = 0;
	static protected final int STOP   = 1;

	static protected final int NEW    = 2;
	static protected final int OPEN   = 3;
	static protected final int SAVE   = 4;
	static protected final int EXPORT = 5;

	public TweakToolbar(Editor editor, Base base) {
		super(editor, base);
	}
}
