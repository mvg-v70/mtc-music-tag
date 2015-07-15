package com.mvgv70.mtc_music_tag;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

public class Main implements IXposedHookLoadPackage {
	
  final static String TAG = "music-tag";
  private static Activity musicActivity;
  private static BroadcastReceiver usbReceiver = null;
  	
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
    //
    // MusicActivity.onCreate(Bundle)
    XC_MethodHook onCreate = new XC_MethodHook() {
        
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onCreate");
        musicActivity = (Activity)param.thisObject;
        // обработчик интента при смене проигрывемого файла
        IntentFilter mi = new IntentFilter();
        mi.addAction("com.microntek.musictitle");
        musicActivity.registerReceiver(titleReceiver, mi);
        // обработчик MEDIA_MOUNT/UNMOUNT/EJECT
        usbReceiver = (BroadcastReceiver)XposedHelpers.getObjectField(musicActivity,"UsbCardBroadCastReceiver");
        if (usbReceiver != null)
      	{
      	  // выключаем receiver на монтирование флешки
          musicActivity.unregisterReceiver(usbReceiver);
      	  // заменяем его другим, который никогда не вызывается
          musicActivity.registerReceiver(usbReceiver, new IntentFilter());
          // включаем свой обработчик
          IntentFilter ui = new IntentFilter();
          // обработчик носителей media
          ui.addAction(Intent.ACTION_MEDIA_MOUNTED);
          ui.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
          ui.addAction(Intent.ACTION_MEDIA_EJECT);
          // TODO: заменить на константу
          ui.addDataScheme("file");
          musicActivity.registerReceiver(mediaReceiver, ui);
      	  Log.d(TAG,"UsbCardBroadCastReceiver changed");
      	}
      }
    };
    // MusicActivity.onDestroy()
    XC_MethodHook onDestroy = new XC_MethodHook() {
        
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    	Log.d(TAG,"onDestroy");
        musicActivity.unregisterReceiver(titleReceiver);
        if (usbReceiver != null)
          musicActivity.unregisterReceiver(mediaReceiver);
      }
    };
    
    // start hooks
    if (!lpparam.packageName.equals("com.microntek.music")) return;
    Log.d(TAG,"com.microntek.music");
    XposedHelpers.findAndHookMethod("com.microntek.music.MusicActivity", lpparam.classLoader, "onCreate", Bundle.class, onCreate);
    XposedHelpers.findAndHookMethod("com.microntek.music.MusicActivity", lpparam.classLoader, "onDestroy", onDestroy);
    Log.d(TAG,"com.microntek.music hook OK");
  }
  
  public static Object getMusicActivity()
  {
    return musicActivity;
  }
  
  // обработчик смены треков
  private BroadcastReceiver titleReceiver = new BroadcastReceiver()
  {
	  
    public void onReceive(Context context, Intent intent)
    {
      Log.d(TAG,"com.microntek.musictitle");
      String musictitle = intent.getStringExtra("musictitle");
      if (musictitle != null)
      {
        int currentPlayIndex = XposedHelpers.getIntField(musicActivity,"currentPlayIndex");
        Log.d(TAG,"currentPlayIndex="+currentPlayIndex);
        // список файлов 
        @SuppressWarnings("unchecked")
        ArrayList<String> music_list = (ArrayList<String>)XposedHelpers.getObjectField(musicActivity, "music_list");
        if (music_list == null) Log.d(TAG,"music_list = NULL");
        Log.d(TAG,"length="+music_list.size());
        // показываем тэги проигрываемого файла
        Resource.setMp3Tags(music_list.get(currentPlayIndex));
      }
      else
    	// очистим поля с тегами
    	Resource.clearMp3Tags();
    }
  };
  
  // обработчик MEDIA_MOUNT/UNMOUNT/EJECT
  private BroadcastReceiver mediaReceiver = new BroadcastReceiver()
  {
	  
    public void onReceive(Context context, Intent intent)
    {
      String action = intent.getAction(); 
      Log.d(TAG,"media receiver:"+action);
      if(action.equals(Intent.ACTION_MEDIA_MOUNTED))
      {
    	// подключение флешки, карты
        int automedia_enable = Settings.System.getInt(context.getContentResolver(),"MusicAutoPlayEN",0);
        Log.d(TAG,"automedia_enable="+automedia_enable);
        if (automedia_enable == 0)
        {
          // автоматическое воспроизведение выключено 
          return;
        }
      }
      // вызываем обработчик по умолчанию
      Log.d(TAG,"call default usbReceiver");
      usbReceiver.onReceive(context, intent);
    }
   
 };
  
}
	