package com.example.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.Camera.Parameters;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.CookieHandler;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements SetDialog.DialogListener {


    public static final String LOG_TAG = "myLogs";
    public static int frame = 1;

    CameraService[] myCameras = null;

    private CameraManager mCameraManager    = null;
    private final int CAMERA1   = 0;
    private final int CAMERA2   = 1;
    private CameraCharacteristics characteristics = null;
    private int images = 1;
    private int fps = 1;
    private int focusRange = 100;
    private int exposureTime = 100000;
    private Button settings;

    private Range ExposureTimes;

    private Button mButtonOpenCamera1 = null;
    private Button mButtonOpenCamera2 = null;
    private Button mButtonToMakeShot = null;
    private ListView mListView = null;

    private TextureView mImageView = null;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler = null;

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void openDialog() {
        SetDialog dialog = new SetDialog();

        Bundle bundle = new Bundle();
        bundle.putString("images", String.valueOf(images));
        bundle.putString("fps", String.valueOf(fps));
        bundle.putString("focusRange", String.valueOf(focusRange));
        bundle.putString("exposureTime", String.valueOf(exposureTime));
        dialog.setArguments(bundle);

        dialog.show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void applyParameters(String mimages, String mfps, String mfocusRange, String mexposureTime) {
        images = Integer.parseInt(mimages);
        fps = Integer.parseInt(mfps);
        focusRange = Integer.parseInt(mfocusRange);
        exposureTime = Integer.parseInt(mexposureTime);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                int [] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                Range<Long> exposure_time_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                if( exposure_time_range != null ) {
                    Long min_exposure = exposure_time_range.getLower();
                    Long max_exposure = exposure_time_range.getUpper();
                    Log.i(LOG_TAG, "min_exposure " + min_exposure);
                    Log.i(LOG_TAG, "max_exposure" + max_exposure);
                }

//                float minFocalDist = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                Log.i(LOG_TAG, "cameraId - " + cameraId);
                if (facing != null && facing.equals(CameraCharacteristics.LENS_FACING_FRONT)) {

                }
                // Do something with the characteristics
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        settings = (Button) findViewById(R.id.settings);

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog();
            }
        });



        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                ||
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        )
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }

        

        mButtonOpenCamera1 =  findViewById(R.id.button1);
        mButtonOpenCamera2 =  findViewById(R.id.button2);
        mButtonToMakeShot =findViewById(R.id.button3);
        mImageView = findViewById(R.id.textureView);
        settings.setEnabled(false);
        mButtonToMakeShot.setEnabled(false);
        mButtonOpenCamera1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myCameras[CAMERA2].isOpen()) {
                    myCameras[CAMERA2].closeCamera();

                }
                if (myCameras[CAMERA1] != null) {
                    if (!myCameras[CAMERA1].isOpen()) {
                        myCameras[CAMERA1].openCamera();
                        settings.setEnabled(true);
                        mButtonToMakeShot.setEnabled(true);
                    }
                }
            }
        });

        mButtonOpenCamera2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myCameras[CAMERA1].isOpen()) {
                    myCameras[CAMERA1].closeCamera();

                }
                if (myCameras[CAMERA2] != null) {
                    if (!myCameras[CAMERA2].isOpen())  {

                        myCameras[CAMERA2].openCamera();
                        settings.setEnabled(true);
                        mButtonToMakeShot.setEnabled(true);
                    }
                }
            }
        });

        final CountDownTimer[] a = new CountDownTimer[1];

        mButtonToMakeShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mButtonToMakeShot.getText().toString().equals("Capture")) {
                    mButtonToMakeShot.setText("Pressed");
                    mButtonOpenCamera1.setEnabled(false);
                    mButtonOpenCamera2.setEnabled(false);

                    settings.setEnabled(false);

                    a[0] = new CountDownTimer(images * fps * 1000,fps * 1000){

                        @Override
                        public void onFinish() {
//                            mCamera.takePicture(null, null, mPicture);
                            mButtonToMakeShot.setText("Capture");

                            mButtonOpenCamera1.setEnabled(true);
                            mButtonOpenCamera2.setEnabled(true);
                            settings.setEnabled(true);
                        }

                        @Override
                        public void onTick(long millisUntilFinished) {
                            MainActivity.frame += 1;
                            Log.i(LOG_TAG, "countdown");
                            if (myCameras[CAMERA1].isOpen())
                                myCameras[CAMERA1].makePhoto();
                            if (myCameras[CAMERA2].isOpen())
                                myCameras[CAMERA2].makePhoto();
                        }

                    }.start();


                }
                else{
                    mButtonToMakeShot.setText("Capture");
                    a[0].cancel();
                    mButtonOpenCamera1.setEnabled(true);
                    mButtonOpenCamera2.setEnabled(true);
                    settings.setEnabled(true);
                }
            }
        });


        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{


            myCameras = new CameraService[mCameraManager.getCameraIdList().length];



            for (String cameraID : mCameraManager.getCameraIdList()) {
                Log.i(LOG_TAG, "cameraID: "+cameraID);
                int id = Integer.parseInt(cameraID);


                myCameras[id] = new CameraService(mCameraManager,cameraID);

            }

        }
        catch(CameraAccessException e){
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }


    }


    public class CameraService {

        private String mCameraID;
        private CameraDevice mCameraDevice = null;
        private CameraCaptureSession mCaptureSession;
        private ImageReader mImageReader;


        public CameraService(CameraManager cameraManager, String cameraID) {

            mCameraManager = cameraManager;
            mCameraID = cameraID;

        }

        public void makePhoto (){

            try {
                final CaptureRequest.Builder captureBuilder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                Log.i(LOG_TAG, "auto-focus off");

                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 100f/focusRange);
                captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) exposureTime);
                captureBuilder.addTarget(mImageReader.getSurface());
                CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                   @NonNull CaptureRequest request,
                                                   @NonNull TotalCaptureResult result) {


                    }
                };

                mCaptureSession.stopRepeating();
                mCaptureSession.abortCaptures();
                mCaptureSession.capture(captureBuilder.build(), CaptureCallback, mBackgroundHandler);
                createCameraPreviewSession();
            }
            catch (CameraAccessException e) {
                e.printStackTrace();


            }

        }

        private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
                = new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.i(LOG_TAG, "test" + MainActivity.frame +".jpg");
                Long tsLong = System.currentTimeMillis()/1000;
                String ts = tsLong.toString();

                File mFile2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "test" + ts +".jpg");
                mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile2));

            }

        };


        private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(CameraDevice camera) {
                mCameraDevice = camera;
                Log.i(LOG_TAG, "Open camera  with id:"+mCameraDevice.getId());

                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                mCameraDevice.close();

                Log.i(LOG_TAG, "disconnect camera  with id:"+mCameraDevice.getId());
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.i(LOG_TAG, "error! camera id:"+camera.getId()+" error:"+error);
            }
        };


        private void createCameraPreviewSession() {
            mImageReader = ImageReader.newInstance(1920,1080, ImageFormat.JPEG,10);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);

            SurfaceTexture texture = mImageView.getSurfaceTexture();

            texture.setDefaultBufferSize(1920,1080);
            Surface surface = new Surface(texture);

            try {
                Log.i(LOG_TAG, "capturing");
                final CaptureRequest.Builder builder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 100f/focusRange);
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) exposureTime);

