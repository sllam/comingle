package comingle.battleship.player;

public class Point {

	public static final int UP = 0;
	public static final int DOWN = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 3;
	
	public int x;
	public int y;
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public Point left(int s) {
		return new Point(x,y-s);
	}

	public Point right(int s) {
		return new Point(x,y+s);
	}
	
	public Point up(int s) {
		return new Point(x-s,y);
	}

	public Point down(int s) {
		return new Point(x+s,y);
	}
	
	public Point progress(int direction, int steps) {
		switch(direction) {
			case UP: return up(steps);
			case DOWN: return down(steps);
			case LEFT: return left(steps);
			case RIGHT: return right(steps);
			default: return null;
		}
	}
	
	public Point progress(int dir1, int s1, int dir2, int s2) {
		return progress(dir1,s1).progress(dir2, s2);
	}
	
	@Override
	public boolean equals(Object obj) {
		Point other = (Point) obj;
		return x == other.x && y == other.y;
	}
	
}
