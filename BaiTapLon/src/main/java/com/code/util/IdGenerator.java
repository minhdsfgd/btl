package com.code.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sinh ID duy nhất tăng dần — thread-safe.
 * Dùng AtomicInteger thay vì int++ để tránh trùng ID khi nhiều thread gọi đồng thời.
 */
public class IdGenerator {
    private static final AtomicInteger counter = new AtomicInteger(1);

    private IdGenerator() {}

    /** Trả về ID tiếp theo, luôn tăng dần, an toàn với đa luồng. */
    public static int getId() {
        return counter.getAndIncrement();
    }

    /** Reset về 1 — chỉ dùng cho unit test. */
    public static void reset() {
        counter.set(1);
    }
}