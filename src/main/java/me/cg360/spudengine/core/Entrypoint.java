package me.cg360.spudengine.core;

@FunctionalInterface
public interface Entrypoint {

    GameComponent create(SpudEngine engine);

}
