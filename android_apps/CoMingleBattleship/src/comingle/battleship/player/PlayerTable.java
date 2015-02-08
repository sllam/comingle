package comingle.battleship.player;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import sllam.extras.admin.NodeInfo;

import comingle.battleship.SeaActivity;
import comingle.tuple.Tuple2;
import comingle.tuple.Tuple3;

import android.app.Activity;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TableLayout;

public class PlayerTable {

	private final static String TAG = "PlayerTable";
	
	private final static Random seed = new Random();
	
	public final static int MY_TABLE_TYPE = 0;
	public final static int OPP_TABLE_TYPE = 1;
	
	private final PlayerGrid[][] table;
	
	private final int tableID;
	private final int height;
	private final int length;
	private final int shipColor;
	private final int tableType;
	private TableLayout tableLayout = null;
	private final List<PlayerShip> ships = new LinkedList<PlayerShip>();
	private SeaActivity activity;
	
	private NodeInfo node = null;
	
	public PlayerTable(int tableID, int height, int length, int shipColor, int tableType) {
		this.tableID = tableID;
		this.height = height;
		this.length = length;
		this.shipColor = shipColor;
		this.tableType = tableType;
		table = new PlayerGrid[height][length];
		for(int h=0; h<height; h++) {
			for(int l=0; l<length; l++) {
				table[h][l] = new PlayerGrid(SeaActivity.COORDS[h][l], shipColor, this, h, l);		
			}
		}
	}
	
	public void hit(int x, int y) {
		for (PlayerShip ship: ships) {
			if(ship.hit(x, y)) {
				table[x][y].drawHit();
				if(ship.isSunk()) {
					ship.drawSunk();
				}
			}
		}
	}
	
	public PlayerTable(int tableID, int shipColor, int tableType) {
		this(tableID, SeaActivity.TABLE_HEIGHT, SeaActivity.TABLE_LENGTH, shipColor, tableType);
	}
	
	public int getTableType() { return tableType; } 
	
	public void set(SeaActivity act) {
		activity = act;
		tableLayout = (TableLayout) act.findViewById(tableID);
		for(int h=0; h<height; h++) {
			for(int l=0; l<length; l++) {
				RelativeLayout grid = (RelativeLayout) tableLayout.findViewById(SeaActivity.COORDS[h][l]);
				table[h][l].set(act, grid, tableType == OPP_TABLE_TYPE); 
			}
		}
	}
	
	public void setNode(NodeInfo node) {
		this.node = node;
	}
	
	public int getLocation() {
		return node.location;
	}
	
	public String getName() {
		return node.name;
	}
	
	public void initGrids() {
		for(int h=0; h<height; h++) {
			for(int l=0; l<length; l++) {
				table[h][l].drawGrid();
			}
		}	
	}
	
	public static final int SHIP_DESTROYER = 0;
	public static final int SHIP_FRIGATE   = 1;
	public static final int SHIP_CARRIER   = 2;
	
	private int destroyerId = 1;
	private int frigateId = 1;
	private int carrierId = 1;
	
	private String makeShipName(int lg) {
		String name = "";
		switch(lg) {
			case 2: name = String.format("Destroyer #%s", destroyerId);
			        destroyerId++; break;
			case 3: name = String.format("Frigate #%s", frigateId);
	        	    frigateId++; break;
			case 4: name = String.format("Carrier #%s", carrierId);
	                carrierId++; break;
		}
		return name;
	}
	
	private Tuple3<Integer,Integer,Integer> getOrientationAndHead(List<Tuple2<Integer,Integer>> pts) {
		Tuple2<Integer,Integer> head = pts.get(0);
		Tuple2<Integer,Integer> end  = pts.get(pts.size()-1);
		int orientation = -1;
		if (head.t1 != end.t1) {
			orientation = PlayerShip.VERTICAL;
			int y = head.t2;
			int x = 10000;
			for(Tuple2<Integer,Integer> pt: pts) {
				if(x > pt.t1) { x = pt.t1; }
			}
			return new Tuple3<Integer,Integer,Integer>(orientation,x,y);
		} else {
			orientation = PlayerShip.HORIZONTAL;
			int x = head.t1;
			int y = 10000;
			for(Tuple2<Integer,Integer> pt: pts) {
				if(y > pt.t2) { y = pt.t2; }
			}
			return new Tuple3<Integer,Integer,Integer>(orientation,x,y);
		}
	}
	
	public boolean addShip(List<Tuple2<Integer,Integer>> pts) {
		Tuple3<Integer,Integer,Integer> tup = getOrientationAndHead(pts);
		int orientation = tup.t1;
		int x = tup.t2; int y = tup.t3;
		int length = pts.size();		
		return addShip(x, y, length, orientation);
	}
	
