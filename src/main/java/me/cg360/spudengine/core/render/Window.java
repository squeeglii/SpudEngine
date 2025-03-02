package me.cg360.spudengine.core.render;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.input.MouseInput;
import me.cg360.spudengine.core.util.VkHandleWrapper;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryUtil;
import org.tinylog.Logger;

public class Window implements VkHandleWrapper {

    private final MouseInput mouseInput;

    private final long handle;
    private int width;
    private int height;
    private boolean hasBeenResized;

    public Window(String title) {
        this(title, null);
    }

    public Window(String title, GLFWKeyCallbackI keyCallback) {
        if(!GLFW.glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
        if(!GLFWVulkan.glfwVulkanSupported()) throw new IllegalStateException("Vulkan is unsupported.");

        // monitor resolution?
        // GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        this.width = EngineProperties.STARTING_WIDTH;
        this.height = EngineProperties.STARTING_HEIGHT;

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_FALSE);

        this.handle = GLFW.glfwCreateWindow(this.width, this.height, title, MemoryUtil.NULL, MemoryUtil.NULL);

        if(this.handle == MemoryUtil.NULL)
            throw new RuntimeException("Failed to create a window.");

        GLFW.glfwSetFramebufferSizeCallback(this.handle, (window, width, height) -> this.resize(width, height));
        GLFW.glfwSetKeyCallback(this.handle, (window, key, scancode, action, mods) -> {
            if(key == GLFW.GLFW_KEY_F4 && action == GLFW.GLFW_RELEASE)
                this.requestClose();

            if(keyCallback != null)
                keyCallback.invoke(window, key, scancode, action, mods);
        });

        this.mouseInput = new MouseInput(this.handle);
        Logger.info("Created Vulkan GLFW window");
    }


    public void pollEvents() {
        GLFW.glfwPollEvents();
        this.mouseInput.tickInput(this);
    }

    public void cleanup() {
        Callbacks.glfwFreeCallbacks(this.handle);
        GLFW.glfwDestroyWindow(this.handle);
        GLFW.glfwTerminate();
    }

    public void resize(int width, int height) {
        this.hasBeenResized = true;
        this.width = width;
        this.height = height;
    }

    public void requestClose() {
        Logger.info("Attempting to close window");
        GLFW.glfwSetWindowShouldClose(this.handle, true);
    }


    public void setRequiresSizeUpdate(boolean resized) {
        this.hasBeenResized = resized;
    }


    public boolean isKeyPressed(int keyCode) {
        return GLFW.glfwGetKey(this.handle, keyCode) == GLFW.GLFW_PRESS;
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(this.handle);
    }

    public MouseInput getMouseInput() {
        return this.mouseInput;
    }

    public boolean requiresSizeUpdate() {
        return this.hasBeenResized;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public long getHandle() {
        return this.handle;
    }

}
