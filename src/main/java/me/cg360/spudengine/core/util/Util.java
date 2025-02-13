package me.cg360.spudengine.core.util;

import java.util.Locale;

public class Util {

    public static OSType getOS() {
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);

        if ((os.contains("mac")) || (os.contains("darwin"))) {
            return OSType.MACOS;

        } else if (os.contains("win")) {
            return OSType.WINDOWS;

        } else if (os.contains("nux")) {
            return OSType.LINUX;

        } else {
            return OSType.OTHER;
        }
    }

}
