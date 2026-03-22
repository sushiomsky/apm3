/**
 * JNI bridge: Android USB file descriptor → libusb
 *
 * Android gives us a raw USB file descriptor via UsbDeviceConnection.getFileDescriptor().
 * We pass that to libusb_wrap_sys_device() which creates a proper libusb_device_handle
 * without needing root or udev.  From there we use standard libusb bulk I/O which
 * bypasses Android's quirky USB Host Java API entirely.
 */

#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <time.h>
#include <android/log.h>
#include "libusb/libusb.h"

#define TAG "ProxmarkNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

static libusb_context       *ctx    = NULL;
static libusb_device_handle *handle = NULL;
static int claimed_interfaces[8];
static int num_claimed = 0;

/* ------------------------------------------------------------------ */
/*  init: wrap an existing Android USB fd into a libusb device handle */
/* ------------------------------------------------------------------ */
JNIEXPORT jint JNICALL
Java_com_example_proxmark_usb_UsbLibusb_nativeInit(
        JNIEnv *env, jobject thiz, jint fd)
{
    int rc;

    if (handle) {
        LOGI("nativeInit: already initialised – closing previous handle");
        for (int i = 0; i < num_claimed; i++) {
            libusb_release_interface(handle, claimed_interfaces[i]);
        }
        num_claimed = 0;
        libusb_close(handle);
        handle = NULL;
    }
    if (ctx) {
        libusb_exit(ctx);
        ctx = NULL;
    }

    rc = libusb_set_option(NULL, LIBUSB_OPTION_NO_DEVICE_DISCOVERY);
    if (rc != LIBUSB_SUCCESS) {
        LOGE("libusb_set_option(NO_DEVICE_DISCOVERY) failed: %s",
             libusb_strerror(rc));
        return rc;
    }

    rc = libusb_init(&ctx);
    if (rc != LIBUSB_SUCCESS) {
        LOGE("libusb_init failed: %s", libusb_strerror(rc));
        return rc;
    }

    rc = libusb_wrap_sys_device(ctx, (intptr_t)fd, &handle);
    if (rc != LIBUSB_SUCCESS) {
        LOGE("libusb_wrap_sys_device(fd=%d) failed: %s", fd,
             libusb_strerror(rc));
        libusb_exit(ctx);
        ctx = NULL;
        return rc;
    }

    LOGI("libusb handle created from fd %d", fd);

    /* Dump device descriptor info */
    libusb_device *dev = libusb_get_device(handle);
    if (dev) {
        struct libusb_device_descriptor desc;
        if (libusb_get_device_descriptor(dev, &desc) == 0) {
            LOGI("Device: VID=%04x PID=%04x, configs=%d",
                 desc.idVendor, desc.idProduct, desc.bNumConfigurations);
        }

        struct libusb_config_descriptor *config;
        if (libusb_get_active_config_descriptor(dev, &config) == 0) {
            LOGI("Active config: %d interfaces", config->bNumInterfaces);
            for (int i = 0; i < config->bNumInterfaces; i++) {
                const struct libusb_interface *iface = &config->interface[i];
                for (int j = 0; j < iface->num_altsetting; j++) {
                    const struct libusb_interface_descriptor *alt = &iface->altsetting[j];
                    LOGI("  Interface %d alt %d: class=%d sub=%d proto=%d eps=%d",
                         alt->bInterfaceNumber, alt->bAlternateSetting,
                         alt->bInterfaceClass, alt->bInterfaceSubClass,
                         alt->bInterfaceProtocol, alt->bNumEndpoints);
                    for (int k = 0; k < alt->bNumEndpoints; k++) {
                        const struct libusb_endpoint_descriptor *ep = &alt->endpoint[k];
                        LOGI("    EP 0x%02X: %s %s maxPkt=%d interval=%d",
                             ep->bEndpointAddress,
                             (ep->bEndpointAddress & 0x80) ? "IN" : "OUT",
                             ((ep->bmAttributes & 0x03) == 2) ? "BULK" :
                             ((ep->bmAttributes & 0x03) == 3) ? "INT" : "OTHER",
                             ep->wMaxPacketSize, ep->bInterval);
                    }
                }
            }
            libusb_free_config_descriptor(config);
        }
    }

    return LIBUSB_SUCCESS;
}

