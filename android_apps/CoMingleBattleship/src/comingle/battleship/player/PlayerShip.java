package comingle.battleship.player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PlayerShip {

	////////////////////
	// Static Methods //
	////////////////////
	
	public final static int HORIZONTAL = 0;
	public final static int VERTICAL = 1;
	
	public static boolean checkCollision(PlayerTable table, List<Point> coordinates) {
		for(Point pt: coordinates) {
			if(table.isOccupied(pt)) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean checkBounds(PlayerTable table, List<Point> coordinates) {
		int maxX = table.height();
		int maxY = table.length();
		for(Point pt: coordinates) {
			if(maxX <= pt.x || maxY <= pt.y) {
				return false;
			}
		}
		return true;
	}
	
	public static PlayerShip createShip(PlayerTable table, String name, int head_x, int head_y, int length, int orientation) {
		Point head = new Point(head_x, head_y);
		List<Point> coordinates = new LinkedList<Point>();
		switch(orientation) {
			case HORIZONTAL: 
				for(int x=0; x<length; x++) {
					coordinates.add(head.right(x));
				}
				break;
			case VERTICAL: 
				for(int x=0; x<length; x++) {
					coordinates.add(head.down(x));
				}
				break;
		}
		if(checkBounds(table, coordinates) && checkCollision(table, coordinates)) {
			return new PlayerShip(table, name, coordinates, orientation);
		}
		return null;
	}
	/*
	public static PlayerShip createDestroyer(PlayerTable table, int x, int y, int orientation) {
		return createShip(table, x, y, 2, orientation);
	}

	public static PlayerShip createFrigate(PlayerTable table, int x, int y, int orientation) {
		return createShip(table, x, y, 3, orientation);
	}

	public static PlayerShip createCarrier(PlayerTable table, int x, int y, int orientation) {
		return createShip(table, x, y, 4, orientation);
	}*/
	
	////////////////////////
	// Non-static Methods //
	////////////////////////
	
	private final PlayerTable table;
	private final Point[] coordinates;
	private final int orientation;
	private final List<Point> hits;
	private boolean destroyed = false;
	private final String name;
	
	public PlayerShip(PlayerTable table, String name, List<Point> coordinates, int orientation) {
		this.table = table;
		this.name = name;
		this.orientation = orientation;
		this.coordinates = new Point[coordinates.size()];
		for(int x=0; x<coordinates.size(); x++) {
			this.coordinates[x] = coordinates.get(x);
		}
		this.hits = new LinkedList<Point>();
	}
	
	public boolean hit(int x, int y) {
		for(Point pt: coordinates) {
			if(pt.x == x && pt.y == y && !hits.contains(pt)) {
				hits.add(pt);
				if(hits.size() == coordinates.length) {
					destroyed = true;
				}
				return true;
			}
		}
		return false;
	}
	
	public String getName() { return name; }
	
	public boolean isSunk() { return destroyed; }
	
	public void draw() {
		switch(orientation) {
			case HORIZONTAL:
				Point head = coordinates[0];
				table.getGrid(head.x, head.y).drawShipBowLeft();
				table.getGrid(head.x, head.y).setShip(this);
				for(int x=1; x<coordinates.length-1; x++) {
					Point mid = coordinates[x];
					table.getGrid(mid.x, mid.y).drawShipMidLeftRight();
					table.getGrid(mid.x, mid.y).setShip(this);
				}
				Point end = coordinates[coordinates.length-1];
				table.getGrid(end.x, end.y).drawShipBowRight();
				table.getGrid(end.x, end.y).setShip(this);
				break;
			case VERTICAL:
				head = coordinates[0];
				table.getGrid(head.x, head.y).drawShipBowUp();
				table.getGrid(head.x, head.y).setShip(this);
				for(int x=1; x<coordinates.length-1; x++) {
					Point mid = coordinates[x];
					table.getGrid(mid.x, mid.y).drawShipMidTopDown();
					table.getGrid(mid.x, mid.y).setShip(this);
				}
				end = coordinates[coordinates.length-1];
				table.getGrid(end.x, end.y).drawShipBowDown();
				table.getGrid(end.x, end.y).setShip(this);
				break;
		}
	}
	
	public void drawSunk() {
		for(Point pt: coordinates) {
			table.getGrid(pt.x, pt.y).drawDestHull();
		}
	}
	
}
