package com.servicenow.ctaskcreator;

public class Launcher {
    public static void main(String[] args) {
        // Disable XML entity size limit for large Excel files
        System.setProperty("jdk.xml.entityExpansionLimit", "0");
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");

        Main.main(args);
    }
}
