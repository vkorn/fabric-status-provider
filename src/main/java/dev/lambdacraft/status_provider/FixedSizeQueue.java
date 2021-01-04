package dev.lambdacraft.status_provider;

import java.util.ArrayList;

public class FixedSizeQueue<T> extends ArrayList<T> {
    private int maxSize;

    public FixedSizeQueue(int size) {
        this.maxSize = size;
    }

    public boolean add(T t) {
        boolean r = super.add(t);

        if (size() > maxSize) {
            removeRange(0, size() - this.maxSize);
        }

        return r;
    }
}
