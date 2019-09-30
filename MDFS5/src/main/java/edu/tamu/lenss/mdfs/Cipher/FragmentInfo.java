package edu.tamu.lenss.mdfs.Cipher;

import java.io.Serializable;
import edu.tamu.lenss.mdfs.Utils.IOUtilities;


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
	public byte _fragmentNumber;
	private byte _kNumber;
	private byte _nNumber;
	
	public FragmentInfo(String filename, byte fragmentType, long filesize, byte[] fragment, byte fragmentNumber, byte kNumber, byte nNumber, long lastModified) {
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

	public byte getK() {
		return _kNumber;
	}
	public byte getN() {
		return _nNumber;
	}

	public byte getFragmentNumber() {
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
