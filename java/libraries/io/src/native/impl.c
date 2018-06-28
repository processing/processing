/*
  Copyright (c) The Processing Foundation 2015
  Hardware I/O library developed by Gottfried Haider as part of GSoC 2015

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
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/param.h>
#include <time.h>
#include <unistd.h>
#include "iface.h"


static const int servo_pulse_oversleep = 35;  // amount of uS to account for when sleeping


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


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_raspbianGpioMemRead
  (JNIEnv *env, jclass cls, jint offset)
{
	// validate offset
	if (4096 <= offset) {
		return -EINVAL;
	}

	int file = open("/dev/gpiomem", O_RDWR|O_SYNC);
	if (file < 0) {
		return -errno;
	}

	uint32_t *mem = mmap(NULL, 4096, PROT_READ, MAP_SHARED, file, 0);
	if (mem == MAP_FAILED) {
		close(file);
		return -errno;
	}

	uint32_t value = mem[offset];

	munmap(mem, 4096);
	close(file);
	return value;
}


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_raspbianGpioMemWrite
  (JNIEnv *env, jclass cls, jint offset, jint mask, jint value)
{
	// validate offset
	if (4096 <= offset) {
		return -EINVAL;
	}

	int file = open("/dev/gpiomem", O_RDWR|O_SYNC);
	if (file < 0) {
		return -errno;
	}

	uint32_t *mem = mmap(NULL, 4096, PROT_READ|PROT_WRITE, MAP_SHARED, file, 0);
	if (mem == MAP_FAILED) {
		close(file);
		return -errno;
	}

	mem[offset] = (mem[offset] & ~mask) | (value & mask);

	munmap(mem, 4096);
	close(file);
	return 1;	// number of bytes written
}


#define BCM2835_GPPUD_OFFSET (0x94 >> 2)
#define BCM2835_GPPUDCLK0_OFFSET (0x98 >> 2)
#define BCM2835_GPPUDCLK1_OFFSET (0x9c >> 2)

JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_raspbianGpioMemSetPinBias
  (JNIEnv *env, jclass cls, jint gpio, jint mode)
{
	int ret = 0;	// success

	int file = open("/dev/gpiomem", O_RDWR|O_SYNC);
	if (file < 0) {
		return -errno;
	}

	uint32_t *mem = mmap(NULL, 4096, PROT_READ|PROT_WRITE, MAP_SHARED, file, 0);
	if (mem == MAP_FAILED) {
		close(file);
		return -errno;
	}

	// validate arguments
	if (gpio < 0 || 53 < gpio) {
		ret = -EINVAL;
		goto out;
	}

	// see BCM2835 datasheet, p. 101
	uint32_t pud;
	if (mode == 0) {
		pud = 0;	// floating
	} else if (mode == 2) {
		pud = 2;	// pull-up
	} else if (mode == 3) {
		pud = 1;	// pull-down
	} else {
		ret = -EINVAL;
		goto out;
	}

	/*
	 * From the BCM2835 datasheet, p. 101:
	 *
	 * The following sequence of events is required:
	 * 1. Write to GPPUD to set the required control signal (i.e. Pull-up or
	 *    Pull-Down or neither to remove the current Pull-up/down)
	 * 2. Wait 150 cycles – this provides the required set-up time for the
	 *    control signal
	 * 3. Write to GPPUDCLK0/1 to clock the control signal into the GPIO pads
	 *    you wish to modify – NOTE only the pads which receive a clock will
	 *    be modified, all others will retain their previous state.
	 * 4. Wait 150 cycles – this provides the required hold time for the
	 *    control signal
	 * 5. Write to GPPUD to remove the control signal
	 * 6. Write to GPPUDCLK0/1 to remove the clock
	 */

	// python-gpiozero uses a delay of 214 ns, so we do the same
	struct timespec wait;
	wait.tv_sec = 0;
	wait.tv_nsec = 214;

	mem[BCM2835_GPPUD_OFFSET] = pud;
	nanosleep(&wait, NULL);
	if (gpio < 32) {
		mem[BCM2835_GPPUDCLK0_OFFSET] = 1 << gpio;
	} else {
		mem[BCM2835_GPPUDCLK1_OFFSET] = 1 << (gpio-32);
	}
	nanosleep(&wait, NULL);
	mem[BCM2835_GPPUD_OFFSET] = 0;
	if (gpio < 32) {
		mem[BCM2835_GPPUDCLK0_OFFSET] = 0;
	} else {
		mem[BCM2835_GPPUDCLK1_OFFSET] = 0;
	}

