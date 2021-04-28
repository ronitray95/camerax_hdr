package com.example.cameraxdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import net.alhazmy13.imagefilter.ImageFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final Executor executor = Executors.newSingleThreadExecutor();
    private PreviewView previewView;
    private Button buttonHDR;
    private boolean useFallBack;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
        else
            startCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];
                if (permission.equals(Manifest.permission.CAMERA) && grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Camera permission required", Toast.LENGTH_SHORT).show();
                    finish();
                }
                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Storage permission required", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            startCamera();
        }
    }

    private void startCamera() {
        Size z = getBackCameraResolutionInMp();
        Log.e("Using size", String.valueOf(z));
        buttonHDR = findViewById(R.id.buttonHDR);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider, z);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider, Size z) {
        Preview preview = new Preview.Builder().build();
        useFallBack = false;
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        //ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

        ImageCapture.Builder builder = new ImageCapture.Builder();
        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation()).setTargetResolution(z)
                .build();

        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            Toast.makeText(MainActivity.this, "HDR is available and enabled", Toast.LENGTH_SHORT).show();
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        } else {
            useFallBack = true;
            //Toast.makeText(MainActivity.this, "HDR is not available", Toast.LENGTH_SHORT).show();
        }

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview, imageCapture);

        buttonHDR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation.start(MainActivity.this);
                //cameraProvider.unbindAll();
                SimpleDateFormat mDateFormat = new SimpleDateFormat("HHmmss", Locale.getDefault());
                String name = mDateFormat.format(new Date()) + ".jpg";
                String dest = getBatchDirectoryName();
                File file = new File(dest, name);

                ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
                imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Image " + name + " saved at " + dest, Toast.LENGTH_SHORT).show();
                                if (useFallBack) {
                                    Log.e("Fallback", "");
                                    Bitmap res = ImageFilter.applyFilter(BitmapFactory.decodeFile(file.getAbsolutePath()), ImageFilter.Filter.HDR);
                                    Log.e("Size", String.valueOf(res.getHeight()));
                                    Matrix matrix = new Matrix();
                                    matrix.postRotate(90);
                                    Bitmap newb = Bitmap.createBitmap(res, 0, 0, res.getWidth(), res.getHeight(), matrix, true);

                                    try (FileOutputStream out = new FileOutputStream(dest + "/hdr_" + name)) {
                                        newb.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                        Animation.stop(MainActivity.this);
                                    } catch (IOException e) {
                                        Log.e("Bitmap save exception", e.getMessage());
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException error) {
                        error.printStackTrace();
                    }
                });
            }
        });

    }

    public String getBatchDirectoryName() {
        String app_folder_path;
        app_folder_path = Environment.getExternalStorageDirectory().toString() + "/images";
        File dir = new File(app_folder_path);
        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(MainActivity.this, "Error creating folder", Toast.LENGTH_SHORT).show();
        }
        return app_folder_path;
    }

    public Size getBackCameraResolutionInMp() {
        int noOfCameras = android.hardware.Camera.getNumberOfCameras();
        float maxResolution = -1;
        long pixelCount = -1;
        for (int i = 0; i < noOfCameras; i++) {
            android.hardware.Camera.CameraInfo cameraInfo = new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) {
                android.hardware.Camera camera = android.hardware.Camera.open(i);
                android.hardware.Camera.Parameters cameraParams = camera.getParameters();
                for (int j = 0; j < cameraParams.getSupportedPictureSizes().size(); j++) {
                    //Log.e("For " + j, cameraParams.getSupportedPictureSizes().get(j).width + " " + cameraParams.getSupportedPictureSizes().get(j).height);
                    long pixelCountTemp = cameraParams.getSupportedPictureSizes().get(j).width * cameraParams.getSupportedPictureSizes().get(j).height; // Just changed i to j in this loop
                    if (pixelCountTemp > pixelCount) {
                        pixelCount = pixelCountTemp;
                        maxResolution = ((float) pixelCountTemp) / (1024000.0f);
                    }
                }
                camera.release();
                return new Size(cameraParams.getSupportedPictureSizes().get(0).width, cameraParams.getSupportedPictureSizes().get(0).height);
            }
        }
        return new Size(0, 0);
    }
}