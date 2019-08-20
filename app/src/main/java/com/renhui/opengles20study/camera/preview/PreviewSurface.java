package com.renhui.opengles20study.camera.preview;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import com.renhui.opengles20study.R;


import java.io.IOException;

public class PreviewSurface extends AppCompatActivity implements SurfaceHolder.Callback, TextureView.SurfaceTextureListener {
    SurfaceView surfaceView;
    TextureView textureView;
    Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        textureView = findViewById(R.id.textureView);
        surfaceView.getHolder().addCallback(this);
        textureView.setSurfaceTextureListener(this);
        // 打开摄像头并将展示方向旋转90度
        camera = Camera.open();
        camera.setDisplayOrientation(90);

    }

    //------ Surface 预览 -------
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        camera.release();
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            camera.setPreviewTexture(surface);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        camera.release();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
