package me.cg360.spudengine.core;

@FunctionalInterface
public interface Entrypoint {

    GameInstance create(SpudEngine engine);

}
