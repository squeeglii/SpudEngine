package me.cg360.spudengine.util;

// This is more a thing so everything gets named consistently.
public interface VkHandleWrapper {

    void cleanup();

    long getHandle();

}
