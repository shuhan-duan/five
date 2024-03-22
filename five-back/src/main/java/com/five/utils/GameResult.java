package com.five.utils;

public enum GameResult {
    CONTINUE(0),
    BLACK_WIN(1),
    WHITE_WIN(2),
    DRAW(3);

    private final int value;

    GameResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static GameResult getGameResultFromPlayer(int i) {
        for (GameResult result : GameResult.values()) {
            if (result.getValue() == i) {
                return result;
            }
        }
        return CONTINUE; // Default
    }
}
