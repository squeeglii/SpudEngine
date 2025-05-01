package me.cg360.spudengine.core.render.impl.layered;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.sync.Semaphore;

public record LayeredSemaphores(Semaphore imageAcquisitionSemaphore, Semaphore layersCompleteSemaphore,
                                Semaphore composeCompleteSemaphore) {

    public LayeredSemaphores(LogicalDevice device) {
        this(
                new Semaphore(device),
                new Semaphore(device),
                new Semaphore(device)
        );
    }

    public void cleanup() {
        this.imageAcquisitionSemaphore.cleanup();
        this.layersCompleteSemaphore.cleanup();     // only used for Layered Renderer.
        this.composeCompleteSemaphore.cleanup();
    }

}
