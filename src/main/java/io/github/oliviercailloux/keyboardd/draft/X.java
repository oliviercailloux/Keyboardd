package io.github.oliviercailloux.keyboardd.draft;

import static com.google.common.base.Verify.verify;

import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.Display;
import com.sun.jna.platform.unix.X11.KeySym;

public class X {
  private static final X11 x11 = X11.INSTANCE;
  private final Display display;

  private X() {
    display = x11.XOpenDisplay(null);
    verify(display != null);
  }
  // file:///home/olivier/.m2/repository/com/google/guava/guava/32.0.1-jre/guava-32.0.1-jre-javadoc.jar

  public String toKeyName(KeySym keysym) {
    return x11.XKeysymToString(keysym);
  }

  public KeySym toKeySym(String keyName) {
    return x11.XStringToKeysym(keyName);
  }

  public byte toKeycode(KeySym keysym) {
    return x11.XKeysymToKeycode(display, keysym);
  }

  public KeySym toKeysym(byte keycode, int index) {
    return x11.XKeycodeToKeysym(display, keycode, index);
  }
}
