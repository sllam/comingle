package comingle.comms.ntp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Calendar;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

public class NTPClient {

	static Calendar sysTimeNow = null;
	static Calendar ntpTimeNow = null;
	
	public static Calendar getNTPTime() {
		try {
            NTPUDPClient client = new NTPUDPClient();
            try {
                // Set timeout of 60 seconds
                client.setDefaultTimeout(60000);
                // Connecting to time server
                // Other time servers can be found at : http://tf.nist.gov/tf-cgi/servers.cgi#
                // Make sure that your program NEVER queries a server more frequently than once every 4 seconds
                TimeInfo timeInfo = client.getTime( InetAddress.getByName( "time.nist.gov" ));
                long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();
                Calendar calTime = Calendar.getInstance();
                calTime.setTimeInMillis(returnTime);
                return calTime;
            } finally {
                // client.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		return null;
	}
	
	public static long getOffSetTime() {
		ntpTimeNow = getNTPTime();
		sysTimeNow = Calendar.getInstance();
		return sysTimeNow.getTimeInMillis() - ntpTimeNow.getTimeInMillis();
	}
	
	public static Calendar getLatestSysTime() {
		return sysTimeNow;
	}
	
	public static Calendar getLatestNTPTime() {
		return ntpTimeNow;
	}
	
}
