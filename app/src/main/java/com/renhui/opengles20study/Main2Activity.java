package com.renhui.opengles20study;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Main2Activity extends AppCompatActivity {

    private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath();

    private MediaExtractor mMediaExtractor;
    private MediaMuxer mMediaMuxer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取权限
        int checkWriteExternalPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int checkReadExternalPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (checkWriteExternalPermission != PackageManager.PERMISSION_GRANTED ||
                checkReadExternalPermission != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }

        setContentView(R.layout.activity_main);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    process();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean process() throws IOException {
        mMediaExtractor = new MediaExtractor();
        Log.d("yanghua",SDCARD_PATH);//没有这个目录，更改一下/sdcard/BaiduNetdisk/视频教学/01崩溃优化：关于“崩溃”那些事儿.mp4
        mMediaExtractor.setDataSource("/sdcard/BaiduNetdisk/视频教学/01崩溃优化：关于“崩溃”那些事儿.mp4");

        int mVideoTrackIndex = -1;
        int framerate = 0;
        for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
            MediaFormat format = mMediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (!mime.startsWith("video/")) {
                continue;
            }
            framerate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
            mMediaExtractor.selectTrack(i);
            mMediaMuxer = new MediaMuxer(getFilesDir() + "/ouput.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mVideoTrackIndex = mMediaMuxer.addTrack(format);
            mMediaMuxer.start();
        }

        if (mMediaMuxer == null) {
            return false;
        }

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        int sampleSize = 0;
        while ((sampleSize = mMediaExtractor.readSampleData(buffer, 0)) > 0) {

            info.offset = 0;
            info.size = sampleSize;
            info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
            info.presentationTimeUs += 1000 * 1000 / framerate;
            mMediaMuxer.writeSampleData(mVideoTrackIndex, buffer, info);
            mMediaExtractor.advance();
        }

        mMediaExtractor.release();

        mMediaMuxer.stop();
        mMediaMuxer.release();

        return true;
    }
}
