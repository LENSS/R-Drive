package edu.tamu.lenss.mdfs;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.network.TCPReceive.TCPReceiverData;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.Logger;

/**
 * All functions in this class are blocking calls. Need to be handled in Thread
 * @author Jay
 *
 */

//this class is used to exchange file fragment (receive fragment request, send back fragment etc)
public class FragExchangeHelper {
	private static final String TAG = FragExchangeHelper.class.getSimpleName();
	
	public FragExchangeHelper(){
	}

	//this function is called when this node is receiving a fragment from the other side
	private void receiveBlockFragment(final TCPReceiverData data,  FragmentTransferInfo header){
		boolean success=false;
		File tmp0=null;
		try {

			//send header reply
			ObjectOutputStream oos = new ObjectOutputStream(data.getDataOutputStream());
			header.setNeedReply(false);
			header.setReady(true);
			oos.writeObject(header);

			//receive data
			byte[] buffer = new byte[Constants.TCP_COMM_BUFFER_SIZE];
			tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(header.getFileName(),header.getCreatedTime(), header.getBlockIndex()));
			
			if(!tmp0.exists()){
				if(!tmp0.mkdirs()){
					Logger.e(TAG, "Fail to create block directory for " + header.getFileName());
					return;
				}
			}
			
			tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(header.getFileName(), header.getCreatedTime(), header.getBlockIndex(), header.getFragIndex()));
			FileOutputStream fos = new FileOutputStream(tmp0);
			int readLen=0;
			DataInputStream in = data.getDataInputStream();
			while ((readLen = in.read(buffer)) >= 0) {
                fos.write(buffer, 0, readLen);
			}
			Logger.v(TAG, "Finish downloading fragment of " + header.getFileName());


			fos.close();
			data.close();
			oos.close();
			success = true;
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			if(success && tmp0.length() > 0){ // Hacky way to avoid 0 byte file
				// update directory
				ServiceHelper.getInstance().getDirectory().addBlockFragment(header.getCreatedTime(), header.getBlockIndex(), header.getFragIndex());
			}
			else if(tmp0 != null) {
				tmp0.delete();
			}
		}
	}


	//this function is called when this node is sending fragment to the other side
	private void sendBlockFragment(final TCPReceiverData data, FragmentTransferInfo header){
		byte [] mybytearray  = new byte [Constants.TCP_COMM_BUFFER_SIZE];
		File fileFrag = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(header.getFileName(), 
				header.getCreatedTime(), header.getBlockIndex(), header.getFragIndex()));
		if(!fileFrag.exists() || fileFrag.length() < 1){	// Handle the situation that 0 byte file got stored...
			Logger.e(TAG, "File Fragment does not exist");
			fileFrag.delete();
			data.close();  //closing socket without letting other side know that file frag doesnt exist

			// Update directory
			ServiceHelper.getInstance().getDirectory().removeBlockFragment(
					header.getCreatedTime(), header.getBlockIndex(), header.getFragIndex());
			return;
		}
		
		try {
			//file sending happening here
			int readLen=0;
			FileInputStream fis = new FileInputStream(fileFrag);
			BufferedInputStream bis = new BufferedInputStream(fis);
			DataOutputStream out = data.getDataOutputStream();
			while((readLen=bis.read(mybytearray,0,Constants.TCP_COMM_BUFFER_SIZE))>=0){
				out.write(mybytearray,0,readLen);
			}
			Logger.v(TAG, "Finish uploading data to OutStream");
			
			out.close();
			bis.close();
			fis.close();
			data.close();  //closing socket after data has been written on the socket
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public void newFragTransfer(final TCPReceiverData data, final FragmentTransferInfo header){
		Logger.v(TAG, "Receive fragment request of file " + header.getFileName());

		if(header.getReqType() == FragmentTransferInfo.REQ_TO_SEND){
			// Download fragment from the requester
			receiveBlockFragment(data, header);
		}
		else if(header.getReqType() == FragmentTransferInfo.REQ_TO_RECEIVE){
			// Send fragment to the requester
			sendBlockFragment(data, header);
		}
	}
}
