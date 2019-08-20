package com.renhui.opengles20study.camera.takepic;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.renhui.opengles20study.camera.filter.BaseFilter;
import com.renhui.opengles20study.camera.filter.GroupFilter;
import com.renhui.opengles20study.camera.filter.NoFilter;
import com.renhui.opengles20study.camera.filter.TextureFilter;
import com.renhui.opengles20study.camera.takepic.utils.EasyGlUtils;
import com.renhui.opengles20study.camera.takepic.utils.MatrixUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

/**
 * Description: 借助GLSurfaceView创建的GL环境，做渲染工作。不将内容渲染到GLSurfaceView
 * 的Surface上，而是将内容绘制到外部提供的Surface、SurfaceHolder或者SurfaceTexture上。
 */
public class TextureController implements GLSurfaceView.Renderer {

    private Object surface;

    private GLView mGLView;
    private Context mContext;

    private Renderer mRenderer;                                 //用户附加的Renderer或用来监听Renderer
    private TextureFilter mEffectFilter;                        //特效处理的Filter
    private GroupFilter mGroupFilter;                           //中间特效
    private BaseFilter mShowFilter;                                //用来渲染输出的Filter
    private Point mDataSize;                                    //数据的大小
    private Point mWindowSize;                                  //输出视图的大小
    private AtomicBoolean isParamSet = new AtomicBoolean(false);
    private float[] SM = new float[16];                           //用于绘制到屏幕上的变换矩阵
    private int mShowType = MatrixUtils.TYPE_CENTERCROP;          //输出到屏幕上的方式
    private int mDirectionFlag = -1;                               //AiyaFilter方向flag

    private float[] callbackOM = new float[16];                   //用于绘制回调缩放的矩阵

    //创建离屏buffer，用于最后导出数据
    private int[] mExportFrame = new int[1];
    private int[] mExportTexture = new int[1];

    private boolean isShoot = false;                              //一次拍摄flag
    private ByteBuffer[] outPutBuffer = new ByteBuffer[3];      //用于存储回调数据的buffer
    private FrameCallback mFrameCallback;                       //回调
    private int frameCallbackWidth, frameCallbackHeight;        //回调数据的宽高
    private int indexOutput = 0;                                  //回调数据使用的buffer索引

    public TextureController(Context context) {
        this.mContext = context;
        init();
    }

    public void surfaceCreated(Object nativeWindow) {
        this.surface = nativeWindow;
        mGLView.surfaceCreated(null);
    }

    public void surfaceChanged(int width, int height) {
        this.mWindowSize.x = width;
        this.mWindowSize.y = height;
        mGLView.surfaceChanged(null, 0, width, height);
    }

    public void surfaceDestroyed() {
        mGLView.surfaceDestroyed(null);
    }

    private void init() {

        mGLView = new GLView(mContext);

        //避免GLView的attachToWindow和detachFromWindow崩溃
        ViewGroup v = new ViewGroup(mContext) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {}
        };
        v.addView(mGLView);
        v.setVisibility(View.GONE);

        mEffectFilter = new TextureFilter();
        mShowFilter = new NoFilter();
        mGroupFilter = new GroupFilter();

