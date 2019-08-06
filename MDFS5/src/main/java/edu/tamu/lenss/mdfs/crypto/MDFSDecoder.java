package edu.tamu.lenss.mdfs.crypto;

import java.io.File;
import java.util.List;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;


public class MDFSDecoder {
	private static final String TAG = MDFSDecoder.class.getSimpleName(); 
	private int n2, k2;	// Can be accesses from shares and fileFragments
	private byte[] rawSecretKey;
	
	public MDFSDecoder(int k, int n, byte[] secretKey){
		this.n2 = n;
		this.k2 = k;
		this.rawSecretKey = secretKey;
	}
	
	public boolean decodeNow(final List<FragmentInfo> fileFragments, String decodedFilePath){
		// Combine
		byte[] encryptedBytes = combineFileFragments(fileFragments);
		if(encryptedBytes == null)
			return false;
		File tmpEncryptFile = IOUtilities.byteToFile(encryptedBytes, 
				AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_CACHE), "decodercache_" + System.nanoTime());
		// Decrypt
		MDFSCipher myCipher = MDFSCipher.getInstance();
		boolean isSuccess = myCipher.decrypt(tmpEncryptFile.getAbsolutePath(), decodedFilePath, rawSecretKey);
		tmpEncryptFile.delete();
		return isSuccess;
	}
	
	
	private byte[] combineFileFragments(final List<FragmentInfo> fileFragments){
		int blocksize = fileFragments.get(0).getFragment().length;
		long filesize = fileFragments.get(0).getFilesize();
		int m = n2-k2;
		
		// Jerasure parameters
		int numerased = 0;
		int[] erased = new int[n2];
		int[] erasures = new int[n2];
		byte[][] data = new byte[k2][blocksize];
		byte[][] coding = new byte[m][blocksize];
		Logger.v(TAG, "Block Size: " + blocksize + " File Size: " + filesize);
		// initialize erased
		for (int i = 0; i < n2; i++) {
			erased[i] = 0;
		}
		
		// initialize data and coding
		for (int i = 0; i < k2; i++) {
			data[i] = null;
		}
		for (int i = 0; i < m; i++) {
			coding[i] = null;
		}
		int index; 
		for(FragmentInfo frag : fileFragments){
			index = frag.getFragmentNumber();
			if(frag.getType() == FragmentInfo.DATA_TYPE){
				data[index] = frag.getFragment();
			}
			else{
				coding[index-k2]=frag.getFragment();
			}
		}

		// process erased and erasures
		for (int i = 0; i < k2; i++) {
			if (data[i] == null) {
				erased[i] = 1;
				erasures[numerased] = i;
				numerased++;
			}
		}
		for (int i = 0; i < m; i++) {
			if (coding[i] == null) {
				erased[k2 + i] = 1;
				erasures[numerased] = k2 + i;
				numerased++;
			}
		}
		
		erasures[numerased] = -1;	// Indicate the end of erasure
		if(numerased > m){
			Logger.e(TAG, "Not Enough Fragments to recover the file");
			return null;
		}
		
		byte[] encryptedByteFile = new ReedSolomon().decoder(data, coding,
				erasures, erased, filesize, blocksize, k2, n2);
		
		Logger.v(TAG, encryptedByteFile != null ? "Combine Successfully" : "Combine Failed");
		return encryptedByteFile;
	}
	
	
}
