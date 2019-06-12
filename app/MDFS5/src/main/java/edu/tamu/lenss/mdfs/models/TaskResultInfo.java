package edu.tamu.lenss.mdfs.models;

/**
 * A Header sent before transferring a task result through TCP. It contains the information of the transferred task result.
 * @author Jay
 *
 */
public class TaskResultInfo extends MDFSTCPHeader {
	private static final long serialVersionUID = 1L;
	private long jobId;
	private String resultFileName;
	private boolean isReady = false;
	

	public TaskResultInfo(String fileName, long jobIdentifier) {
		super(MDFSTCPHeader.TYPE_TASK_RESULT);
		this.resultFileName = fileName;
		this.jobId = jobIdentifier;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public String getResultFileName() {
		return resultFileName;
	}

	public void setResultFileName(String resultFileName) {
		this.resultFileName = resultFileName;
	}

	public boolean isReady() {
		return isReady;
	}

	public void setReady(boolean isReady) {
		this.isReady = isReady;
	}
	
}
