package org.recursivedescent;

public class PointedArray {
    public final byte[] array;
    public int cursor;

    public PointedArray(byte[] array, int initial) {
        this.array = array;
        this.cursor = initial;
    }

    public byte head() {
        return array[cursor];
    }

    public short shortHead() {
        return (short) ((array[cursor] << 8) | array[cursor + 1]);
    }

    public PointedArray tail() {
        return new PointedArray(array, cursor + 1);
    }

    public PointedArray shortTail() {
        return new PointedArray(array, cursor + 2);
    }

    public PointedArray shift(int n) {
        return new PointedArray(array, cursor + n);
    }

    public String toString() {
        return "-> Size: " + array.length + " Cursor: " + cursor;
    }
}
