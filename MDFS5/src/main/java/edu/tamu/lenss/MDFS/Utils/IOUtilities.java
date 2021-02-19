package edu.tamu.lenss.MDFS.Utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.RandomAccessFile;
import java.io.StreamCorruptedException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.APPEND;
public class IOUtilities {

	//miscellaneous purpose
	public static BlockingQueue<Pair> miscellaneousWorks = new LinkedBlockingQueue(5);

	//takes a fullpath of a file and returns the name.
	//this function assumes that the last entry in the filePath is indeed fileName.
	public static String getFileNameFromFullPath(String filePath){
		//first tokenize the filePath
		String[] tokens = filePath.split("/");

		//delete empty strings
		tokens = delEmptyStr(tokens);

		//return the last element
		return tokens[tokens.length-1];

	}

	public static List<String> getOwnIPv4s() {
		List<String> addrList = new ArrayList<String>();
		try {
			Enumeration<NetworkInterface> enumNI = NetworkInterface.getNetworkInterfaces();
			while ( enumNI.hasMoreElements() ){
				NetworkInterface ifc = enumNI.nextElement();
				if( ifc.isUp() ){
					Enumeration<InetAddress> enumAdds = ifc.getInetAddresses();
					while ( enumAdds.hasMoreElements() ){
						InetAddress addr = enumAdds.nextElement();
						// Now discard the local and loopback addresses
						if (! (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()))
							if (addr instanceof Inet4Address)
								addrList.add(addr.getHostAddress());
					}
				}
			}
		}catch(SocketException e) {}
		return addrList;
	}


	//takes a string and tokenizes
	public static String[] removeAllSpacesAndTokenize(String command, String delimeter){

		//replace tab with space
		command = command.replaceAll("\t", " ");

		//trim the leading and trailing whitespaces
		command = command.trim();

		//replace multiple spaces into one
		command = command.replaceAll("( +)"," ");

		//split the command into separated tokens
		String[] cmd = command.split(delimeter);

		return IOUtilities.delEmptyStr(cmd);
	}


	//eliminates empty string tokens
	public static String[] delEmptyStr(String[] tokens){
		List<String> newTokens = new ArrayList<>();
		for(String token: tokens){
			if(!token.equals("")){
				newTokens.add(token);
			}
		}

		return newTokens.toArray(new String[0]);
	}


	public static boolean isValidInet4Address(String ip) {

		//tokenize
		String[] IPnumbers = IOUtilities.removeAllSpacesAndTokenize(ip, "\\.");

		//check if there are four tokens
		if(IPnumbers.length!= 4){
			return false;
		}

		//check each value is between 0 to 255
		for(String val: IPnumbers){
			int value = -1;
			try{ value = Integer.parseInt(val); }catch(Exception e){ return false; }
			if(value<0 || value > 255){
				return false;
			}
		}

		//coming here means all checks succeeded.
		return true;
	}


	/**
	 * Create a new file and directory in the specified directory <br>
	 * Return the file handler if the file already exists <br>
	 * Create one if it does not exist
	 */
	public static File createNewFile(File dir, String name){
		if(dir == null || MyTextUtils.isEmpty(name))
			return null;

		if(!dir.exists()){
			if(!dir.mkdirs()){
				return null;
			}
		}

		File f = new File(dir, name);
		if(f.exists())
			return f;
		else{
			try{
				if(f.createNewFile())
					return f;
			}catch(IOException e){
				f = null;
			}
		}
		return f;
	}

	//Create a new file and its parent directories in the specified path <br>
	//Return the file handler if the file already exists <br>
	public static File createNewFile(String path){
		File f = new File(path);
		return createNewFile(f.getParentFile(), f.getName());
	}


	//Create a new file and its parent directories.
	//Return the file handler if the file already exists.
	//Create one if it does not exist.
	public static File createNewFile(File filePath){
		return createNewFile(filePath.getParentFile(), filePath.getName());
	}



	//Parse the first 3 parts of the IP address, including the last dot, .
	public static String parsePrefix(String IP){
		try{
			//String prefix = IP.substring(0,IP.lastIndexOf("."));
			return IP.substring(0,IP.lastIndexOf(".")+1);
		}
		catch(IndexOutOfBoundsException e){
			return null;
		}
	}

