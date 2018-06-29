package org.iii.plugin.lcr;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import sdk.itri.com.itrisdklibrary.LightCodeRecognition;

import java.io.InputStream;
import java.util.Arrays;

import static android.hardware.camera2.CameraCharacteristics.LENS_FACING;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

/* AppCompatActivity */
public class ScanCamera2Activity extends Activity implements SurfaceHolder.Callback{

    private String TAG = "surfaceCamera2";
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    String cameraId="0";
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private Handler childHandler, mainHandler;
    private CameraManager mCameraManager;//摄像头管理器
    private CameraCaptureSession mCameraCaptureSession;

    private Size mPreviewSize;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;
    private Handler mBackgroundHandler;
    private String DebugString = "";
    private Vibrator vibrator;
    private TextView textView;
    private boolean DEBUG = false;

    //
    private LightCodeRecognition sdk = new LightCodeRecognition();

    //
    private String m_strClose;
    private String m_strScaning;
    private String m_strCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //
        Bundle b = getIntent().getExtras();
        if (b == null)
        {
            Log.d("ScanCamera2Activity","bundle is null");
            m_strScaning = "Scaning...";
            m_strCamera = "1";
        }
        else
        {
            m_strScaning = b.getString("key_scan", "Scaning ...");
            m_strCamera = b.getString("key_camera", "1");
        }
        //

