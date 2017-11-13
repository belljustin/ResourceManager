package Test;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CSVTestWriter {
	
	PrintWriter pw; 
	Lock printWriterLock;
	
	public CSVTestWriter(String filename){
	    printWriterLock = new ReentrantLock();
		try {
			pw = new PrintWriter(new File(filename+".csv"));
			StringBuilder sb = new StringBuilder();
			sb.append("TxnID,");
			sb.append("Time,");
			sb.append("Key,");
			sb.append("Method Name,");
			sb.append("Layer");
			sb.append("\n");
			pw.write(sb.toString());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void addRow(int txnID, long start, long end, String pKey, String pMethodName, String pLayer){
		long timeDifference = end - start;
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
	
	public void addRow(int txnID, long start, long end, String pMethodName, String pLayer){
		long timeDifference = end - start;
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
	
	public void addData(ArrayList<TestData> pList){
		StringBuilder sb = new StringBuilder();
		for(TestData i : pList){
			sb.append(i.txnID);
			sb.append(",");
			sb.append(i.time);
			sb.append(",");
			sb.append(i.akey);
			sb.append(",");
			sb.append(i.methodName);
			sb.append(",");
			sb.append(i.layer);
			sb.append("\n");
		}
		
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