        //设置默认的DateSize
        mDataSize = new Point(1080, 1920);
        mWindowSize = new Point(1080, 1920);
    }

    //在Surface创建前，应该被调用
    public void setDataSize(int width, int height) {
        mDataSize.x = width;
        mDataSize.y = height;
    }

    public SurfaceTexture getTexture() {
        return mEffectFilter.getTexture();
    }

    public void setImageDirection(int flag) {
        this.mDirectionFlag = flag;
    }

    public void setRenderer(Renderer renderer) {
        mRenderer = renderer;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mEffectFilter.create();
        mShowFilter.create();
        mGroupFilter.create();
        if (!isParamSet.get()) {
            if (mRenderer != null) {
                mRenderer.onSurfaceCreated(gl, config);
            }
            sdkParamSet();
        }
        calculateCallbackOM();
        mEffectFilter.setFlag(mDirectionFlag);

        deleteFrameBuffer();
        GLES20.glGenFramebuffers(1, mExportFrame, 0);
        EasyGlUtils.genTexturesWithParameter(1, mExportTexture, 0, GLES20.GL_RGBA, mDataSize.x, mDataSize.y);
    }

    private void deleteFrameBuffer() {
        GLES20.glDeleteFramebuffers(1, mExportFrame, 0);
        GLES20.glDeleteTextures(1, mExportTexture, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        MatrixUtils.getMatrix(SM, mShowType, mDataSize.x, mDataSize.y, width, height);
        mShowFilter.setSize(width, height);
        mShowFilter.setMatrix(SM);
        mEffectFilter.setSize(mDataSize.x, mDataSize.y);
        mGroupFilter.setSize(mDataSize.x, mDataSize.y);
        mShowFilter.setSize(mDataSize.x, mDataSize.y);
        if (mRenderer != null) {
            mRenderer.onSurfaceChanged(gl, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (isParamSet.get()) {
            mEffectFilter.draw();  // 特效的过滤器先画
            mGroupFilter.setTextureId(mEffectFilter.getOutputTexture()); // 其他过滤器拿到当前的SurfaceTexture再画
            mGroupFilter.draw();
            //显示传入的texture上，一般是显示在屏幕上
            GLES20.glViewport(0, 0, mWindowSize.x, mWindowSize.y);
            mShowFilter.setMatrix(SM);
            mShowFilter.setTextureId(mGroupFilter.getOutputTexture());
            mShowFilter.draw();  // 其他过滤器拿到当前的SurfaceTexture再画
            if (mRenderer != null) {
                mRenderer.onDrawFrame(gl);
            }
            callbackIfNeeded();
        }
    }

    /**
     * 增加滤镜
     *
     * @param filter 滤镜
     */
    public void addFilter(BaseFilter filter) {
        mGroupFilter.addFilter(filter);
    }


    public void takePhoto() {
        isShoot = true;
    }

    public void setFrameCallback(int width, int height, FrameCallback frameCallback) {
        this.frameCallbackWidth = width;
        this.frameCallbackHeight = height;
        if (frameCallbackWidth > 0 && frameCallbackHeight > 0) {
            if (outPutBuffer != null) {
                outPutBuffer = new ByteBuffer[3];
            }
            calculateCallbackOM();
            this.mFrameCallback = frameCallback;
        } else {
            this.mFrameCallback = null;
        }
    }

    private void calculateCallbackOM() {
        if (frameCallbackHeight > 0 && frameCallbackWidth > 0 && mDataSize.x > 0 && mDataSize.y > 0) {
            //计算输出的变换矩阵
            MatrixUtils.getMatrix(callbackOM, MatrixUtils.TYPE_CENTERCROP, mDataSize.x, mDataSize.y, frameCallbackWidth, frameCallbackHeight);
            MatrixUtils.flip(callbackOM, false, true);
        }
    }

    private void sdkParamSet() {
        if (!isParamSet.get() && mDataSize.x > 0 && mDataSize.y > 0) {
            isParamSet.set(true);
        }
    }

    //需要回调，则缩放图片到指定大小，读取数据并回调
    private void callbackIfNeeded() {
        if (mFrameCallback != null && (isShoot)) {
            indexOutput = indexOutput++ >= 2 ? 0 : indexOutput;
            if (outPutBuffer[indexOutput] == null) {
                outPutBuffer[indexOutput] = ByteBuffer.allocate(frameCallbackWidth * frameCallbackHeight * 4);
            }
            GLES20.glViewport(0, 0, frameCallbackWidth, frameCallbackHeight);
            EasyGlUtils.bindFrameTexture(mExportFrame[0], mExportTexture[0]);
            mShowFilter.setMatrix(callbackOM);
            mShowFilter.draw();
            frameCallback();
            isShoot = false;
            EasyGlUtils.unBindFrameBuffer();
            mShowFilter.setMatrix(SM);
        }
    }

    //读取数据并回调
    private void frameCallback() {
        GLES20.glReadPixels(0, 0, frameCallbackWidth, frameCallbackHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, outPutBuffer[indexOutput]);
        mFrameCallback.onFrame(outPutBuffer[indexOutput].array(), 0);
    }


    public void destroy() {
        if (mRenderer != null) {
            mRenderer.onDestroy();
        }
        mGLView.surfaceDestroyed(null);
        mGLView.detachedFromWindow();
    }

    public void requestRender() {
        mGLView.requestRender();
    }

    public void onPause() {
        mGLView.onPause();
    }

    public void onResume() {
        mGLView.onResume();
    }

    /**
     * 自定义GLSurfaceView，暴露出onAttachedToWindow
     * 方法及onDetachedFromWindow方法，取消holder的默认监听
     * onAttachedToWindow及onDetachedFromWindow必须保证view
     * 存在Parent
     */
    private class GLView extends GLSurfaceView {

        public GLView(Context context) {
            super(context);
            init();
        }

        private void init() {
            getHolder().addCallback(null);
            setEGLWindowSurfaceFactory(new GLSurfaceView.EGLWindowSurfaceFactory() {
                @Override
                public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig
                        config, Object window) {
                    return egl.eglCreateWindowSurface(display, config, surface, null);
                }

                @Override
                public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
                    egl.eglDestroySurface(display, surface);
                }
            });
            setEGLContextClientVersion(2);
            setRenderer(TextureController.this);
            setRenderMode(RENDERMODE_WHEN_DIRTY);
            setPreserveEGLContextOnPause(true);
        }

        public void attachedToWindow() {
            super.onAttachedToWindow();
        }

        public void detachedFromWindow() {
            super.onDetachedFromWindow();
        }
    }
}