        FrameLayout fl = new FrameLayout(this);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);

        textView = new TextView(this);
        mSurfaceView = new SurfaceView(this);

        ll.addView(textView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        fl.addView(mSurfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fl.addView(ll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        textView.setText("result text");

        ////////////////////////
        //add control UI
        createRectangularView(fl);
        createCloseButton(fl);
        createSwitchButton(fl);

        setContentView(fl);
        textView.setVisibility(DEBUG?View.VISIBLE:View.GONE);
/*
        setContentView(R.layout.activity_scan);

        textView = (TextView)findViewById(R.id.textView);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
*/
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceHolder.addCallback(this);

        vibrator = (Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image img = null;
                    img = reader.acquireLatestImage();

                    if(img != null){
                        /****************************
                         讀取LightCode
                         ************************/
                        String mainResult = "";
                        mainResult = sdk.imageRecognition(img);
                        if(!mainResult.isEmpty()){
                            vibrator.vibrate(100);
                            textView.setText("Result: "+mainResult);
                            Log.d("asdf","OOK result is "+mainResult);


                            Intent intent = new Intent();
                            Bundle bundle = new Bundle();
                            bundle.putString("data", mainResult);
                            intent.putExtras(bundle);
                            setResult(RESULT_OK, intent);
                            finish();

                        }
                        img.close();
                    }
                }

            };

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            openCamera2();
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(mCameraDevice != null){
            mCameraDevice.close();
            ScanCamera2Activity.this.mCameraDevice = null;
        }
    }

    private void openCamera2() throws CameraAccessException {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());

        ////////////
        int camera_num = LENS_FACING_BACK;
        if (m_strCamera.equals("0")) {
            camera_num = LENS_FACING_FRONT;
        }

        mCameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        String[] cameraIdList = mCameraManager.getCameraIdList();
        for(int i=0;i<cameraIdList.length;i++){
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraIdList[i]);
            if(characteristics.get(LENS_FACING) == camera_num){
                cameraId = cameraIdList[i];
                break;
            }
        }
        CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
        //set resolution
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);
        boolean FINDHD = false;
        for(int i=0;i< outputSizes.length;i++){
            if(outputSizes[i].getHeight() == 1080 && outputSizes[i].getWidth()==1920){
                mPreviewSize = outputSizes[i];
                FINDHD = true;
                break;
            }
        }
        if(!FINDHD)
            mPreviewSize = outputSizes[0];
        Log.d(TAG,"support setting previewSize "+mPreviewSize.getWidth()+"x"+mPreviewSize.getHeight());

        //Set image reader
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
        //获取摄像头管理
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //打开摄像头
            mCameraManager.openCamera(cameraId, mStateCallback, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onOpened");
            mCameraDevice = camera;
            takePreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onError");
        }

    };

    private void takePreview() {
        try {
            // 创建预览需要的CaptureRequest.Builder
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //將imageredaer 作為builder的目標
            mPreviewBuilder.addTarget(mImageReader.getSurface());
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            mPreviewBuilder.addTarget(mSurfaceHolder.getSurface());
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface(), mImageReader.getSurface()), new CameraCaptureSession.StateCallback() // ③
            {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) return;
                    // 当摄像头已经准备好时，开始显示预览
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                        /****************************
                        設定相機相關參數
                        呼叫Light Code API 設定相機參數
                         ****************************/
                        mPreviewBuilder = sdk.Camera2LCCamera(mPreviewBuilder, characteristics);
                        if(mPreviewBuilder==null){
                            Log.d("asdf","this camera not support");
                            //
                            setResult(RESULT_FIRST_USER);
                            finish();
                        }
                        else{
                            // 顯示畫面預覽
                            CaptureRequest previewRequest = mPreviewBuilder.build();
                            mCameraCaptureSession.setRepeatingRequest(previewRequest, null, childHandler);
                        }
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    //Toast.makeText(ScanCamera2Activity.this, "相機開啟失敗", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_FIRST_USER);
                    finish();
                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void switchCamera() {
        //close camera
        closeCamera();
        //reopen camera
        if (m_strCamera.equals("0")) {
            m_strCamera = "1";
        }
        else {
            m_strCamera = "0";
        }
        try {
            openCamera2();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if(mImageReader != null){
            mImageReader.close();
            mImageReader = null;
        }
        mainHandler = null;
        childHandler = null;
    }

    private void createCloseButton(FrameLayout fl) {
        // get icon stream
        InputStream ims = getResources().openRawResource(getResources().getIdentifier("close_icon","drawable",getPackageName()));
        // load image as Drawable
        Bitmap bitmap = BitmapFactory.decodeStream(ims);
        ImageButton button1 = new ImageButton(this);
        button1.setImageBitmap(bitmap);
        button1.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button1.setBackgroundDrawable(null);
        //
        Rect rc = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rc);
        ////////////////////
        //Set button position => params.setMargins(X, Y, 0, 0)
        //Set button width/height => params.width and params.height
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(20,20,0,0);
        params.width = rc.width() / 8;
        params.height = params.width;
        //
        fl.addView(button1, params);
        //
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    private void createSwitchButton(FrameLayout fl) {
        // get icon stream
        InputStream ims = getResources().openRawResource(getResources().getIdentifier("camera_switch_icon","drawable",getPackageName()));
        // load image as Drawable
        Bitmap bitmap = BitmapFactory.decodeStream(ims);
        ImageButton button1 = new ImageButton(this);
        button1.setImageBitmap(bitmap);
        button1.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button1.setBackgroundDrawable(null);
        //
        Rect rc = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rc);
        ////////////////////
        //Set button position => params.setMargins(X, Y, 0, 0)
        //Set button width/height => params.width and params.height
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.width = rc.width() / 8;
        params.height = params.width;
        params.setMargins(rc.width()-params.width-20,20,0,0);
        //
        fl.addView(button1, params);
        //
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //switch camera
                switchCamera();
            }
        });
    }

    private void createRectangularView(FrameLayout fl) {
        //create layout
        RelativeLayout rl = new RelativeLayout(this);

        //get icon stream
        InputStream rectIms = getResources().openRawResource(getResources().getIdentifier("rectangular_icon","drawable",getPackageName()));
        // load image as Drawable
        Drawable rectDraw = Drawable.createFromStream(rectIms, null);
        // set image to ImageView
        ImageView rectView = new ImageView(this);
        rectView.setImageDrawable(rectDraw);
        rectView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        //
        Rect rc = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rc);
        //
        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params2.addRule(RelativeLayout.CENTER_IN_PARENT);
        //////////////
        //Set image view width and height => params2.width params2.height
        //Poistion is center in parent
        params2.width = rc.width() * 2 / 3;
        params2.height = params2.width;
        rectView.setLayoutParams(params2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            rectView.setId(View.generateViewId());
        }
        rl.addView(rectView);
        //create result view
        TextView tv = new TextView(this);
        tv.setText(m_strScaning);
        //////////////////
        //Set scaning text color and size
        tv.setTextSize(30.0f);
        tv.setTextColor(Color.WHITE);
        //
        RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params3.addRule(RelativeLayout.BELOW, rectView.getId());
        params3.addRule(RelativeLayout.CENTER_HORIZONTAL);
        tv.setLayoutParams(params3);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            tv.setId(View.generateViewId());
        }
        rl.addView(tv);
        //blink animation
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(500); //You can manage the time of the blink with this parameter
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        tv.startAnimation(anim);

        //
        fl.addView(rl, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }
}
