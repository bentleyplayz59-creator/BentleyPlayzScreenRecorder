package com.bentleyplayz.screenrecorder

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.media.AudioRecord
import android.media.AudioFormat
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import android.content.pm.ServiceInfo
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class FloatingBallService:Service(){
 companion object{private const val CHANNEL="bentley_floating";private const val NOTIFICATION_ID=7102}
 private lateinit var wm:WindowManager
 private var ball:TextView?=null
 private var menu:LinearLayout?=null
 private var meterView:TextView?=null
 private var audioRecord:AudioRecord?=null
 private var audioThread:Thread?=null
 private var tone:ToneGenerator?=null
 private val handler=Handler(Looper.getMainLooper())
 private lateinit var lp:WindowManager.LayoutParams
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
  when(intent?.action){
   "com.bentleyplayz.screenrecorder.HIDE_BALL"->{stopAudioTest();stopSelf();return START_NOT_STICKY}
   "com.bentleyplayz.screenrecorder.MIC_TEST_START"->{startAudioTest()}
   "com.bentleyplayz.screenrecorder.MIC_TEST_STOP"->{stopAudioTest()}
  }
  return START_STICKY
 }
 override fun onCreate(){
  super.onCreate()
  if(!Settings.canDrawOverlays(this)){stopSelf();return}
  if(Build.VERSION.SDK_INT>=26)(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CHANNEL,"Floating recorder controls",NotificationManager.IMPORTANCE_LOW))
  val hide=PendingIntent.getService(this,72,Intent(this,FloatingBallService::class.java).setAction("com.bentleyplayz.screenrecorder.HIDE_BALL"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notice=Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("BentleyPlayz floating controls").setContentText("BP quick controls are available on screen.").addAction(Notification.Action.Builder(null,"Hide",hide).build()).setOngoing(true).build()
  if(Build.VERSION.SDK_INT>=34)startForeground(NOTIFICATION_ID,notice,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)else startForeground(NOTIFICATION_ID,notice)
  wm=getSystemService(WINDOW_SERVICE) as WindowManager
  val size=dp(58)
  lp=WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=dp(18);y=dp(190)}
  val v=TextView(this).apply{text="BP";textSize=17f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setBackgroundResource(android.R.drawable.btn_default);background.setTint(Color.rgb(78,111,255));elevation=dp(12).toFloat()}
  var downX=0f;var downY=0f;var originX=0;var originY=0;var dragged=false
  v.setOnTouchListener{view,event->when(event.action){
   MotionEvent.ACTION_DOWN->{downX=event.rawX;downY=event.rawY;originX=lp.x;originY=lp.y;dragged=false;true}
   MotionEvent.ACTION_MOVE->{val dx=(event.rawX-downX).toInt();val dy=(event.rawY-downY).toInt();if(abs(dx)>5||abs(dy)>5)dragged=true;lp.x=originX+dx;lp.y=originY+dy;wm.updateViewLayout(view,lp);true}
   MotionEvent.ACTION_UP->{if(!dragged)toggleMenu();true}
   else->false
  }}
  ball=v;wm.addView(v,lp)
 }
 private fun toggleMenu(){if(menu!=null){removeMenu();return}
  val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),dp(10),dp(10),dp(10));setBackgroundColor(Color.rgb(24,31,53));elevation=dp(16).toFloat()}
  panel.addView(menuButton("START RECORDING"){requestRecordingStart()})
  panel.addView(menuButton("OPEN RECORDER"){openRecorder()})
  panel.addView(menuButton("PAUSE / RESUME"){send(RecorderService.ACTION_TOGGLE_PAUSE)})
  panel.addView(menuButton("STOP RECORDING"){send(RecorderService.ACTION_STOP)})
  panel.addView(labelSmall("AUDIO TEST"))
  val micButton=menuActionButton("START MICROPHONE TEST"){requestMicTest()}
  panel.addView(micButton)
  val meter=TextView(this).apply{text="INPUT  ▂▂▂▂▂▂▂▂";textSize=12f;setTextColor(Color.rgb(125,220,170));setPadding(dp(12),dp(5),dp(12),dp(10))}
  meterView=meter
  panel.addView(meter)
  panel.addView(menuActionButton("STOP MICROPHONE TEST"){stopAudioTest()})
  panel.addView(menuActionButton("PLAY TEST SOUND"){playTestSound()})
  panel.addView(menuButton("HIDE FLOATING BALL"){stopAudioTest();stopSelf()})
  val mp=WindowManager.LayoutParams(dp(210),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=(lp.x-dp(145)).coerceAtLeast(0);y=(lp.y+dp(66)).coerceAtLeast(0)}
  menu=panel;wm.addView(panel,mp)
 }
 private fun labelSmall(text:String)=TextView(this).apply{textSize=11f;this.text=text;setTextColor(Color.rgb(142,166,255));setPadding(dp(12),dp(12),dp(12),dp(3))}
 private fun menuActionButton(text:String,action:()->Unit)=TextView(this).apply{textSize=13f;this.text=text;gravity=Gravity.CENTER_VERTICAL;setTextColor(Color.WHITE);setPadding(dp(12),dp(12),dp(12),dp(12));setOnClickListener{action()}}
 private fun menuButton(text:String,action:()->Unit)=TextView(this).apply{textSize=13f;this.text=text;gravity=Gravity.CENTER_VERTICAL;setTextColor(Color.WHITE);setPadding(dp(12),dp(12),dp(12),dp(12));setOnClickListener{removeMenu();action()}}
 private fun removeMenu(){stopAudioTest();menu?.let{if(it.isAttachedToWindow)wm.removeView(it)};menu=null;meterView=null}
 private fun requestMicTest(){
  if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED){
   startService(Intent(this,FloatingBallService::class.java).setAction("com.bentleyplayz.screenrecorder.MIC_TEST_START"))
  }else{
   startActivity(Intent(this,MicTestPermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
  }
 }
 private fun startAudioTest(){
  if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){handler.post{meterView?.text="Microphone permission required"};return}
  try{
   stopAudioTest(false)
   if(Build.VERSION.SDK_INT>=29){
    val n=Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("BentleyPlayz audio test").setContentText("Microphone level test is running.").setOngoing(true).build()
    startForeground(NOTIFICATION_ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
   }
   val rate=48000
   val min=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
   if(min<=0)throw IllegalStateException("Microphone is unavailable")
   val rec=AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,maxOf(min,rate/5))
   if(rec.state!=AudioRecord.STATE_INITIALIZED){rec.release();throw IllegalStateException("Microphone could not start")}
   audioRecord=rec;rec.startRecording()
   audioThread=Thread{
    val data=ShortArray(1024)
    while(!Thread.currentThread().isInterrupted && audioRecord===rec){
     val count=rec.read(data,0,data.size,AudioRecord.READ_BLOCKING)
     if(count>0){
      var peak=0
      for(i in 0 until count){val v=kotlin.math.abs(data[i].toInt());if(v>peak)peak=v}
      val level=(20.0*kotlin.math.log10(maxOf(peak,1).toDouble()/32767.0)).coerceIn(-60.0,0.0)
      val bars=((level+60.0)/6.0).toInt().coerceIn(0,10)
      handler.post{meterView?.text="INPUT  "+"▮".repeat(bars)+"▯".repeat(10-bars)}
     }else if(count<0){handler.post{meterView?.text="Microphone read error: $count"};break}
    }
   }.apply{name="BPMicLevelMeter";start()}
  }catch(e:Exception){handler.post{meterView?.text="Mic test unavailable: ${e.message?: "audio error"}"}}
 }
 private fun stopAudioTest(downgrade:Boolean=true){
  val rec=audioRecord
  audioRecord=null
  audioThread?.interrupt();audioThread=null
  try{rec?.stop()}catch(_:Exception){}
  try{rec?.release()}catch(_:Exception){}
  tone?.release();tone=null
  meterView?.text="INPUT  ▂▂▂▂▂▂▂▂"
  if(downgrade && Build.VERSION.SDK_INT>=29 && ::wm.isInitialized){
   try{
    val n=Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("BentleyPlayz floating controls").setContentText("BP quick controls are available on screen.").setOngoing(true).build()
    startForeground(NOTIFICATION_ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
   }catch(_:Exception){}
  }
 }
 private fun playTestSound(){
  try{tone?.release();tone=ToneGenerator(AudioManager.STREAM_MUSIC,80);tone?.startTone(ToneGenerator.TONE_PROP_BEEP,700)}
  catch(_:Exception){handler.post{meterView?.text="Playback test unavailable"}}
 }
 private fun requestRecordingStart(){
  val launch=Intent(this,CapturePermissionActivity::class.java).apply{
   addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
  }
  startActivity(launch)
 }
 private fun openRecorder(){startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))}
 private fun send(action:String){startService(Intent(this,RecorderService::class.java).setAction(action))}
 override fun onDestroy(){stopAudioTest();removeMenu();ball?.let{if(it.isAttachedToWindow)wm.removeView(it)};ball=null;super.onDestroy()}
 override fun onBind(intent:Intent?):IBinder?=null
}
class MicTestPermissionActivity:android.app.Activity(){
 override fun onCreate(savedInstanceState:android.os.Bundle?){super.onCreate(savedInstanceState)
  if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED){startMic();return}
  requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO),701)
 }
 private fun startMic(){startService(Intent(this,FloatingBallService::class.java).setAction("com.bentleyplayz.screenrecorder.MIC_TEST_START"));finish()}
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==701&&grantResults.isNotEmpty()&&grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)startMic()else finish()}
}
