package br.unb.cic.tdp.permutation;

import lombok.Getter;
import lombok.val;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class OneLinePermutation {

    private final byte[] oneLine;

    // ThreadLocal cache for CycleVisitor instances, indexed by buffer length (0-63)
    private static final ThreadLocal<CycleCollector[]> CYCLE_VISITOR_CACHE =
            ThreadLocal.withInitial(() -> new CycleCollector[64]);

    public OneLinePermutation(final byte[] oneLine) {
        this.oneLine = oneLine;
    }

    public int getNumberOfSymbols() {
        return oneLine.length;
    }

    public int image(final int symbol) {
        return oneLine[symbol] & 0xFF;
    }

    public int cycleCount() {
        final int[] count = {0};
        visitCycles(new CycleVisitor() {
            @Override
            public void endCycle(final byte start, final int size) {
                count[0]++;
            }
        }, false, 0);
        return count[0];
    }

    public int countTwoCycles() {
        final int[] count = {0};
        visitCycles(new CycleVisitor() {
            @Override
            public void endCycle(final byte start, final int size) {
                if (size == 2) {
                    count[0]++;
                }
            }
        }, false, 0);
        return count[0];
    }

    public int countEvenCycles() {
        final int[] count = {0};
        visitCycles(new CycleVisitor() {
            @Override
            public void endCycle(final byte start, final int size) {
                if (size % 2 == 1) {
                    count[0]++;
                }
            }
        }, false, 0);
        return count[0];
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OneLinePermutation other)) {
            return false;
        }
        return Arrays.equals(oneLine, other.oneLine);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(oneLine);
    }

    public void traverseCycle(byte start, final ByteConsumer o) {
        var current = start;
        do {
            o.accept(current);
            current = oneLine[current];
        } while (current != start);
    }

    @FunctionalInterface
    public interface ByteConsumer {

        void accept(byte value);
    }

    public List<byte[]> cycles() {
        final int bufferLength = this.getOneLine().length;

        // Get or create the cached visitor for this buffer length
        val cache = CYCLE_VISITOR_CACHE.get();
        var visitor = cache[bufferLength];

        if (visitor == null) {
            visitor = new CycleCollector(bufferLength);
            cache[bufferLength] = visitor;
        }

        // Reset the visitor for reuse
        visitor.reset();

        // Visit cycles using the cached visitor
        this.visitCycles(visitor, false, 0);

        return visitor.getCycles();
    }

    public void visitCycles(final CycleVisitor v) {
        visitCycles(v, false, 0);
    }

    /**
     * Visits all cycles; if skipFixedPoints=true, omits 1-cycles (i -> i).
     */
    public void visitCycles(
            final CycleVisitor v,
            final boolean skipFixedPoints,
            long visitedMask
    ) {
        val n = OneLinePermutation.this.getNumberOfSymbols();

        val all = (n == 64) ? -1L : ((1L << n) - 1L);
        var unvisited = (~visitedMask) & all;

        while (unvisited != 0) {
            var start = Long.numberOfTrailingZeros(unvisited);

            val img = OneLinePermutation.this.image(start);
            if (skipFixedPoints && img == start) {
                visitedMask |= (1L << start);
                unvisited &= unvisited - 1; // clear lowest set bit
                continue;
            }

            val bStart = (byte) start;
            v.beginCycle(bStart);

            var x = start;
            var size = 0;
            do {
                v.symbol((byte) x);
                visitedMask |= (1L << x);
                unvisited &= ~(1L << x);
                size++;
                x = OneLinePermutation.this.image(x);
            } while (x != start);

            v.endCycle(bStart, size);
        }
    }

    public abstract static class CycleVisitor {
        public void beginCycle(final byte start) {
        }

        public void symbol(final byte x) {
        }

        public void endCycle(final byte start, final int size) {
        }
    }

    /**
     * Reusable CycleVisitor implementation for collecting cycles.
     * Designed to be cached in ThreadLocal to reduce garbage generation.
     */
    private static class CycleCollector extends CycleVisitor {
        private final byte[] buffer;
        private List<byte[]> cycles;
        private int size;

        CycleCollector(final int bufferLength) {
            this.buffer = new byte[bufferLength];
            this.cycles = new ArrayList<>();
            this.size = 0;
        }

        void reset() {
            cycles = new ArrayList<>();
            size = 0;
        }

        List<byte[]> getCycles() {
            return cycles;
        }

        @Override
        public void beginCycle(final byte start) {
            size = 0;
        }

        @Override
        public void symbol(final byte x) {
            buffer[size++] = x;
        }

        @Override
        public void endCycle(final byte start, final int size) {
            cycles.add(Arrays.copyOf(buffer, size));
        }
    }
}
