package com.renhui.opengles20study;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.renhui.opengles20study.camera.preview.PreviewCameraActivity;
import com.renhui.opengles20study.camera.preview.PreviewSurface;
import com.renhui.opengles20study.camera.takepic.TakePictureActivity;
import com.renhui.opengles20study.glsv.ImageGLSurfaceView;
import com.renhui.opengles20study.glsv.TriangleGLSurfaceView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_CAMERA_PERMISSION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions( new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

//         setContentView(new TriangleGLSurfaceView(this)); // 绘制三角形

        // setContentView(new SquareGLSurfaceView(this));  // 绘制正方形

        // setContentView(new OvalGLSurfaceView(this)); // 绘制圆形

        // setContentView(new PaintPointGLSurfaceView(this)); // 手绘点

        // setContentView(new RotateTriangleGLSurfaceView(this)); // 旋转三角形

//        try {
//            setContentView(new ImageGLSurfaceView(this)); // 加载图片
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        startActivity(new Intent(this, Main2Activity.class));

//                 startActivity(new Intent(this, PreviewSurface.class));  // 预览摄像头

//         startActivity(new Intent(this, PreviewCameraActivity.class));  // OpenGL预览摄像头

//        startActivity(new Intent(this, TakePictureActivity.class));  // OpenGL 拍照


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("yanghua", "onRequestPermissionsResult granted");
                }
            }
            break;
        }
    }
}