//                CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
//                try {
//                    for (String cameraId : cameraManager.getCameraIdList()) {
//                        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
//                        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
//                        if (facing != null && facing.equals(CameraCharacteristics.LENS_FACING_FRONT)) {
//
//                        }
//                        // Do something with the characteristics
//                    }
//                } catch (CameraAccessException e) {
//                    e.printStackTrace();
//                }

                builder.addTarget(surface);




                mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageReader.getSurface()),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mCaptureSession = session;
                                try {
                                    mCaptureSession.setRepeatingRequest(builder.build(),null,mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) { }}, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }




        public boolean isOpen() {
            if (mCameraDevice == null) {
                return false;
            } else {
                return true;
            }
        }

        public void openCamera() {
            try {

                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {


                    mCameraManager.openCamera(mCameraID,mCameraCallback,mBackgroundHandler);

                }



            } catch (CameraAccessException e) {
                Log.i(LOG_TAG,e.getMessage());

            }
        }

        public void closeCamera() {

            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }



    }


    @Override
    public void onPause() {
        if(myCameras[CAMERA1].isOpen()){myCameras[CAMERA1].closeCamera();}
        if(myCameras[CAMERA2].isOpen()){myCameras[CAMERA2].closeCamera();}
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();


    }


    private static class ImageSaver implements Runnable {


        private final Image mImage;
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}