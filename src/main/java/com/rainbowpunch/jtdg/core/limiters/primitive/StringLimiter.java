package com.rainbowpunch.jtdg.core.limiters.primitive;

import com.rainbowpunch.jtdg.core.limiters.ObjectLimiter;
import com.rainbowpunch.jtdg.util.ReadableCharList;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
public class StringLimiter extends ObjectLimiter<String> {

    private Integer length = 30;

    @Override
    protected List<String> configureObjectList() {
        return ReadableCharList.LIST_OF_ALL_CHAR.stream()
                .map(ch -> Character.toString(ch))
                .collect(Collectors.toList());
    }

    @Override
    public Supplier<String> generateSupplier(Random random) {
        return () -> {
            return IntStream.range(0, length)
                    .sequential()
                    .mapToObj((i) -> super.generateSupplier(random))
                    .map(Supplier::get)
                    .collect(Collectors.joining());
        };
    }
}