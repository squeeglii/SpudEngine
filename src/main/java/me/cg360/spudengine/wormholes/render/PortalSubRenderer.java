package me.cg360.spudengine.wormholes.render;

import me.cg360.spudengine.core.render.impl.RenderProcessAddon;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.DescriptorSet;

public class PortalSubRenderer implements RenderProcessAddon {

    @Override
    public DescriptorSet[] getDescriptorSets() {
        return new DescriptorSet[0];
    }

}
