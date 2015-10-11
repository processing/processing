/*
  Copyright (c) The Processing Foundation 2015
  I/O library developed by Gottfried Haider as part of GSOC 2015

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <linux/i2c.h>
#include <linux/i2c-dev.h>
#include <linux/spi/spidev.h>
#include <poll.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/param.h>
#include <unistd.h>
#include "iface.h"


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_openDevice
  (JNIEnv *env, jclass cls, jstring _fn)
{
	const char *fn = (*env)->GetStringUTFChars(env, _fn, JNI_FALSE);
	int file = open(fn, O_RDWR);
	(*env)->ReleaseStringUTFChars(env, _fn, fn);
	if (file < 0) {
		return -errno;
	} else {
		return file;
	}
}


JNIEXPORT jstring JNICALL Java_processing_io_NativeInterface_getError
  (JNIEnv *env, jclass cls, jint _errno)
{
	char *msg = strerror(abs(_errno));
	if (msg) {
		return (*env)->NewStringUTF(env, msg);
	} else {
		return NULL;
	}
}


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_closeDevice
  (JNIEnv *env, jclass cls, jint handle)
{
	if (close(handle) < 0) {
		return -errno;
	} else {
		return 0;
	}
}


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_readFile
  (JNIEnv *env, jclass cls, jstring _fn, jbyteArray _in)
{
	const char *fn = (*env)->GetStringUTFChars(env, _fn, JNI_FALSE);
	int file = open(fn, O_RDONLY);
	(*env)->ReleaseStringUTFChars(env, _fn, fn);
	if (file < 0) {
		return -errno;
	}

	jbyte *in = (*env)->GetByteArrayElements(env, _in, NULL);
	int len = read(file, in, (*env)->GetArrayLength(env, _in));
	if (len < 0) {
		len = -errno;
	}
	(*env)->ReleaseByteArrayElements(env, _in, in, 0);

	close(file);
	return len;
}


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_writeFile
  (JNIEnv *env, jclass cls, jstring _fn, jbyteArray _out)
{
	const char *fn = (*env)->GetStringUTFChars(env, _fn, JNI_FALSE);
	int file = open(fn, O_WRONLY);
	(*env)->ReleaseStringUTFChars(env, _fn, fn);
	if (file < 0) {
		return -errno;
	}

	jbyte *out = (*env)->GetByteArrayElements(env, _out, JNI_FALSE);
	int len = write(file, out, (*env)->GetArrayLength(env, _out));
	if (len < 0) {
		len = -errno;
	}
	(*env)->ReleaseByteArrayElements(env, _out, out, JNI_ABORT);

	close(file);
	return len;
}


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_pollDevice
  (JNIEnv *env, jclass cls, jstring _fn, jint timeout)
{
	const char *fn = (*env)->GetStringUTFChars(env, _fn, JNI_FALSE);
	int file = open(fn, O_RDONLY|O_NONBLOCK);
	(*env)->ReleaseStringUTFChars(env, _fn, fn);
	if (file < 0) {
		return -errno;
	}

	// dummy read
	char tmp;
	while (0 < read(file, &tmp, 1));

	struct pollfd fds[1];
	memset(fds, 0, sizeof(fds));
	fds[0].fd = file;
	fds[0].events = POLLPRI|POLLERR;

	// and wait
	int ret = poll(fds, 1, timeout);
	close(file);

	if (ret < 0) {
		return -errno;
	} else if (ret == 0) {
		// timeout
		return 0;
	} else if (fds[0].revents & POLLPRI) {
		// interrupt
		return 1;
	} else {
		// POLLERR?
		return -ENOMSG;
	}
}


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_transferI2c
  (JNIEnv *env, jclass cls, jint handle, jint slave, jbyteArray _out, jbyteArray _in)
{
	struct i2c_rdwr_ioctl_data packets;
	struct i2c_msg msgs[2];
	jbyte *out, *in;

	packets.msgs = msgs;

	msgs[0].addr = slave;
	msgs[0].flags = 0;
	msgs[0].len = (*env)->GetArrayLength(env, _out);
	out = (*env)->GetByteArrayElements(env, _out, NULL);
	msgs[0].buf = out;
	if (_in != NULL) {
		in = (*env)->GetByteArrayElements(env, _in, NULL);
		msgs[1].addr = slave;
		msgs[1].flags = I2C_M_RD;	// I2C_M_RECV_LEN is not supported
		msgs[1].len = (*env)->GetArrayLength(env, _in);
		msgs[1].buf = in;
		packets.nmsgs = 2;
	} else {
		packets.nmsgs = 1;
	}

	int ret = ioctl(handle, I2C_RDWR, &packets);
	if (ret < 0) {
		ret = -errno;
	}

	(*env)->ReleaseByteArrayElements(env, _out, out, JNI_ABORT);
	if (_in != NULL) {
		(*env)->ReleaseByteArrayElements(env, _in, in, 0);
	}

	return ret;
}


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_setSpiSettings
  (JNIEnv *env, jclass cls, jint handle, jint _maxSpeed, jint dataOrder, jint mode)
{
	uint8_t tmp;
	uint32_t maxSpeed;

	tmp = (uint8_t)mode;
	int ret = ioctl(handle, SPI_IOC_WR_MODE, &tmp);
	if (ret < 0) {
		return ret;
	}

	tmp = (uint8_t)dataOrder;
	ret = ioctl(handle, SPI_IOC_WR_LSB_FIRST, &tmp);
	if (ret < 0) {
		return ret;
	}

	maxSpeed = (uint32_t)_maxSpeed;
	ret = ioctl(handle, SPI_IOC_WR_MAX_SPEED_HZ, &maxSpeed);
	if (ret < 0) {
		return ret;
	}

	return 0;
}


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_transferSpi
  (JNIEnv *env, jclass cls, jint handle, jbyteArray _out, jbyteArray _in)
{
	jbyte* out = (*env)->GetByteArrayElements(env, _out, NULL);
	jbyte* in = (*env)->GetByteArrayElements(env, _in, NULL);

	struct spi_ioc_transfer xfer = {
		.tx_buf = (unsigned long)out,
		.rx_buf = (unsigned long)in,
		.len = MIN((*env)->GetArrayLength(env, _out), (*env)->GetArrayLength(env, _in)),
	};

	int ret = ioctl(handle, SPI_IOC_MESSAGE(1), &xfer);

	(*env)->ReleaseByteArrayElements(env, _out, out, JNI_ABORT);
	(*env)->ReleaseByteArrayElements(env, _in, in, 0);

	return ret;
}
