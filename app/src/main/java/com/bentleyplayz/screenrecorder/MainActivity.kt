package com.bentleyplayz.screenrecorder

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
 private val bg=Color.rgb(12,15,27); private val panel=Color.rgb(26,33,55); private val blue=Color.rgb(82,116,255)
 private lateinit var status:TextView; private lateinit var library:LinearLayout
 private val captureRequest=4101; private val micRequest=4102; private var pendingStart=false; private var pendingOverlay=false
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);window.statusBarColor=bg;window.navigationBarColor=bg;render();refreshLibrary() }
 private fun render() {
  val scroll=ScrollView(this)
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(28),dp(22),dp(30));setBackgroundColor(bg) }
  scroll.addView(root)
  root.addView(label("B E N T L E Y P L A Y Z",12,Color.rgb(135,160,255)))
  root.addView(label("Screen Recorder",30,Color.WHITE,dp(8)))
  root.addView(label("A complete capture studio for Android",14,Color.LTGRAY))
  val hero=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(20),dp(18),dp(20));setBackgroundColor(panel) }
  hero.addView(label("RECORDING STUDIO",13,Color.rgb(142,166,255)))
  hero.addView(label("Screen + internal audio + microphone",17,Color.WHITE,dp(12)))
  hero.addView(label("Capture supported app playback and your voice together in one MP4.",13,Color.LTGRAY,dp(8)))
  root.addView(hero,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(24)})
  status=label("READY",13,Color.rgb(110,220,170),dp(20));root.addView(status)
  root.addView(label("RECORDING CONTROLS",12,Color.rgb(142,166,255),dp(20)))
  root.addView(button("START RECORDING"){startRecording()})
  root.addView(button("PAUSE / RESUME"){sendAction(RecorderService.ACTION_TOGGLE_PAUSE)})
  root.addView(button("STOP RECORDING"){sendAction(RecorderService.ACTION_STOP)})
  root.addView(button("ENABLE FLOATING BALL"){enableBall()})
  root.addView(button("HIDE FLOATING BALL"){stopService(Intent(this,FloatingBallService::class.java));toast("Floating control hidden")})
  root.addView(label("SETTINGS",12,Color.rgb(142,166,255),dp(22)))
  root.addView(button("VIDEO QUALITY"){qualityDialog()})
  root.addView(button("AUDIO SOURCES"){AlertDialog.Builder(this).setTitle("Audio capture").setMessage("Microphone records your voice. Internal audio captures supported app playback on Android 10 and later. Some apps, calls and protected content block playback capture. If internal capture is blocked, microphone recording can still work.").setPositiveButton("OK",null).show()})
  root.addView(label("MY RECORDINGS",12,Color.rgb(142,166,255),dp(24)))
  library=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(12),dp(12),dp(12));setBackgroundColor(panel)}
  root.addView(library,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
  setContentView(scroll)
 }
 private fun label(s:String,size:Int,color:Int,top:Int=0)=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(color);if(top>0)setPadding(0,top,0,0)}
 private fun button(s:String,action:()->Unit)=Button(this).apply{text=s;isAllCaps=false;textSize=14f;setTextColor(Color.WHITE);setBackgroundColor(blue);setOnClickListener{action()};layoutParams=LinearLayout.LayoutParams(-1,dp(54)).apply{topMargin=dp(9)}}
 private fun startRecording(){
  val needed=mutableListOf<String>()
  if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)needed.add(android.Manifest.permission.RECORD_AUDIO)
  if(needed.isNotEmpty()){pendingStart=true;requestPermissions(needed.toTypedArray(),micRequest);return}
  val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
  startActivityForResult(mgr.createScreenCaptureIntent(),captureRequest)
 }
 @Deprecated("MediaProjection consent callback")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==captureRequest){if(resultCode==RESULT_OK&&data!=null){val i=Intent(this,RecorderService::class.java).setAction(RecorderService.ACTION_START).putExtra(RecorderService.EXTRA_RESULT_CODE,resultCode).putExtra(RecorderService.EXTRA_DATA,data);if(Build.VERSION.SDK_INT>=26)startForegroundService(i)else startService(i);status.text="RECORDING";status.setTextColor(Color.GREEN)}else toast("Screen capture permission was not granted.")}}
 override fun onRequestPermissionsResult(requestCode:Int,p:Array<out String>,r:IntArray){super.onRequestPermissionsResult(requestCode,p,r);if(requestCode==micRequest&&pendingStart){pendingStart=false;if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED)startRecording()else toast("Microphone permission is required.")}}
 private fun sendAction(a:String){startService(Intent(this,RecorderService::class.java).setAction(a))}
 private fun enableBall(){if(Settings.canDrawOverlays(this)){if(Build.VERSION.SDK_INT>=26)startForegroundService(Intent(this,FloatingBallService::class.java))else startService(Intent(this,FloatingBallService::class.java));toast("Floating BP control enabled")}else{pendingOverlay=true;startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))}}
 private fun qualityDialog(){val names=arrayOf("720p · 30 FPS","1080p · 30 FPS","1080p · 60 FPS");val prefs=getSharedPreferences("recorder",MODE_PRIVATE);AlertDialog.Builder(this).setTitle("Video quality").setSingleChoiceItems(names,prefs.getInt("quality",1)){d,n->prefs.edit().putInt("quality",n).apply();d.dismiss();toast("Quality saved for the next recording.")}.setNegativeButton("Cancel",null).show()}
 private fun refreshLibrary(){
  if(!::library.isInitialized)return
  library.removeAllViews();val dir=File(getExternalFilesDir(null),"Recordings");val files=dir.listFiles()?.filter{it.extension.equals("mp4",true)}?.sortedByDescending{it.lastModified()}?:emptyList()
  if(files.isEmpty()){library.addView(label("No recordings yet. MP4 videos will appear here.",13,Color.LTGRAY));return}
  files.forEach{f->
   val item=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,dp(8),0,dp(12))}
   item.addView(label(f.name,14,Color.WHITE))
   val date=SimpleDateFormat("MMM d, yyyy · h:mm a",Locale.getDefault()).format(Date(f.lastModified()))
   item.addView(label("$date · ${f.length()/1_000_000} MB",12,Color.LTGRAY,dp(3)))
   val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
   fun fileUri()=FileProvider.getUriForFile(this@MainActivity,"${packageName}.provider",f)
   row.addView(Button(this).apply{text="PLAY";isAllCaps=false;setOnClickListener{startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(fileUri(),"video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Play recording"))}})
   row.addView(Button(this).apply{text="SHARE";isAllCaps=false;setOnClickListener{startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM,fileUri()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Share recording"))}})
   row.addView(Button(this).apply{text="DELETE";isAllCaps=false;setOnClickListener{AlertDialog.Builder(this@MainActivity).setMessage("Delete this recording?").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->f.delete();refreshLibrary()}.show()}})
   item.addView(row);library.addView(item)
  }
 }
 override fun onResume(){super.onResume()
  if(pendingOverlay&&Settings.canDrawOverlays(this)){pendingOverlay=false;if(Build.VERSION.SDK_INT>=26)startForegroundService(Intent(this,FloatingBallService::class.java))else startService(Intent(this,FloatingBallService::class.java));toast("Floating BP control enabled")}
  if(::status.isInitialized){val prefs=getSharedPreferences("recorder",MODE_PRIVATE);val recording=prefs.getBoolean("is_recording",false);val paused=prefs.getBoolean("is_paused",false);status.text=if(!recording)"READY"else if(paused)"PAUSED"else"RECORDING";status.setTextColor(if(recording)Color.rgb(255,190,90)else Color.rgb(110,220,170))}
  if(::library.isInitialized)refreshLibrary()
}
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
