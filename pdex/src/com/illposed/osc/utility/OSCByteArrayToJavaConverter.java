/*
 * Copyright (C) 2004-2006, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.utility;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacket;

/**
 * Utility class to convert a byte array,
 * conforming to the OSC byte stream format,
 * into Java objects.
 *
 * @author Chandrasekhar Ramakrishnan
 */
public class OSCByteArrayToJavaConverter {

	private byte[] bytes;
	private int bytesLength;
	private int streamPosition;

	/**
	 * Creates a helper object for converting from a byte array
	 * to an {@link OSCPacket} object.
	 */
	public OSCByteArrayToJavaConverter() {
	}

	/**
	 * Converts a byte array into an {@link OSCPacket}
	 * (either an {@link OSCMessage} or {@link OSCBundle}).
	 */
	public OSCPacket convert(byte[] byteArray, int bytesLength) {
		this.bytes = byteArray;
		this.bytesLength = bytesLength;
		this.streamPosition = 0;
		if (isBundle()) {
			return convertBundle();
		} else {
			return convertMessage();
		}
	}

	/**
	 * Is my byte array a bundle?
	 * @return true if it the byte array is a bundle, false o.w.
	 */
	private boolean isBundle() {
		// only need the first 7 to check if it is a bundle
		String bytesAsString = new String(bytes, 0, 7);
		return bytesAsString.startsWith("#bundle");
	}

	/**
	 * Converts the byte array to a bundle.
	 * Assumes that the byte array is a bundle.
	 * @return a bundle containing the data specified in the byte stream
	 */
	private OSCBundle convertBundle() {
		// skip the "#bundle " stuff
		streamPosition = 8;
		Date timestamp = readTimeTag();
		OSCBundle bundle = new OSCBundle(timestamp);
		OSCByteArrayToJavaConverter myConverter
				= new OSCByteArrayToJavaConverter();
		while (streamPosition < bytesLength) {
			// recursively read through the stream and convert packets you find
			int packetLength = ((Integer) readInteger()).intValue();
			byte[] packetBytes = new byte[packetLength];
			for (int i = 0; i < packetLength; i++) {
				packetBytes[i] = bytes[streamPosition++];
			}
			OSCPacket packet = myConverter.convert(packetBytes, packetLength);
			bundle.addPacket(packet);
		}
		return bundle;
	}

	/**
	 * Converts the byte array to a simple message.
	 * Assumes that the byte array is a message.
	 * @return a message containing the data specified in the byte stream
	 */
	private OSCMessage convertMessage() {
		OSCMessage message = new OSCMessage();
		message.setAddress(readString());
		List<Character> types = readTypes();
		if (null == types) {
			// we are done
			return message;
		}
		moveToFourByteBoundry();
		for (int i = 0; i < types.size(); ++i) {
			if ('[' == types.get(i).charValue()) {
				// we're looking at an array -- read it in
				message.addArgument(readArray(types, ++i).toArray());
				// then increment i to the end of the array
				while (types.get(i).charValue() != ']') {
					i++;
				}
			} else {
				message.addArgument(readArgument(types.get(i)));
			}
		}
		return message;
	}

	/**
	 * Reads a string from the byte stream.
	 * @return the next string in the byte stream
	 */
	private String readString() {
		int strLen = lengthOfCurrentString();
		char[] stringChars = new char[strLen];
		for (int i = 0; i < strLen; i++) {
			stringChars[i] = (char) bytes[streamPosition++];
		}
		moveToFourByteBoundry();
		return new String(stringChars);
	}

	/**
	 * Reads the types of the arguments from the byte stream.
	 * @return a char array with the types of the arguments
	 */
	private List<Character> readTypes() {
		// the next byte should be a ','
		if (bytes[streamPosition] != 0x2C) {
			return null;
		}
		streamPosition++;
		// find out how long the list of types is
		int typesLen = lengthOfCurrentString();
		if (0 == typesLen) {
			return null;
		}

		// read in the types
		List<Character> typesChars = new ArrayList<Character>(typesLen);
		for (int i = 0; i < typesLen; i++) {
			typesChars.add((char) bytes[streamPosition++]);
		}
		return typesChars;
	}

	/**
	 * Reads an object of the type specified by the type char.
	 * @param type type of the argument to read
	 * @return a Java representation of the argument
	 */
	private Object readArgument(char type) {
		switch (type) {
			case 'i' :
				return readInteger();
			case 'h' :
				return readBigInteger();
			case 'f' :
				return readFloat();
			case 'd' :
				return readDouble();
			case 's' :
				return readString();
			case 'c' :
				return readChar();
			case 'T' :
				return Boolean.TRUE;
			case 'F' :
				return Boolean.FALSE;
			case 't' :
				return readTimeTag();
			default:
				return null;
		}
	}

	/**
	 * Reads a char from the byte stream.
	 * @return a {@link Character}
	 */
	private Object readChar() {
		return new Character((char) bytes[streamPosition++]);
	}

	/**
	 * Reads a double from the byte stream.
	 * This just reads a float.
	 * @return a {@link Double}
	 */
	private Object readDouble() {
		return readFloat();
	}