/* ------------------------------------------------------------------ */
/*  claim a USB interface                                              */
/* ------------------------------------------------------------------ */
JNIEXPORT jint JNICALL
Java_com_example_proxmark_usb_UsbLibusb_nativeClaimInterface(
        JNIEnv *env, jobject thiz, jint interfaceNum)
{
    if (!handle) return -99;

    /* Try to detach kernel driver (may fail – that's okay on Android) */
    libusb_detach_kernel_driver(handle, interfaceNum);

    int rc = libusb_claim_interface(handle, interfaceNum);
    if (rc == LIBUSB_SUCCESS) {
        if (num_claimed < 8) {
            claimed_interfaces[num_claimed++] = interfaceNum;
        }
        LOGI("Claimed interface %d", interfaceNum);
    } else {
        LOGE("Claim interface %d failed: %s", interfaceNum,
             libusb_strerror(rc));
    }
    return rc;
}

/* ------------------------------------------------------------------ */
/*  CDC SET_LINE_CODING control transfer                               */
/* ------------------------------------------------------------------ */
JNIEXPORT jint JNICALL
Java_com_example_proxmark_usb_UsbLibusb_nativeSetLineCoding(
        JNIEnv *env, jobject thiz,
        jint interfaceNum, jint baudRate)
{
    if (!handle) return -99;

    /* SET_LINE_CODING (0x20): 7-byte payload */
    unsigned char data[7];
    data[0] = (unsigned char)(baudRate & 0xFF);
    data[1] = (unsigned char)((baudRate >> 8)  & 0xFF);
    data[2] = (unsigned char)((baudRate >> 16) & 0xFF);
    data[3] = (unsigned char)((baudRate >> 24) & 0xFF);
    data[4] = 0; /* 1 stop bit  */
    data[5] = 0; /* no parity   */
    data[6] = 8; /* 8 data bits */

    int rc = libusb_control_transfer(handle,
        0x21,               /* bmRequestType: Host→Device, Class, Interface */
        0x20,               /* bRequest: SET_LINE_CODING                    */
        0,                  /* wValue                                       */
        interfaceNum,       /* wIndex                                       */
        data, sizeof(data),
        1000);

    LOGD("SET_LINE_CODING(baud=%d, iface=%d): rc=%d", baudRate, interfaceNum, rc);
    return rc;
}

/* ------------------------------------------------------------------ */
/*  CDC SET_CONTROL_LINE_STATE (DTR/RTS)                               */
/* ------------------------------------------------------------------ */
JNIEXPORT jint JNICALL
Java_com_example_proxmark_usb_UsbLibusb_nativeSetControlLineState(
        JNIEnv *env, jobject thiz,
        jint interfaceNum, jboolean dtr, jboolean rts)
{
    if (!handle) return -99;

    uint16_t value = (dtr ? 0x01 : 0x00) | (rts ? 0x02 : 0x00);

    int rc = libusb_control_transfer(handle,
        0x21,           /* bmRequestType */
        0x22,           /* bRequest: SET_CONTROL_LINE_STATE */
        value,          /* wValue: DTR + RTS bits */
        interfaceNum,   /* wIndex */
        NULL, 0,
        1000);

    LOGD("SET_CONTROL_LINE_STATE(dtr=%d, rts=%d, iface=%d): rc=%d",
         dtr, rts, interfaceNum, rc);
    return rc;
}

/* ------------------------------------------------------------------ */
/*  CDC SET_CONFIGURATION + SET_INTERFACE to fully reset device state  */
/* ------------------------------------------------------------------ */
JNIEXPORT jint JNICALL
Java_com_example_proxmark_usb_UsbLibusb_nativeResetDevice(
        JNIEnv *env, jobject thiz)
{
    if (!handle) return -99;

    /* Try SET_CONFIGURATION(1) — some devices need explicit config activation */
    int rc = libusb_set_configuration(handle, 1);
    LOGD("SET_CONFIGURATION(1): rc=%d (%s)", rc, libusb_strerror(rc));

    return rc;
}

/* ------------------------------------------------------------------ */
/*  Read interrupt endpoint for CDC serial-state notifications         */
/* ------------------------------------------------------------------ */
JNIEXPORT jint JNICALL
Java_com_example_proxmark_usb_UsbLibusb_nativeInterruptRead(
        JNIEnv *env, jobject thiz,
        jint endpoint, jbyteArray data, jint length, jint timeoutMs)
{
    if (!handle) return -99;

    jbyte *buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (!buf) return -1;

    int transferred = 0;
    int rc = libusb_interrupt_transfer(handle,
        (unsigned char)endpoint,
        (unsigned char *)buf,
        length,
        &transferred,
        (unsigned int)timeoutMs);

    (*env)->ReleaseByteArrayElements(env, data, buf, 0);

    LOGD("interruptRead(ep=0x%02x): rc=%s, transferred=%d",
         endpoint, libusb_strerror(rc), transferred);

    if (rc == LIBUSB_SUCCESS || rc == LIBUSB_ERROR_TIMEOUT) {
        return transferred;
    }
    return rc;
}

