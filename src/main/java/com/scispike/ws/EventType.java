package com.scispike.ws;

public enum EventType {
    OPEN("o"), ACCEPT("a"), MESSAGE("m"), CLOSE("c"), HEARTBEAT("h"), ;
    String name;
    EventType(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public static EventType getFromName(String name) {
        for (EventType event : EventType.values()) {
            if (event.getName().equals(name)) {
                return event;
            }
        }
        return null;
    }

}
