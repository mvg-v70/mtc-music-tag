package com.mvgv70.mtc_music_tag;

import java.io.FileInputStream;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import android.widget.RelativeLayout.LayoutParams;
import android.graphics.Bitmap;

import java.util.Properties;

public class Resource implements IXposedHookInitPackageResources {
	
  private final static String TAG = "music-tag";
  private final static String MUSIC_PACKAGE = "com.microntek.music";
  private final static String RKUTF8 = "rkutf8";
  private final static String INI_FILE_NAME = Environment.getExternalStorageDirectory().getPath()+"/mtc-music/mtc-music.ini";
  private static TextView title;
  private static TextView album;
  private static TextView artist;
  private static ImageView cover;
  private static View visualizer;
  private static Properties props;
	
  public static void setMp3Tags(String fileName)
  {
    Log.d(TAG,"read tags from "+fileName);
    // очистим поля
    title.setText("");
    album.setText("");
    artist.setText("");
    cover.setImageBitmap(null);
    // прочитаем тэги mp3-файла
    try
    {
      MediaMetadataRetriever mmr = new MediaMetadataRetriever();
      mmr.setDataSource(fileName);
      // title
      String title_tag = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
      if (title_tag != null)
        if (title_tag.startsWith(RKUTF8))
          title_tag = title_tag.substring(6,title_tag.length());
      // author
      String author_tag = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR);
      if (author_tag != null)
        if (author_tag.startsWith(RKUTF8))
          author_tag = author_tag.substring(6,author_tag.length());
      // album
      String album_tag = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
      if (album_tag != null)
        if (album_tag.startsWith(RKUTF8))
          album_tag = album_tag.substring(6,album_tag.length());
      // artist
      String artist_tag = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
      if (artist_tag != null)
        if (artist_tag.startsWith(RKUTF8))
          artist_tag = artist_tag.substring(6,artist_tag.length());
      //
      Log.d(TAG,"title="+title_tag);
      Log.d(TAG,"album="+album_tag);
      Log.d(TAG,"author="+author_tag);
      Log.d(TAG,"artist="+artist_tag);
      // заполняем поля
      title.setText(title_tag);
      album.setText(album_tag);
      artist.setText(artist_tag);
      // cover
      if (getBooleanProperty("cover.show",true))
      {
        byte[] tagPicture =  mmr.getEmbeddedPicture();
        if (tagPicture != null)
        {
          Log.d(TAG,"has picture");
          visualizer.setVisibility(View.INVISIBLE);
          Bitmap bm = BitmapFactory.decodeByteArray(tagPicture, 0, tagPicture.length);
          cover.setImageBitmap(bm);
          cover.setVisibility(View.VISIBLE);
        }
        else
        {
          Log.d(TAG,"no picture in file");
          // показываем visualizer
          visualizer.setVisibility(View.VISIBLE);
          cover.setVisibility(View.INVISIBLE);
        }
      }
      // TODO: добавить блок finally
      mmr.release();
    } catch (Exception e) {
      Log.d(TAG,"tag read error: "+e.getMessage());
    }
    Log.d(TAG,"end read tags");
  }
  
  public static void clearMp3Tags()
  {
    Log.d(TAG,"clear tags");
    // очистим поля
    title.setText("");
    album.setText("");
    artist.setText("");
    cover.setImageBitmap(null);
    cover.setVisibility(View.INVISIBLE);
    visualizer.setVisibility(View.VISIBLE);
  }

	
  private static int getIntProperty(String name, int defValue)
  {
    try
    {
      String val = props.getProperty(name);
      return Integer.valueOf(val);
    } catch (Exception e) {
      return defValue;
    }
  }
	
  private static boolean getBooleanProperty(String name, boolean defValue)
  {
    String val = props.getProperty(name);
    if (val != null)
	{
      if (val.equalsIgnoreCase("true"))
        return true;
      else if (val.equalsIgnoreCase("false"))
        return false;
      else
       return defValue;
	}
    return defValue;
	}
	
  @Override
  public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable 
  {
    if (!resparam.packageName.equals(MUSIC_PACKAGE)) return;
    Log.d(TAG,MUSIC_PACKAGE+": resource");

    resparam.res.hookLayout(MUSIC_PACKAGE, "layout", "main", new XC_LayoutInflated() {
      @Override
      public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable 
	  {
        Log.d(TAG,"handleLayoutInflated begin");
        //
        // настройки
        //
        try
        {
          Log.d(TAG,"inifile load from "+INI_FILE_NAME);
          props = new Properties();
          props.load(new FileInputStream(INI_FILE_NAME));
          Log.d(TAG,"ini file loaded, line count="+props.size());
        } catch (Exception e) {
          Log.e(TAG,e.getMessage());
        }
        //
        // создание новых полей
        //
        Log.d(TAG,"create new fields");
        // layout
        RelativeLayout layout = (RelativeLayout)liparam.view.findViewById(liparam.res.getIdentifier("bar_middle", "id", MUSIC_PACKAGE));
        // music_number
        int musicnumberId = liparam.res.getIdentifier("music_number", "id", MUSIC_PACKAGE);
        // title
        title = new TextView(layout.getContext());
        RelativeLayout.LayoutParams paramsTitle = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        paramsTitle.addRule(RelativeLayout.BELOW, musicnumberId);
        paramsTitle.topMargin = getIntProperty("title.top",-20); 
        paramsTitle.leftMargin = getIntProperty("title.left",400);
        paramsTitle.width = getIntProperty("title.width",350);
        title.setLayoutParams(paramsTitle);
        title.setTransformationMethod(null);
        title.setTextSize(getIntProperty("title.textsize",16));
        title.setMaxLines(getIntProperty("title.lines",3));
        layout.addView(title);
        // album
        album = new TextView(layout.getContext());
        RelativeLayout.LayoutParams paramsAlbum = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        paramsAlbum.addRule(RelativeLayout.BELOW, musicnumberId);
        paramsAlbum.topMargin = getIntProperty("album.top",65);
        paramsAlbum.leftMargin = getIntProperty("album.left",400);
        paramsAlbum.width = getIntProperty("album.width",340);
        album.setLayoutParams(paramsAlbum);
        album.setTransformationMethod(null);
        album.setTextSize(getIntProperty("album.textsize",16));
        album.setMaxLines(getIntProperty("album.lines",3));
        layout.addView(album);
        // artist
        artist = new TextView(layout.getContext());
        RelativeLayout.LayoutParams paramsArtist = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        paramsArtist.addRule(RelativeLayout.BELOW, musicnumberId);
        paramsArtist.topMargin = getIntProperty("artist.top",150);
		paramsArtist.leftMargin = getIntProperty("artist.left",400);
		paramsArtist.width = getIntProperty("artist.width",350);
		artist.setLayoutParams(paramsArtist);
		artist.setTransformationMethod(null);
		artist.setTextSize(getIntProperty("artist.textsize",16));
		artist.setMaxLines(getIntProperty("artist.lines",1));
		layout.addView(artist);
		//
		// размеры штатных полей
		//
		Log.d(TAG,"standard fields sizes");
		// music_number
		Log.d(TAG,"musicNumber");
		TextView musicNumber = (TextView)liparam.view.findViewById(musicnumberId);
		((RelativeLayout.LayoutParams)musicNumber.getLayoutParams()).leftMargin = getIntProperty("music_number.left",50); 
		// music_title
		Log.d(TAG,"music_title");
		TextView music_title = (TextView)liparam.view.findViewById(liparam.res.getIdentifier("music_title", "id", MUSIC_PACKAGE));
		((RelativeLayout.LayoutParams)music_title.getLayoutParams()).width = getIntProperty("music_title.width",650);
		music_title.setTransformationMethod(null);
		music_title.setMaxLines(getIntProperty("music_title.lines",2));
		// LrcView
		Log.d(TAG,"LrcView");
		View LyricShow = (View)liparam.view.findViewById(liparam.res.getIdentifier("LyricShow", "id", MUSIC_PACKAGE));
		LyricShow.setVisibility(View.GONE);
		// bar_right
		Log.d(TAG,"bar_right");
		LinearLayout bar_right = (LinearLayout)liparam.view.findViewById(liparam.res.getIdentifier("bar_right", "id", MUSIC_PACKAGE));
		((RelativeLayout.LayoutParams)bar_right.getLayoutParams()).topMargin = getIntProperty("bar_right.top",60);
		// content_list
		Log.d(TAG,"content_list");
		RelativeLayout content_list = (RelativeLayout)liparam.view.findViewById(liparam.res.getIdentifier("content_list", "id", MUSIC_PACKAGE));
		content_list.getLayoutParams().width = getIntProperty("content_list.width",410);
		content_list.setBackgroundColor(Color.BLACK);
		// btn_style
		Log.d(TAG,"btn_style");
		Button btn_style = (Button)liparam.view.findViewById(liparam.res.getIdentifier("btn_style", "id", MUSIC_PACKAGE));
		((LinearLayout.LayoutParams)btn_style.getLayoutParams()).width = getIntProperty("btn_Style.width",105);
		// btn_del
		Log.d(TAG,"btn_del");
		ImageButton btn_del = (ImageButton)liparam.view.findViewById(liparam.res.getIdentifier("btn_del", "id", MUSIC_PACKAGE));
		((LinearLayout.LayoutParams)btn_del.getLayoutParams()).width = getIntProperty("btn_del.width",85);
		// ((LinearLayout.LayoutParams)btn_del.getLayoutParams()).leftMargin = getIntProperty("btn_del.left",5);
		// itemlist
	    Log.d(TAG,"itemlist");
		ListView itemlist = (ListView)liparam.view.findViewById(liparam.res.getIdentifier("itemlist", "id", MUSIC_PACKAGE));
	    itemlist.setBackgroundColor(Color.BLACK);
		// icon_music
		Log.d(TAG,"musicico");
		ImageView musicico = (ImageView)liparam.view.findViewById(liparam.res.getIdentifier("musicico", "id", MUSIC_PACKAGE));
		musicico.setOnClickListener(new ImageView.OnClickListener() {
          public void onClick(View view)
          {
            int shift = getIntProperty("return_back",20);
            Log.d(TAG,"musicActivity, shift="+shift);
            Activity musicActivity = (Activity)Main.getMusicActivity();
            Log.d(TAG,"mServer");
            Object mServer = XposedHelpers.getObjectField(musicActivity,"mServer");
            Log.d(TAG,"mPlayer");
            MediaPlayer mPlayer = (MediaPlayer)XposedHelpers.getObjectField(mServer,"mPlayer");
            if ((mPlayer.isPlaying()) && (shift > 0))
            {
              Log.d(TAG,"playing");
              // в режиме проигрывания
              int position = XposedHelpers.getIntField(Main.getMusicActivity(),"currentPosition");
              int duration = XposedHelpers.getIntField(Main.getMusicActivity(),"currentDuration");
              //
              Log.d(TAG,"position="+position);
              Log.d(TAG,"duration="+duration);
              // перемотка назад на несколько секунд назад
              if (position > shift*1000)
              	 position = position - shift*1000;
              else
                position = 0;
              if (duration > 0)
              {
                Log.d(TAG,"set position to "+position);
              	XposedHelpers.callMethod(musicActivity, "setPosition", new Object[] {Integer.valueOf(100*position/duration)});
              	Log.d(TAG,"position changed OK");
              	// TODO: взять из res/xml
              	Toast.makeText(musicActivity, "возврат на "+shift+" секунд", Toast.LENGTH_SHORT).show();
              }
            }
          }
        });
	    //
	    // картинка вместо visualizer
	    //
	    Log.d(TAG,"visualizer");
	    // visualizer
	    visualizer = (View)liparam.view.findViewById(liparam.res.getIdentifier("pingpu", "id", MUSIC_PACKAGE));
	    // картинка
	    Log.d(TAG,"cover");
	    cover = new ImageView(layout.getContext());
	    RelativeLayout.LayoutParams paramsCover = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
	    paramsCover.addRule(RelativeLayout.BELOW, musicnumberId);
	    paramsCover.leftMargin = getIntProperty("cover.left",50);
	    paramsCover.width = getIntProperty("cover.width",200);
	    paramsCover.height = getIntProperty("cover.height",200);
	    cover.setLayoutParams(paramsCover);
	    cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
	    cover.setVisibility(View.INVISIBLE);
	    layout.addView(cover);
	    //
	    Log.d(TAG,"handleLayoutInflated end");
	  }
    });
    Log.d(TAG,"resource hook OK");
  }
  
}
