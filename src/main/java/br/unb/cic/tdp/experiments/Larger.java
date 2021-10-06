package br.unb.cic.tdp.experiments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

public class Larger {

    // Function to return the next random number
    static int getNum(ArrayList<Integer> v)
    {
        // Size of the vector
        int n = v.size();

        // Make sure the number is within
        // the index range
        int index = (int)(Math.random() * n);

        // Get random number from the vector
        int num = v.get(index);

        // Remove the number from the vector
        v.set(index, v.get(n - 1));
        v.remove(n - 1);

        // Return the removed number
        return num;
    }

    // Function to generate n
    // non-repeating random numbers
    static void generateRandom(int n, PrintStream out)
    {
        ArrayList<Integer> v = new ArrayList<>(n);

        // Fill the vector with the values
        // 1, 2, 3, ..., n
        for (int i = 0; i < n; i++)
            v.add(i + 1);

        // While vector has elements
        // get a random number from the vector and print it
        while (v.size() > 0)
        {
            out.print(getNum(v) + ",");
        }
    }

    // Driver code
    public static void main(String []args) throws IOException {

        for (int j = 2; j <= 10; j++) {
            File yourFile = new File("C:\\Users\\Luiz Silva\\Projects\\tdp1375\\src\\main\\resources\\large" + j * 10 + ".txt");
            yourFile.createNewFile(); // if file already exists will do nothing
            FileOutputStream oFile = new FileOutputStream(yourFile, false);
            PrintStream out = new PrintStream(oFile);
            for (int i = 0; i < 1000; i++) {
                out.print("0,");
                generateRandom(j * 10, out);
                out.println();
            }
        }

    }
}
