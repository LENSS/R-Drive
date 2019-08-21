package edu.tamu.lenss.mdfs.cipher;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


//this class is used for ciphering a file block/ byteArray.
public class MDFSCipher {
	private static MDFSCipher instance = null;
	

	private MDFSCipher() {}
	
	public static MDFSCipher getInstance() {
		if (instance == null) {
			instance = new MDFSCipher();
		}
		return instance;
	}
	

	private Cipher getCipher(byte[] rawSecretKey, int mode){
		SecretKeySpec skeySpec = new SecretKeySpec(rawSecretKey, "AES");
		// Instantiate the cipher
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
		try {
			cipher.init(mode, skeySpec); 
			return cipher;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean encrypt(String cleartextFile , String ciphertextFile,  byte[] rawSecretKey ) {
		Cipher cipher = getCipher(rawSecretKey, Cipher.ENCRYPT_MODE);
		try {
			FileInputStream fis = new FileInputStream(cleartextFile);
			FileOutputStream fos = new FileOutputStream(ciphertextFile);
			CipherOutputStream cos = new CipherOutputStream(fos, cipher);
			
			byte[] block = new byte[1024];
			int len;
			while ((len = fis.read(block)) >= 0) {
				cos.write(block, 0, len);
			}

			cos.flush();
			cos.close();
			fos.close();
			fis.close();

			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public byte[] encrypt(byte[] plainMessage, byte[] rawSecretKey) {
		
		byte[] encryptedMessage = null;
		Cipher cipher = getCipher(rawSecretKey, Cipher.ENCRYPT_MODE);
		
		try {
			encryptedMessage = cipher.doFinal(plainMessage);
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		
		return encryptedMessage;
	}
	
	public byte[] decrypt(byte[] encryptedMessage, byte[] rawSecretKey) {
		byte[] plainMessage = null;
		Cipher cipher = getCipher(rawSecretKey, Cipher.DECRYPT_MODE);
		
		try {
			plainMessage = cipher.doFinal(encryptedMessage);
		} catch (IllegalBlockSizeException e) {
			System.out.println("illegal blocksize exception");
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
			System.out.println("bad padding exception");
		} catch(OutOfMemoryError e){
			System.out.println("out of memory exception");
			e.printStackTrace();
		}
		
		return plainMessage;
	}

	public boolean decrypt(String ciphertextFile, String cleartextFile,  byte[] rawSecretKey ) {
		Cipher cipher = getCipher(rawSecretKey, Cipher.DECRYPT_MODE);
		try {
			FileInputStream fis = new FileInputStream(ciphertextFile);
			CipherInputStream cis = new CipherInputStream(fis, cipher);
			FileOutputStream fos = new FileOutputStream(cleartextFile);
			
			byte[] block = new byte[1024];
			int len;
			while ((len = cis.read(block)) != -1) {
				fos.write(block, 0, len);
			}
			fos.close();
			cis.close();
			fis.close();
			
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	
}