	/**
	 * Reads a float from the byte stream.
	 * @return a {@link Float}
	 */
	private Object readFloat() {
		byte[] floatBytes = new byte[4];
		floatBytes[0] = bytes[streamPosition++];
		floatBytes[1] = bytes[streamPosition++];
		floatBytes[2] = bytes[streamPosition++];
		floatBytes[3] = bytes[streamPosition++];
//		int floatBits =
//			(floatBytes[0] << 24)
//				| (floatBytes[1] << 16)
//				| (floatBytes[2] << 8)
//				| (floatBytes[3]);
		BigInteger floatBits = new BigInteger(floatBytes);
		return new Float(Float.intBitsToFloat(floatBits.intValue()));
	}

	/**
	 * Reads a Big Integer (64 bit integer) from the byte stream.
	 * @return a {@link BigInteger}
	 */
	private Object readBigInteger() {
		byte[] longintBytes = new byte[8];
		longintBytes[0] = bytes[streamPosition++];
		longintBytes[1] = bytes[streamPosition++];
		longintBytes[2] = bytes[streamPosition++];
		longintBytes[3] = bytes[streamPosition++];
		longintBytes[4] = bytes[streamPosition++];
		longintBytes[5] = bytes[streamPosition++];
		longintBytes[6] = bytes[streamPosition++];
		longintBytes[7] = bytes[streamPosition++];
		return new BigInteger(longintBytes);
	}

	/**
	 * Reads an Integer (32 bit integer) from the byte stream.
	 * @return an {@link Integer}
	 */
	private Object readInteger() {
		byte[] intBytes = new byte[4];
		intBytes[0] = bytes[streamPosition++];
		intBytes[1] = bytes[streamPosition++];
		intBytes[2] = bytes[streamPosition++];
		intBytes[3] = bytes[streamPosition++];
		BigInteger intBits = new BigInteger(intBytes);
		return new Integer(intBits.intValue());
	}

	/**
	 * Reads the time tag and convert it to a Java Date object.
	 * A timestamp is a 64 bit number representing the time in NTP format.
	 * The first 32 bits are seconds since 1900, the second 32 bits are
	 * fractions of a second.
	 * @return a {@link Date}
	 */
	private Date readTimeTag() {
		byte[] secondBytes = new byte[8];
		byte[] fractionBytes = new byte[8];
		for (int i = 0; i < 4; i++) {
			// clear the higher order 4 bytes
			secondBytes[i] = 0; fractionBytes[i] = 0;
		}
			// while reading in the seconds & fraction, check if
			// this timetag has immediate semantics
		boolean isImmediate = true;
		for (int i = 4; i < 8; i++) {
			secondBytes[i] = bytes[streamPosition++];
			if (secondBytes[i] > 0) {
				isImmediate = false;
			}
		}
		for (int i = 4; i < 8; i++) {
			fractionBytes[i] = bytes[streamPosition++];
			if (i < 7) {
				if (fractionBytes[i] > 0) {
					isImmediate = false;
				}
			} else {
				if (fractionBytes[i] > 1) {
					isImmediate = false;
				}
			}
		}

		if (isImmediate) {
			return OSCBundle.TIMESTAMP_IMMEDIATE;
		}

		BigInteger secsSince1900 = new BigInteger(secondBytes);
		long secsSince1970 =  secsSince1900.longValue()
				- OSCBundle.SECONDS_FROM_1900_TO_1970.longValue();

		// no point maintaining times in the distant past
		if (secsSince1970 < 0) {
			secsSince1970 = 0;
		}
		long fraction = (new BigInteger(fractionBytes).longValue());

		// this line was cribbed from jakarta commons-net's NTP TimeStamp code
		fraction = (fraction * 1000) / 0x100000000L;

		// I do not know where, but I'm losing 1ms somewhere...
		fraction = (fraction > 0) ? fraction + 1 : 0;
		long millisecs = (secsSince1970 * 1000) + fraction;
		return new Date(millisecs);
	}

	/**
	 * Reads an array from the byte stream.
	 * @param types
	 * @param pos at which position to start reading
	 * @return the array that was read
	 */
	private List<Object> readArray(List<Character> types, int pos) {
		int arrayLen = 0;
		while (types.get(pos + arrayLen).charValue() != ']') {
			arrayLen++;
		}
		List<Object> array = new ArrayList<Object>(arrayLen);
		for (int j = 0; j < arrayLen; j++) {
			array.add(readArgument(types.get(pos + j)));
		}
		return array;
	}

	/**
	 * Get the length of the string currently in the byte stream.
	 */
	private int lengthOfCurrentString() {
		int i = 0;
		while (bytes[streamPosition + i] != 0) {
			i++;
		}
		return i;
	}

	/**
	 * Move to the next byte with an index in the byte array
	 * which is dividable by four.
	 */
	private void moveToFourByteBoundry() {
		// If i am already at a 4 byte boundry, I need to move to the next one
		int mod = streamPosition % 4;
		streamPosition += (4 - mod);
	}
}
