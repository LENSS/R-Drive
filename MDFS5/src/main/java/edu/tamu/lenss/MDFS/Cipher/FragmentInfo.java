package edu.tamu.lenss.MDFS.Cipher;

import java.io.Serializable;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;


//This class is used only during erasure coding.
//aka, when a fileBlock(a block of an actual file) is divided into fragments(multiple smaller files),
// we use this class to identify each file fragments.
public class FragmentInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final byte DATA_TYPE = 0;
	public static final byte CODING_TYPE = 1;
	
	private String _filename;
	private byte _type;
	private long _filesize;   //entire block size
	private byte[] _fragment;
	private long _lastModifiedTS;
	public int _fragmentNumber;
	private int _kNumber;
	private int _nNumber;
	
	public FragmentInfo(String filename, byte fragmentType, long filesize, byte[] fragment, int fragmentNumber, int kNumber, int nNumber, long lastModified) {
		super();
		_filename = filename;
		_fragment = fragment;
		_fragmentNumber = fragmentNumber;
		_kNumber = kNumber;
		_nNumber = nNumber;
		_lastModifiedTS = lastModified;
		_type = fragmentType;
		_filesize = filesize;
	}

	public int getK() {
		return _kNumber;
	}
	public int getN() {
		return _nNumber;
	}

	public int getFragmentNumber() {
		return _fragmentNumber;
	}

	public byte[] getFragment() {
		return _fragment;
	}

	public byte getType() {
		return _type;
	}

	public long getFilesize() {
		return _filesize;
	}
	
	public byte[] toByteArray(){
		return IOUtilities.objectToByteArray(this);
	}
}
