package network.artic.clusterfunk;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.BitSet;
import java.util.Set;

public class Constellation {
    static String constellation1 = "ABCDEFGHI";
    static String constellation2 = "0BCDEFGHI";
    static String constellation3 = "A0CDEFGHI";
    static String constellation4 = "AB0DEFGHI";
    static String constellation5 = "BBCDEFGHI";
    static String constellation6 = "AXCDEFGHI";
    static String constellation7 = "ABYDEFGHI";

    static final char REFERENCE_CHARACTER = '0';

    static String getHexCode(String constellation) {
        long number = 0;
        int shift = 0;
        for (byte b : constellation.getBytes()) {
            number += (b - 'A') << shift;
            shift += 3;
        }
        return String.format("%06x", number);
    }

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    static String getBase64Code(String constellation) {
        long number = 0;
        int shift = 0;
        for (byte b : constellation.getBytes()) {
            number += (b - 'A') << shift;
            shift += 3;
        }
        return Base64.getEncoder().withoutPadding().encodeToString(longToBytes(number));
    }

    public static void main(String[] args) {
        System.out.println(constellation1 + " -> " + getHexCode(constellation1) + " -> " + getBase64Code(constellation1));
        System.out.println(constellation2 + " -> " + getHexCode(constellation2) + " -> " + getBase64Code(constellation2));
        System.out.println(constellation3 + " -> " + getHexCode(constellation3) + " -> " + getBase64Code(constellation3));
        System.out.println(constellation4 + " -> " + getHexCode(constellation4) + " -> " + getBase64Code(constellation4));
        System.out.println(constellation5 + " -> " + getHexCode(constellation5) + " -> " + getBase64Code(constellation5));
        System.out.println(constellation6 + " -> " + getHexCode(constellation6) + " -> " + getBase64Code(constellation6));
        System.out.println(constellation7 + " -> " + getHexCode(constellation7) + " -> " + getBase64Code(constellation7));
    }
}
