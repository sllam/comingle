package comingle.battleship;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sllam.extras.admin.NodeInfo;
import sllam.extras.wifidirect.WifiDirectDirectory;
import sllam.extras.wifidirect.listeners.DirectoryChangedListener;
import sllam.extras.wifidirect.listeners.LocalNodeInfoAvailableListener;
import sllam.extras.wifidirect.listeners.NetworkStatusChangedListener;
import battleship.Battleship;
import comingle.actuation.ActuatorAction;
import comingle.battleship.player.PlayerGrid;
import comingle.battleship.player.PlayerTable;
import comingle.tuple.*;
import comingle.mset.*;
import comingle.wifidirect.runtime.WifiDirectComingleRuntime;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class SeaActivity extends Activity {
	 
	public static final int[][] COORDS = new int[][] { 
		{ R.id.coord00, R.id.coord01, R.id.coord02, R.id.coord03, R.id.coord04, R.id.coord05, R.id.coord06 },
		{ R.id.coord10, R.id.coord11, R.id.coord12, R.id.coord13, R.id.coord14, R.id.coord15, R.id.coord16 },
		{ R.id.coord20, R.id.coord21, R.id.coord22, R.id.coord23, R.id.coord24, R.id.coord25, R.id.coord26 },
		{ R.id.coord30, R.id.coord31, R.id.coord32, R.id.coord33, R.id.coord34, R.id.coord35, R.id.coord36 },
		{ R.id.coord40, R.id.coord41, R.id.coord42, R.id.coord43, R.id.coord44, R.id.coord45, R.id.coord46 },
		{ R.id.coord50, R.id.coord51, R.id.coord52, R.id.coord53, R.id.coord54, R.id.coord55, R.id.coord56 },
		{ R.id.coord60, R.id.coord61, R.id.coord62, R.id.coord63, R.id.coord64, R.id.coord65, R.id.coord66 }
	};
	
	public static final int TABLE_HEIGHT = COORDS.length;
	public static final int TABLE_LENGTH = COORDS[0].length;
	
	public static final int MY_SHIP_COLOR = Color.parseColor("#FF58b866");
	public static final int OPP_SHIP_COLOR = Color.RED;
	
	public static final int NUM_OF_DESTROYERS = 4;
	public static final int NUM_OF_FRIGATES   = 3;
	public static final int NUM_OF_CARRIERS   = 2;
	
	private static final String BS_REQ_CODE = "BATTLESHIP_COMINGLE";
	private static final int BS_ADMIN_PORT  = 8181;
	private static final int BS_FACT_PORT   = 8819;
	
	private static final int RET_WIFI_SETTINGS = 203;
	
	private static final String TAG = "SeaActivity";
	
	// TableLayout tableLayout;
	
	private Menu options_menu;
	private boolean gameStarted = false;
	
	private PlayerTable myTable;
	private Map<Integer,PlayerTable> oppTables = new HashMap<Integer,PlayerTable>();
 	
	private WifiDirectComingleRuntime<Battleship> battleshipRuntime;
	
	private SeaActivity self;
	
	private Handler alfred;
	
	protected void calDeviceDimens() {
		Point outSize = new Point();
		getWindowManager().getDefaultDisplay().getSize(outSize);
		Toast.makeText(getApplicationContext(),
				String.format("%s,%s",outSize.x, outSize.y), Toast.LENGTH_SHORT).show();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sea);
		
		// calDeviceDimens();
		
		self = this;
		alfred = new Handler();
		
		initMyTable();
		
		initBattleshipNeighborhood();
		
		// myTable.randomFleet(destroyers, frigates, carriers);
		
		

		// oppTable.randomFleet(destroyers, frigates, carriers);		
		
		

	}
	
	private void initMyTable() {
		myTable = new PlayerTable(R.id.myGrid, MY_SHIP_COLOR, PlayerTable.MY_TABLE_TYPE);
		myTable.set(this);
		myTable.initGrids();
	}
	
	private void initOppTables(List<NodeInfo> added_nodes) {
		int myLoc = battleshipRuntime.getLocation();
		for(NodeInfo node: added_nodes) {
			if(myLoc != node.location && !oppTables.containsKey(node.location)) {
				PlayerTable oppTable = new PlayerTable(R.id.oppGrid, OPP_SHIP_COLOR, PlayerTable.OPP_TABLE_TYPE);
				oppTable.set(this);
				oppTable.initGrids();
				oppTable.setNode(node);
				oppTables.put(node.location, oppTable);
			}
		}
	}
	
	private void initBattleshipNeighborhood() {
		final SeaActivity self = this;
		battleshipRuntime = new WifiDirectComingleRuntime<Battleship>(
				                 this, Battleship.class, BS_REQ_CODE, WifiDirectDirectory.OWNER_IP, 
				                 BS_ADMIN_PORT, BS_FACT_PORT);
		battleshipRuntime.initWifiDirectEnvironent();
		battleshipRuntime.getDirectory().addNetworkStatusChangedListener(new NetworkStatusChangedListener() {
			@Override
			public void doWifiAdapterStatusChangedAction(boolean enabled) {
				/*if(!enabled && !postedWifiComplaint) {
					postConfigWifiConnection("Please form a wifi-direct group!");
				}*/
			}
			@Override
			public void doWifiConnectionStatusChangedAction(boolean connected) {
				if(battleshipRuntime.getDirectory().wifiEnabled() && 
			       !battleshipRuntime.getDirectory().wifiConnected()) {
					battleshipRuntime.postConfigWifiConnection("Please form a wifi-direct group!", RET_WIFI_SETTINGS);
				}
			}
		});
		
    	battleshipRuntime.getDirectory().addLocalNodeInfoAvailableListener(new LocalNodeInfoAvailableListener() {
			@Override
			public void doLocalNodeInfoAvailableAction(NodeInfo local, final int role) {
				self.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						myTable.setNode(battleshipRuntime.getDirectory().getLocalNode());
				    	self.startBattleshipRewriteMachine();
					}
				});
			}
    	});
    	battleshipRuntime.getDirectory().addDirectoryChangedListener(new DirectoryChangedListener() {
			@Override
			public void doDirectoryChangedAction(final List<NodeInfo> new_peers,
					final List<NodeInfo> added_nodes, final List<NodeInfo> dropped_nodes, int role) {
				self.runOnUiThread(new Runnable() {
					@Override
					public void run() {	
						initOppTables(added_nodes);
						if(new_peers.size() > 2) {
							battleshipRuntime.postAlert("Too Many Players", "N-Way Battle Currently Not Supported! Please connect to exactly one player");
						}
						if(dropped_nodes.size() > 0) {
							battleshipRuntime.postAlert("Player Dropped", "A player has dropped out! Please restart the app!");
						}
					}
				});
			}
    	});
		
	}

	public void startBattleshipRewriteMachine() {
		battleshipRuntime.initRewriteMachine();
		
		battleshipRuntime.getRewriteMachine().setRandomFleetActuator(new ActuatorAction<Tuple3<Integer, Integer, Integer>>() {
			@Override
			public void doAction(Tuple3<Integer, Integer, Integer> input) {
				final int destroyers = input.t1;
				final int frigates   = input.t2;
				final int carriers   = input.t3;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						myTable.randomFleet(destroyers, frigates, carriers);
				    	myTable.storeGrids();	
					}
				});
			}
		});
		
		battleshipRuntime.getRewriteMachine().setNotifyTurnActuator(new ActuatorAction<Unit>() {
			@Override
			public void doAction(Unit input) {
				setMyTurn(true);
				battleshipRuntime.postToast("Its your turn!");
			}
		});
		
		battleshipRuntime.getRewriteMachine().setMissedActuator(new ActuatorAction<Tuple4<Integer, Integer, Integer, Integer>>() {
			@Override
			public void doAction(Tuple4<Integer, Integer, Integer, Integer> input) {
				final int attLoc = input.t1;
				final int defLoc = input.t2;
				final int x = input.t3;
				final int y = input.t4;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						self.notifyMissed(attLoc, defLoc, x, y);	
					}
				});
			}
		});
		
		battleshipRuntime.getRewriteMachine().setHitActuator(new ActuatorAction<Tuple4<Integer, Integer, Integer, Integer>>() {
			@Override
			public void doAction(Tuple4<Integer, Integer, Integer, Integer> input) {
				final int attLoc = input.t1;
				final int defLoc = input.t2;
				final int x = input.t3;
				final int y = input.t4;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						self.notifyHit(attLoc, defLoc, x, y);	
					}
				});
			}
		});
		
		battleshipRuntime.getRewriteMachine().setSunkActuator(new ActuatorAction<Tuple4<Integer, Integer, String, SimpMultiset<Tuple2<Integer,Integer>>>>() {
			@Override
			public void doAction(Tuple4<Integer, Integer, String, SimpMultiset<Tuple2<Integer, Integer>>> input) {
				final int attLoc = input.t1;
				final int defLoc = input.t2;
				final String shipName = input.t3;
				final SimpMultiset<Tuple2<Integer,Integer>> shipPts = input.t4;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						self.notifySunk(attLoc, defLoc, shipName, shipPts);	
					}
				});				
			}
		});
		
		battleshipRuntime.getRewriteMachine().setNotifyDeadActuator(new ActuatorAction<Integer>() {
			@Override
			public void doAction(Integer deadLoc) {
				final String msg = String.format("%s's fleet has been wiped!", battleshipRuntime.getDirectory().getName(deadLoc));
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						battleshipRuntime.postAlert("Enemy Fleet Destroyed",msg);	
					}
				});
			}
		});
		
		battleshipRuntime.getRewriteMachine().setNotifyWinnerActuator(new ActuatorAction<Integer>() {
			@Override
			public void doAction(final Integer winnerLoc) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(battleshipRuntime.getLocation() == winnerLoc) {
							battleshipRuntime.postAlert("Victory!", "Your fleet has defeated your enemies!");	
						} else {
							String msg = String.format("%s's fleet has defeated you!", battleshipRuntime.getDirectory().getName(winnerLoc));
							battleshipRuntime.postAlert("Defeat!", msg);								
						}
					}
				});	
			}
		});
		
    	battleshipRuntime.startRewriteMachine();
    	
		if(battleshipRuntime.isRewriteReady() && battleshipRuntime.isOwner()) {
			this.setMenuItemVisibility(R.id.action_start, true);
		}
		
	}
	
	private void initGame() {		
		int destroyers = NUM_OF_DESTROYERS;
		int frigates   = NUM_OF_FRIGATES;
		int carriers   = NUM_OF_CARRIERS;
		
		LinkedList<Integer> locs = (LinkedList<Integer>) battleshipRuntime.getDirectory().getLocations();
		battleshipRuntime.getRewriteMachine().addInitGame(destroyers, frigates, carriers, locs);
		this.setMenuItemVisibility(R.id.action_start, false);
		this.gameStarted = true;
	}
	
	private Boolean myTurn = false;
	public void setMyTurn(boolean turn) { myTurn = turn; }
	public boolean isMyTurn() { return myTurn; }
	
	public void storeGrid(PlayerGrid grid, int x, int y) {
		if(grid.isOccupied()) {
			battleshipRuntime.getRewriteMachine().addHull(grid.getShipName(), x, y);
		} else {
			battleshipRuntime.getRewriteMachine().addEmpty(x, y);
		}
	}
	
	public void fireAt(PlayerTable table, int x, int y) {
		boolean popup = false;
		synchronized(myTurn) {
			if(isMyTurn()) {
				setMyTurn(false);
				battleshipRuntime.getRewriteMachine().addFireAt(table.getLocation(), x, y);
			} else {
				popup = true;
			}
		}
		if(popup) {
			battleshipRuntime.postAlert("Not Your Turn", "Guns are still loading, please wait!");
		}
	}
	
	public void notifyMissed(int attLoc, int defLoc, int x, int y) {
		PlayerTable table;
		String msg;
		if(attLoc == battleshipRuntime.getLocation()) {
			table = oppTables.get(defLoc);
			msg = "Splash!! You missed!";
		} else if(defLoc == battleshipRuntime.getLocation()) {
			table = myTable;
			msg = String.format("Splash!! %s missed your ships!", battleshipRuntime.getDirectory().getName(attLoc));
		} else { 
			Log.e(TAG,String.format("Unknown miss notification: %s %s", attLoc, defLoc));
			return; 	
		}
		final PlayerGrid missedGrid = table.getGrid(x, y);
		missedGrid.drawMiss();
		battleshipRuntime.postToast(msg);
		alfred.postDelayed(new Runnable() {
			@Override
			public void run() {
				missedGrid.resetGrid();
			}
		}, 3000);
	}
	
	public void notifyHit(int attLoc, int defLoc, int x, int y) {
		PlayerTable table;
		String msg;
		if(attLoc == battleshipRuntime.getLocation()) {
			table = oppTables.get(defLoc);
			msg = String.format("You hit %s's ship!", battleshipRuntime.getDirectory().getName(defLoc));
		} else if(defLoc == battleshipRuntime.getLocation()) {
			table = myTable;
			msg = String.format("%s hit your ship!", battleshipRuntime.getDirectory().getName(attLoc));
		} else { 
			Log.e(TAG,String.format("Unknown hit notification: %s %s", attLoc, defLoc));
			return; 	
		}
		final PlayerGrid hitGrid = table.getGrid(x, y);
		hitGrid.drawHit();
		battleshipRuntime.postToast(msg);
	}
	
	public void notifySunk(int attLoc, int defLoc, String shipName, SimpMultiset<Tuple2<Integer, Integer>> shipPts) {
		PlayerTable table;
		String msg;
		if(attLoc == battleshipRuntime.getLocation()) {
			table = oppTables.get(defLoc);
			msg = String.format("Kaboom! You sank %s's ship, %s!", battleshipRuntime.getDirectory().getName(defLoc), shipName);
			table.addShip( shipPts.toList() );
		} else if(defLoc == battleshipRuntime.getLocation()) {
			table = myTable;
			msg = String.format("Kaboom! %s sunk your ship, %s!", battleshipRuntime.getDirectory().getName(attLoc), shipName);
		} else { 
			table = oppTables.get(defLoc);
			msg = String.format("Kaboom! %s sunk %s's ship, %s!", battleshipRuntime.getDirectory().getName(attLoc)
					           ,battleshipRuntime.getDirectory().getName(defLoc), shipName);
			table.addShip( shipPts.toList() );
		}
		for(Tuple2<Integer,Integer> pt: shipPts) {
			table.getGrid(pt.t1, pt.t2).drawDestHull();
		}
		battleshipRuntime.postToast(msg);
	}
	
    @Override
    protected void onResume() {
    	super.onResume();
    	battleshipRuntime.registerReceiver();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	battleshipRuntime.unregisterReceiver();
    }
	
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	battleshipRuntime.close();
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sea, menu);
		options_menu = menu;
		if(battleshipRuntime.isRewriteReady() && battleshipRuntime.isOwner()) {
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
		switch (id) {
			case R.id.action_settings: return true;
			case R.id.action_start:
				if (battleshipRuntime.isOwner()) {
					initGame();
				}
				return true;
		} 
		return super.onOptionsItemSelected(item);
	}
	
	public void setMenuItemVisibility(int id, boolean visible) {
		if (options_menu == null) {
			String msg = "Option menu is null";
			Log.e(TAG, msg);
			return; 
		}
		MenuItem item = options_menu.findItem(id);
		if (item != null) {
			item.setVisible(visible);
		} else {
			String msg = "Failed to find item " + id;
			Log.e(TAG, msg);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		battleshipRuntime.wifiSettingsReturned( RET_WIFI_SETTINGS );
		
	}
	
	
	
}

