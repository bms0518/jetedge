package com.rainbowpunch.interfaces.limiters;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This limiter allows the user to add objects that are permitted for injection into a field.
 */
public class DefaultObjectLimiter<T> extends ObjectLimiter<T> {

    private List<T> listOfObjects;

    public DefaultObjectLimiter(List<T> listOfObjects) {
        configureObjectList(listOfObjects);
    }

    public DefaultObjectLimiter(T... listOfObjects) {
        configureObjectList(Arrays.asList(listOfObjects));
    }

    @Override
    protected List<T> configureObjectList() {
        return null;
    }
}
