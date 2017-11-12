import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.locks.Lock;

public class CSVTestWriter {
	
	PrintWriter pw; 
	Lock printWriterLock;
	
	public CSVTestWriter(String filename){
		try {
			pw = new PrintWriter(new File(filename+".csv"));
			StringBuilder sb = new StringBuilder();
			sb.append("TxnID");
			sb.append("Time");
			sb.append("Key");
			sb.append("Method Name");
			sb.append("Layer");
			pw.write(sb.toString());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void addRow(int txnID, Date pStartTime, Date pEndTime, String pKey, String pMethodName, String pLayer){
		long timeDifference = pEndTime.getTime() - pStartTime.getTime();
		StringBuilder sb = new StringBuilder();
		sb.append(txnID);
		sb.append(timeDifference);
		sb.append(pKey);
		sb.append(pMethodName);
		sb.append(pLayer);
		synchronized(printWriterLock){
			pw.write(sb.toString());	
		}
				
	}
	
	public void addRow(int txnID, Date pStartTime, Date pEndTime, String pMethodName, String pLayer){
		long timeDifference = pEndTime.getTime() - pStartTime.getTime();
		StringBuilder sb = new StringBuilder();
		sb.append(txnID);
		sb.append(timeDifference);
		sb.append("NO KEY");
		sb.append(pMethodName);
		sb.append(pLayer);
		synchronized(printWriterLock){
			pw.write(sb.toString());	
		}
				
	}
	
	public void closeFile(){
	
		synchronized(printWriterLock){
			pw.close();
		}
	}
	
	

}
