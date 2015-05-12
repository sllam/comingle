package comingle.pretty;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class PrettyPrinter {

	public static String pretty(Calendar cal) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df.format( cal.getTime() );
	}
	
}
