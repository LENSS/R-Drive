package edu.tamu.lenss.mdfs.crypto;

import edu.tamu.lenss.mdfs.utils.Logger;

public class ReedSolomon {
	private static final String DEBUG_TAG = "ReedSolomon";
    private final int WORDSIZE = 8;

    //Native method declaration
    private native byte[] decode(byte[][] data, byte[][] coding, int[] erasures, int[] erased,
                    long filesize, int blocksize, int k, int n, int w);
    //private native byte[] decode(byte[][] data, byte[][] coding, int[] erasures);

    private native byte[][] encode(byte[] fileToEncode, int k, int n, int w);

    // dummy test function
    private native void dummyCall();

    //Load the library
    static {
    	try{
    		System.loadLibrary("ReedSolomon");
    		//System.load("./libs/armeabi/ReedSolomon.dll");
    	} catch (UnsatisfiedLinkError ule) {
    		Logger.e(DEBUG_TAG, "WARNING: Could not load ReedSolomon.so");
    		System.err.println("WARNING: Could not load ReedSolomon Library");
    	}
    }

    /**
     * @param byte[][] data - a byte array containing byte[] data fragments
     * @param byte[][] coding - a byte array containing byte[] coding fragments
     * @param erased
     * @param erasures
     * @param filesize
     * @param blocksize
     * @param k - the threshold value
     * @param n - the number of fragments
     * @return byte[] combined - the byte[] array containing the combined message
     */
    public byte[] decoder(byte[][] data, byte[][] coding, int[] erasures, int[] erased, long filesize, int blocksize, int k, int n) {
        return decode(data, coding, erasures, erased, filesize, blocksize, k, n, WORDSIZE);

    }


    /**
     * @param byte[] inBytes the data to be erasure encoded in byte[] form
     * @param int k - the threshold
     * @param int n - the number of fragments to generate
     * @return byte[][] - a byte array of length n containing a byte array for each encoded chunk
     */
    public byte[][] encoder(byte[] file, int k, int n) {

        return encode(file, k, n, WORDSIZE);
    }

}
