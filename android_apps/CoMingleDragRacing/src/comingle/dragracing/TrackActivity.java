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
Nabeeha Fatima        nhaque@andrew.cmu.edu

* This implementation was made possible by an JSREP grant (JSREP 4-003-2-001, Effective Distributed 
Programming via Join Patterns with Guards, Propagation and More) from the Qatar National Research Fund 
(a member of the Qatar Foundation). The statements made herein are solely the responsibility of the authors.
**/

package comingle.dragracing;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import comingle.actuation.ActuatorAction;
import comingle.dragracing.R;
import comingle.tuple.*;
import comingle.wifidirect.runtime.WifiDirectComingleRuntime;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.LinearLayout;

import android.widget.Toast;

import sllam.extras.admin.NodeInfo;
import sllam.extras.wifidirect.WifiDirectDirectory;
import sllam.extras.wifidirect.listeners.DirectoryChangedListener;
import sllam.extras.wifidirect.listeners.LocalNodeInfoAvailableListener;
import sllam.extras.wifidirect.listeners.NetworkStatusChangedListener;

import dragracing.Dragracing;

public class TrackActivity extends FragmentActivity {
	
	class Car {
		private Rect sprite;
		private Paint paint;
		private boolean throttle;
		private int owner;
		private int impulse_time;
		
		public Car(int owner) {
			this.owner = owner;
			paint = new Paint();
			paint.setColor(CAR_COLORS[owner]);
			throttle = false;
			int left;
			
			//adjusting movt. of car from one phone to the next
			if(getLocation() > 0) { left = 0; }
			else{left = 90;}
			
	      	int right  = left + TRACK_WIDTH;
	      	int top    = TOP_MARGIN + TRACK_WIDTH*owner - 12; 
	      	int bottom = TOP_MARGIN + TRACK_WIDTH*(owner+1) - 15;
	      	sprite = new Rect(left, top, right, bottom);			
	      	impulse_time = 0;
		}
	
		
		public void setSprite(Rect new_sprite) {
			sprite = new_sprite;
		}
		
		public Rect getSprite() {
			return sprite;
		}
		
		public Paint getPaint() {
			return paint;
		}
		
		public boolean getThrottle() {
			return throttle;
		}
		
		public int getIdx() { return owner; }
		
		public int getOwner() {
			return owner;
		}
		
		public void toggle(boolean tog) {
			throttle = tog;
		}
		
		public void move(int new_pos) {
	    	int left   = new_pos;
	   	   	int right  = new_pos + TRACK_WIDTH;
	   	   	int top    = TOP_MARGIN + TRACK_WIDTH*owner - 12; 
	   	   	int bottom = TOP_MARGIN + TRACK_WIDTH*(owner+1) - 15;
	   	   	sprite = new Rect(left, top, right, bottom);
	   	   	//rendering doubletrack after car has moved on the sprite
	   	 Paint paint = new Paint();
	        for (int i=0; i<=lanes; i++) {
	        		paint.setColor(CAR_COLORS[i]);
	        	 float top_track_y = TOP_MARGIN + TRACK_WIDTH*i;
	        	//adjusting movt. of car from one phone to the next
	 			if(getLocation() > 0) { 
	 				track_canvas.drawLine(0, top_track_y, 780, top_track_y, paint);   
	   	   			track_canvas.drawLine(0, top_track_y+7, 780, top_track_y+7, paint);}
	 			else{
	   	   			track_canvas.drawLine(75, top_track_y, 800, top_track_y, paint);   
	   	   			track_canvas.drawLine(75, top_track_y+7, 800, top_track_y+7, paint);}
	        }
		}
		
		public void impulse() {
			impulse_time = (int) System.currentTimeMillis();
		}
		
		public boolean checkImpulse(int refresh_time) {
			if(impulse_time == 0) { return false; }
			return refresh_time - impulse_time <= IMPULSE_LENGTH;
		}
		
	}
	
	private static final String TAG = "DragRacingTrackActivity";
	
	private static final int LEGEND_MARGIN = 50;
	
	private static final int TOP_MARGIN = 300;
	private static final int TRACK_WIDTH = 30;
	private static final int TRACK_LENGTH = 800;
	private static final int TRACK_CENTER = 500;
	private static final int CAR_DISPLACEMENT = 10;
	
