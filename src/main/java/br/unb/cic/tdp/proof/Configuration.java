package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.hash.HashCodes;
import com.google.common.hash.Hashing;

import java.util.TreeSet;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.CommonOperations.signature;
import static br.unb.cic.tdp.util.ByteArrayOperations.compare;

public class Configuration {

    private final Cycle pi;
    private final MulticyclePermutation spi;
    private final byte[] signature;
    private TreeSet<byte[]> signatures;
    private Integer hashCode;

    public Configuration(final Cycle pi, final MulticyclePermutation spi) {
        this.pi = pi;
        this.spi = spi;
        this.signature = signature(spi, pi);
    }

    private TreeSet<byte[]> getSignatures() {
        if (signatures != null) {
            return signatures;
        }

        signatures = new TreeSet<>((a, b) -> compare(a, b));

        // shifting
        for (var i = 0; i < pi.size(); i++) {
            signatures.add(
                    signature(spi, pi.getStartingBy(pi.get(i))));
        }

        // mirroring
        for (var i = 0; i < pi.getInverse().size(); i++) {
            signatures.add(
                    signature(spi, pi.getInverse().getStartingBy(pi.getInverse().get(i))));
        }

        return signatures;
    }

    public Cycle getPi() {
        return pi;
    }

    public MulticyclePermutation getSpi() {
        return spi;
    }

    @Override
    public int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = Hashing.combineOrdered(getSignatures().stream().map(HashCodes::fromBytes)
                    .collect(Collectors.toList())).hashCode();
        }
        return this.hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Configuration)) {
            return false;
        }

        final var other = (Configuration) obj;

        if (this.signature.length != other.signature.length || this.spi.size() != other.spi.size()) {
            return false;
        }

        return getSignatures().contains(((Configuration) obj).signature);
    }

    @Override
    public String toString() {
        return "Configuration{ spi=" + spi + ", hashCode=" + hashCode() + " }";
    }
}
