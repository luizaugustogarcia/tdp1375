package br.unb.cic.tdp.util;

import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

@Getter
public class Pair<K, V> implements Serializable {
    private final K key;
    private final V value;

    public Pair(K k, V v) {
        key = k;
        value = v;
    }

    public Pair(Pair<? extends K, ? extends V> entry) {
        this(entry.getKey(), entry.getValue());
    }

    public K getFirst() {
        return key;
    }

    public V getSecond() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pair)) {
            return false;
        } else {
            Pair<?, ?> oP = (Pair<?, ?>) o;
            return (Objects.equals(key, oP.key)) &&
                    (Objects.equals(value, oP.value));
        }
    }

    @Override
    public int hashCode() {
        int result = key == null ? 0 : key.hashCode();

        final int h = value == null ? 0 : value.hashCode();
        result = 37 * result + h ^ (h >>> 16);

        return result;
    }
}