	private static final int FPS = 32;
	private static final int FPS_RATE = 1000 / FPS;
	
	private static final int IMPULSE_LENGTH = 30;
	
	private static final int BG_COLOR = Color.parseColor("#FFf3f3f3");
	private static final int[] CAR_COLORS = { Color.CYAN, Color.parseColor("#FF58b866")
		                                    , Color.parseColor("#FFEE9C00"), Color.RED, Color.MAGENTA, Color.BLUE
		                                    , Color.BLACK, Color.LTGRAY };
	
	private static final String[] INTERVAL_MARKERS = { "A", "B", "C", "D", "E", "F", "G", "H" };
	
	private static final String DR_REQ_CODE = "DRAGRACING_COMINGLE";
	private static final int DR_ADMIN_PORT  = 8181;
	private static final int DR_FACT_PORT   = 8819;
	
	private static final int RET_WIFI_SETTINGS = 203;
	
	private WifiDirectComingleRuntime<Dragracing> dragracingRuntime;
	
	private boolean gameStarted = false;
	private boolean postedWifiComplaint = false;
 	
	private Canvas track_canvas;
	private LinkedList<Car> cars;
	private LinearLayout track;
	
	boolean timerStarted = false;
	Timer timer;
	TimerTask refreshTrackTask;
	
	int time_pressed;
	int time_released;
	
	int prev_refresh_time;
	
	boolean brakes = true;

	private Menu options_menu;
	
	//to keep track of number of lanes while moving
	public int lanes = 0;
	
	public void recordTimePressed() {
		time_pressed = (int) System.currentTimeMillis();
	}

	public void reportTimeReleased() {
		time_released = (int) System.currentTimeMillis();
		Toast.makeText(this, String.format("Duration: %s", time_released - time_pressed)
				      ,Toast.LENGTH_SHORT).show();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_track);
    	Bitmap bg = Bitmap.createBitmap(TRACK_LENGTH, 800, Bitmap.Config.ARGB_8888);
    	track_canvas = new Canvas(bg); 
       