/* ------------------------------------------------------------------ */
/*  Comprehensive diagnostic: try writing + reading with verbose log   */
/* ------------------------------------------------------------------ */
JNIEXPORT jint JNICALL
Java_com_example_proxmark_usb_UsbLibusb_nativeDiagnostic(
        JNIEnv *env, jobject thiz,
        jint outEp, jint inEp, jint intEp,
        jbyteArray pingPacket, jint pingLen)
{
    if (!handle) return -99;

    unsigned char readBuf[512];
    int transferred = 0;
    int rc;

    /* Step 1: SET_INTERFACE for data interface (alt setting 0)
     * The Linux cdc_acm driver does this — it may be required to
     * activate the bulk endpoints on the data interface. */
    LOGI("DIAG Step 1: SET_INTERFACE(iface=1, alt=0)");
    rc = libusb_set_interface_alt_setting(handle, 1, 0);
    LOGI("DIAG  -> SET_INTERFACE: rc=%s", libusb_strerror(rc));

    /* Step 2: Clear halt on both endpoints */
    LOGI("DIAG Step 2: Clear halt on endpoints");
    rc = libusb_clear_halt(handle, (unsigned char)inEp);
    LOGI("DIAG  -> clear_halt IN 0x%02x: rc=%s", inEp, libusb_strerror(rc));
    rc = libusb_clear_halt(handle, (unsigned char)outEp);
    LOGI("DIAG  -> clear_halt OUT 0x%02x: rc=%s", outEp, libusb_strerror(rc));

    /* Step 3: Re-do CDC init AFTER SET_INTERFACE */
    LOGI("DIAG Step 3: Re-do CDC init after SET_INTERFACE");
    {
        unsigned char lc[7] = {0x00, 0xC2, 0x01, 0x00, 0x00, 0x00, 0x08}; /* 115200-8N1 */
        rc = libusb_control_transfer(handle, 0x21, 0x20, 0, 0, lc, 7, 1000);
        LOGI("DIAG  -> SET_LINE_CODING: rc=%d", rc);

        rc = libusb_control_transfer(handle, 0x21, 0x22, 0x00, 0, NULL, 0, 1000);
        LOGI("DIAG  -> SET_CONTROL_LINE_STATE(off): rc=%d", rc);

        /* Wait 100ms */
        struct timespec ts = {0, 100000000};
        nanosleep(&ts, NULL);

        rc = libusb_control_transfer(handle, 0x21, 0x22, 0x03, 0, NULL, 0, 1000);
        LOGI("DIAG  -> SET_CONTROL_LINE_STATE(DTR+RTS): rc=%d", rc);

        /* Wait 300ms for device to settle */
        ts.tv_nsec = 300000000;
        nanosleep(&ts, NULL);
    }

    /* Step 4: Flush any pending data */
    LOGI("DIAG Step 4: Flush");
    for (int i = 0; i < 3; i++) {
        transferred = 0;
        libusb_bulk_transfer(handle, (unsigned char)inEp,
                readBuf, sizeof(readBuf), &transferred, 50);
        if (transferred > 0)
            LOGI("DIAG  -> flushed %d bytes", transferred);
    }

    /* Step 5: Write PING packet */
    LOGI("DIAG Step 5: Write PING (%d bytes)", pingLen);
    jbyte *pingBuf = (*env)->GetByteArrayElements(env, pingPacket, NULL);
    if (!pingBuf) return -1;

    transferred = 0;
    rc = libusb_bulk_transfer(handle, (unsigned char)outEp,
            (unsigned char *)pingBuf, pingLen, &transferred, 2000);
    (*env)->ReleaseByteArrayElements(env, pingPacket, pingBuf, JNI_ABORT);
    LOGI("DIAG  -> write: rc=%s, transferred=%d", libusb_strerror(rc), transferred);

    /* Step 6: Read response with longer timeout */
    LOGI("DIAG Step 6: Read response");
    for (int i = 0; i < 12; i++) {
        transferred = 0;
        rc = libusb_bulk_transfer(handle, (unsigned char)inEp,
                readBuf, sizeof(readBuf), &transferred, 500);
        LOGI("DIAG  -> read %d: rc=%s, transferred=%d", i, libusb_strerror(rc), transferred);
        if (transferred > 0) {
            int showLen = transferred < 16 ? transferred : 16;
            char hex[128];
            for (int j = 0; j < showLen; j++)
                sprintf(hex + j*3, "%02x ", readBuf[j]);
            LOGI("DIAG  -> GOT DATA! [%s]", hex);
            return transferred;
        }
    }

    /* Step 7: Try GET_LINE_CODING to check device state */
    LOGI("DIAG Step 7: GET_LINE_CODING");
    {
        unsigned char glc[7] = {0};
        rc = libusb_control_transfer(handle, 0xA1, 0x21, 0, 0, glc, 7, 1000);
        LOGI("DIAG  -> GET_LINE_CODING: rc=%d", rc);
        if (rc >= 7) {
            uint32_t baud = glc[0] | (glc[1]<<8) | (glc[2]<<16) | (glc[3]<<24);
            LOGI("DIAG  -> baud=%u stop=%d parity=%d databits=%d",
                 baud, glc[4], glc[5], glc[6]);
        }
    }

    /* Step 8: Try SEND_BREAK */
    LOGI("DIAG Step 8: SEND_BREAK");
    rc = libusb_control_transfer(handle, 0x21, 0x23, 0, 0, NULL, 0, 1000);
    LOGI("DIAG  -> SEND_BREAK: rc=%d", rc);

    /* Step 9: Try reading after SEND_BREAK */
    {
        struct timespec ts = {0, 200000000};
        nanosleep(&ts, NULL);
    }
    for (int i = 0; i < 5; i++) {
        transferred = 0;
        rc = libusb_bulk_transfer(handle, (unsigned char)inEp,
                readBuf, sizeof(readBuf), &transferred, 500);
        if (transferred > 0) {
            LOGI("DIAG  -> post-BREAK read got %d bytes!", transferred);
            return transferred;
        }
    }

    LOGI("DIAG Complete: no response received from device");
    return 0;
}

