package me.cg360.spudengine.core;

public class Main {

    private static SpudEngine engineInstance;

    public static void main(String[] args) {
        EngineProperties.initialize(args);
        engineInstance = new SpudEngine().setAsInstance();
        engineInstance.start();
    }

    public static SpudEngine getEngineInstance() {
        return engineInstance;
    }
}
