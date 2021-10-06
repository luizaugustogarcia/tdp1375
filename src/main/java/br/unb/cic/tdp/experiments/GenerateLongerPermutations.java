package br.unb.cic.tdp.experiments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class GenerateLongerPermutations {

    static int getNum(final List<Integer> v) {
        int n = v.size();
        int index = (int)(Math.random() * n);
        int num = v.get(index);
        v.set(index, v.get(n - 1));
        v.remove(n - 1);
        return num;
    }

    static void generateRandom(final int n, final PrintStream out) {
        final var v = new ArrayList<Integer>(n);

        for (int i = 0; i < n; i++)
            v.add(i + 1);

        while (v.size() > 0) {
            final var r = getNum(v);
            if (v.size() == 0)
                out.print(r);
            else
                out.print(r + ",");
        }
    }

    public static void main(String []args) throws IOException {
        for (int j = 2; j <= 50; j++) {
            final var file = new File(args[0] + "large" + j * 10 + ".txt");
            file.createNewFile();
            final var oFile = new FileOutputStream(file, false);
            final var out = new PrintStream(oFile);
            for (int i = 0; i < 1000; i++) {
                out.print("0,");
                generateRandom(j * 10, out);
                out.println();
            }
        }
    }
}
