package fr.bakaaless.api.utils;

public class Pair<F, S> {

    private final F firstValue;
    private final S secondValue;

    public Pair(final F firstValue, final S secondValue) {
        this.firstValue = firstValue;
        this.secondValue = secondValue;
    }

    public F getFirst() {
        return this.firstValue;
    }

    public S getSecond() {
        return this.secondValue;
    }

    public static <F, S> Pair<F, S> from(final F firstValue, final S secondValue) {
        return new Pair<>(firstValue, secondValue);
    }
}
