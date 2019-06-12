package edu.tamu.lenss.mdfs.crypto;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class MDFSEncoder {
	private static final String TAG = MDFSEncoder.class.getSimpleName(); 
	private File clearFile;
	private byte  n2, k2;
	private byte[] rawSecretKey;
	private long timeStamp;
	
	public MDFSEncoder(File file, byte n2, byte k2){
		this.clearFile = file;
		this.timeStamp = file.lastModified();
		this.n2 = n2;
		this.k2 = k2;
		this.timeStamp = System.currentTimeMillis();
	}
	
	
	public List<FragmentInfo> encodeNow(){
		File tmpFile = AndroidIOUtils.getExternalFile(Constants.DIR_CACHE + File.separator + "encrypt_" + clearFile.getName());
		IOUtilities.createNewFile(tmpFile);
		MDFSCipher myCipher = MDFSCipher.getInstance();
		if(rawSecretKey == null){
			generateRawSecreteKey();
		}
		if(myCipher.encrypt(clearFile.getAbsolutePath(), tmpFile.getAbsolutePath(), rawSecretKey)){
			byte[] encryptedByte = IOUtilities.fileToByte(tmpFile); 
			tmpFile.delete();
			return generateFileShares(encryptedByte);
		}
		tmpFile.delete();
		return null;
	}
	
	public void setKey(byte[] key){
		this.rawSecretKey = key;
	}
	
	private void generateRawSecreteKey(){
		// Make Sure the generated key is a positive BigInteger.
		do{
			// Generate the secret key
			SecretKey skey = MDFSCipher.getInstance().generateSecretKey();
			rawSecretKey = skey.getEncoded(); 
			
			// Store the file fragments in local SDCard
			/*File keyDir = AndroidIOUtils.getExternalFile(Constants.DIR_ROOT);
			IOUtilities.byteToFile(rawSecretKey, keyDir, "keystore");			
			rawSecretKey = IOUtilities.fileToByte(AndroidIOUtils.getExternalFile(Constants.DIR_ROOT + "/keystore"));*/
			
		}while(new BigInteger(rawSecretKey).signum()<=0);
	}
	
	
	/*
	 * Need encryptedByteFile, k2, n2
	 */
	private List<FragmentInfo> generateFileShares(byte[] encryptedByteFile){
		List<FragmentInfo> fileFragments = new ArrayList<FragmentInfo>();
		
		try{
			byte[][] fragments = new ReedSolomon().encoder(encryptedByteFile, (int)k2, (int)n2);
			if(fragments == null){
				return null;
			}
			byte type;
			for(int i=0; i < fragments.length; i++){
				if(i < k2)
					type = FragmentInfo.DATA_TYPE;
				else
					type = FragmentInfo.CODING_TYPE;
				
				fileFragments.add(new FragmentInfo(clearFile.getName(), type, encryptedByteFile.length, fragments[i], (byte)i, k2, n2, timeStamp ));
			}
			return fileFragments;
		}
		catch(OutOfMemoryError e){
			e.printStackTrace();
			return null;
		}
	}
	

	public int getN2() {
		return n2;
	}

	public int getK2() {
		return k2;
	}

}
