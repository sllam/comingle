package comingle.dragracing;

public class RacerLib {

	public static final int SPEED = 600;
	
	public static int newPos(int pos, int time_start, int time_end) {
		int time_diff = time_end - time_start;
		return pos + (SPEED * time_diff / 1000);
	}
	
}
