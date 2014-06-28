/**
 *
 * Copyright ##copyright## ##author##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author      ##Wilm Thoben##
 * @modified    ##10/23/2013##
 */

package processing.sound;
import processing.core.*;

public class Sound{
	
	// myParent is a reference to the parent sketch
	PApplet parent;
	MethClaInterface methCla;
	
	public final static String VERSION = "##library.prettyVersion##";
	
	public Sound(PApplet parent, int sampleRate, int bufferSize) {
		this.parent = parent;
		parent.registerMethod("dispose", this);
		welcome();
		methCla = new MethClaInterface();
		methCla.engineNew(sampleRate, bufferSize);
		methCla.engineStart();
	}

	public Sound(PApplet theParent) {
		this(theParent, 44100, 512);	
	}
	
	private void welcome() {
		System.out.println("##library.name## ##library.prettyVersion## by ##author##");
	}
	
	public void engineStop() {
		methCla.engineStop();
	}
	
	public static String version() {
		return VERSION;
	}
	
	public void dispose() {
		methCla.engineStop();
	}
}

