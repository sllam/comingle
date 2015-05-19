package comingle.pretty;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class PrettyPrinter {

	public static String pretty(Calendar cal) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df.format( cal.getTime() );
	}
	
	public static String prettyTime(long date) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df.format(new Date(date));
	}
	
}
