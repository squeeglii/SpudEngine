package me.cg360.spudengine.core.render.impl.forward;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.sync.Semaphore;

public record ForwardSemaphores(Semaphore imageAcquisitionSemaphore, Semaphore renderCompleteSemaphore) {

    public ForwardSemaphores(LogicalDevice device) {
        this (
                new Semaphore(device),
                new Semaphore(device)
        );
    }

    public void cleanup() {
        this.imageAcquisitionSemaphore.cleanup();
        this.renderCompleteSemaphore.cleanup();
    }
}
