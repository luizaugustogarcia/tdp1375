package br.unb.cic.tdp.util;

public class CyclicOrderChecker {
    public static boolean isCyclicOrder(int[] p, int a, int b, int c) {
        int n = p.length;
        int posA = -1, posB = -1, posC = -1;

        // Find positions of a, b, c in the array
        for (int i = 0; i < n; i++) {
            if (p[i] == a) posA = i;
            else if (p[i] == b) posB = i;
            else if (p[i] == c) posC = i;
        }

        // If any of the elements is not found
        if (posA == -1 || posB == -1 || posC == -1) return false;

        // Normalize positions to determine order
        // Convert all positions to be relative to posA
        int relB = (posB - posA + n) % n;
        int relC = (posC - posA + n) % n;

        // a -> b -> c iff relB < relC
        return relB != 0 && relC != 0 && relB < relC;
    }
}
