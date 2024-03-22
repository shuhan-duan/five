package com.five.utils;

public enum MessageType {
    CONFIRM_JOIN_ROOM(0),
    JOIN_ROOM(1),
    USER_ACTOR_CONFIRM(2),
    CHAT_MESSAGE(3),
    MOVE(4) ,
    ROOM_COUNT_UPDATE(6),
    OBSERVER_UPDATE(7);

    private final int value;

    MessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
