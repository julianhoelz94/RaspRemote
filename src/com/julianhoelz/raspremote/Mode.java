package com.julianhoelz.raspremote;

public enum Mode {
    ON("on"),
    OFF("off"),
    SWITCH("switch"),
    IMPULSE("impulse");

    private final String name;

    Mode(String name) {
        this.name = name;
    }

    public static Mode getMode(String name) {
        switch(name) {
            case "on":
                return ON;
            case "off":
                return OFF;
            case "switch":
                return SWITCH;
            case "impulse":
                return IMPULSE;
            default:
                return null;
        }
    }

}