/* ------------------------------------------------------------------ */
/*  synchronous bulk write                                             */
/* ------------------------------------------------------------------ */
JNIEXPORT jint JNICALL
Java_com_example_proxmark_usb_UsbLibusb_nativeBulkWrite(
        JNIEnv *env, jobject thiz,
        jint endpoint, jbyteArray data, jint length, jint timeoutMs)
{
    if (!handle) return -99;

    jbyte *buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (!buf) return -1;

    int transferred = 0;
    int rc = libusb_bulk_transfer(handle,
        (unsigned char)endpoint,
        (unsigned char *)buf,
        length,
        &transferred,
        (unsigned int)timeoutMs);

    (*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);

    if (rc == LIBUSB_SUCCESS || rc == LIBUSB_ERROR_TIMEOUT) {
        LOGD("bulkWrite(ep=0x%02x, len=%d): transferred=%d",
             endpoint, length, transferred);
        return transferred;
    }
    LOGE("bulkWrite(ep=0x%02x) error: %s", endpoint, libusb_strerror(rc));
    return rc;  /* negative libusb error code */
}

/* ------------------------------------------------------------------ */
/*  synchronous bulk read                                              */
/* ------------------------------------------------------------------ */
JNIEXPORT jint JNICALL
Java_com_example_proxmark_usb_UsbLibusb_nativeBulkRead(
        JNIEnv *env, jobject thiz,
        jint endpoint, jbyteArray data, jint length, jint timeoutMs)
{
    if (!handle) return -99;

    jbyte *buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (!buf) return -1;

    int transferred = 0;
    int rc = libusb_bulk_transfer(handle,
        (unsigned char)endpoint,
        (unsigned char *)buf,
        length,
        &transferred,
        (unsigned int)timeoutMs);

    /* Copy data back into Java array */
    (*env)->ReleaseByteArrayElements(env, data, buf, 0);

    if (rc == LIBUSB_SUCCESS || rc == LIBUSB_ERROR_TIMEOUT) {
        if (transferred > 0) {
            LOGI("bulkRead(ep=0x%02x, len=%d): transferred=%d",
                 endpoint, length, transferred);
        }
        /* Log first few reads to confirm the loop is active */
        {
            static int read_count = 0;
            read_count++;
            if (read_count <= 5 || (read_count % 100 == 0)) {
                LOGD("bulkRead #%d: rc=%s, transferred=%d, timeout=%d",
                     read_count, libusb_strerror(rc), transferred, timeoutMs);
            }
        }
        return transferred;  /* 0 on timeout with no data */
    }
    LOGE("bulkRead(ep=0x%02x) error: %s (rc=%d)", endpoint, libusb_strerror(rc), rc);
    return rc;
}

/* ------------------------------------------------------------------ */
/*  clean up                                                           */
/* ------------------------------------------------------------------ */
JNIEXPORT void JNICALL
Java_com_example_proxmark_usb_UsbLibusb_nativeClose(
        JNIEnv *env, jobject thiz)
{
    if (handle) {
        for (int i = 0; i < num_claimed; i++) {
            libusb_release_interface(handle, claimed_interfaces[i]);
        }
        num_claimed = 0;
        libusb_close(handle);
        handle = NULL;
        LOGI("libusb handle closed");
    }
    if (ctx) {
        libusb_exit(ctx);
        ctx = NULL;
        LOGI("libusb context destroyed");
    }
}
