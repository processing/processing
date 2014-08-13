/*
 * Copyright (C) 2003-2006, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import com.illposed.osc.utility.OSCJavaToByteArrayConverter;

/**
 * OSCPacket is the abstract superclass for the various
 * kinds of OSC Messages.
 *
 * The actual packets are:
 * <ul>
 * <li>{@link OSCMessage}: simple OSC messages
 * <li>{@link OSCBundle}: OSC messages with timestamps
 *   and/or made up of multiple messages
 * </ul>
 *
 * This implementation is based on
 * <a href="http://www.emergent.de/Goodies/">Markus Gaelli</a> and
 * Iannis Zannos' OSC implementation in Squeak Smalltalk.
 */
public abstract class OSCPacket {

	private boolean isByteArrayComputed;
	private byte[] byteArray;

	/**
	 * Default constructor for the abstract class
	 */
	public OSCPacket() {
	}

	/**
	 * Generate a representation of this packet conforming to the
	 * the OSC byte stream specification. Used Internally.
	 */
	protected byte[] computeByteArray() {
		OSCJavaToByteArrayConverter stream = new OSCJavaToByteArrayConverter();
		return computeByteArray(stream);
	}

	/**
	 * Subclasses should implement this method to product a byte array
	 * formatted according to the OSC specification.
	 * @param stream OscPacketByteArrayConverter
	 */
	protected abstract byte[] computeByteArray(OSCJavaToByteArrayConverter stream);

	/**
	 * Return the OSC byte stream for this packet.
	 * @return byte[]
	 */
	public byte[] getByteArray() {
		if (!isByteArrayComputed) {
			byteArray = computeByteArray();
		}
		return byteArray;
	}

	/**
	 * Run any post construction initialization. (By default, do nothing.)
	 */
	protected void init() {

	}
}
