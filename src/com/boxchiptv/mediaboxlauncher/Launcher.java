package com.boxchiptv.mediaboxlauncher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Launcher extends Activity
{

	private final static String TAG = "MediaBoxLauncher";

	private GridView lv_status;

	private final String SD_PATH = "/mnt/extsd/";
	private final String USB_PATH = "/storage/external_storage";
	private final String net_change_action = "android.net.conn.CONNECTIVITY_CHANGE";
	private final String wifi_signal_action = "android.net.wifi.RSSI_CHANGED";
	private final String weather_request_action = "android.boxchiptv.launcher.REQUEST_WEATHER";
	private final String weather_receive_action = "android.boxchiptv.settings.WEATHER_INFO";

	private final static String INTENT_PARAM_PATH = "/system/etc/intent.cfg";
	private final static String ONLINE_TV_KEY = "OnlineTV:";
	private final static String SETTING_KEY = "Setting:";
	/*
	 * private final static String MUSIC_KEY = "Music"; private final static
	 * String VIDEO_KEY = "Video"; private final static String PICTURE_KEY =
	 * "Picure";
	 */
	private final static String WEB_SITE_KEY = "WebSite:";

	private static int time_count = 0;
	private final int time_freq = 180;
	private final int SCREEN_HEIGHT = 719;

	public static View prevFocusedView;
	public static RelativeLayout layoutScaleShadow;
	public static View trans_frameView;
	public static View frameView;
	public static View viewHomePage = null;
	public static MyViewFlipper viewMenu = null;
	public static View pressedAddButton = null;

	public static boolean isShowHomePage;
	public static boolean dontRunAnim;
	public static boolean dontDrawFocus;
	public static boolean ifChangedShortcut;
	public static boolean IntoCustomActivity;
	public static boolean IntoApps;
	public static boolean isAddButtonBeTouched;
	public static boolean isInTouchMode;
	public static boolean animIsRun;
	public static boolean cantGetDrawingCache;
	public static int accessBoundaryCount = 0;
	public static int preDec;
	public static int HOME_SHORTCUT_COUNT = 9;
	public static View saveHomeFocusView = null;
	public static MyGridLayout homeShortcutView = null;
	public static MyGridLayout videoShortcutView = null;
	public static MyGridLayout onlineTVShortcutView = null;
	public static MyGridLayout appShortcutView = null;
	public static MyGridLayout marketShortcutView = null;

	// public static MyGridLayout musicShortcutView = null;
	// public static MyGridLayout localShortcutView = null;
	public static TextView tx_video_count = null;
	public static TextView tx_onlineTV_count = null;
	public static TextView tx_app_count = null;

	public static TextView tx_local_count = null;
	private TextView tx_video_allcount = null;
	private TextView tx_onlineTV_allcount = null;
	private TextView tx_app_allcount = null;

	private TextView tx_local_allcount = null;
	private static boolean bHasGetWether = false;

	public static Bitmap screenShot;
	public static Bitmap screenShot_keep;

	private  String[] list_homeShortcut ;
	private  String[] list_videoShortcut;
	private  String[] list_onlineTVShortcut;
	private  String[] list_musicShortcut;
	private  String[] list_localShortcut;
	private  String[] list_MarketShortcut;

	public static String[] List_OnlineTV;
	public static String[] list_Setting;
	/*
	 * public static String[] list_Music; public static String[] list_Video;
	 * public static String[] list_Picure;
	 */
	public static String[] List_WebSite;

	private boolean is24hFormart = false;
	private int popWindow_top = -1;
	private int popWindow_bottom = -1;
	public static float startX;
	private static boolean updateAllShortcut;
	private static boolean checkOobe = true;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.d(TAG, "------onCreate");

		/*
		 * if (DesUtils.isboxchiptvChip() == false){ finish(); }
		 */
		// loadIntentParam();

		initStaticVariable();
		initChildViews();
		// displayShortcuts();
		// displayStatus();
		// displayDate();
		setRectOnKeyListener();
		sendWeatherBroadcast();

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_EJECT);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addDataScheme("file");
		registerReceiver(mediaReceiver, filter);

		filter = new IntentFilter();
		filter.addAction(net_change_action);
		filter.addAction(wifi_signal_action);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(weather_receive_action);
		registerReceiver(netReceiver, filter);

		filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		filter.addDataScheme("package");
		registerReceiver(appReceiver, filter);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		Log.d(TAG, "------onResume");

		if(SystemProperties.getBoolean("ro.platform.has.mbxuimode", false))
		{
			if(SystemProperties.getBoolean("ubootenv.var.has.accelerometer", true) && SystemProperties.getBoolean("sys.keeplauncher.landcape", false))
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			else
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}

		if(isInTouchMode || (IntoCustomActivity && isShowHomePage && ifChangedShortcut))
		{
			Launcher.dontRunAnim = true;
			layoutScaleShadow.setVisibility(View.INVISIBLE);
			frameView.setVisibility(View.INVISIBLE);
		}
		Log.d(TAG, "------onResume step1 ");
		displayShortcuts();
		Log.d(TAG, "------onResume step2 ");
		displayStatus();
		Log.d(TAG, "------onResume step3 ");
		displayDate();
		Log.d(TAG, "------onResume step4 ");

		if(isShowHomePage)
		{
			IntoCustomActivity = false;
		}

		if(cantGetDrawingCache)
		{
			resetShadow();
		}
		Log.d(TAG, "------onResume end");
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		Log.d(TAG, "------onPause");
		prevFocusedView = null;
	}

	@Override
	protected void onDestroy()
	{
		unregisterReceiver(mediaReceiver);
		unregisterReceiver(netReceiver);
		unregisterReceiver(appReceiver);
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		if(Intent.ACTION_MAIN.equals(intent.getAction()))
		{
			// Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ HOME");
			viewHomePage.setVisibility(View.VISIBLE);
			viewMenu.setVisibility(View.GONE);
			isShowHomePage = true;
			IntoCustomActivity = false;
			if(saveHomeFocusView != null && !isInTouchMode)
			{
				prevFocusedView = null;
				dontRunAnim = true;
				saveHomeFocusView.clearFocus();
				dontRunAnim = true;
				saveHomeFocusView.requestFocus();
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		Log.d(TAG, "the component get a toouch at ========================== 2> ");

		if(event.getAction() == MotionEvent.ACTION_DOWN)
		{
			startX = event.getX();
		}
		else if(event.getAction() == MotionEvent.ACTION_UP)
		{
			// Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ touch ="+ this);
			if(pressedAddButton != null && isAddButtonBeTouched)
			{
				Rect rect = new Rect();
				pressedAddButton.getGlobalVisibleRect(rect);

				popWindow_top = rect.top - 10;
				popWindow_bottom = rect.bottom + 10;

				new Thread(new Runnable()
				{
					public void run()
					{
						mHandler.sendEmptyMessage(1);
					}
				}).start();

				Intent intent = new Intent();
				intent.putExtra("top", popWindow_top);
				intent.putExtra("bottom", popWindow_bottom);
				intent.putExtra("left", rect.left);
				intent.putExtra("right", rect.right);
				intent.setClass(this, CustomAppsActivity.class);
				startActivity(intent);
				IntoCustomActivity = true;
				isAddButtonBeTouched = false;
			}
			else if(!isShowHomePage)
			{
				// Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@ getX = " +event.getX() +
				// " startX = " + startX);
				if(event.getX() + 20 < startX && startX != -1f)
				{
					viewMenu.setInAnimation(this, R.anim.push_right_in);
					viewMenu.setOutAnimation(this, R.anim.push_right_out);
					viewMenu.showNext();
				}
				else if(event.getX() - 20 > startX && startX != -1f)
				{
					viewMenu.setInAnimation(this, R.anim.push_left_in);
					viewMenu.setOutAnimation(this, R.anim.push_left_out);
					viewMenu.showPrevious();
				}
			}
		}
		return true;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			// Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@ KEYCODE_BACK");
			if(!isShowHomePage)
			{
				viewHomePage.setVisibility(View.VISIBLE);
				viewMenu.setVisibility(View.GONE);
				isShowHomePage = true;
				IntoCustomActivity = false;
				if(saveHomeFocusView != null && !isInTouchMode)
				{
					prevFocusedView = null;
					dontRunAnim = true;
					saveHomeFocusView.clearFocus();
					dontRunAnim = true;
					saveHomeFocusView.requestFocus();
				}
			}
			return true;
		}
		else if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
		{
			ViewGroup view = (ViewGroup) getCurrentFocus();
			if(view.getChildAt(0) instanceof ImageView)
			{
				ImageView img = (ImageView) view.getChildAt(0);
				if(img != null && img.getDrawable().getConstantState().equals(getResources().getDrawable(R.drawable.item_img_add).getConstantState()))
				{
					Rect rect = new Rect();
					view.getGlobalVisibleRect(rect);

					popWindow_top = rect.top - 10;
					popWindow_bottom = rect.bottom + 10;
					new Thread(new Runnable()
					{
						public void run()
						{
							mHandler.sendEmptyMessage(1);
						}
					}).start();
					Intent intent = new Intent();
					intent.putExtra("top", popWindow_top);
					intent.putExtra("bottom", popWindow_bottom);
					intent.putExtra("left", rect.left);
					intent.putExtra("right", rect.right);
					intent.setClass(this, CustomAppsActivity.class);
					startActivity(intent);//go to custom activity
					IntoCustomActivity = true;
					if(saveHomeFocusView != null)
					{
						saveHomeFocusView.clearFocus();
					}
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void displayStatus()
	{
		WifiManager mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
		int wifi_rssi = mWifiInfo.getRssi();
		int wifi_level = WifiManager.calculateSignalLevel(wifi_rssi, 5);

		LocalAdapter ad = new LocalAdapter(Launcher.this, getStatusData(wifi_level, isEthernetOn()), R.layout.homelist_item, new String[]
		{"item_type", "item_name", "item_sel"}, new int[]
		{R.id.item_type, 0, 0});
		lv_status.setAdapter(ad);
	}

	private void displayDate()
	{

		is24hFormart = DateFormat.is24HourFormat(this);

		TextView time = (TextView) findViewById(R.id.tx_time);
		TextView date = (TextView) findViewById(R.id.tx_date);
		time.setText(getTime());
		time.setTypeface(Typeface.DEFAULT_BOLD);
		date.setText(getDate());
	}

	private void initStaticVariable()
	{
		isShowHomePage = true;
		dontRunAnim = false;
		dontDrawFocus = false;
		ifChangedShortcut = true;
		IntoCustomActivity = false;
		IntoApps = true;
		isAddButtonBeTouched = false;
		isInTouchMode = false;
		animIsRun = false;
		updateAllShortcut = true;
		animIsRun = false;
		cantGetDrawingCache = false;
	}

	private void initChildViews()
	{
		lv_status = (GridView) findViewById(R.id.list_status); // GridView in
																// top status
																// bar

		layoutScaleShadow = (RelativeLayout) findViewById(R.id.layout_focus_unit);// selected
																					// status

		// shortcut at bottom of the screen
		homeShortcutView = (MyGridLayout) findViewById(R.id.gv_shortcut);
		videoShortcutView = (MyGridLayout) findViewById(R.id.gv_shortcut_video);
		onlineTVShortcutView = (MyGridLayout) findViewById(R.id.gv_shortcut_onlinetv);
		appShortcutView = (MyGridLayout) findViewById(R.id.gv_shortcut_app);
		marketShortcutView = (MyGridLayout) findViewById(R.id.gv_shortcut_market);

		viewHomePage = findViewById(R.id.layout_homepage);// home menu
															// RelativeLayout

		viewMenu = (MyViewFlipper) findViewById(R.id.menu_flipper);// a
																	// viewfipper

		frameView = findViewById(R.id.img_frame);// a picture
		trans_frameView = findViewById(R.id.img_trans_frame);// a picture

		tx_video_count = (TextView) findViewById(R.id.tx_video_count);
		tx_video_allcount = (TextView) findViewById(R.id.tx_video_allcount);
		tx_onlineTV_count = (TextView) findViewById(R.id.tx_onlinetv_count);
		tx_onlineTV_allcount = (TextView) findViewById(R.id.tx_onlinetv_allcount);
		tx_app_count = (TextView) findViewById(R.id.tx_app_count);
		tx_app_allcount = (TextView) findViewById(R.id.tx_app_allcount);
		tx_local_count = (TextView) findViewById(R.id.tx_local_count);
		tx_local_allcount = (TextView) findViewById(R.id.tx_local_allcount);

		/*
		 * new Thread( new Runnable() { public void run() { try{
		 * Thread.sleep(500); } catch (Exception e) { Log.d(TAG,""+e); }
		 * //Message msg = new Message(); //msg.what = 2;
		 * mHandler.sendEmptyMessage(2); } }).start();
		 */

	}

	private void displayShortcuts()
	{
		if(ifChangedShortcut == true)
		{
			loadApplications();
			ifChangedShortcut = false;

			if(!isShowHomePage)
			{
				// sendKeyCode(KeyEvent.KEYCODE_0);
				new Thread(new Runnable()
				{
					public void run()
					{
						ViewGroup findGridLayout = null;
						while (findGridLayout == null)
						{
							try
							{   
			//get viewgroup in myfillper - crrent view/com.boxchiptv.mediaboxlauncher.MyScrollView/om.boxchiptv.mediaboxlauncher.MyGridLayout
								findGridLayout = ((ViewGroup) ((ViewGroup) ((ViewGroup) viewMenu.getCurrentView()).getChildAt(3)).getChildAt(0));
							} catch (Exception e)
							{
								Log.d(TAG, "DisplayShortcuts " + e);
							}
						}
						//if findGridLayout.getcl
						mHandler.sendEmptyMessage(3);
					}
				}).start();
			}
			else if(IntoCustomActivity)
			{
				new Thread(new Runnable()
				{
					public void run()
					{
						try
						{
							Thread.sleep(200);
						} catch (Exception e)
						{
							Log.d(TAG, "IntoCustomActivity = " + e);
						}
						mHandler.sendEmptyMessage(4);
					}
				}).start();
			}
		}
	}

	private void updateStatus()
	{
		((BaseAdapter) lv_status.getAdapter()).notifyDataSetChanged();
	}

	public List<Map<String, Object>> getStatusData(int wifi_level, boolean is_ethernet_on)
	{
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = new HashMap<String, Object>();

		switch (wifi_level)
		{
			case 0 :
				map.put("item_type", R.drawable.wifi1);
				break;
			case 1 :
				map.put("item_type", R.drawable.wifi2);
				;
				break;
			case 2 :
				map.put("item_type", R.drawable.wifi3);
				break;
			case 3 :
				map.put("item_type", R.drawable.wifi4);
				break;
			case 4 :
				map.put("item_type", R.drawable.wifi5);
				break;
			default :
				break;
		}
		list.add(map);

		if(isSdcardExists() == true)
		{
			map = new HashMap<String, Object>();
			map.put("item_type", R.drawable.img_status_sdcard);
			list.add(map);
		}

		if(isUsbExists() == true)
		{
			map = new HashMap<String, Object>();
			map.put("item_type", R.drawable.img_status_usb);
			list.add(map);
		}

		if(is_ethernet_on == true)
		{
			map = new HashMap<String, Object>();
			map.put("item_type", R.drawable.img_status_ethernet);
			list.add(map);
		}

		return list;
	}

	public boolean isUsbExists()
	{
		File dir = new File(USB_PATH);
		if(dir.exists() && dir.isDirectory())
		{
			if(dir.listFiles() != null)
			{
				if(dir.listFiles().length > 0)
				{
					for (File file : dir.listFiles())
					{
						String path = file.getAbsolutePath();
						if(path.startsWith(USB_PATH + "/sd") && !path.equals(SD_PATH))
						{
							// if (path.startsWith("/mnt/sd[a-z]")){
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	public boolean isSdcardExists()
	{
		/*
		 * if(Environment.getExternalStorage2State().startsWith(Environment.
		 * MEDIA_MOUNTED)) { File dir = new File(SD_PATH); if (dir.exists() &&
		 * dir.isDirectory()) { return true; } }
		 */

		return false;
	}

	/*
	 * public boolean isSdcardExists(){ //String SD_PATH = "/mnt/extsd";
	 * StorageVolume[] storageVolumes; mStorageManager = (StorageManager)
	 * this.getSystemService(Context.STORAGE_SERVICE); StorageVolume[] volumes =
	 * mStorageManager.getVolumeList(); storageVolumes = new StorageVolume[0];
	 * 
	 * for(StorageVolume volume:volumes){ if(volume.getPath().equals(SD_PATH) &&
	 * mStorageManager.getVolumeState(volume.getPath()).equals("mounted")){
	 * return true; } } return false; }
	 */

	private boolean isEthernetOn()
	{
		ConnectivityManager connectivity = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connectivity.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

		if(info.isConnected())
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public String getTime()
	{
		final Calendar c = Calendar.getInstance();
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);

		String time = "";

		if(hour >= 10)
		{
			time += Integer.toString(hour);
		}
		else
		{
			time += "0" + Integer.toString(hour);
		}
		time += ":";

		if(minute >= 10)
		{
			time += Integer.toString(minute);
		}
		else
		{
			time += "0" + Integer.toString(minute);
		}

		return time;
	}

	private String getDate()
	{
		final Calendar c = Calendar.getInstance();
		int int_Month = c.get(Calendar.MONTH);
		String mDay = Integer.toString(c.get(Calendar.DAY_OF_MONTH));
		int int_Week = c.get(Calendar.DAY_OF_WEEK) - 1;
		String str_week = this.getResources().getStringArray(R.array.week)[int_Week];
		String mMonth = this.getResources().getStringArray(R.array.month)[int_Month];

		String date;
		if(Locale.getDefault().getLanguage().equals("zh"))
		{
			date = str_week + ", " + mMonth + " " + mDay + this.getResources().getString(R.string.str_day);
		}
		else
		{
			date = str_week + ", " + mMonth + " " + mDay;
		}

		// Log.d(TAG, "@@@@@@@@@@@@@@@@@@@ "+ date + "week = " +int_Week);
		return date;
	}

	private void loadCustomsApps(String path)
	{
		File mFile = new File(path);
		File default_file = new File(CustomAppsActivity.DEFAULT_SHORTCUR_PATH);
		BufferedReader br = null;
		
		if(!mFile.exists())
		{
			mFile = default_file;
			getShortcutFromDefault(CustomAppsActivity.DEFAULT_SHORTCUR_PATH, CustomAppsActivity.SHORTCUT_PATH);
		}

		if(!mFile.exists())
		{
			Log.d(TAG, "File not found " + CustomAppsActivity.DEFAULT_SHORTCUR_PATH);
			Log.d(TAG, "File not found " + CustomAppsActivity.SHORTCUT_PATH);
			return;
		}
		if(!mFile.canRead())
		{
			Log.d(TAG, "File cannot be read " + mFile.getPath());
			return;
		}
		try
		{
			br = new BufferedReader(new FileReader(mFile));

		} catch (Exception e)
		{
			Log.d(TAG, "Cannot create read buffer " + e);
			return;
		}

		try
		{
			String str = null;
			while ((str = br.readLine()) != null)
			{
				if(str.toString().startsWith(CustomAppsActivity.HOME_SHORTCUT_HEAD))
				{
					str = str.toString().replaceAll(CustomAppsActivity.HOME_SHORTCUT_HEAD, "");
					list_homeShortcut=str.toString().split(";");	
					//Log.d(TAG, "str = br.readLine() = " + str);					
				}
				else if(str.toString().startsWith(CustomAppsActivity.VIDEO_SHORTCUT_HEAD))
				{
					str = str.toString().replaceAll(CustomAppsActivity.VIDEO_SHORTCUT_HEAD, "");
					list_videoShortcut = str.toString().split(";");
				}
				else if(str.toString().startsWith(CustomAppsActivity.TV_ONLINE_SHORTCUT_HEAD))
				{
					str = str.toString().replaceAll(CustomAppsActivity.TV_ONLINE_SHORTCUT_HEAD, "");
					list_onlineTVShortcut = str.split(";");
				}
				else if(str.toString().startsWith(CustomAppsActivity.MUSIC_SHORTCUT_HEAD))
				{
					str = str.toString().replaceAll(CustomAppsActivity.MUSIC_SHORTCUT_HEAD, "");
					list_musicShortcut = str.split(";");
				}
				else if(str.toString().startsWith(CustomAppsActivity.LOCAL_SHORTCUT_HEAD))
				{
					str = str.toString().replaceAll(CustomAppsActivity.LOCAL_SHORTCUT_HEAD, "");
					list_localShortcut = str.split(";");
				}
				else if(str.startsWith(CustomAppsActivity.MARKET_SHORTCUT_HEAD))
				{
					str = str.replaceAll(CustomAppsActivity.MARKET_SHORTCUT_HEAD, "");
					list_MarketShortcut = str.split(";");
				}
			}
			br.close();
		} catch (Exception e)
		{
			Log.d(TAG, "Read File Failed go " + e);
			return;
		}
		
	}

	public void getShortcutFromDefault(String srcPath, String desPath)
	{
		File srcFile = new File(srcPath);
		File desFile = new File(desPath);
		if(!srcFile.exists())
		{
			return;
		}
		if(!desFile.exists())
		{
			try
			{
				desFile.createNewFile();
			} catch (Exception e)
			{
				Log.e(TAG, e.getMessage().toString());
			}
		}

		try
		{
			BufferedReader br = new BufferedReader(new FileReader(srcFile));
			String str = null;
			List list = new ArrayList();

			while ((str = br.readLine()) != null)
			{
				list.add(str);
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(desFile));
			for (int i = 0; i < list.size(); i++)
			{
				bw.write(list.get(i).toString());
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (Exception e)
		{
			Log.d(TAG, "   " + e);
		}
	}

	public void copyFile(String oldPath, String newPath)
	{
		try
		{
			int bytesum = 0;
			int byteread = 0;
			File oldfile = new File(oldPath);
			// Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@ copy file");
			if(!oldfile.exists())
			{
				InputStream inStream = new FileInputStream(oldPath);
				FileOutputStream fs = new FileOutputStream(newPath);
				byte[] buffer = new byte[1444];
				int length;
				while ((byteread = inStream.read(buffer)) != -1)
				{
					bytesum += byteread;
					System.out.println(bytesum);
					fs.write(buffer, 0, byteread);
				}
				inStream.close();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private List<Map<String, Object>> loadShortcutList(PackageManager manager, final List<ResolveInfo> apps, String[] customapplist)
	{
		Map<String, Object> map = null;
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		if(customapplist != null)
		{
			for (int i = 0; i < customapplist.length; i++)
			{
				if(apps != null)
				{
					final int count = apps.size();
					for (int j = 0; j < count; j++)
					{
						ApplicationInfo application = new ApplicationInfo();

						ResolveInfo info = apps.get(j);

						application.title = info.loadLabel(manager);
						application.setActivity(new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name),
								Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
						application.icon = info.activityInfo.loadIcon(manager);
						if(application.componentName.getPackageName().equals(customapplist[i]))
						{
							map = new HashMap<String, Object>();
							map.put("item_name", application.title.toString());
							map.put("file_path", application.intent);
							map.put("item_type", application.icon);
							map.put("item_symbol", application.componentName);
							list.add(map);
							break;
						}
					}
				}
			}
		}

		return list;
	}

	private Map<String, Object> getAddMap()
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("item_name", this.getResources().getString(R.string.str_add));
		map.put("file_path", null);
		map.put("item_type", R.drawable.item_img_add);

		return map;
	}

	// / Add by Liu On 2013 11 19

	private void loadIntentParam()
	{
		String path = INTENT_PARAM_PATH;
		File mFile = new File(path);

		if(!mFile.exists())
		{
			finish();
		}
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(mFile));
			String str = null;
			while ((str = br.readLine()) != null)
			{
				if(str.startsWith(ONLINE_TV_KEY))
				{
					str = str.replaceAll(ONLINE_TV_KEY, "");
					List_OnlineTV = str.split(";");
				}
				else if(str.startsWith(SETTING_KEY))
				{
					str = str.replaceAll(SETTING_KEY, "");
					list_Setting = str.split(";");
				}
				/*
				 * else if (str.startsWith(MUSIC_KEY)) { str =
				 * str.replaceAll(MUSIC_KEY, ""); list_Music = str.split(";"); }
				 * 
				 * else if (str.startsWith(VIDEO_KEY)) { str =
				 * str.replaceAll(VIDEO_KEY, ""); list_Video = str.split(";"); }
				 * 
				 * else if (str.startsWith(PICTURE_KEY)) { str =
				 * str.replaceAll(PICTURE_KEY, ""); list_Picure =
				 * str.split(";"); }
				 */
				else if(str.startsWith(WEB_SITE_KEY))
				{
					str = str.replaceAll(WEB_SITE_KEY, "");
					List_WebSite = str.split(";");
				}

			}
		} catch (Exception e)
		{
			Log.d(TAG, "" + e);
		}

	}

	private void loadApplications()
	{
		List<Map<String, Object>> HomeShortCutList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> videoShortCutList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> onlineTVShortCutList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> appShortCutList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> MarketShortCutList = new ArrayList<Map<String, Object>>();
		// List<Map<String, Object>> musicShortCutList = new
		// ArrayList<Map<String, Object>>();
		// List<Map<String, Object>> localShortCutList = new
		// ArrayList<Map<String, Object>>();

		PackageManager manager = getPackageManager();
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
		Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

		HomeShortCutList.clear();
		videoShortCutList.clear();
		onlineTVShortCutList.clear();
		appShortCutList.clear();
		MarketShortCutList.clear();
		// musicShortCutList.clear();
		// localShortCutList.clear();

		loadCustomsApps(CustomAppsActivity.SHORTCUT_PATH);

		if(updateAllShortcut == true)
		{
			HomeShortCutList = loadShortcutList(manager, apps, list_homeShortcut);
			videoShortCutList = loadShortcutList(manager, apps, list_videoShortcut);
			onlineTVShortCutList = loadShortcutList(manager, apps, list_onlineTVShortcut);
			MarketShortCutList = loadShortcutList(manager, apps, list_MarketShortcut);
			// musicShortCutList = loadShortcutList(manager, apps,
			// list_musicShortcut);
			// localShortCutList = loadShortcutList(manager, apps,
			// list_localShortcut);

			if(apps != null)
			{
				final int count = apps.size();
				for (int i = 0; i < count; i++)
				{
					ApplicationInfo application = new ApplicationInfo();
					ResolveInfo info = apps.get(i);

					application.title = info.loadLabel(manager);
					application.setActivity(new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name),
							Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					application.icon = info.activityInfo.loadIcon(manager);

					Map<String, Object> map = new HashMap<String, Object>();
					map.put("item_name", application.title.toString());
					map.put("file_path", application.intent);
					map.put("item_type", application.icon);
					map.put("item_symbol", application.componentName);
					// Log.d(TAG, ""+ application.componentName.getPackageName()
					// + " path="+application.intent);
					appShortCutList.add(map);
				}
			}
			// add + map
			Map<String, Object> map = getAddMap();
			HomeShortCutList.add(map);
			videoShortCutList.add(map);
			onlineTVShortCutList.add(map);
			MarketShortCutList.add(map);

			homeShortcutView.setLayoutView(HomeShortCutList, 0);
			videoShortcutView.setLayoutView(videoShortCutList, 1);
			onlineTVShortcutView.setLayoutView(onlineTVShortCutList, 1);
			appShortcutView.setLayoutView(appShortCutList, 1);
			marketShortcutView.setLayoutView(MarketShortCutList, 1);

			tx_video_allcount.setText("/" + Integer.toString(videoShortCutList.size()));
			tx_onlineTV_allcount.setText("/" + Integer.toString(onlineTVShortCutList.size()));
			tx_app_allcount.setText("/" + Integer.toString(appShortCutList.size()));

			updateAllShortcut = false;
		}
		else if(CustomAppsActivity.current_shortcutHead.equals(CustomAppsActivity.VIDEO_SHORTCUT_HEAD))
		{
			videoShortCutList = loadShortcutList(manager, apps, list_videoShortcut);
			Map<String, Object> map = getAddMap();
			videoShortCutList.add(map);
			videoShortcutView.setLayoutView(videoShortCutList, 1);
			tx_video_allcount.setText("/" + Integer.toString(videoShortCutList.size()));
		}
		else if(CustomAppsActivity.current_shortcutHead.equals(CustomAppsActivity.TV_ONLINE_SHORTCUT_HEAD))
		{
			onlineTVShortCutList = loadShortcutList(manager, apps, list_onlineTVShortcut);
			Map<String, Object> map = getAddMap();
			onlineTVShortCutList.add(map);
			onlineTVShortcutView.setLayoutView(onlineTVShortCutList, 1);
			tx_onlineTV_allcount.setText("/" + Integer.toString(onlineTVShortCutList.size()));
		}
		else if(CustomAppsActivity.current_shortcutHead.equals(CustomAppsActivity.MARKET_SHORTCUT_HEAD))
		{
			MarketShortCutList = loadShortcutList(manager, apps, list_MarketShortcut);
			Map<String, Object> map = getAddMap();
			MarketShortCutList.add(map);
			marketShortcutView.setLayoutView(MarketShortCutList, 1);
			// tx_onlineTV_allcount.setText("/" +
			// Integer.toString(onlineTVShortCutList.size()));
		}
		else
		{
			HomeShortCutList = loadShortcutList(manager, apps, list_homeShortcut);
			Map<String, Object> map = getAddMap();
			HomeShortCutList.add(map);
			homeShortcutView.setLayoutView(HomeShortCutList, 0);
		}
		// Log.d(TAG,
		// "CustomAppsActivity.current_shortcutHead="+CustomAppsActivity.current_shortcutHead);
	}

	private void setRectOnKeyListener()
	{
		findViewById(R.id.layout_video).setOnKeyListener(new MyOnKeyListener(this, null));
		findViewById(R.id.layout_onlinetv).setOnKeyListener(new MyOnKeyListener(this, null));
		findViewById(R.id.layout_setting).setOnKeyListener(new MyOnKeyListener(this, null));
		findViewById(R.id.layout_app).setOnKeyListener(new MyOnKeyListener(this, null));
		findViewById(R.id.layout_web).setOnKeyListener(new MyOnKeyListener(this, null));
		findViewById(R.id.layout_local).setOnKeyListener(new MyOnKeyListener(this, null));

		findViewById(R.id.layout_video).setOnTouchListener(new MyOnTouchListener(this, null));
		findViewById(R.id.layout_onlinetv).setOnTouchListener(new MyOnTouchListener(this, null));
		findViewById(R.id.layout_setting).setOnTouchListener(new MyOnTouchListener(this, null));
		findViewById(R.id.layout_app).setOnTouchListener(new MyOnTouchListener(this, null));
		findViewById(R.id.layout_web).setOnTouchListener(new MyOnTouchListener(this, null));
		findViewById(R.id.layout_local).setOnTouchListener(new MyOnTouchListener(this, null));
	}

	public static void playClickMusic()
	{
		/*
		 * if (isSystemSoundOn == true) { sp_button.stop(music_prio_button);
		 * sp_button.play(music_prio_button, 1, 1, 0, 0, 1); }
		 */
	}

	public void setPopWindow(int top, int bottom)
	{
		View view = this.getWindow().getDecorView();
		Display display = this.getWindowManager().getDefaultDisplay();
		view.layout(0, 0, 1279, SCREEN_HEIGHT);
		view.setDrawingCacheEnabled(true);
		Bitmap bmp = Bitmap.createBitmap(view.getDrawingCache());
		view.destroyDrawingCache();
		// Log.d(TAG, "@@@@@@@@@@@@@@@@@@ window height="+ display.getHeight());

		if(bottom > SCREEN_HEIGHT / 2)
		{
			if(top + 3 - CustomAppsActivity.CONTENT_HEIGHT > 0)
			{
				screenShot = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), top);
				screenShot_keep = Bitmap.createBitmap(bmp, 0, CustomAppsActivity.CONTENT_HEIGHT, bmp.getWidth(), top + 3 - CustomAppsActivity.CONTENT_HEIGHT);
			}
			else
			{
				screenShot = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), CustomAppsActivity.CONTENT_HEIGHT);
				screenShot_keep = null;
			}
		}
		else
		{
			screenShot = Bitmap.createBitmap(bmp, 0, bottom, bmp.getWidth(), SCREEN_HEIGHT - bottom);
			screenShot_keep = Bitmap.createBitmap(bmp, 0, bottom, bmp.getWidth(), SCREEN_HEIGHT - (bottom + CustomAppsActivity.CONTENT_HEIGHT));
		}
	}

	private void sendWeatherBroadcast()
	{
		Intent intent = new Intent();
		intent.setAction(weather_request_action);
		sendBroadcast(intent);

		// Log.d(TAG,
		// "@@@@@@@@@@@@@@@@@@@@@@@@      send weather broadcast: "+weather_request_action);
	}

	private void setWeatherView(String str_weather)
	{
		if(str_weather == null || str_weather.length() == 0)
		{
			return;
		}

		String[] list_data = str_weather.split(",");
		ImageView img_weather = (ImageView) findViewById(R.id.img_weather);
		if(list_data.length >= 3 && list_data[2] != null)
			img_weather.setImageResource(parseIcon(list_data[2]));

		String str_temp = list_data[1] + " ";
		TextView tx_temp = (TextView) findViewById(R.id.tx_temp);
		tx_temp.setTypeface(Typeface.DEFAULT_BOLD);
		if(list_data.length >= 3 && str_temp.length() >= 1)
			tx_temp.setText(str_temp);

		String str_city = list_data[0];
		TextView tx_city = (TextView) findViewById(R.id.tx_city);
		if(list_data.length >= 3 && str_city.length() >= 1)
			tx_city.setText(str_city);
	}

	private int parseIcon(String strIcon)
	{
		if(strIcon == null)
			return -1;
		if("0".equals(strIcon))
			return R.drawable.sunny03;
		if("1".equals(strIcon))
			return R.drawable.cloudy03;
		if("2".equals(strIcon))
			return R.drawable.shade03;
		if("3".equals(strIcon))
			return R.drawable.shower01;
		if("4".equals(strIcon))
			return R.drawable.thunder_shower03;
		if("5".equals(strIcon))
			return R.drawable.rain_and_hail;
		if("6".equals(strIcon))
			return R.drawable.rain_and_snow;
		if("7".equals(strIcon))
			return R.drawable.s_rain03;
		if("8".equals(strIcon))
			return R.drawable.m_rain03;
		if("9".equals(strIcon))
			return R.drawable.l_rain03;
		if("10".equals(strIcon))
			return R.drawable.h_rain03;
		if("11".equals(strIcon))
			return R.drawable.hh_rain03;
		if("12".equals(strIcon))
			return R.drawable.hhh_rain03;
		if("13".equals(strIcon))
			return R.drawable.snow_shower03;
		if("14".equals(strIcon))
			return R.drawable.s_snow03;
		if("15".equals(strIcon))
			return R.drawable.m_snow03;
		if("16".equals(strIcon))
			return R.drawable.l_snow03;
		if("17".equals(strIcon))
			return R.drawable.h_snow03;
		if("18".equals(strIcon))
			return R.drawable.fog03;
		if("19".equals(strIcon))
			return R.drawable.ics_rain;
		if("20".equals(strIcon))
			return R.drawable.sand_storm02;
		if("21".equals(strIcon))
			return R.drawable.m_rain03;
		if("22".equals(strIcon))
			return R.drawable.l_rain03;
		if("23".equals(strIcon))
			return R.drawable.h_rain03;
		if("24".equals(strIcon))
			return R.drawable.hh_rain03;
		if("25".equals(strIcon))
			return R.drawable.hhh_rain03;
		if("26".equals(strIcon))
			return R.drawable.m_snow03;
		if("27".equals(strIcon))
			return R.drawable.l_snow03;
		if("28".equals(strIcon))
			return R.drawable.h_snow03;
		if("29".equals(strIcon))
			return R.drawable.smoke03;
		if("30".equals(strIcon))
			return R.drawable.sand_blowing03;
		if("31".equals(strIcon))
			return R.drawable.sand_storm03;
		return 0;
	}

	public static int parseItemIcon(String packageName)
	{
		if(packageName.equals("com.fb.FileBrower"))
		{
			return R.drawable.icon_filebrowser;
		}
		else if(packageName.equals("com.boxchiptv.OOBE"))
		{
			return R.drawable.icon_oobe;
		}
		else if(packageName.equals("com.android.browser"))
		{
			return R.drawable.icon_browser;
		}
		else if(packageName.equals("com.gsoft.appinstall"))
		{
			return R.drawable.icon_appinstaller;
		}
		else if(packageName.equals("com.farcore.videoplayer"))
		{
			return R.drawable.icon_videoplayer;
		}
		else if(packageName.equals("com.aml.settings"))
		{
			return R.drawable.icon_amlsetting;
		}
		else if(packageName.equals("com.boxchiptv.mediacenter"))
		{
			return R.drawable.icon_mediacenter;
		}
		else if(packageName.equals("com.amlapp.update.otaupgrade"))
		{
			return R.drawable.icon_backupandupgrade;
		}
		else if(packageName.equals("com.android.gallery3d"))
		{
			return R.drawable.icon_pictureplayer;
		}
		else if(packageName.equals("com.boxchiptv.netfilebrowser"))
		{
			return R.drawable.icon_networkneiborhood;
		}
		else if(packageName.equals("st.com.xiami"))
		{
			return R.drawable.icon_xiami;
		}
		else if(packageName.equals("com.android.providers.downloads.ui"))
		{
			return R.drawable.icon_download;
		}
		else if(packageName.equals("app.android.applicationxc"))
		{
			return R.drawable.icon_xiaocong;
		}
		else if(packageName.equals("com.example.airplay"))
		{
			return R.drawable.icon_airplay;
		}
		else if(packageName.equals("com.boxchiptv.miracast"))
		{
			return R.drawable.icon_miracast;
		}
		else if(packageName.equals("com.boxchiptv.PPPoE"))
		{
			return R.drawable.icon_pppoe;
		}
		else if(packageName.equals("com.android.service.remotecontrol"))
		{
			return R.drawable.icon_remotecontrol;
		}
		else if(packageName.equals("com.mbx.settingsmbox"))
		{
			return R.drawable.icon_setting;
		}
		else if(packageName.equals("com.android.music"))
		{
			return R.drawable.icon_music;
		}
		return -1;
	}

	private void sendKeyCode(final int keyCode)
	{
		new Thread()
		{
			public void run()
			{
				try
				{
					Instrumentation inst = new Instrumentation();
					inst.sendKeyDownUpSync(keyCode);
				} catch (Exception e)
				{
					Log.e("Exception when sendPointerSync", e.toString());
				}
			}
		}.start();
	}

	private void resetShadow()
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					Thread.sleep(500);
				} catch (Exception e)
				{
					Log.d(TAG, "" + e);
				}
				// Message msg = new Message();
				// msg.what = 2;
				mHandler.sendEmptyMessage(2);
			}
		}).start();
	}

	private Handler mHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case 1 :
					setPopWindow(popWindow_top, popWindow_bottom);
					break;
				case 2 :
					MyRelativeLayout view = (MyRelativeLayout) getCurrentFocus();
					view.setSurface();
					break;
				case 3 :
					ViewGroup findGridLayout = ((ViewGroup) ((ViewGroup) ((ViewGroup) viewMenu.getCurrentView()).getChildAt(3)).getChildAt(0));
					if (findGridLayout == null) 
					{
						Log.d(TAG, "Handle Message findGridLayout = Null");
						break;
					}
					
					int count = findGridLayout.getChildCount();
					Log.d(TAG, "findGridLayout.getChildCount()"+count);
					if(count<=0)
					{
						Log.d(TAG, "Break findGridLayout.getChildCount()"+count);
						break;
					}
					Launcher.dontRunAnim = true;
					findGridLayout.getChildAt(count - 1).requestFocus();
					Launcher.dontRunAnim = false;
					break;
				case 4 :
					int i = homeShortcutView.getChildCount();
					Launcher.dontRunAnim = true;
					homeShortcutView.getChildAt(i - 1).requestFocus();
					Launcher.dontRunAnim = false;
					layoutScaleShadow.setVisibility(View.VISIBLE);
					frameView.setVisibility(View.VISIBLE);
					break;
				default :
					break;
			}
		}
	};

	private BroadcastReceiver mediaReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			// Log.d(TAG,
			// " mediaReceiver		  action = "
			// + action);
			if(action == null)
				return;

			if(Intent.ACTION_MEDIA_EJECT.equals(action) || Intent.ACTION_MEDIA_UNMOUNTED.equals(action) || Intent.ACTION_MEDIA_MOUNTED.equals(action))
			{
				displayStatus();
				updateStatus();
			}
		}
	};

	private BroadcastReceiver netReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			if(action == null)
				return;

			// Log.d(TAG,
			// "netReceiver         action = "
			// + action);

			if(action.equals(Intent.ACTION_TIME_TICK))
			{
				displayDate();

				time_count++;
				if(bHasGetWether == false)
				{
					sendWeatherBroadcast();
				}
				else
				{
					if(time_count >= time_freq)
					{
						sendWeatherBroadcast();
						time_count = 0;
					}
				}
			}
			else if(action.equals(weather_receive_action))
			{
				String weatherInfo = intent.getExtras().getString("weather_today");
				// Log.d(TAG,
				// "@@@@@@@@@@@@@@@@@@@@@@@@@@ receive "
				// + action +
				// " weather:" +
				// weatherInfo);
				setWeatherView(weatherInfo);
				bHasGetWether = true;
			}
			else if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION) || action.equals(net_change_action))
			{
				sendWeatherBroadcast();
				displayStatus();
				updateStatus();
			}

			else
			{
				displayStatus();
				updateStatus();
			}
		}

	};

	private BroadcastReceiver appReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// TODO Auto-generated
			// method stub

			final String action = intent.getAction();
			if(Intent.ACTION_PACKAGE_CHANGED.equals(action) || Intent.ACTION_PACKAGE_REMOVED.equals(action) || Intent.ACTION_PACKAGE_ADDED.equals(action))
			{

				final String packageName = intent.getData().getSchemeSpecificPart();
				final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

				if(packageName == null || packageName.length() == 0)
				{
					// they sent us a
					// bad intent
					return;
				}
                Log.d(TAG, "BroadcastReceiver = "+ action);
				updateAllShortcut = true;
				loadApplications();
			}
		}
	};
}
