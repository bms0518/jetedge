package com.rainbowpunch.jetedge.core.limiters.collections;

import com.rainbowpunch.jetedge.core.exception.LimiterConstructionException;
import com.rainbowpunch.jetedge.core.exception.PojoConstructionException;
import com.rainbowpunch.jetedge.core.limiters.Limiter;
import com.rainbowpunch.jetedge.core.limiters.RequiresDefaultLimiter;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A Limiter for creating lists.
 */
public class ListLimiter implements Limiter<List<Object>>, RequiresDefaultLimiter<ListLimiter> {

    private final int range;
    private final int offset;
    private Limiter limiter;

    public static ListLimiter createListLimiter(Limiter<?> limiter) {
        return new ListLimiter(limiter);
    }

    public ListLimiter(Limiter limiter) {
        this(limiter, 2, 5);
    }

    public ListLimiter(int range, int offset) {
        this(null, range, offset);
    }

    public ListLimiter(Limiter limiter, int range, int offset) {
        this.range = range;
        this.offset = offset;
        this.limiter = limiter;
        validate();
    }

    public Limiter getLimiter() {
        return limiter;
    }

    private void validate() {
        if (range < 0 || offset < 0) {
            throw new LimiterConstructionException("Error creating ListLimiter : Offset and Range cannot be less than 0");
        }
    }

    @Override
    public boolean hasLimiter() {
        return limiter != null;
    }

    @Override
    public ListLimiter reconcile(ListLimiter baseLimiter) {
        return new ListLimiter(baseLimiter.getLimiter(), this.range, this.offset);
    }

    @Override
    public Supplier<List<Object>> generateSupplier(Random random) {
        if (this.limiter == null) {
            throw new LimiterConstructionException("Error creating ListLimiter : Missing internal Limiter");
        }
        return () -> {
            try {
                int count = range == 0 ? offset : random.nextInt(range) + offset;
                return IntStream.range(0, count)
                        .mapToObj(i -> limiter.generateSupplier(random).get())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new PojoConstructionException("Failed to create object for ListLimiter: ", e);
            }
        };
    }
}
