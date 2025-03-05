package me.cg360.spudengine.core.render.impl;

import me.cg360.spudengine.core.render.pipeline.descriptor.active.DescriptorSet;

public interface RenderProcessAddon {

    DescriptorSet[] getDescriptorSets();

}
