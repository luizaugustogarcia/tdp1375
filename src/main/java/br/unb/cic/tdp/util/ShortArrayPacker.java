package br.unb.cic.tdp.util;

public class ShortArrayPacker {

    // Pack shorts (0..25) into a compact byte[]
    public static byte[] encode(short[] arr) {
        int bitCount = arr.length * 5;
        int byteCount = (bitCount + 7) / 8 + 1; // +1 for the size byte
        byte[] bytes = new byte[byteCount];

        bytes[0] = (byte) arr.length; // Store the size in the first byte

        int bitPos = 8; // Start after the first byte
        for (short val : arr) {
            if (val < 0 || val > 25) {
                throw new IllegalArgumentException("Value out of range: " + val);
            }
            for (int b = 0; b < 5; b++) {
                if (((val >> b) & 1) == 1) {
                    int pos = bitPos + b;
                    bytes[pos / 8] |= (1 << (pos % 8));
                }
            }
            bitPos += 5;
        }
        return bytes;
    }

    public static short[] decode(byte[] bytes) {
        int length = bytes[0] & 0xFF; // Extract the size from the first byte
        short[] arr = new short[length];
        int bitPos = 8; // Start after the first byte
        for (int i = 0; i < length; i++) {
            int val = 0;
            for (int b = 0; b < 5; b++) {
                int pos = bitPos + b;
                if (((bytes[pos / 8] >> (pos % 8)) & 1) == 1) {
                    val |= (1 << b);
                }
            }
            arr[i] = (short) val;
            bitPos += 5;
        }
        return arr;
    }
}