    	final TrackActivity self = this;
    	dragracingRuntime = new WifiDirectComingleRuntime<Dragracing>(this, Dragracing.class, DR_REQ_CODE, WifiDirectDirectory.OWNER_IP
    			                                                     ,DR_ADMIN_PORT, DR_FACT_PORT);
		dragracingRuntime.initWifiDirectEnvironent();
		dragracingRuntime.addNetworkStatusChangedListener(new NetworkStatusChangedListener() {
			@Override
			public void doWifiAdapterStatusChangedAction(boolean enabled) {
				/*if(!enabled && !postedWifiComplaint) {
					postConfigWifiConnection("Please form a wifi-direct group!");
				}*/
			}
			@Override
			public void doWifiConnectionStatusChangedAction(boolean connected) {
				if(!postedWifiComplaint && dragracingRuntime.getDirectory().wifiEnabled() && !dragracingRuntime.getDirectory().wifiConnected()) {
					postConfigWifiConnection("Please form a wifi-direct group!");
				}
			}
		});
    	dragracingRuntime.addLocalNodeInfoAvailableListener(new LocalNodeInfoAvailableListener() {
			@Override
			public void doLocalNodeInfoAvailableAction(NodeInfo local, final int role) {
				self.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						self.initSelfInfo();
				    	self.startDragRacingRewriteMachine();
					}
				});
			}
    	});
    	dragracingRuntime.addDirectoryChangedListener(new DirectoryChangedListener() {
			@Override
			public void doDirectoryChangedAction(final List<NodeInfo> new_peers,
					List<NodeInfo> added_nodes, final List<NodeInfo> dropped_nodes, int role) {
				self.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						self.initOpponentsInfo(new_peers);	
						if(dropped_nodes.size() > 0) {
							self.postAlert("Player Dropped", "A player has dropped out! Please restart the app!");
						}
					}
				});
			}
    	});
    	
    	cars = new LinkedList<Car>();
		
		track = (LinearLayout) findViewById(R.id.track);
		track.setBackground(new BitmapDrawable(getResources(), bg));   
       
		track.setOnTouchListener(new View.OnTouchListener() {
		
    	   @Override
    	   public boolean onTouch(View v, MotionEvent event) {
    		   switch(event.getAction()) {
    		   		case MotionEvent.ACTION_DOWN: 
    		   			if (!released()) {
    		   			  dragracingRuntime.getRewriteMachine().addSendTap();
    		   			}
    		   			return true;
    		   }
    		   return true;
    	   }
       });
    }
    
    private void startDragRacingRewriteMachine() {
    	
    	dragracingRuntime.initRewriteMachine();
    	
    	ActuatorAction<LinkedList<Integer>> renderTrackAction = new ActuatorAction<LinkedList<Integer>>() {
			@Override
			public void doAction(LinkedList<Integer> locs) {
				renderTrack(locs);
			}    		
    	};
    	dragracingRuntime.getRewriteMachine().setRenderTrackActuator(renderTrackAction);
    	
    	ActuatorAction<Unit> releaseAction = new ActuatorAction<Unit>() {
			@Override
			public void doAction(Unit arg0) {
				release();
			}
    	};
    	dragracingRuntime.getRewriteMachine().setReleaseActuator(releaseAction);
    	
    	ActuatorAction<Integer> recvTapAction = new ActuatorAction<Integer>() {
			@Override
			public void doAction(Integer car_idx) {
				tap(car_idx);
			}
    	};
    	dragracingRuntime.getRewriteMachine().setRecvTapActuator(recvTapAction);
    	
    	ActuatorAction<Integer> hasAction = new ActuatorAction<Integer>() {
			@Override
			public void doAction(Integer player_idx) {
				addPlayer(player_idx, true);
			}
    	};
    	dragracingRuntime.getRewriteMachine().setHasActuator(hasAction);
    	
    	ActuatorAction<Integer> decWinnerAction = new ActuatorAction<Integer>() {
			@Override
			public void doAction(Integer winner) {
					declareWinner(winner);
			}
    	};
    	dragracingRuntime.getRewriteMachine().setDecWinnerActuator(decWinnerAction);
    	
    	dragracingRuntime.startRewriteMachine();
    	
		if(dragracingRuntime.isRewriteReady() && dragracingRuntime.isOwner()) {
			this.setMenuItemVisibility(R.id.action_start, true);
		}
    }
    
    public int getLocation() { return dragracingRuntime.getLocation(); }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	startTimerIfNeeded();
    	dragracingRuntime.registerReceiver();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	stopTimerIfNeeded();
    	dragracingRuntime.unregisterReceiver();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	stopTimerIfNeeded();
    	dragracingRuntime.close();
    	// p2p_dir.close();
    }

    
    public void startTimerIfNeeded() {
    	if (!timerStarted) {
    		prev_refresh_time = (int) System.currentTimeMillis();
    		timer = new Timer();
    		final TrackActivity self = this;
    		refreshTrackTask = new TimerTask() {
    	    	   @Override
    	    	   public void run() {
    	    		   self.refreshTrack();
    	    	   }
    			};    		
    		timer.schedule(refreshTrackTask, 0, FPS_RATE);
    		timerStarted = true;
    	}
    }
    
    public void stopTimerIfNeeded() {
    	if (timerStarted) {
    		timer.cancel();
    		timer.purge();
    		timer = null;
    		timerStarted = false;
    	}
    }
    	
	
    public int getPrevRefreshTime() {
    	return prev_refresh_time;
    }
    
    public void setPrevRefreshTime(int new_refresh_time) {
    	prev_refresh_time = new_refresh_time;
    }

    public void renderTrack(LinkedList<Integer> locs) {
		
		//decide phone_order for rendering tracks
		for(int x=0; x<locs.size(); x++) {
			if (locs.get(x) == this.getLocation()) {
			if(x > 0) 
			{ initTrack(locs.size()-1,1); }
			if(x < locs.size()-1) 
			{
				initTrack(locs.size()-1,0);
				
			break;
			}
	      }	
    	}
    	
    	initMarkers(locs);
    }
   
    private void initTrack(int num_of_lanes, int phone_order) {
        Paint paint = new Paint();
        lanes = num_of_lanes;
        for (int i=0; i<=num_of_lanes; i++) {
           paint.setColor(CAR_COLORS[i]);
     	   float top_track_y = TOP_MARGIN + TRACK_WIDTH*i;
     	   // float bot_track_y = TOP_MARGIN + TRACK_CENTER + TRACK_WIDTH*i;
     	   
     	   //rendering double track instead of single
     	   if(phone_order==0)
     	   {
     		   track_canvas.drawLine(75, top_track_y, 800, top_track_y, paint);   
     		   track_canvas.drawLine(75, top_track_y+7, 800, top_track_y+7, paint);  
     	   }
     	   else
     	   {
     		   track_canvas.drawLine(0, top_track_y, 800, top_track_y, paint);   
    		   track_canvas.drawLine(0, top_track_y+7, 800, top_track_y+7, paint); 
     	   }
           // track_canvas.drawLine(0, bot_track_y, 800, bot_track_y, paint);
           
        }   		
    }  
    
    private void initMarkers(LinkedList<Integer> locs) {
       	Paint text_paint = new Paint();
    	text_paint.setColor(Color.BLACK);
    	text_paint.setTextSize(20);
    	
    	String prev = "Start";
    	String next = "End";
    	int x = locs.indexOf(getLocation());
    
    	if(x == locs.size()-1) { initEndFlag(); }
    	
    	if(x == 0) { initStartFlag(); }
    	
    	if(x > 0) { prev = String.format("<< %s", INTERVAL_MARKERS[x-1]); }
    	 
    	if(x < locs.size()-1) {
    		next = String.format("%s >>", INTERVAL_MARKERS[x]);
    	}
    	
    	//adjusted placement of markers
		track_canvas.drawText(prev, 20, TOP_MARGIN-70, text_paint);
		track_canvas.drawText(next, 800-50, TOP_MARGIN-70, text_paint);
		
    }
    
    private void initStartFlag() {
       		Paint text_paint = new Paint();
       		text_paint.setColor(Color.BLACK);
    		text_paint.setTextSize(20);
    	
			switch (lanes) {
            case 1: case 2: case 3:
            	for (int j= 0; j<=50;j=j+50)
				{
					for(int i= 250; i<=380;i=i+40)
					{
						track_canvas.drawRect(j, i, j+25, i+20, text_paint);
					}
				}
				for(int k= 270; k<=400;k=k+40)
				{
					track_canvas.drawRect(25, k, 50, k+20, text_paint);
				}
                     break;
            case 4:  
            	for (int j= 0; j<=50;j=j+50)
				{
					for(int i= 250; i<=420;i=i+40)
					{
						track_canvas.drawRect(j, i, j+25, i+20, text_paint);
					}
				}
				for(int k= 270; k<=440;k=k+40)
				{
					track_canvas.drawRect(25, k, 50, k+20, text_paint);
				};
                     break;
            case 5:  
            	for (int j= 0; j<=50;j=j+50)
				{
					for(int i= 250; i<=460;i=i+40)
					{
						track_canvas.drawRect(j, i, j+25, i+20, text_paint);
					}
				}
				for(int k= 270; k<=480;k=k+40)
				{
					track_canvas.drawRect(25, k, 50, k+20, text_paint);
				};
                     break;
            case 6: 
            	for (int j= 0; j<=50;j=j+50)
				{
					for(int i= 250; i<=500;i=i+40)
					{
						track_canvas.drawRect(j, i, j+25, i+20, text_paint);
					}
				}
				for(int k= 270; k<=520;k=k+40)
				{
					track_canvas.drawRect(25, k, 50, k+20, text_paint);
				};
                     break;
			}
    }

    private void initEndFlag() {
   		Paint text_paint = new Paint();
   		text_paint.setColor(Color.BLACK);
		text_paint.setTextSize(20);
    	switch (lanes) {
        case 1: case 2: case 3:
        	
    				track_canvas.drawRect(780, 250, 800, 380, text_paint);
                 break;
        case 4:  
        	track_canvas.drawRect(780, 250, 800, 420, text_paint);
                 break;
        case 5:  
        	track_canvas.drawRect(780, 250, 800, 460, text_paint);
                 break;
        case 6: 
        	track_canvas.drawRect(780, 250, 800, 500, text_paint);
                 break;
    	}
    }
        
        public void moveCar(Car car, int time_prev, int time_curr) { 
        	Paint bg_paint = new Paint();
        	bg_paint.setColor(BG_COLOR);
        	track_canvas.drawRect(car.getSprite(), bg_paint);
        	int new_pos = RacerLib.newPos(car.getSprite().left, time_prev, time_curr);
        	
        	if (new_pos < TRACK_LENGTH) {
        		car.move(new_pos);    	
        		track_canvas.drawRect(car.getSprite(), car.getPaint());
        	} else {
        		removeCar(car.getOwner());
        		dragracingRuntime.getRewriteMachine().addExiting(car.getOwner());
        	}
        }
    
    public void refreshTrack() {
    	final TrackActivity self = this;
    	this.runOnUiThread(new Runnable() {
    		@Override
    		public void run() {
    			int curr_refresh_time = (int) System.currentTimeMillis();
    			int prev_refresh_time = self.getPrevRefreshTime();
    	    	for(int i=0; i<cars.size(); i++) {
    				   Car car = cars.get(i);
    				   if(car.checkImpulse(curr_refresh_time)) {
    					   self.moveCar(car, prev_refresh_time, curr_refresh_time);
    				   }
    			}
    	    	self.setPrevRefreshTime(curr_refresh_time);
    			track.invalidate();
    		}
    	});
    }
    
    private int myLocation() { return dragracingRuntime.getLocation(); }
    
    private void placeCarAvailable(int car_idx, int left_margin, int pos_idx) {
    	if (car_idx >= CAR_COLORS.length) { return; }
    	
    	Paint car_paint = new Paint();
    	if(car_idx >= 0) {
    		car_paint.setColor(CAR_COLORS[car_idx]);
    	} else {
    		car_paint.setColor(BG_COLOR);
    	}
    		
    	int left   = left_margin + TRACK_WIDTH*pos_idx + 20;
   	   	int right  = left_margin + TRACK_WIDTH*(pos_idx+1) + 20;
   	   	int top    = LEGEND_MARGIN + 2; 
   	   	int bottom = LEGEND_MARGIN + TRACK_WIDTH - 2;
   	   	Rect sprite = new Rect(left, top, right, bottom);
    	
   	   	track_canvas.drawRect(sprite, car_paint);
   	   	
    }
    
    private void initSelfInfo() {
    	Paint text_paint = new Paint();
    	text_paint.setColor(Color.BLACK);
    	text_paint.setTextSize(20);
    	
    	track_canvas.drawText("YOU:", 20, LEGEND_MARGIN-20, text_paint);
    	track_canvas.drawText("THE BAD & UGLY:", 520, LEGEND_MARGIN-20, text_paint);
    	
    	placeCarAvailable( myLocation(), 0, 0);
    }
    
    private void initOpponentsInfo(List<NodeInfo> new_nodes) {
    	if(!dragracingRuntime.isRewriteReady()) { return; }
    	for(int x=0; x<CAR_COLORS.length-1; x++) {
    		placeCarAvailable( -1, 500, x );
    	}
    	int opp_count = 0;
    	for(NodeInfo opp: new_nodes) {
    		if(opp.location != myLocation()) {
    			placeCarAvailable( opp.location, 500, opp_count );
    			opp_count++;
    		}
    	}
    }
    
	public void beginRace() {
		this.gameStarted = true;
		// int myLoc = dragracingRuntime.getLocation();
		LinkedList<Integer> locs = (LinkedList<Integer>) dragracingRuntime.getDirectory().getLocations();
		dragracingRuntime.getRewriteMachine().addInitRace(locs);
		this.setMenuItemVisibility(R.id.action_go, true);
		this.setMenuItemVisibility(R.id.action_start, false);
	}
    
    public void restartGame() {
    	
    	setBrakes(true);
    	
    	cars = new LinkedList<Car>();
    	
    	track_canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    	
    	startDragRacingRewriteMachine();
		
    	if(dragracingRuntime.isOwner()) {
    		beginRace();
    	}
		
    }
    
    public void declareWinner(final int car_idx) {
    	
    	final TrackActivity self = this;
    	
    	self.runOnUiThread(new Runnable() {
			@Override
			public void run() {
		    	String msg;
		    	if (self.getLocation() == car_idx) { 
		    		msg = "YOU WIN!";
		    	} else {
		    		msg = "YOU LOSE!";
		    	}
		    	self.restartGame();
		    	self.postAlert("Game Over", msg);					
			}
    	});
    
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.track, menu);
		options_menu = menu;
		if(dragracingRuntime.isRewriteReady() && dragracingRuntime.isOwner()) {
			this.setMenuItemVisibility(R.id.action_start, true);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		int myLoc = getLocation();
		switch (id) {
			case R.id.action_settings: return true;
			case R.id.action_start:
				if (myLoc == 0) {
					beginRace();
				}
				return true;
			case R.id.action_go:
				if (myLoc == 0) {
					dragracingRuntime.getRewriteMachine().addGo();
				}
				return true;
		} 
		return super.onOptionsItemSelected(item);
	}
	
	public void setMenuItemVisibility(int id, boolean visible) {
		if (options_menu == null) {
			String msg = "Option menu is null";
			Log.e(TAG, msg);
			// postToast(msg);
			return; 
		}
		MenuItem item = options_menu.findItem(id);
		if (item != null) {
			item.setVisible(visible);
		} else {
			String msg = "Failed to find item " + id;
			Log.e(TAG, msg);
			// postToast(msg);
		}
	}
	
	public void postAlert(final String title, final String msg) {
		final TrackActivity self = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder alert = new AlertDialog.Builder(self);
				alert.setTitle(title);
				alert.setMessage(msg);
				alert.setPositiveButton("Ok", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				alert.show();
			}
		});
	}
	
	public void postConfigWifiConnection(final String msg) {
		if(postedWifiComplaint) { return; }
		postedWifiComplaint = true;
		final TrackActivity self = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder alert = new AlertDialog.Builder(self);
				alert.setTitle("No Wifi-Direct Connection");
				alert.setMessage(msg);
				alert.setPositiveButton("Settings", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						self.startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), RET_WIFI_SETTINGS);
						dialog.dismiss();
					}
				});
				alert.setNegativeButton("Close", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						self.finish();
					}
				});
				alert.show();
			}
		});
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RET_WIFI_SETTINGS){
			postedWifiComplaint = false;
		}
	}

	public void postToast(final String msg) {
		final TrackActivity self = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(self, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}	
	
	// Car Methods
	public void addPlayer(int loc) {
		Car car = new Car(loc);
		cars.addLast(car);
		track_canvas.drawRect(car.getSprite(), car.getPaint());
	}
	
	public void addPlayer(int loc, boolean acc) {
		Car car = new Car(loc);
		car.toggle(acc);
		cars.addLast(car);
		track_canvas.drawRect(car.getSprite(), car.getPaint());
	}
	
	public void removeCar(int loc) {
		for (int x=0; x < cars.size(); x++) {
			Car car = cars.get(x);
			if (car.getOwner() == loc) {
		    	Paint bg_paint = new Paint();
		    	bg_paint.setColor(BG_COLOR);
		    	track_canvas.drawRect(car.getSprite(), bg_paint);
				cars.remove(x);
				return;
			}
		}
	}
	
	public void tap(int loc) {
		for (Car car: cars) {
			if (loc == car.getOwner()) {
				car.impulse();
				return;
			}
		}
	}
	
	public void countDown(final int count) {
		final TrackActivity self = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					int cnt = count;
					while(cnt > 0) {
						Toast.makeText(self, String.format("Get Ready: %s" , cnt), Toast.LENGTH_SHORT).show();
						Thread.sleep(1000);
						cnt--;
					}
					Toast.makeText(self, "GO!!!", Toast.LENGTH_SHORT).show();
					self.setBrakes(false);
				} catch(InterruptedException e) {
					e.printStackTrace();	
				}
				
			}
		});
	}	
	
	public void release() { countDown(0); }
	
	public void setBrakes(boolean br) {
		brakes = br;
	}
	
	public boolean released() { return brakes; }
	
}
