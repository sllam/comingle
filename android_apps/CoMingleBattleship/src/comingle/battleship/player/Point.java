/**
This file is part of CoMingle.

CoMingle is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoMingle is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoMingle. If not, see <http://www.gnu.org/licenses/>.

CoMingle Version 1.0, Beta Prototype

Authors:
Edmund S. L. Lam      sllam@qatar.cmu.edu

* This implementation was made possible by an JSREP grant (JSREP 4-003-2-001, Effective Distributed 
Programming via Join Patterns with Guards, Propagation and More) from the Qatar National Research Fund 
(a member of the Qatar Foundation). The statements made herein are solely the responsibility of the authors.
**/

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
