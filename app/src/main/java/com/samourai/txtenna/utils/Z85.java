package com.samourai.txtenna.utils;

public class Z85 {

    private static Z85 instance = null;

    private static final int[] decoders = new int[]{
            0x00, 0x44, 0x00, 0x54, 0x53, 0x52, 0x48, 0x00, 0x4B, 0x4C, 0x46, 0x41, 0x00, 0x3F, 0x3E, 0x45,
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x40, 0x00, 0x49, 0x42, 0x4A, 0x47,
            0x51, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32,
            0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x4D, 0x00, 0x4E, 0x43, 0x00,
            0x00, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18,
            0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x4F, 0x00, 0x50, 0x00, 0x00
    };

    private static char[] encoders = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D',
            'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', '.', '-', ':', '+', '=', '^', '!', '/', '*', '?', '&', '<', '>', '(', ')', '[', ']', '{',
            '}', '@', '%', '$', '#'
    };

    private Z85() { ; }

    public static Z85 getInstance() {

        if(instance == null)    {
            instance = new Z85();
        }

        return instance;

    }

    public byte[] decode(String s) {
        int remainder = s.length() % 5;
        int padding = 5 - (remainder == 0 ? 5 : remainder);
        for (int p = 0; p < padding; p++) {
            s += encoders[encoders.length - 1];
        }
        int length = s.length();
        byte[] ret = new byte[(length * 4 / 5) - padding];
        int index = 0;
        long value = 0;
        for (int i = 0; i < length; i++) {
            int code = s.charAt(i) - 32;
            value = value * 85 + decoders[code];
            if ((i + 1) % 5 == 0) {
                int div = (int)pow(256, 3); // 256 * 256 * 256
                while (div >= 1) {
                    if (index < ret.length) {
                        ret[index++] = (byte)((value / div) % 256);
                    }
                    div /= 256;
                }
                value = 0;
            }
        }

        return ret;
    }

    public String encode(byte[] bytes) {

        int remainder = bytes.length % 4;
        int padding = (remainder > 0) ? 4 - remainder : 0;
        StringBuilder ret = new StringBuilder();
        long value = 0;
        for (int i = 0; i < bytes.length + padding; i++) {
            boolean isPadding = i >= bytes.length;
            value = value * 256 + (isPadding ? 0 : bytes[i] & 0xFF);
            if ((i + 1) % 4 == 0) {
                int div = (int)pow(85, 4);  // 85 * 85 * 85 * 85
                for (int j = 5; j > 0; j--) {
                    if (!isPadding || j > padding) {
                        int code = (int)((value / div) % 85);
                        ret.append(encoders[code]);
                    }
                    div /= 85;
                }
                value = 0;
            }
        }

        return ret.toString();
    }

    public boolean isZ85(String s) {

        String regexZ85 = "^[0-9A-Za-z\\.\\-:\\+\\=\\^!\\/\\*\\?&<>\\(\\)\\[\\]\\{\\}\\@%\\$\\#]+$";
        String regexHEX = "^[0-9A-Fa-f]+$";

        if(s.matches(regexZ85) && !s.matches(regexHEX))    {
            return true;
        }
        else    {
            return false;
        }

    }

    private long pow(long a, int b)    {

        if(b == 1)    {
            return a;
        }
        if(b == 0){
            return 1;
        }
        if(b % 2 == 0)  {
            return pow(a * a, b / 2); // even a=(a^2)^b/2
        }
        else    {
            return a * pow(a * a, b / 2); // odd a=a*(a^2)^b/2
        }

    }

}
