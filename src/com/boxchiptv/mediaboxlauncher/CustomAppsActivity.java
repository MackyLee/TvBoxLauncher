package com.boxchiptv.mediaboxlauncher;

import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.content.IntentFilter;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.GridView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CustomAppsActivity extends Activity
{
	/** Called when the activity is first created. */
	private static final String TAG = "CustomAppsActivity";

	private ImageView img_screen_shot = null;
	private ImageView img_screen_shot_keep = null;
	private ImageView img_arrow = null;
	private ImageView img_dim = null;
	private GridView gv = null;
	private Context mContext = null;

	// private AnimationDrawable anim_selector = null;

	private File mFile;
	private String[] customapplist;
	private String customapps;

	public final static String SHORTCUT_PATH = "/data/data/com.boxchiptv.mediaboxlauncher/shortcut.cfg";
	public final static String DEFAULT_SHORTCUR_PATH = "/system/etc/default_shortcut.cfg";

	public final static String HOME_SHORTCUT_HEAD = "HOME_SHORTCUT:";
	public final static String VIDEO_SHORTCUT_HEAD = "VIDEO_SHORTCUT:";
	public final static String MUSIC_SHORTCUT_HEAD = "MUSIC_SHORTCUT:";
	public final static String LOCAL_SHORTCUT_HEAD = "Local_Shortcut:";
	public final static String MARKET_SHORTCUT_HEAD = "MARKET_SHORTCUT:";
	public final static String RECOMMEND_SHORTCUT_HEAD = "RECOMMEND_SHORTCUT:";
	public final static String TV_ONLINE_SHORTCUT_HEAD = "TVONLINE_SHORTCUT:";
	public final static String SYSTEM_SETTING = "SYSTEM_SETTING:";
	public final static String LOCAL_MEDIA = "LOCAL_MEDIA:";
	public final static String MY_APPLICATIONS = "MY_APPLICATIONS:";
	
	public final static String INVALID_HEAD = "INVALID_HEAD:";
	
	public static String current_shortcutHead = HOME_SHORTCUT_HEAD;

	public static final int CONTENT_HEIGHT = 300;
	private final int SCREEN_HEIGHT = 719;
	private int homeShortcutCount = 0;
	private TranslateAnimation exitTransAnim;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.d(TAG, "------onCreate");
		Bundle extras = getIntent().getExtras();
		int top = extras.getInt("top");
		int bottom = extras.getInt("bottom");
		int left = extras.getInt("left");
		int right = extras.getInt("right");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// getWindow().setWindowAnimations(R.style.dialogWindowAnim);
		setContentView(R.layout.layout_custom_apps);

		gv = (GridView) findViewById(R.id.grid_add_apps);
		gv.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id)
			{
				Map<String, Object> item = (Map<String, Object>) parent.getItemAtPosition(pos);

				// Launcher.playClickMusic();
				Launcher.ifChangedShortcut = true;

				if(item.get("item_type").equals(R.drawable.item_img_exit))
				{
					finish();
				}
				else if(item.get("item_sel").equals(R.drawable.item_img_unsel))
				{
					if(current_shortcutHead.equals(HOME_SHORTCUT_HEAD) && homeShortcutCount >= Launcher.HOME_SHORTCUT_COUNT)
					{
						Toast.makeText(mContext, mContext.getResources().getString(R.string.str_nospace), Toast.LENGTH_SHORT).show();
						return;
					}
					String str_package_name = ((ComponentName) item.get("item_symbol")).getPackageName();
					if(customapps == null)
					{
						customapps = current_shortcutHead + str_package_name + ";";
					}
					else
					{
						customapps += str_package_name + ";";
					}
					((Map<String, Object>) parent.getItemAtPosition(pos)).put("item_sel", R.drawable.item_img_sel);
					updateView();
					homeShortcutCount++;
				}
				else
				{
					String str_package_name = ((ComponentName) item.get("item_symbol")).getPackageName();
					customapps = customapps.replaceAll(str_package_name + ";", "");
					((Map<String, Object>) parent.getItemAtPosition(pos)).put("item_sel", R.drawable.item_img_unsel);
					updateView();
					homeShortcutCount--;
				}

			}
		});

		mContext = this;
		img_screen_shot = (ImageView) findViewById(R.id.img_screenshot);
		img_screen_shot_keep = (ImageView) findViewById(R.id.img_screenshot_keep);
		img_arrow = (ImageView) findViewById(R.id.img_arrow);
		img_dim = (ImageView) findViewById(R.id.img_dim);
		// img_screen_shot.setVisibility(View.INVISIBLE);
		displayView();
		setViewPosition(top, bottom, left, right);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		Log.d(TAG, "------onResume");

		customapps = "";
		displayView();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		Log.d(TAG, "------onPause");

		saveShortcut(SHORTCUT_PATH, customapps);
	}

	@Override
	public void onStop()
	{
		super.onStop();
		Log.d(TAG, "------onStop");

	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.d(TAG, "------onDestroy");
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			img_screen_shot.bringToFront();
			// Animation anim = AnimationUtils.loadAnimation(mContext,
			// R.anim.anim_alpha_disappear);
			// img_dim.startAnimation(anim);
			img_dim.setVisibility(View.INVISIBLE);
			img_screen_shot.startAnimation(exitTransAnim);

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void displayView()
	{
		LocalAdapter ad = new LocalAdapter(CustomAppsActivity.this,
                                           loadCustomAppInfo(),
                                           R.layout.add_apps_grid_item, 
                                           new String[]	{"item_type", "item_name", "item_sel", "item_bg"}, 
                                           new int[]{R.id.item_type, R.id.item_name, R.id.item_sel, R.id.relative_layout}
		);
		gv.setAdapter(ad);
	}

	private void updateView()
	{
		((BaseAdapter) gv.getAdapter()).notifyDataSetChanged();
	}

	/*
	 * private void setBackgroundForItem(){ Log.d(TAG,
	 * "@@@@@@@@@@@@@@@@@@@@@@@@ unit count =" + gv.getChildCount()); int count
	 * = gv.getChildCount(); for (int i = 0; i < count; i++){ View item =
	 * gv.getChildAt(i).findViewById(R.id.item_type);
	 * item.setBackgroundResource(parseItemBackground(i)); } }
	 */

	private int parseItemBackground(int num)
	{
		switch (num % 13)
		{
			case 0 :
				return R.drawable.item_1;
			case 1 :
				return R.drawable.item_2;
			case 2 :
				return R.drawable.item_3;
			case 3 :
				return R.drawable.item_4;
			case 4 :
				return R.drawable.item_5;
			case 5 :
				return R.drawable.item_6;
			case 6 :
				return R.drawable.item_7;
			case 7 :
				return R.drawable.item_8;
			case 8 :
				return R.drawable.item_9;
			case 9 :
				return R.drawable.item_10;
			case 10 :
				return R.drawable.item_11;
			case 11 :
				return R.drawable.item_12;
			case 12 :
				return R.drawable.item_13;
			default :
				return R.drawable.item_1;
		}
	}

	private List<Map<String, Object>> loadCustomAppInfo()
	{

		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = new HashMap<String, Object>();

		PackageManager manager = getPackageManager();
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
		Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

		customapplist = loadCustomApps(SHORTCUT_PATH, current_shortcutHead);

		/*
		 * map = new HashMap<String, Object>();
		 * map.put("item_name",getString(R.string.str_exit));
		 * map.put("file_path", null); map.put("item_type",
		 * R.drawable.item_img_exit); map.put("item_sel",
		 * R.drawable.item_img_unsel); list.add(map);
		 */

		if(apps != null)
		{
			final int count = apps.size();

			for (int i = 0; i < count; i++)
			{
				ApplicationInfo application = new ApplicationInfo();
				ResolveInfo info = apps.get(i);

				application.title = info.loadLabel(manager);
				application.setActivity(new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name), Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				application.icon = info.activityInfo.loadIcon(manager);

				map = new HashMap<String, Object>();
				map.put("item_name", application.title.toString());
				map.put("file_path", application.intent);
				if(Launcher.parseItemIcon(application.componentName.getPackageName()) != -1)
				{
					map.put("item_type", Launcher.parseItemIcon(application.componentName.getPackageName()));
				}
				else
				{
					map.put("item_type", application.icon);
				}
				map.put("item_sel", R.drawable.item_img_unsel);
				map.put("item_bg", parseItemBackground(i));
				map.put("item_symbol", application.componentName);

				if(customapplist != null)
				{
					for (int j = 0; j < customapplist.length; j++)
					{

						if(application.componentName.getPackageName().equals(customapplist[j]))
						{
							map.put("item_sel", R.drawable.item_img_sel);
							break;
						}
					}
				}
				list.add(map);
				// Log.i(TAG, ""+application.title.toString());
			}

		}

		return list;
	}

	private void setViewPosition(int top, int bottom, int left, int right)
	{
		TranslateAnimation translateAnimation;
		int arrow_x_center = left + (right - left) / 2;
		img_screen_shot.setImageBitmap(Launcher.screenShot);

		if(bottom > SCREEN_HEIGHT / 2)
		{
			android.widget.AbsoluteLayout.LayoutParams lp = new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT, 0, 0);
			lp.y = 0;
			img_screen_shot.setLayoutParams(lp);
			translateAnimation = new TranslateAnimation(0.0f, 0.0f, 0.0f, (float) (0 - CONTENT_HEIGHT));
			translateAnimation.setDuration(500);
			exitTransAnim = new TranslateAnimation(0.0f, 0.0f, (float) (0 - CONTENT_HEIGHT), 0.0f);
			exitTransAnim.setDuration(300);

			android.widget.AbsoluteLayout.LayoutParams lp1 = new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT, 0, 0);
			lp1.height = CONTENT_HEIGHT;
			if(top - CONTENT_HEIGHT > 0)
			{
				lp1.y = top - CONTENT_HEIGHT;
			}
			else
			{
				lp1.y = 0;
			}
			gv.setLayoutParams(lp1);
			translateAnimation.setAnimationListener(new MyAnimationListener(lp1.y, arrow_x_center, 0));
		}
		else
		{
			android.widget.AbsoluteLayout.LayoutParams lp = new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT, 0, 0);
			lp.y = bottom;
			img_screen_shot.setLayoutParams(lp);
			translateAnimation = new TranslateAnimation(0.0f, 0.0f, 0.0f, (float) (CONTENT_HEIGHT));
			translateAnimation.setDuration(500);
			exitTransAnim = new TranslateAnimation(0.0f, 0.0f, (float) (CONTENT_HEIGHT), 0.0f);
			exitTransAnim.setDuration(300);

			android.widget.AbsoluteLayout.LayoutParams lp1 = new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT, 0, 0);
			lp1.y = bottom;
			lp1.height = CONTENT_HEIGHT;
			gv.setLayoutParams(lp1);
			translateAnimation.setAnimationListener(new MyAnimationListener(lp1.y, arrow_x_center, 1));
		}

		exitTransAnim.setAnimationListener(new exitAnimationListener());
		img_screen_shot.startAnimation(translateAnimation);
	}

	private void setArrowPosition(int top, int x_center, int flag)
	{
		android.widget.AbsoluteLayout.LayoutParams lp2 = new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				0, 0);
		lp2.x = x_center - 13;
		if(flag == 0)
		{
			lp2.y = top + CONTENT_HEIGHT - 4;
			img_arrow.setImageResource(R.drawable.arrow_down);
		}
		else
		{
			lp2.y = top - 15 + 4;
			img_arrow.setImageResource(R.drawable.arrow_up);
		}
		img_arrow.setLayoutParams(lp2);
	}

	private String[] loadCustomApps(String path, String shortcut_head)
	{
		String[] list_custom_apps = {};
		String appstr;
		
		mFile = new File(path);
		
		 Log.d(TAG, "Begin loadCustomApps  " + shortcut_head+"1  "+ path);
		 
		if(!mFile.exists())
		{
			Log.d(TAG, "File not found  " + shortcut_head+"2  "+ path);
			return null;
		}

		try
		{
			BufferedReader br = new BufferedReader(new FileReader(mFile));
			String str = null;
			while ((str = br.readLine()) != null)
			{
				if(str.startsWith(shortcut_head))
				{
					break;
				}
			}
			if(str == null)
			{
				Log.d(TAG, "NUll String 3" + shortcut_head+" "+ str);
				br.close();
				return null;
			}
			
			appstr = str.replaceAll(shortcut_head, "");
			list_custom_apps = appstr.split(";");
			
			customapps = appstr;
			homeShortcutCount = list_custom_apps.length;

/*			 Log.d(TAG, "str=" + str);
			 Log.d(TAG, "list_custom_apps=" + list_custom_apps.toString());
			 Log.d(TAG, "shortcut_head=" + shortcut_head);
			 Log.d(TAG, "path=" + path);*/
			br.close();
		} catch (Exception e)
		{
			 Log.d(TAG, "Failed E = " + e);
			return null;
		}
		
		return list_custom_apps;

	}

	public void saveShortcut(String path, String str_apps)
	{
		mFile = new File(path);
		if(!mFile.exists())
		{
			try
			{
				mFile.createNewFile();
			} catch (Exception e)
			{
				Log.e(TAG, e.getMessage().toString());
				return;
			}
		}
        if(str_apps.toString().equals(INVALID_HEAD)||(str_apps=="")||(str_apps==INVALID_HEAD))
        {
           return;
        }
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(mFile));
			String str = null;
			List list = new ArrayList();

			while ((str = br.readLine()) != null)
			{
				list.add(str);
			}
			br.close();
			if(list.size() == 0)
			{
				list.add(HOME_SHORTCUT_HEAD);
				list.add(VIDEO_SHORTCUT_HEAD);
				list.add(RECOMMEND_SHORTCUT_HEAD);
				list.add(MUSIC_SHORTCUT_HEAD);
				list.add(LOCAL_SHORTCUT_HEAD);
				list.add(MARKET_SHORTCUT_HEAD);
				list.add(TV_ONLINE_SHORTCUT_HEAD);
			}
			 Log.d(TAG, "current_shortcutHead=" + current_shortcutHead+"   "+str_apps);

			BufferedWriter bw = new BufferedWriter(new FileWriter(mFile));
			for (int i = 0; i < list.size(); i++)
			{
				if(list.get(i).toString().startsWith(current_shortcutHead))
				{
					if (str_apps.toString().startsWith(current_shortcutHead))
					{
						bw.write(str_apps);
						Log.d(TAG, "wirte " + str_apps);
					}
					else
					{
					    str_apps = current_shortcutHead + str_apps;
					    bw.write(str_apps);
					    Log.d(TAG, "Saved " + str_apps);
					}
				 }
				else
				{
					bw.write(list.get(i).toString());//same to save
					// Log.d(TAG, "@@@@@@@@@@@@@@@@@@ wirte " +
					// list.get(i).toString());
				}
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (Exception e)
		{
			Log.d(TAG, "   " + e);
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

		try
		{
			BufferedReader br = new BufferedReader(new FileReader(srcFile));
			String str = null;
			List list = new ArrayList();

			while ((str = br.readLine()) != null)
			{
				list.add(str);
			}
			br.close();
			BufferedWriter bw = new BufferedWriter(new FileWriter(mFile));
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

	private class MyAnimationListener implements AnimationListener
	{
		private int mTop;
		private int up_or_down;
		private int arrow_x_center;

		// private Animation mAnim;

		public MyAnimationListener(int top, int x_center, int flag)
		{
			mTop = top;
			up_or_down = flag;
			arrow_x_center = x_center;
		}

		@Override
		public void onAnimationStart(Animation animation)
		{
			// setArrowPosition(mTop, arrow_x_center, up_or_down);
		}

		@Override
		public void onAnimationEnd(Animation animation)
		{
			android.widget.AbsoluteLayout.LayoutParams lp = new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT, 0, 0);
			img_screen_shot_keep.setImageBitmap(Launcher.screenShot_keep);
			if(up_or_down == 0)
			{
				lp.y = 0;
			}
			else
			{
				lp.y = mTop + CONTENT_HEIGHT;
			}
			img_screen_shot_keep.setLayoutParams(lp);
			// mAnim.reset();
			img_screen_shot.setVisibility(View.INVISIBLE);
			Animation anim = AnimationUtils.loadAnimation(mContext, R.anim.anim_alpha_show);
			anim.setAnimationListener(new dimAnimationListener());
			img_dim.startAnimation(anim);
			// img_dim.setVisibility(View.VISIBLE);
			gv.bringToFront();
		}

		@Override
		public void onAnimationRepeat(Animation animation)
		{

		}

	}

	private class dimAnimationListener implements AnimationListener
	{
		@Override
		public void onAnimationStart(Animation animation)
		{
		}

		@Override
		public void onAnimationEnd(Animation animation)
		{
			img_dim.setVisibility(View.VISIBLE);
		}

		@Override
		public void onAnimationRepeat(Animation animation)
		{
		}
	}

	private class exitAnimationListener implements AnimationListener
	{
		@Override
		public void onAnimationStart(Animation animation)
		{
		}

		@Override
		public void onAnimationEnd(Animation animation)
		{
			img_screen_shot.setVisibility(View.VISIBLE);
			finish();
		}

		@Override
		public void onAnimationRepeat(Animation animation)
		{
		}
	}
}
