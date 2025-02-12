package me.cg360.spudengine.render.sync;

import me.cg360.spudengine.render.hardware.LogicalDevice;

public record SyncSemaphores(Semaphore imgAcquisitionSemaphore, Semaphore renderCompleteSemaphore) {

    public SyncSemaphores(LogicalDevice device) {
        this(new Semaphore(device), new Semaphore(device));
    }

    public void cleanup() {
        this.imgAcquisitionSemaphore.cleanup();
        this.renderCompleteSemaphore.cleanup();
    }
}
