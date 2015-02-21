package edu.ece671.reportclient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.google.common.collect.ImmutableSet;

import edu.ece671.reportclient.R;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

public class ShowLocation extends Activity implements IOnLandmarkSelectedListener, BeaconConsumer{
	//Parameters for googleMap
	public static int landmarknum;
	private GoogleMap mMap;
	String mUrl = "http://percept.ecs.umass.edu/course/marcusbasement/{z}/{x}/{y}.png";
	
	double marcusLat = 42.393985;
	double marcusLng = -72.528622;
	int knowlesZoom = 25;
	
	public int currentMode;

	public final String ACTION_VIEW_MAP = "Overview Map";
	public final String ACTION_ADD_LANDMARKS = "Add Landmarks";
	public final String ACTION_REMOVE_LANDMARKS = "Remove Landmarks";

	public final int MODE_VIEW = 0;
	public final int MODE_ADD_LANDMARK = 1;
	public final int MODE_GENERATE_PATH = 2;
	public final int MODE_REMOVE_LANDMARK = 3;
	public final int MODE_SELECT_PATH = 4;

	public String title = "Title ";
	public int titleNumber = 0;
	private IOnLandmarkSelectedListener landmarkListener;
	public Uri imageUri;
	private Landmarks landmarks;	
	private Activity activity;
	
	//Parameters for iBeacon
	/** A set of valid Gimbal beacon identifier */
	private final Set<String> validGimbalIdentifiers = ImmutableSet.of("00100001", "10011101");

	/** Log for TagSearchingActivity. */
	private static final String TAG_SEARCHING_ACTIVITY_LOG = "TAG_SEA_ACT_LOG";

	private List<Beacon> discoveredBeaconList;
	public List<Beacon> strongestBeaconList;
	public String[] bcnTitle = new String[17];
	public Double[] bcnLat = new Double[17];
	public Double[] bcnLong = new Double[17];
	public double myLatitude = 0;
	public double myLongitude = 0;
	public Marker myMarker;
	public File myFile;
	public FileOutputStream fOut;
    public OutputStreamWriter myOutWriter;
    public String ReportMessage = "Report Message";
    public String[] ClientMessage = new String[2];
    public static InetAddress serverAddress;
	public Socket socket;
	public int flag = 0;
	//public int num = 0;
	CheckBox mycheck;
	int checkFlag = 0;
    
