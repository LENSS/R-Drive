package edu.tamu.cse.lenss.MDFS5.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.tamu.cse.lenss.MDFS5.Constants;
import edu.tamu.cse.lenss.MDFS5.Model.MDFSFileInfo;

/**
 * This class track the current status of MDFS File System. <br>
 * Available files in the network, local available files, or local available
 * fragments...
 * 
 * @author Jay
 *
 */


public class MDFSDirectory implements Serializable {

	//constructor
	private MDFSDirectory() {}

	//delete everything of a file from local mdfs directory
	public static void deleteFile(String fileId, String fName) {

		File rootDir = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT);
		if (!rootDir.exists())
			return;
		// Delete File Folder
		File fileDir = new File(rootDir, MDFSFileInfo.getFileDirName(fName,	fileId));
		if (fileDir.exists() && fileDir.isDirectory()) {
			try {
				IOUtilities.deleteRecursively(fileDir);
				fileDir.delete();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	//get fragment index from name
	public static int getFragmentIndexFromName(String name) {
		String[] tokens = name.split("__frag__");

		tokens = delEmptyStr(tokens);

		return Integer.parseInt(tokens[tokens.length-1]);
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
}
