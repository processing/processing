/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package processing.mode.experimental;

/**
 * A Listener for line number changes.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public interface LineListener {

    /**
     * Event handler for line number changes (due to editing).
     *
     * @param line the line that has changed
     * @param oldLineIdx the old line index (0-based)
     * @param newLineIdx the new line index (0-based)
     */
    void lineChanged(LineID line, int oldLineIdx, int newLineIdx);
}
