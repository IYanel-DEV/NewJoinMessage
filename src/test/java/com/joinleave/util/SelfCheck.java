package com.joinleave.util;

/** Runs every self-check so `mvn exec:java` verifies the whole plugin in one command. */
public final class SelfCheck {

    private SelfCheck() {
    }

    public static void main(String[] args) throws Exception {
        ColorUtilsSelfCheck.main(args);
        YamlResourcesSelfCheck.main(args);
        PlayerStoreSelfCheck.main(args);
        System.out.println("All self-checks passed.");
    }
}