out:
	munmap(mem, 4096);
	close(file);
	return ret;
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
	packets.nmsgs = 0;

	if (_out != NULL) {
		msgs[packets.nmsgs].addr = slave;
		msgs[packets.nmsgs].flags = 0;
		msgs[packets.nmsgs].len = (*env)->GetArrayLength(env, _out);
		out = (*env)->GetByteArrayElements(env, _out, NULL);
		msgs[packets.nmsgs].buf = out;
		packets.nmsgs++;
	}
	if (_in != NULL) {
		msgs[packets.nmsgs].addr = slave;
		msgs[packets.nmsgs].flags = I2C_M_RD;	// I2C_M_RECV_LEN is not supported
		msgs[packets.nmsgs].len = (*env)->GetArrayLength(env, _in);
		in = (*env)->GetByteArrayElements(env, _in, NULL);
		msgs[packets.nmsgs].buf = in;
		packets.nmsgs++;
	}

	// set the timeout to 100ms - this helps slow devices such as the
	// Arduino Uno to keep up
	ioctl(handle, I2C_TIMEOUT, 10);
	int ret = ioctl(handle, I2C_RDWR, &packets);
	if (ret < 0) {
		ret = -errno;
	}

	if (_out != NULL) {
		(*env)->ReleaseByteArrayElements(env, _out, out, JNI_ABORT);
	}
	if (_in != NULL) {
		(*env)->ReleaseByteArrayElements(env, _in, in, 0);
	}

	return ret;
}


typedef struct {
	int fd;
	pthread_t thread;
	int pulse;
	int period;
} SERVO_STATE_T;


static void* servoThread(void *ptr) {
	SERVO_STATE_T *state = (SERVO_STATE_T*)ptr;
	struct timespec on, off;
	on.tv_sec = 0;
	off.tv_sec = 0;

	do {
		write(state->fd, "1", 1);

		on.tv_nsec = state->pulse * 1000;
		nanosleep(&on, NULL);

		write(state->fd, "0", 1);

		off.tv_nsec = (state->period - state->pulse) * 1000;
		nanosleep(&off, NULL);
	} while (1);
}


JNIEXPORT jlong JNICALL Java_processing_io_NativeInterface_servoStartThread
  (JNIEnv *env, jclass cls, jint gpio, jint pulse, jint period)
{
	char path[26 + 19 + 1];
	int fd;
	pthread_t thread;

	// setup struct holding our state
	SERVO_STATE_T *state = malloc(sizeof(SERVO_STATE_T));
	if (!state) {
		return -ENOMEM;
	}
	memset(state, 0, sizeof(*state));
	state->pulse = (pulse - servo_pulse_oversleep > 0) ? pulse - servo_pulse_oversleep : 0;
	// we're obviously also oversleeping in the general period case
	// but other than the pulse, this doesn't seem to be crucial with servos
	state->period = period;

	// open gpio
	sprintf(path, "/sys/class/gpio/gpio%d/value", gpio);
	state->fd = open(path, O_WRONLY);
	if (state->fd < 0) {
		free(state);
		return -errno;
	}

	// start thread
	int ret = pthread_create(&state->thread, NULL, servoThread, state);
	if (ret != 0) {
		free(state);
		return -ret;
	}

	// set scheduling policy and priority
	struct sched_param param;
	param.sched_priority = 75;
	ret = pthread_setschedparam(state->thread, SCHED_FIFO, &param);
	if (ret != 0) {
		fprintf(stderr, "Error setting thread policy: %s\n", strerror(ret));
	}

	return (intptr_t)state;
}


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_servoUpdateThread
  (JNIEnv *env, jclass cls, jlong handle, jint pulse, jint period)
{
	SERVO_STATE_T *state = (SERVO_STATE_T*)(intptr_t)handle;
	state->pulse = (pulse - servo_pulse_oversleep > 0) ? pulse - servo_pulse_oversleep : 0;
	state->period = period;
	return 0;
}


JNIEXPORT jint JNICALL Java_processing_io_NativeInterface_servoStopThread
  (JNIEnv *env, jclass cls, jlong handle)
{
	SERVO_STATE_T *state = (SERVO_STATE_T*)(intptr_t)handle;

	// signal thread to stop
	pthread_cancel(state->thread);
	pthread_join(state->thread, NULL);

	close(state->fd);
	free(state);
	return 0;
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
