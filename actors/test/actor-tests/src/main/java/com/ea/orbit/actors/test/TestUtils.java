package com.ea.orbit.actors.test;

public class TestUtils
{
    private static final char[] HEXES = "0123456789abcdef".toCharArray();

    public static String hexDump(int columns, byte[] raw, int offset, int length)
    {
        int i = offset, j = offset, end = offset + length;

        int x = 0, w = ((length / columns) + 1) * columns;
        final StringBuilder hex = new StringBuilder(w * 4 + columns + 20);
        hex.append("size: ").append(length).append("\r\n");
        for (; x < w; x++)
        {
            if (i < end)
            {
                // while there are chars to read
                final byte ch = raw[i++];
                hex.append(HEXES[(ch & 0xF0) >> 4]).append(HEXES[ch & 0x0F]);
            }
            else
            {
                // complete the rest of the line
                hex.append(' ').append(' ');
            }
            hex.append((x % 8 == 7) ? '|' : ' ');
            if (x % columns == (columns - 1))
            {
                // print char representation of the bytes
                hex.append(' ');
                for (; j < i; j++)
                {
                    final byte ch = raw[j];
                    hex.append(ch >= 32 ? (char) ch : '_');
                }
                hex.append("\r\n");
            }
        }
        return hex.toString();
    }

}