	/** The map used for storing discovered beacons */
	protected HashMap<String, Beacon> discoveredBeaconMap;
	/** Declare and initiate the a BeaconManager object.*/
	private BeaconManager beaconManager = BeaconManager
			.getInstanceForApplication(this);
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_location);
		
		try {
            landmarkListener = (IOnLandmarkSelectedListener) this;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
        }
		
		discoveredBeaconMap = new HashMap<String, Beacon>();
		discoveredBeaconList = new ArrayList<Beacon>();
		strongestBeaconList = new ArrayList<Beacon>();	
		Bundle myBundle = getIntent().getExtras();
		ClientMessage[0] = myBundle.getString(ReportDetails.BUNDLE_REPORT);
		mycheck= (CheckBox)findViewById(R.id.pickup);
		mycheck.setOnClickListener(myCheckListener);
		
	}
	
	private OnClickListener myCheckListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(mycheck.isChecked()){
				AlertDialog builder = new AlertDialog.Builder(ShowLocation.this)
				.setTitle("Warning!")
	            .setMessage("Are you sure you are picked up? If submitted, it would influence your rescue!!!")
	            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	            	public void onClick(DialogInterface dialog, int id) {
	            		checkFlag = 1;
	                	}
	               })
	            .setNegativeButton("Not yet", new DialogInterface.OnClickListener() {
	            	public void onClick(DialogInterface dialog, int id) {
	                       // User cancelled the dialog
	                   }
	               }).create();
				builder.show();
			}
		}
		
		
	};
	
	protected void onResume() {
		super.onResume();
		setupMap();
		beaconManager
		.getBeaconParsers()
		.add(new BeaconParser()
				.setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
		beaconManager.bind(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		beaconManager.unbind(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			socket.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	private void InitializeMarker(){

		String marcusBeacons = "" +
		"@Dis001,42.39354668258381,-72.52833187580109," +
		"@Dis002,42.39359447166392,-72.52839054912329," +
		"@Dis003,42.393661574404135,-72.52840027213097," +
		"@Dis004,42.39370936339676,-72.52845861017704," +
		"@Dis005,42.39377498034809,-72.52846665680408," +
		"@Dis006,42.39382895952455,-72.52852533012629," +
		"@Dis007,42.39389135741379,-72.52853605896235," +
		"@Dis008,42.393932460751394,-72.52859741449356," +
		"@Dis009,42.393999315520105,-72.52860512584448," +
		"@Dis010,42.39404537098601,-72.52866346389055," +
		"@Dis011,42.394103806904866,-72.52867016941309," +
		"@Dis012,42.39417412802318,-72.52873621881008," +
		"@Dis013,42.39423256382214,-72.52873655408621," +
		"@Dis014,42.39422736402869,-72.52877611666918," +
		"@Dis015,42.39416001428392,-72.52882674336433," +
		"@Dis016,42.39402060998703,-72.52868391573429," +
		"@Dis017,42.39397727821532,-72.52871174365282,";

		String[] marcusBeaconsArray = marcusBeacons.split("@");
		int index = 0;
		for(String marcusBeacon : marcusBeaconsArray){
			
			if(marcusBeacon.equals("")){
				continue;
			}
			int titleIndex = 0;
			int latitudeIndex = 1;
			int longitutdeIndex = 2;
		
			String[] beaconComponents = marcusBeacon.split(",");
			String beaconTitle = beaconComponents[titleIndex];
			double beaconLat = Double.parseDouble(beaconComponents[latitudeIndex]);
			double beaconLong = Double.parseDouble(beaconComponents[longitutdeIndex]);
			
			bcnTitle[index] = beaconTitle;
			bcnLat[index] = beaconLat;
			bcnLong[index] = beaconLong;
			index = index + 1;
			System.out.println(beaconTitle);
			System.out.println(beaconLong);
			System.out.println(beaconLat);
			
			LatLng position = new LatLng(beaconLat,beaconLong);
			Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(beaconTitle).draggable(true));
			landmarks.addMarker(beaconTitle, marker);
		}	
	}
	
	int num = 1;
	private void showMyLocation(){
		Double[] distance = new Double[3];
		Double[] Lat = new Double[3];
		Double[] Long = new Double[3];
		
		if(strongestBeaconList.size() < 2){
			Toast.makeText(getApplicationContext(), "We get less than two beacons!", Toast.LENGTH_LONG).show();
		}else if(strongestBeaconList.size() >= 2){
			double weight;
			weight = Math.PI * 6370856 / 180;
			//calculate distance and my location
			for(int n=0; n<strongestBeaconList.size();n++){
				Beacon bcn = strongestBeaconList.get(n);
				String minor = bcn.getId3().toString();
				int rssi = bcn.getRssi();
				int minorInt = Integer.parseInt(minor);
					
				double Lat1 = bcnLat[minorInt-1];
				double Long1 = bcnLong[minorInt-1];
				double dist = 0.008459 * rssi * rssi + 0.6711* rssi + 15.32;
			    dist = dist * 0.3048  / weight;
					
				distance[n] = dist;
				Lat[n] = Lat1;
				Long[n] = Long1;
			}	
			
			double a = (distance[0]*distance[0] - distance[1]*distance[1] - Lat[0]*Lat[0] + Lat[1]*Lat[1] 
						- Long[0]*Long[0] + Long[1]*Long[1]) / (-2 * (Lat[0] - Lat[1]));
			double b = - (Long[0] - Long[1]) / (Lat[0] - Lat[1]);
			double i = 1 + b*b;
			double j = 2*(a*b - b*Lat[0] - Long[0]);
			double k = Lat[0]*Lat[0] - 2*a*Lat[0] + a*a + Long[0]*Long[0] - distance[0]*distance[0];
			double theta = j*j - 4*i*k;
			
			if(theta > 0){
				theta = Math.sqrt(theta);
				double x_long1 = (-j + theta) / (2*i);
				double x_lat1 = a + b*(x_long1);
				double x_long2= (-j - theta) / (2*i);
				double x_lat2 = a + b*(x_long2);
				
				if(strongestBeaconList.size() == 2){
					myLatitude = (x_lat1 + x_lat2)/2;
					myLongitude = (x_long1 + x_long2)/2;
				}else if(strongestBeaconList.size() == 3){
					if((Lat[2] - x_lat1) * ( Lat[2] - x_lat1) + (Long[2] - x_long1)*(Long[2] - x_long1) == distance[2]*distance[2]){
						myLatitude=x_lat1;
						myLongitude=x_long1;
						Log.d("My location: ", "" + myLatitude + myLongitude);
					}
					else{
						myLatitude=x_lat2;
						myLongitude=x_long2;
						Log.d("My location: ", "" + myLatitude + myLongitude);
					}
				}
		
			}else if(theta == 0){
				double x_long = (-j) / (2*i);
				double x_lat = a + b*(x_long);
				myLatitude=x_lat;
				myLongitude=x_long;
				Log.d("My location: ", "" + myLatitude+ myLongitude);
				
			}
			//else if(theta < 0){
				//System.out.println("We can not get the location.");
				//Toast.makeText(getApplicationContext(), "We can not get your location!", Toast.LENGTH_LONG).show();
			//}
			Log.d("My location: ", "" + myLatitude + myLongitude);
			if(myLatitude != 0){
				LatLng myPosition = new LatLng(myLatitude, myLongitude);
				if(myMarker != null){
					myMarker.remove();
				}
				myMarker = mMap.addMarker(new MarkerOptions().position(myPosition).title("myPosition").draggable(true)
						.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
				landmarks.addMarker("myPosition", myMarker);
			}			
		}
			
	}

	private void setupMap(){
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		changeMapPositionAndZoom(new LatLng(marcusLat,marcusLng), knowlesZoom);
		MyUrlTileProvider mTileProvider = new MyUrlTileProvider(256, 256, mUrl);
		mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mTileProvider).zIndex(0));
	    // display all the landmarks
		landmarks = new Landmarks();
		InitializeMarker();
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		return true;
	}
	
	public class MyUrlTileProvider extends UrlTileProvider {

		private String baseUrl;

		public MyUrlTileProvider(int width, int height, String url) {
		    super(width, height);
		    this.baseUrl = url;
		}

		@Override
		public URL getTileUrl(int x, int y, int zoom) {
		    try {
		        return new URL(baseUrl.replace("{z}", ""+zoom).replace("{x}",""+x).replace("{y}",""+y));
		    } catch (MalformedURLException e) {
		        e.printStackTrace();
		    }
		    return null;
		}
	}
	
	private void changeMapPositionAndZoom(LatLng moveToPosition, int zoomLevel){
		changeMapPosition(moveToPosition);
		changeMapZoom(zoomLevel);
	}
	
	private void changeMapPosition(LatLng moveToPosition){
		CameraUpdate center = CameraUpdateFactory.newLatLng(moveToPosition);
		mMap.moveCamera(center);
	}
	
	private void changeMapZoom(int zoomLevel){
		CameraUpdate zoom=CameraUpdateFactory.zoomTo(zoomLevel);
		mMap.animateCamera(zoom);
	}

	@Override
	public void onLandmarkSelected(Marker landmark) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onModeChange() {
		// TODO Auto-generated method stub	
	}
	
	private void getStrongestRSSI() {
		// TODO Auto-generated method stub
		int index1, index2;
		for(index1=0; index1<discoveredBeaconList.size()-1; index1++){
			for(index2=0; index2<discoveredBeaconList.size()-1; index2++){
				Beacon bcn1 = discoveredBeaconList.get(index2);
				Beacon bcn2 = discoveredBeaconList.get(index2+1);
				System.out.println(bcn1.getId3().toString());
				if(bcn1.getRssi() < bcn2.getRssi())
				{
					discoveredBeaconList.set(index2, bcn2);
					discoveredBeaconList.set(index2+1, bcn1);
				}
			}
		}
		
		if(discoveredBeaconList.size()>=3){
			for(index1=0; index1<3; index1++){
				strongestBeaconList.add(index1, discoveredBeaconList.get(index1));
			}
		}else if(discoveredBeaconList.size()==2){
			for(index1=0; index1<discoveredBeaconList.size()-1; index1++){
				strongestBeaconList.add(index1, discoveredBeaconList.get(index1));
			}
		}else{
			Toast.makeText(getApplicationContext(), "No beacon or just one beacon was discovered.", Toast.LENGTH_LONG).show();
		}	
	}

	/**
	 * Refresh the list of beacon according to current values in the map and
	 * then notify the list UI to change.
	 */
	int addnum = 1;
	private void updateDiscoveredList() {
		
		discoveredBeaconList.clear();
		strongestBeaconList.clear();
		Iterator<Beacon> bIter = discoveredBeaconMap.values().iterator();
		while (bIter.hasNext()) {
			discoveredBeaconList.add(bIter.next());
			//getStrongestRSSI();
		}
		getStrongestRSSI();
		Log.d("Discover strongest beacons.", ""+ strongestBeaconList.size());
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showMyLocation();
				flag = 1;
				ClientMessage[1]  = "@" + String.valueOf(myLatitude) + "@" 
				+ String.valueOf(myLongitude)+ "@" + String.valueOf(checkFlag); 
				Log.d("report message", ClientMessage[1]);
				ReportMessage = ClientMessage[0] + ClientMessage[1];
				Log.d("report message", ReportMessage);
				Thread threadClientSocket = new Thread(new ClientThread(ReportMessage)); 
	    		threadClientSocket.start();	
	    		Log.d("thread", "start thread");
	    		if(checkFlag == 1){
					//closeServerSocket();
	    			Intent intent = new Intent(getApplicationContext(), ReportDetails.class);
					startActivity(intent); //call the activity
					finish();
	    		}
			}
		});
		
	}

	int count = 0;
	int sendNum = 0;
	@Override
	public void onBeaconServiceConnect() {
		// TODO Auto-generated method stub
		beaconManager.setRangeNotifier(new RangeNotifier() {
			@Override
			public void didRangeBeaconsInRegion(Collection<Beacon> beacons,
					Region region) {
				if (beacons.size() > 0) {
					Log.i(TAG_SEARCHING_ACTIVITY_LOG, "Found " + beacons.size()
							+ "beacons");
					for (Iterator<Beacon> bIterator = beacons.iterator(); bIterator
							.hasNext();) {
						final Beacon beacon = bIterator.next();
						if (isGimbalTag(beacon)) {
							String major = beacon.getId2().toString();
							if(Double.parseDouble(major) == 100){
								// generate the HashMap key, which is the
								// combination of tag's UUID, Major and Minor; But
								// you can always choose your own key
								final String key = new StringBuilder()
										.append(beacon.getId1())
										.append(beacon.getId2())
										.append(beacon.getId3()).toString();
								discoveredBeaconMap.put(key, beacon);
							}
							
						}
					}
					count = count + 1;
					if(flag == 0){
						updateDiscoveredList();
						sendNum = sendNum + 1;
					}else if(count == 10){				
						updateDiscoveredList();
						sendNum = sendNum + 1;					
						count = 0;
					}			
					Log.d("timer", String.valueOf(sendNum));
				}
			}
		});

		try {
			beaconManager.startRangingBeaconsInRegion(new Region(
					"myRangingUniqueId", null, null, null));
		} catch (RemoteException e) {
		}
	}
	
	/**
	 * A filter check whether the detected beacon is a Gimbal tag used for
	 * project.
	 * 
	 * @param beacon
	 *            The detected beacon
	 * @return Whether the beacon is a Gimbal tag for project or not.
	 */
	private boolean isGimbalTag(Beacon beacon) {
		final String uuid = beacon.getId1().toString();
		final String tagIdentifier = uuid.split("-")[0];
		if (validGimbalIdentifiers.contains(tagIdentifier)) {
			return true;
		}
		return false;
	}
	
	public void connectToServer(){
    	try {
    		serverAddress = InetAddress.getByName("72.19.102.28");
    		Log.d("TCP", "C: Connecting...");
        	socket = new Socket(serverAddress, 9095);
        	Log.e("TCP", "Connected To Server");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Log.e("TCP", "Client: Error", e);	  
		}  	
    }
    
    public void sendMessageToServer(String msg){
    	try {
    		try{
    			PrintWriter out = new PrintWriter( new BufferedWriter(
    					new OutputStreamWriter(socket.getOutputStream())),true);
    			out.println(msg);
    			Looper.prepare(); 
    			Toast.makeText(getApplicationContext(), 
    					"You report was sent.", Toast.LENGTH_SHORT).show();
    			Log.d("Message sent to server",msg);
    			Looper.loop();
    		}catch(Exception e) { Log.e("TCP", "S: Error", e);
		} finally {
			
		}  	
    }catch(Exception e) { Log.e("TCP", "S: Error", e);}
    
    }
    
    public class ClientThread implements Runnable{
    	String message;
    	public ClientThread(String report) {
			// TODO Auto-generated constructor stub
    		this.message = report;
    	}
		@Override
		public void run() {
			Log.d("thread","running thread");
			connectToServer();	
			try {
				Thread.sleep(150);//sleep 150ms
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sendMessageToServer(message);  	
			Log.d("Client sends message to server:",message);
		}
    	 	
    }
	
    public void closeServerSocket(){ 
    	try {
			socket.close();
			Log.d("socket", "close socket");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
