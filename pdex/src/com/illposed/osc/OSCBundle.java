/*
 * Copyright (C) 2003-2006, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.illposed.osc.utility.OSCJavaToByteArrayConverter;

/**
 * A bundle represents a collection of OSC packets
 * (either messages or other bundles)
 * and has a time-tag which can be used by a scheduler to execute
 * a bundle in the future,
 * instead of immediately.
 * {@link OSCMessage}s are executed immediately.
 *
 * Bundles should be used if you want to send multiple messages to be executed
 * atomically together, or you want to schedule one or more messages to be
 * executed in the future.
 *
 * @author Chandrasekhar Ramakrishnan
 */
public class OSCBundle extends OSCPacket {

	/**
	 * 2208988800 seconds -- includes 17 leap years
	 */
	public static final BigInteger SECONDS_FROM_1900_TO_1970 =
		new BigInteger("2208988800");

	/**
	 * The Java representation of an OSC timestamp with the semantics of
	 * "immediately".
	 */
	public static final Date TIMESTAMP_IMMEDIATE = new Date(0);

	private Date timestamp;
	private List<OSCPacket> packets;

	/**
	 * Create a new empty OSCBundle with a timestamp of immediately.
	 * You can add packets to the bundle with addPacket()
	 */
	public OSCBundle() {
		this(TIMESTAMP_IMMEDIATE);
	}

	/**
	 * Create an OSCBundle with the specified timestamp.
	 * @param timestamp the time to execute the bundle
	 */
	public OSCBundle(Date timestamp) {
		this((Collection<OSCPacket>) null, timestamp);
	}

	// deprecated since version 1.0, March 2012
	/**
	 * Creates an OSCBundle made up of the given packets
	 * with a timestamp of now.
	 * @param packets array of OSCPackets to initialize this object with
	 * @deprecated
	 */
	public OSCBundle(OSCPacket[] packets) {
		this(packets, TIMESTAMP_IMMEDIATE);
	}

	/**
	 * Creates an OSCBundle made up of the given packets
	 * with a timestamp of now.
	 * @param packets array of OSCPackets to initialize this object with
	 */
	public OSCBundle(Collection<OSCPacket> packets) {
		this(packets, TIMESTAMP_IMMEDIATE);
	}

	// deprecated since version 1.0, March 2012
	/**
	 * Creates an OSCBundle, specifying the packets and timestamp.
	 * @param packets the packets that make up the bundle
	 * @param timestamp the time to execute the bundle
	 * @deprecated
	 */
	public OSCBundle(OSCPacket[] packets, Date timestamp) {
		this((packets == null)
				? new LinkedList<OSCPacket>()
				: Arrays.asList(packets),
				timestamp);
	}

	/**
	 * Create an OSCBundle, specifying the packets and timestamp.
	 * @param packets the packets that make up the bundle
	 * @param timestamp the time to execute the bundle
	 */
	public OSCBundle(Collection<OSCPacket> packets, Date timestamp) {

		if (null == packets) {
			this.packets = new LinkedList<OSCPacket>();
		} else {
			this.packets = new ArrayList<OSCPacket>(packets);
		}
		this.timestamp = timestamp;
		init();
	}

	/**
	 * Return the time the bundle will execute.
	 * @return a Date
	 */
	public Date getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the time the bundle will execute.
	 * @param timestamp Date
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Add a packet to the list of packets in this bundle.
	 * @param packet OSCMessage or OSCBundle
	 */
	public void addPacket(OSCPacket packet) {
		packets.add(packet);
	}

	/**
	 * Get the packets contained in this bundle.
	 * @return the packets contained in this bundle.
	 */
	public OSCPacket[] getPackets() {
		OSCPacket[] packetArray = new OSCPacket[packets.size()];
		packets.toArray(packetArray);
		return packetArray;
	}

	/**
	 * Convert the time-tag (a Java Date) into the OSC byte stream.
	 * Used Internally.
	 */
	protected void computeTimeTagByteArray(OSCJavaToByteArrayConverter stream) {
		if ((null == timestamp) || (timestamp == TIMESTAMP_IMMEDIATE)) {
			stream.write((int) 0);
			stream.write((int) 1);
			return;
		}

		long millisecs = timestamp.getTime();
		long secsSince1970 = (long) (millisecs / 1000);
		long secs = secsSince1970 + SECONDS_FROM_1900_TO_1970.longValue();

		// this line was cribbed from jakarta commons-net's NTP TimeStamp code
		long fraction = ((millisecs % 1000) * 0x100000000L) / 1000;

		stream.write((int) secs);
		stream.write((int) fraction);
	}

	/**
	 * Compute the OSC byte stream representation of the bundle.
	 * Used Internally.
	 * @param stream OscPacketByteArrayConverter
	 */
	protected byte[] computeByteArray(OSCJavaToByteArrayConverter stream) {
		stream.write("#bundle");
		computeTimeTagByteArray(stream);
		byte[] packetBytes;
		for (OSCPacket pkg : packets) {
			packetBytes = pkg.getByteArray();
			stream.write(packetBytes.length);
			stream.write(packetBytes);
		}
		return stream.toByteArray();
	}
}
