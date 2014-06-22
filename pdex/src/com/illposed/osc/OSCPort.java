/*
 * Copyright (C) 2003-2006, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import java.net.DatagramSocket;

/**
 * OSCPort is an abstract superclass, to send OSC messages,
 * use {@link OSCPortOut}.
 * To listen for OSC messages, use {@link OSCPortIn}.
 *
 * @author Chandrasekhar Ramakrishnan
 */
public abstract class OSCPort {

	private DatagramSocket socket;
	private int port;

	public static final int DEFAULT_SC_OSC_PORT = 57110;
	public static final int DEFAULT_SC_LANG_OSC_PORT = 57120;

	protected OSCPort(DatagramSocket socket, int port) {
		this.socket = socket;
		this.port = port;
	}

	/**
	 * The port that the SuperCollider <b>synth</b> engine
	 * usually listens to.
	 * @see #DEFAULT_SC_OSC_PORT
	 */
	public static int defaultSCOSCPort() {
		return DEFAULT_SC_OSC_PORT;
	}

	/**
	 * The port that the SuperCollider <b>language</b> engine
	 * usually listens to.
	 * @see #DEFAULT_SC_LANG_OSC_PORT
	 */
	public static int defaultSCLangOSCPort() {
		return DEFAULT_SC_LANG_OSC_PORT;
	}

	/**
	 * Returns the socket associated with this port.
	 * @return this ports socket
	 */
	protected DatagramSocket getSocket() {
		return socket;
	}

	/**
	 * Returns the port number associated with this port.
	 * @return this ports number
	 */
	protected int getPort() {
		return port;
	}

	/**
	 * Close the socket if this hasn't already happened.
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		super.finalize();
		socket.close();
	}

	/**
	 * Close the socket and free-up resources.
	 * It is recommended that clients call this when they are done with the
	 * port.
	 */
	public void close() {
		socket.close();
	}
}
