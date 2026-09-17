package com.example.aquaplay;

public class GameLevel {
    private int currentLevel = 1;

    public int getCurrent() {
        return currentLevel;
    }

    public void next() {
        if (currentLevel < 7) {
            currentLevel++;
        }
    }

    public void reset() {
        currentLevel = 1;
    }

    public int getIntroMessageId() {
        switch (currentLevel) {
            case 1: return R.string.msg_inicio;
            case 2: return R.string.msg_nivel2_intro;
            case 3: return R.string.msg_nivel3_intro;
            case 4: return R.string.msg_nivel4_intro;
            case 5: return R.string.msg_nivel5_intro;
            case 6: return R.string.msg_nivel6_intro;
            default: return R.string.msg_nivel7_intro;
        }
    }

    public boolean isFinal() {
        return currentLevel == 7;
    }
}
