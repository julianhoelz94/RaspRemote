package com.julianhoelz.raspremote;

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import java.io.Serializable;
import java.util.HashMap;


public class Opener implements Serializable {

    static {
        System.setProperty("pi4j.linking", "dynamic");
    }

    private String name;
    private String key;
    private String confirmation;
    private Mode mode;
    private Pin pin;
    private GpioPinDigitalOutput relay;

    private static HashMap<Pin, GpioPinDigitalOutput> pins = new HashMap<>();

    public Opener(String name, String key, int address, String confirmation, Mode mode) {
        this.name = name;
        this.key = key;
        this.confirmation = confirmation;
        this.mode = mode;
        this.pin = RaspiPin.getPinByAddress(address);
        if (pins.containsKey(pin)) {
            this.relay = pins.get(pin);
        } else {
            this.relay = GpioFactory.getInstance().provisionDigitalOutputPin(pin, name, PinState.HIGH);
            pins.put(pin, relay);
        }
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public String getConfirmation() {
        return confirmation;
    }

    public void action() {
        switch (mode) {
            case ON:
                relay.low();
                break;
            case OFF:
                relay.high();
                break;
            case SWITCH:
                relay.toggle();
                break;
            case IMPULSE:
                relay.toggle();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                relay.toggle();
                break;
        }

    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        text.append("Name: " + name + "\n");
        text.append("Pin: " + pin + "\n");
        text.append("Key: " + key + "\n");
        text.append("Confirmation: " + confirmation + "\n");
        text.append("Mode: " + mode + "\n");
        return text.toString();
    }
}
