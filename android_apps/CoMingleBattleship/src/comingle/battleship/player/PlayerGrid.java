package comingle.battleship.player;

import comingle.battleship.SeaActivity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

public class PlayerGrid {

	public static final int BITMAP_SIZE = 40;
	
	public static final int GRID_COLOR = Color.BLUE;
	public static final int SHIP_COLOR = Color.BLACK;
	public static final int HIT_COLOR  = Color.parseColor("#FFe37300");
	public static final int MISS_COLOR  = Color.CYAN;
	public static final int DEST_COLOR = Color.GRAY;
	
	private int coordID;
	private Canvas canvas;
	private Bitmap bmp;
	private RelativeLayout layout = null;
	private boolean occupied;
	private int shipColor;
	private PlayerShip ship;
	
	private final PlayerTable table;
	private final int x;
	private final int y;
	
	public PlayerGrid(int coordID, int shipColor, PlayerTable table, int x, int y) {
		this.bmp = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888);
    	this.canvas = new Canvas(bmp);
    	this.coordID = coordID;
    	this.occupied = false;
    	this.shipColor = shipColor;
    	this.table = table;
    	this.x = x;
    	this.y = y;
	}
	
	public void set(final SeaActivity act, final RelativeLayout layout, final boolean setClickable) {
		layout.setBackground(new BitmapDrawable(act.getResources(), bmp)); 
		this.layout = layout;
		
		if(setClickable) {
			this.layout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					/*
					Toast.makeText(act,
							String.format("Clicked %s", coordID), Toast.LENGTH_SHORT).show();	
					*/
					act.fireAt(table, x, y);
				}
			});
			
			layout.setOnTouchListener(new View.OnTouchListener() {

			    @Override
			    public boolean onTouch(View v, MotionEvent event) {
			        if(event.getActionMasked() == MotionEvent.ACTION_DOWN) {
			            layout.setAlpha((float) 0.2);
			        }
			        if(event.getActionMasked() == MotionEvent.ACTION_UP) {
			        	layout.setAlpha((float) 1);
			        }
			        return false;
			    }

			});
		}
		
	}

	public void hit() {
		table.hit(x, y);
	}
	
	public void setOccupied(boolean occ) { 
		this.occupied = occ;
	}
	
	public boolean isOccupied() {
		return occupied;
	}
	
	//////////////////
	// Drawing Grid //
	//////////////////
	
	public void drawGrid() {
		Paint paint = new Paint();
    	paint.setColor(GRID_COLOR);
    	
    	canvas.drawLine(1, 1, 1, 39, paint);
    	canvas.drawLine(1, 1, 39, 1, paint);
    	canvas.drawLine(1, 39, 39, 39, paint);
    	canvas.drawLine(39, 1, 39, 39, paint);
	}
	
	////////////////////////
	// Drawing Ship Parts //
	////////////////////////
	
	public static final int NO_SHIP = -1;
	public static final int BOW_UP = 0;
	public static final int BOW_DOWN = 1;
	public static final int BOW_LEFT = 2;
	public static final int BOW_RIGHT = 3;
	public static final int MID_TOP_DOWN = 4;
	public static final int MID_LEFT_RIGHT = 5;
	
	private int hullOrientation = NO_SHIP;
	
	public boolean drawDestHull() {
		switch(hullOrientation) {
		case BOW_UP    : drawShipBowUp(true); return true;
		case BOW_DOWN  : drawShipBowDown(true); return true; 
		case BOW_LEFT  : drawShipBowLeft(true); return true;
		case BOW_RIGHT : drawShipBowRight(true); return true;
		case MID_TOP_DOWN: drawShipMidTopDown(true); return true;
		case MID_LEFT_RIGHT: drawShipMidLeftRight(true); return true;
		default: return false;
		}
	}
	
	private Paint getShipPaint(boolean dest) {
		Paint paint = new Paint();
		if(!dest) {
			if(table.getTableType() == PlayerTable.OPP_TABLE_TYPE) {
				paint.setColor(Color.TRANSPARENT);
			} else {
				paint.setColor(shipColor);
			}
		} else {
			paint.setColor(DEST_COLOR);
		}
		return paint;
	}
	
	public void drawShipBowUp(boolean dest) {
		Paint paint = getShipPaint(dest);
		canvas.drawLine(10, 40, 10, 20, paint);
		canvas.drawLine(30, 40, 30, 20, paint);
		canvas.drawLine(10, 20, 20, 10, paint);
		canvas.drawLine(30, 20, 20, 10, paint);
		setOccupied(true);
		hullOrientation = BOW_UP;
		layout.invalidate();
	}
	public void drawShipBowUp() { drawShipBowUp(false); }

	public void drawShipBowDown(boolean dest) {
		Paint paint = getShipPaint(dest);
		canvas.drawLine(10, 0, 10, 20, paint);
		canvas.drawLine(30, 0, 30, 20, paint);
		canvas.drawLine(10, 20, 20, 30, paint);
		canvas.drawLine(30, 20, 20, 30, paint);
		setOccupied(true);
		hullOrientation = BOW_DOWN;
		layout.invalidate();
	}
	public void drawShipBowDown() { drawShipBowDown(false); }
	
	public void drawShipBowLeft(boolean dest) {
		Paint paint = getShipPaint(dest);
		canvas.drawLine(40, 10, 20, 10, paint);
		canvas.drawLine(40, 30, 20, 30, paint);
		canvas.drawLine(20, 10, 10, 20, paint);
		canvas.drawLine(20, 30, 10, 20, paint);
		setOccupied(true);
		hullOrientation = BOW_LEFT;
		layout.invalidate();
	}
	public void drawShipBowLeft() { drawShipBowLeft(false); }
	
	public void drawShipBowRight(boolean dest) {
		Paint paint = getShipPaint(dest);
		canvas.drawLine( 0, 10,  20, 10, paint);
		canvas.drawLine( 0, 30,  20, 30, paint);
		canvas.drawLine( 20, 10, 30, 20, paint);
		canvas.drawLine( 20, 30, 30, 20, paint);
		setOccupied(true);
		hullOrientation = BOW_RIGHT;
		layout.invalidate();
	}
	public void drawShipBowRight() { drawShipBowRight(false); }
	
	public void drawShipMidTopDown(boolean dest) {
		Paint paint = getShipPaint(dest);
		canvas.drawLine(10, 40, 10, 0, paint);
		canvas.drawLine(30, 40, 30, 0, paint);		
		setOccupied(true);
		hullOrientation = MID_TOP_DOWN;
		layout.invalidate();
	}
	public void drawShipMidTopDown() { drawShipMidTopDown(false); }
	
	public void drawShipMidLeftRight(boolean dest) {
		Paint paint = getShipPaint(dest);
		canvas.drawLine(40, 10, 0, 10, paint);
		canvas.drawLine(40, 30, 0, 30,  paint);		
		setOccupied(true);
		hullOrientation = MID_LEFT_RIGHT;
		layout.invalidate();
	}
	public void drawShipMidLeftRight() { drawShipMidLeftRight(false); }
	
	public void setShip(PlayerShip ship) {
		this.ship = ship;
	}
	
	public String getShipName() {
		return ship.getName();
	}
	
	///////////////////////
	// Drawing Reactions //
	///////////////////////
	
	public void drawHit() {
		Paint paint = new Paint();
    	paint.setColor(HIT_COLOR);
    	paint.setStrokeWidth(2);
		
    	canvas.drawLine(0,0,40,40,paint);
    	canvas.drawLine(0, 40, 40, 0, paint);
    	layout.invalidate();
	}
	
	public void drawMiss() {
		Paint paint = new Paint();
    	paint.setColor(MISS_COLOR);
    	paint.setStrokeWidth(2);		
    	
    	canvas.drawLine(0,0,40,40,paint);
    	canvas.drawLine(0, 40, 40, 0, paint);
    	layout.invalidate();
	}
	
	public void resetGrid() {
    	canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    	drawGrid();
    	layout.invalidate();
	}
	
}
