package ru.andreymarkelov.atlas.plugins.simplecal;

public class Counter {
    private static long val = System.currentTimeMillis();

    public synchronized static long getVal() {
        return val++;
    }

    private Counter() {}
}
