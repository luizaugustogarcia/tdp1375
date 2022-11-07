package br.unb.cic.tdp.experiments;

import lombok.val;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class GenerateLongerPermutations {

    static int getNum(final List<Integer> v) {
        val n = v.size();
        val index = (int)(Math.random() * n);
        val num = v.get(index);
        v.set(index, v.get(n - 1));
        v.remove(n - 1);
        return num;
    }

    static void generateRandom(final int n, final PrintStream out) {
        val v = new ArrayList<Integer>(n);

        for (var i = 0; i < n; i++)
            v.add(i + 1);

        while (v.size() > 0) {
            val r = getNum(v);
            if (v.size() == 0)
                out.print(r);
            else
                out.print(r + ",");
        }
    }

    public static void main(final String []args) throws IOException {
        for (var j = 2; j <= 50; j++) {
            val file = new File(args[0] + "large" + j * 10 + ".txt");
            file.createNewFile();
            val oFile = new FileOutputStream(file, false);
            val out = new PrintStream(oFile);
            for (int i = 0; i < 1000; i++) {
                out.print("0,");
                generateRandom(j * 10, out);
                out.println();
            }
        }
    }
}
