/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * As per the Apache license requirements, this file has been modified
 * from its original state.
 *
 * Such modifications are Copyright (C) 2010 Ben Gruver, and are released
 * under the original license
 */

package org.jf.dexlib.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * LEB128 (little-endian base 128) utilities.
 */
public final class Leb128Utils {
    /**
     * This class is uninstantiable.
     */
    private Leb128Utils() {
        // This space intentionally left blank.
    }

    /**
     * Gets the number of bytes in the unsigned LEB128 encoding of the
     * given value.
     *
     * @param value the value in question
     * @return its write size, in bytes
     */
    public static int unsignedLeb128Size(int value) {
        // TODO: This could be much cleverer.

        int remaining = value >>> 7;
        int count = 0;

        while (remaining != 0) {
            value = remaining;
            remaining >>>= 7;
            count++;
        }

        return count + 1;
    }
    
    public static final int getShort(byte[] array, int offset) {
        return (array[offset + 1] & 0xff) << 8 | array[offset] & 0xff; 
    }

    public static final Pair<Integer, Integer> readLEB(byte[] array, int offset) {
        int val = array[offset];
        boolean more = (val & 0x80) != 0;
        val &= 0x7f;
        
        if (!more)
            return new Pair<Integer, Integer>(val, 1);
        else 
            return new Pair<Integer, Integer>(val << 8 | array[offset + 1] & 0xff, 2);
    }
    
    /**
     * Gets the number of bytes in the signed LEB128 encoding of the
     * given value.
     *
     * @param value the value in question
     * @return its write size, in bytes
     */
    public static int signedLeb128Size(int value) {
        // TODO: This could be much cleverer.

        int remaining = value >> 7;
        int count = 0;
        boolean hasMore = true;
        int end = ((value & Integer.MIN_VALUE) == 0) ? 0 : -1;

        while (hasMore) {
            hasMore = (remaining != end)
                || ((remaining & 1) != ((value >> 6) & 1));

            value = remaining;
            remaining >>= 7;
            count++;
        }

        return count;
    }

    /**
     * Writes an unsigned leb128 to the buffer at the specified location
     * @param value the value to write as an unsigned leb128
     * @param buffer the buffer to write to
     * @param bufferIndex the index to start writing at
     */
    public static void writeUnsignedLeb128(int value, byte[] buffer, int bufferIndex) {
        int remaining = value >>> 7;

        while (remaining != 0) {
            buffer[bufferIndex] = (byte)((value & 0x7f) | 0x80);
            bufferIndex++;
            value = remaining;
            remaining >>>= 7;
        }

        buffer[bufferIndex] = (byte)(value & 0x7f);
    }
    
    public static byte[] unsignedLeb128(int value) throws IOException{
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	int remaining = value >>> 7;
    	while(remaining != 0){
    		bos.write((byte)((value & 0x7f) | 0x80));
    		value = remaining;
    		remaining >>>= 7;
    	}
    	bos.write((byte)(value & 0x7f));
    	return bos.toByteArray();
    }
}