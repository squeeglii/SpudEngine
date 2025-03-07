package me.cg360.spudengine.core;

import org.tinylog.Logger;

import java.util.Scanner;

public class Main {

    private static SpudEngine engineInstance;

    public static void main(String[] args) {
        try {
            EngineProperties.initialize(args);
            engineInstance = new SpudEngine().setAsInstance();
            engineInstance.start();
        } catch (Exception e) {
            Logger.error(e);
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
        }
    }

    public static SpudEngine getEngineInstance() {
        return engineInstance;
    }
}
