package io.github.oliviercailloux.keyboardd.draft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public class HelloWorld {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorld.class);

  public interface CLibrary extends Library {
    CLibrary INSTANCE = Native.load("c", CLibrary.class);

    void printf(String format, Object... args);
  }

  public interface XCLibrary extends Library {
    XCLibrary INSTANCE = Native.load("xkbcommon", XCLibrary.class);

    void XStringToKeysymFAILS(String chars);
  }

  public interface XLibrary extends Library {
    XLibrary INSTANCE = Native.load("X11", XLibrary.class);

    long XStringToKeysym(String chars);
  }

  public static void main(String[] args) {
    CLibrary.INSTANCE.printf("Hello, World\n");
    LOGGER.info("Hello: {}.", XLibrary.INSTANCE.XStringToKeysym("Hello, World\n"));
    LOGGER.info("a: {}.", XLibrary.INSTANCE.XStringToKeysym("a"));
    LOGGER.info("F1: {}.", XLibrary.INSTANCE.XStringToKeysym("F1"));
    LOGGER.info("KP_Space: {}.", XLibrary.INSTANCE.XStringToKeysym("KP_Space"));
    LOGGER.info("Space: {}.", XLibrary.INSTANCE.XStringToKeysym("Space"));
    LOGGER.info("space: {}.", XLibrary.INSTANCE.XStringToKeysym("space"));
    {
    }
    {
    }
  }
}