	public boolean addShip(int x, int y, int length, int orientation) {
		String name = makeShipName(length);
		PlayerShip ship = PlayerShip.createShip(this, name, x, y, length, orientation);
		if(ship != null) {
			// table[x][y].drawHit();
			ship.draw();
			ships.add(ship);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean addShip(int x, int y, int length, int[] orientations) {
		for(int o: orientations) {
			if(addShip(x, y, length, o)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean addDestroyer(int x, int y, int orientation) {
		return addShip(x, y, 2, orientation);
	}

	public boolean addFrigate(int x, int y, int orientation) {
		return addShip(x, y, 3, orientation);
	}
	
	public boolean addCarrier(int x, int y, int orientation) {
		return addShip(x, y, 4, orientation);
	}
	
	public boolean isOccupied(Point pt) {
		return isOccupied(pt.x, pt.y);
	}
	
	public boolean isOccupied(int x, int y) {
		return table[x][y].isOccupied();
	}
	
	public int height() { return table.length; }
	
	public int length() { return table[0].length; }
	
	public PlayerGrid getGrid(int x, int y) {
		return table[x][y];
	}
	
	//////////////////////
	// Reaction Methods //
	//////////////////////
	
	public void storeGrids() {
		for(int h=0; h<height; h++) {
			for(int l=0; l<length; l++) {
				activity.storeGrid(table[h][l], h, l);
			}
		}
	}
	
	///////////////////////
	// Random Generators //
	///////////////////////
	
	public static final Integer[] PROGRESS_DIR_1 = { Point.DOWN, Point.RIGHT };
	public static final Integer[] PROGRESS_DIR_2 = { Point.DOWN, Point.LEFT };
	public static final Integer[] PROGRESS_DIR_3 = { Point.UP, Point.RIGHT };
	public static final Integer[] PROGRESS_DIR_4 = { Point.UP, Point.LEFT };
	
	public static final Point START_1 = new Point(0,0);
	public static final Point START_2 = new Point(0,6);
	public static final Point START_3 = new Point(6,0);
	public static final Point START_4 = new Point(6,6);
	
	public static Tuple2<Integer[],Point> randomProgressChoice() {
		List<Tuple2<Integer[],Point>> choices = new LinkedList<Tuple2<Integer[],Point>>();
		choices.add(new Tuple2<Integer[],Point>(PROGRESS_DIR_1,START_1));
		choices.add(new Tuple2<Integer[],Point>(PROGRESS_DIR_2,START_2));
		choices.add(new Tuple2<Integer[],Point>(PROGRESS_DIR_3,START_3));
		choices.add(new Tuple2<Integer[],Point>(PROGRESS_DIR_4,START_4));
		return randomPick( choices ) ;
	}
	
	public static <T> List<T> randomOrd(List<T> ls) {
		List<T> rs = new LinkedList<T>();
		rs.addAll(ls);
		for (int x=0; x<rs.size(); x++) {
			int roll = seed.nextInt(rs.size());
			T obj = rs.remove(0);
			rs.add(roll, obj);
		}
		
		return rs;
	}
	
	public static <T> T randomPick(List<T> ls) {
		int roll = seed.nextInt(1000) % ls.size();
		return ls.get(roll);
	}
	
	public static <T> T randomPick(T[] arr) {
		List<T> ls = new LinkedList<T>();
		for(T t: arr) { ls.add(t); }
		return randomPick(ls);
	}
	 
	public static int[] randomOrientation() {
		int roll = seed.nextInt(100);
		if(roll <= 50) {
			int[] arr = { PlayerShip.HORIZONTAL, PlayerShip.VERTICAL };
			return arr;
		} else {
			int[] arr = { PlayerShip.VERTICAL, PlayerShip.HORIZONTAL };
			return arr;
		}
	}
	
	private static final int COL_RETRIES = 20;
	private static final int SHIP_RETRIES = 3;
	private static final int RESTARTS = 3;
	
	public void randomFleet(int destroyers, int frigates, int carriers) {
		randomFleetPlacement(0, destroyers, frigates, carriers);
	}
	
	private void randomFleetPlacement(int restart, int destroyers, int frigates, int carriers) {
		if(restart > RESTARTS) {  
			// TODO: Randomized fleet placement failed, use presets.
			Log.v(TAG, "Randomized fleet placement fail.. using presets..");
			return;
		}
		List<Integer> listOfShips = new LinkedList<Integer>();
		for(int x=0; x<destroyers; x++) { listOfShips.add(SHIP_DESTROYER); }
		for(int x=0; x<frigates; x++) { listOfShips.add(SHIP_FRIGATE); }
		for(int x=0; x<carriers; x++) { listOfShips.add(SHIP_CARRIER); }		
		listOfShips = randomOrd(listOfShips);
		ListIterator<Integer> iter = listOfShips.listIterator();
		while(iter.hasNext()) {
			Tuple2<Integer[],Point> progress = randomProgressChoice();
			Integer[] progressDir = progress.t1;
			Point progressStart   = progress.t2;
			int length = 0;
			switch(iter.next()) {
				case SHIP_DESTROYER: length = 2; break; 
				case SHIP_FRIGATE: length = 3; break;
				case SHIP_CARRIER: length = 4; break;
			}
			if(!randomShipPlacement(0, length, progressDir, progressStart)) {
				randomFleetPlacement(restart+1, destroyers, frigates, carriers);
			}
		}
		
	}
	
	private boolean randomShipPlacement(int restart, int ship, Integer[] progressDir, Point progressStart) {
		if(restart > SHIP_RETRIES) { return false; }
		
		int dx = seed.nextInt(height/3);
		int dy = seed.nextInt(length/3);
		
		Point randStart = progressStart.progress(progressDir[0], dx, progressDir[1], dy);
		
		int cols = 0;
		while(cols <= COL_RETRIES) {
			if(addShip(randStart.x, randStart.y, ship, randomOrientation())) {
				return true;
			}		
			int randDir = randomPick(progressDir);
			randStart = randStart.progress(randDir, 1);
			cols++;
			if(randStart.x < 0 || randStart.x >= height || randStart.y < 0 || randStart.y >= length) { break; }
		}
		return randomShipPlacement(restart+1, ship, progressDir, progressStart) ;
	}
	
}
