package Test;
import java.util.*;

public class TestData {
	public int txnID;
	public long time;
	public String akey;
	public String methodName;
	public String layer;
	
	

	public TestData(int pTxnID, Date startTime, Date endTime, String pKey, String pMethodName, String pLayer){
		long timeDiff = endTime.getTime() - startTime.getTime();
		txnID = pTxnID;
		time = timeDiff; 
		akey = pKey;
		methodName = pMethodName;
		layer = pLayer;
	}
	
	public TestData(int pTxnID, Date startTime, Date endTime, String pMethodName, String pLayer){
		long timeDiff = endTime.getTime() - startTime.getTime();
		txnID = pTxnID;
		time = timeDiff; 
		akey = "No Key";
		methodName = pMethodName;
		layer = pLayer;
	}
	
	
	

}
