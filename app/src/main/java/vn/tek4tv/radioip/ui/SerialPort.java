package vn.tek4tv.radioip.ui;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPort {

  private static final String TAG = "SerialPort";

  private FileDescriptor mFd;
  private FileInputStream mFileInputStream;
  private FileOutputStream mFileOutputStream;

  public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {

		/* Check access permission */
    if (!device.canRead() || !device.canWrite()) {
        throw new SecurityException();
    }

    mFd = open(device.getAbsolutePath(), baudrate, flags);
    if (mFd == null) {
      Log.e(TAG, "native open returns null");
      throw new IOException();
    }
    mFileInputStream = new FileInputStream(mFd);
    mFileOutputStream = new FileOutputStream(mFd);
  }

  // Getters and setters
  public InputStream getInputStream() {
    return mFileInputStream;
  }

  public OutputStream getOutputStream() {
    return mFileOutputStream;
  }

  // JNI
  private native static FileDescriptor open(String path, int baudrate, int flags);
  public native void close();
  static {
    System.loadLibrary("serial_port");
  }
}