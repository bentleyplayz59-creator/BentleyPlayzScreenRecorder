package com.bentleyplayz.screenrecorder
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs
class FloatingBallService:Service(){
 private lateinit var wm:WindowManager;private var ball:TextView?=null;private lateinit var lp:WindowManager.LayoutParams
 override fun onCreate(){
  super.onCreate();if(!Settings.canDrawOverlays(this)){stopSelf();return}
  wm=getSystemService(WINDOW_SERVICE) as WindowManager;val den=resources.displayMetrics.density;val size=(58*den).toInt()
  lp=WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=(18*den).toInt();y=(190*den).toInt()}
  val v=TextView(this).apply{text="BP";textSize=17f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setBackgroundResource(android.R.drawable.btn_default);background.setTint(Color.rgb(78,111,255));elevation=12*den}
  var dx=0f;var dy=0f;var ox=0;var oy=0;var dragged=false
  v.setOnTouchListener{view,e->when(e.action){
   MotionEvent.ACTION_DOWN->{dx=e.rawX;dy=e.rawY;ox=lp.x;oy=lp.y;dragged=false;true}
   MotionEvent.ACTION_MOVE->{val x=(e.rawX-dx).toInt();val y=(e.rawY-dy).toInt();if(abs(x)>5||abs(y)>5)dragged=true;lp.x=ox+x;lp.y=oy+y;wm.updateViewLayout(view,lp);true}
   MotionEvent.ACTION_UP->{if(!dragged)startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP));true}
   else->false
  }}
  ball=v;wm.addView(v,lp)
 }
 override fun onDestroy(){ball?.let{if(it.isAttachedToWindow)wm.removeView(it)};ball=null;super.onDestroy()}
 override fun onBind(intent:Intent?):IBinder?=null
}
