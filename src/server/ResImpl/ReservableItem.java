// -------------------------------
// Kevin T. Manley
// CSE 593
// -------------------------------
package server.ResImpl;

import java.io.*;

// Superclass for the three reservable items, Flight, Car, and Hotel
public abstract class ReservableItem extends RMItem implements Serializable
{
		private int m_nCount;
		private int m_nPrice;
		private int m_nReserved;
		private String m_strLocation;

		
        public ReservableItem( String location, int count, int price ) {
        super();
				m_strLocation=location;
				m_nCount=count;
				m_nPrice=price;
				m_nReserved=0;
		}

		public void setCount( int count )
			{ m_nCount = count; }

		public int getCount()
			{ return m_nCount; }

		public void setPrice( int price )
			{ m_nPrice = price; }

		public int getPrice()
			{ return m_nPrice; }

		public void setReserved( int r )
		{ m_nReserved = r; }


		public int getReserved()
		{ return m_nReserved; }

		public String getLocation()
			{ return m_strLocation; }

		public String toString()
		{ return "RESERVABLEITEM key='" + getKey() + "', location='" + getLocation() +
				"', count='" + getCount() + "', price='" + getPrice() + "'"; }

		public abstract String getKey();

}