	//Appends file bytes into file from start index upto end
	public static void byteToFile_RAF_append(byte[] source, int offset, int length, File destination, long startIndex){
		try{
			destination.setWritable(true);
			RandomAccessFile randF = new RandomAccessFile(destination, "rw");
			randF.seek(startIndex);
			randF.write(source, offset, length);
			randF.close();

		}catch (Exception e) {e.printStackTrace();}
	}

	//appends a single byte array to a single file using FileOutputStream.
	public static void byteToFile_FOS_append(byte[] source, int offset, int length, File outputFile){
		try {
			FileOutputStream output = new FileOutputStream(outputFile.getAbsolutePath(), true);
			output.write(source, offset, length);
			output.close();
		}catch (Exception e){
			e.printStackTrace();
		}

	}

	//appends a single byte array to a single file using NIO.Files
	public static void byteToFile_NIOFILES_append(byte[] source, File outputFile){

		try {
			Path path = Paths.get(outputFile.getAbsolutePath());
			Files.write(path, source, WRITE, APPEND);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	//appends a single byte array to a single file using FileChannel
	public static void byteToFile_FC_append(byte[] source, int offset,int length, File outputFile){

		try{
			Path path = Paths.get(outputFile.getAbsolutePath());
			FileChannel fc = FileChannel.open(path, WRITE, APPEND);
			ByteBuffer buf = ByteBuffer.wrap(source, offset, length);
			fc.write(buf);
			fc.close();

		}catch (Exception e){
			e.printStackTrace();
		}
	}

	//append a single byte array to a single file using BufferedOutputStream
	public static void byteToFile_BOS_append(byte[] source, int offset,int length, File outputFile, boolean flush){
		FileOutputStream fos;
		BufferedOutputStream bos=null;
		try{
			fos = new FileOutputStream(outputFile.getAbsolutePath(), true);
			bos = new BufferedOutputStream(fos);
			bos.write(source, offset, length);
			if(flush){bos.flush();}
		}catch (Exception e){
			e.printStackTrace();
		}finally {
			//only close the outer most object.
			//no need to close underlying ones.
			if(bos != null){
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}



	//Convert a single byte array to a single file
	//this function creates new file if file already does not exist
	public static File byteToFile_FOS_write(byte[] data, File filedir, String filename){
		File f = createNewFile(filedir, filename);
		try {
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(data);
			fos.close();
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
		return f;
	}

	//Convert a File to a byte array
	public static byte[] fileToByte(File file){
		try {
			RandomAccessFile randF = new RandomAccessFile(file, "r");
			byte[] b = new byte[(int)randF.length()];
			randF.read(b);
			randF.close();
			return b;

		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		return null;
	}

	//read file bytes from start index upto end index
	public static void fileToByte(File source, long startIndex, long endIndex, byte[] destination, int offset){
		try{
			RandomAccessFile randF = new RandomAccessFile(source, "r");
			randF.seek(startIndex);
			randF.read(destination, offset, (int)(endIndex-startIndex));
			randF.close();

		}catch (Exception e) {e.printStackTrace();}
	}

	//Covert a Serializable Object to a byte array
	public static <T extends Object> byte[] objectToByteArray(T object){
		ByteArrayOutputStream byteStr = new ByteArrayOutputStream(10240);
		try {
			ObjectOutputStream output = new ObjectOutputStream(byteStr);
			output.writeObject(object);
			byte[] byteData = byteStr.toByteArray();
			byteStr.close();
			output.close();
			return byteData;
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Convert a bytes array to an Serializable Object
	 * @param
	 * @return
	 */
	public static <T extends Object> T bytesToObject(byte[] packetData, Class<T> type){
		T packet=null;
		try {
			ByteArrayInputStream byteStr = new ByteArrayInputStream(packetData);
			ObjectInputStream input = new ObjectInputStream(byteStr);
			packet = type.cast(input.readObject());

		} catch (OptionalDataException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(ClassCastException e){
			e.printStackTrace();
		}
		return packet;
	}

	/**
	 * Write a serializable object to a file
	 * @param <T>
	 * @param object
	 * @param fileHandle
	 * @return	true if the file is written successfully
	 */
	public static <T extends Object> boolean writeObjectToFile(T object, File fileHandle){
		try {
			FileOutputStream fos = new FileOutputStream(fileHandle);
			ObjectOutputStream output = new ObjectOutputStream(fos);
			output.writeObject(object);
			output.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Read an Object from a file
	 * @param file
	 * @param type
	 * @return	null if the file does not exist or the conversion process fails
	 */
	public static <T extends Object> T readObjectFromFile(File file, Class<T> type){
		if(!file.exists())
			return null;
		T object=null;
		try {
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream input = new ObjectInputStream(fis);
			object = type.cast(input.readObject());
			input.close();
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return object;
	}


	public static void deleteRecursively(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				deleteRecursively(c);
		}
		if (!f.delete())
			throw new FileNotFoundException("Failed to delete file: " + f);
	}

	public static boolean deleteFile(String filepath) {
		if (filepath == null) {
			return false;
		}
		File file = new File(filepath);
		try {
			boolean deleted = file.delete();
			if (deleted) {
				return true;
			}
		} catch (SecurityException exception) {

		}
		return false;
	}

	/**
	 * Convert a 6 byte, Hex-coded mac address to a 8  Bytes Long
	 * @param
	 * @return
	 */
	public static Long mac2Long(String macAdd){
		String[] macAddressParts = macAdd.trim().split(":");
		long value=0;
		// convert hex string to byte values. First element is the most significant byte
		Byte[] macAddressBytes = new Byte[6];
		for(int i=0; i<6; i++){
			Integer hex = Integer.parseInt(macAddressParts[i], 16);
			macAddressBytes[i] = hex.byteValue();
		}
		for (int i = 0; i < macAddressBytes.length; i++)
		{
			value = (value << 8) + (macAddressBytes[i] & 0xff);
		}
		return value;
	}

	/**
	 * Convert an 8 bytes Long to a 6 bytes Mac Address. Assume the first 2 bytes are zero
	 * @param value
	 * @return
	 */
	public static String long2mac(long value){
		ByteBuffer buffer = ByteBuffer.allocate(8);
		StringBuilder str = new StringBuilder();
		buffer.putLong(value);
		byte[] macAddressBytes = buffer.array();
		for(int i=2; i<8; i++){
			str.append(String.format("%02x", macAddressBytes[i] & 0xff));
			if(i < 7)
				str.append(":");
		}
		return str.toString();
	}

	//takes a string ip and returns long version of it
	public static long ipToLong(String ipAddress) {
		long result = 0;
		String[] ipAddressInArray = ipAddress.trim().split("\\.");
		for (int i = 3; i >= 0; i--) {
			long ip = Long.parseLong(ipAddressInArray[3 - i]);
			result |= ip << (i * 8);
		}
		return result;
	}

	//takes a list of string ip and returns a list of long version of it
	//basically calls ipToLong() functions multiple times
	public static List<Long> ipToLongAsALIst(List<String> ipAddresses) {
		List<Long> result = new ArrayList<Long>();
		for(int i=0;i < ipAddresses.size(); i++){
			result.add(ipToLong(ipAddresses.get(i)));
		}
		return result;
	}

	public static String long2Ip(long ip) {
		return ((ip >> 24) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "."
				+ ((ip >> 8) & 0xFF) + "." + (ip & 0xFF);
	}

	public static boolean validateIP(final String ip) {
		final String PATTERN = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
		Pattern pattern = Pattern.compile(PATTERN);
		Matcher matcher = pattern.matcher(ip);
		return matcher.matches();
	}

	public static boolean renameFile(File originalFile, String newName){
		final File renamedFile = new File(originalFile.getParent()+ File.separator + newName);
		return originalFile.renameTo(renamedFile);
	}

	//takes a hashmap and returns sorted map by value
	//order = true, results in ascending order, false= descending order
	public static Map<String, Double> sortByComparator(Map<String, Double> unsortMap, final boolean order) {
		List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());
		// Sorting the list based on values
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				if (order) { return o1.getValue().compareTo(o2.getValue()); }
				else { return o2.getValue().compareTo(o1.getValue()); }
			}
		});

		// Maintaining insertion order with the help of LinkedList
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Map.Entry<String, Double> entry : list) { sortedMap.put(entry.getKey(), entry.getValue()); }
		return sortedMap;
	}


}
