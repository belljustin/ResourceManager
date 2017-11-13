package Test;
import java.util.*;

public class TestData {
	public int txnID;
	public long time;
	public String akey;
	public String methodName;
	public String layer;
	
	

	public TestData(int pTxnID, long start, long end, String pKey, String pMethodName, String pLayer){
		long timeDiff = end - start;
		txnID = pTxnID;
		time = timeDiff; 
		akey = pKey;
		methodName = pMethodName;
		layer = pLayer;
	}
	
	public TestData(int pTxnID, long start, long end, String pMethodName, String pLayer){
		long timeDiff = end - start;
		txnID = pTxnID;
		time = timeDiff; 
		akey = "No Key";
		methodName = pMethodName;
		layer = pLayer;
	}
	
	
	

}
