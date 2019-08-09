/**
 * Command-line program that decodes a file using Reed-Solomon 4+2.
 *
 * Copyright 2015, Backblaze, Inc.  All rights reserved.
 */

package edu.tamu.lenss.mdfs.ReedSolomon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Command-line program that decodes a file using Reed-Solomon 4+2.
 *
 * The file name given should be the name of the file to decode, say
 * "foo.txt".  This program will expected to find "foo.txt.0" through
 * "foo.txt.5", with at most two missing.  It will then write
 * "foo.txt.decoded".
 */
public class SampleDecoder {

    public static final int K2 = 4;
    public static final int PARITY_SHARDS = 2;
    public static final int N2 = 6;

    public static final int BYTES_IN_INT = 4;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String [] arguments) throws IOException {

        // Parse the command line
        if (arguments.length != 1) {
            System.out.println("Usage: SampleDecoder <fileName>");
            return;
        }
        final File originalFile = new File(arguments[0]);
        if (!originalFile.exists()) {
            System.out.println("Cannot read input file: " + originalFile);
            return;
        }

        // Read in any of the shards that are present.
        // (There should be checking here to make sure the input
        // shards are the same size, but there isn't.)
        final byte [] [] shards = new byte [N2] [];
        final boolean [] shardPresent = new boolean [N2];
        int shardSize = 0;
        int shardCount = 0;
        for (int i = 0; i < N2; i++) {
            File shardFile = new File(originalFile.getParentFile(), originalFile.getName() + "." + i);
            if (shardFile.exists()) {
                shardSize = (int) shardFile.length();
                shards[i] = new byte [shardSize];
                shardPresent[i] = true;
                shardCount += 1;
                InputStream in = new FileInputStream(shardFile);
                in.read(shards[i], 0, shardSize);
                in.close();
                System.out.println("Read " + shardFile);
            }
        }

        // We need at least K2 to be able to reconstruct the file.
        if (shardCount < K2) {
            System.out.println("Not enough shards present");
            return;
        }

        // Make empty buffers for the missing shards.
        for (int i = 0; i < N2; i++) {
            if (!shardPresent[i]) {
                shards[i] = new byte [shardSize];
            }
        }

        // Use Reed-Solomon to fill in the missing shards
        ReedSolomon reedSolomon = ReedSolomon.create(K2, PARITY_SHARDS);
        reedSolomon.decodeMissing(shards, shardPresent, 0, shardSize);

        // Combine the data shards into one buffer for convenience.
        // (This is not efficient, but it is convenient.)
        byte [] allBytes = new byte [shardSize * K2];
        for (int i = 0; i < K2; i++) {
            System.arraycopy(shards[i], 0, allBytes, shardSize * i, shardSize);
        }

        // Extract the file length
        int fileSize = ByteBuffer.wrap(allBytes).getInt();

        // Write the decoded file
        File decodedFile = new File(originalFile.getParentFile(), originalFile.getName() + ".decoded");
        OutputStream out = new FileOutputStream(decodedFile);
        out.write(allBytes, BYTES_IN_INT, fileSize);
        System.out.println("Wrote " + decodedFile);
    }
}
