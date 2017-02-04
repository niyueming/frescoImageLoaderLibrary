/*
 * Copyright (c) 2017  Ni YueMing<niyueming@163.com>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 */

package net.nym.frescoimageloaderlibrary.impl;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import net.nym.imageloaderlibrary.operation.NImageDownloadListener;
import net.nym.imageloaderlibrary.operation.NImageLoader;
import net.nym.imageloaderlibrary.operation.NImageOptions;

import okhttp3.OkHttpClient;

/**
 * @author niyueming
 * @date 2017-02-03
 * @time 15:54
 */

public final class FrescoImageLoaderImpl implements NImageLoader<SimpleDraweeView>{
    private static NImageLoader my;

    private FrescoImageLoaderImpl(Context context){
        ImagePipelineConfig config = OkHttpImagePipelineConfigFactory
                .newBuilder(context.getApplicationContext(),new OkHttpClient())
                .build();
        Fresco.initialize(context.getApplicationContext(),config);
    }

    public static NImageLoader<SimpleDraweeView> getInstance(Context context){
        if (my == null){
            synchronized (FrescoImageLoaderImpl.class){
                if (my == null){
                    my = new FrescoImageLoaderImpl(context);
                }
            }
        }
        return my;
    }

    @Override
    public void shutDown() {
        Fresco.shutDown();
    }

    @Override
    public void pause() {
        Fresco.getImagePipeline().pause();
    }

    @Override
    public void resume() {
        Fresco.getImagePipeline().resume();
    }

    @Override
    public boolean isPaused() {
        return Fresco.getImagePipeline().isPaused();
    }

    @Override
    public void setImageURI(@NonNull SimpleDraweeView imageView, @Nullable String uri) {
        imageView.setImageURI(uri);
    }

    @Override
    public void setImageURI(@NonNull SimpleDraweeView imageView, @Nullable Uri uri) {
        imageView.setImageURI(uri);
    }

    @Override
    public void setImageURI(@NonNull SimpleDraweeView imageView, @Nullable String uri, final NImageDownloadListener listener) {
        DraweeController controller = initControllerBuilder().setOldController(imageView.getController())
                .setControllerListener(new BaseControllerListener<ImageInfo>(){
                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        super.onFailure(id, throwable);
                        listener.onFailure(throwable);
                    }

                    @Override
                    public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                        super.onFinalImageSet(id, imageInfo, animatable);
                        try {
                            listener.onSuccess(imageInfo,animatable);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                })
                .setUri(uri)
                .build();
        imageView.setController(controller);
    }

    @Override
    public void setImageURI(@NonNull SimpleDraweeView imageView, @Nullable Uri uri, NImageOptions options) {
        if (imageView.getHierarchy() == null){
            imageView.setHierarchy(initGenericDraweeHierarchyBuilder(imageView.getResources()).build());
        }
        GenericDraweeHierarchy hierarchy = imageView.getHierarchy();
        if (options.getImageResFailure() != 0){
            hierarchy.setFailureImage(options.getImageResFailure());
        }else {
            hierarchy.setFailureImage(options.getImageFailure());
        }
        if (options.getImageResPlaceholder() != 0){
            hierarchy.setPlaceholderImage(options.getImageResPlaceholder());
        }else {
            hierarchy.setPlaceholderImage(options.getImagePlaceholder());
        }
        if (options.getImageResRetry() != 0){
            hierarchy.setRetryImage(options.getImageResRetry());
        }else {
            hierarchy.setRetryImage(options.getImageRetry());
        }
        if (options.getImageResProgressBar() != 0){
            hierarchy.setProgressBarImage(options.getImageResProgressBar());
        }else {
            hierarchy.setProgressBarImage(options.getImageProgressBar());
        }

        if (hierarchy.getRoundingParams() == null){
            hierarchy.setRoundingParams(new RoundingParams());
        }
        hierarchy.getRoundingParams().setRoundAsCircle(options.isCircle());
        if (!options.isCircle()){
            if (options.getCornersRadii() != null){
                hierarchy.getRoundingParams().setCornersRadii(options.getCornersRadii());
            }
        }

        hierarchy.getRoundingParams().setBorder(options.getBorderColor(),options.getBorderWidth());

        imageView.setImageURI(uri);
    }

    @Override
    public void setImageCorner(@NonNull SimpleDraweeView imageView, @Nullable Uri uri, float corner) {
        setImageCornerWithBorder(imageView,uri,corner,-1,0);
    }

    @Override
    public void setImageCornerWithBorder(@NonNull SimpleDraweeView imageView, @Nullable Uri uri, float corner, @ColorRes int borderColor, int borderWidth) {
        checkAndSetHierarchy(imageView);

        checkAndSetRoundingParams(imageView);

        setCorner(imageView.getHierarchy(),corner);

        if (borderWidth > 0){
            setBorder(imageView.getHierarchy(),imageView.getResources(),borderColor,borderWidth);
        }

        imageView.setImageURI(uri);
    }

    private void checkAndSetRoundingParams(@NonNull SimpleDraweeView imageView) {
        if (imageView.getHierarchy().getRoundingParams() == null){
            RoundingParams roundingParams = new RoundingParams();
            imageView.getHierarchy().setRoundingParams(roundingParams);
        }
    }

    /**
     * @description 检测是否有Hierarchy，没有则生成一个
     * @param imageView
     */
    private void checkAndSetHierarchy(@NonNull SimpleDraweeView imageView) {
        if (imageView.getHierarchy() == null){
            imageView.setHierarchy(initGenericDraweeHierarchyBuilder(imageView.getResources())
                    .build()
            );
        }
    }

    /**
     * @description 设置圆角角度
     * @param hierarchy
     * @param corner 角度，单位：弧度
     */
    private void setCorner(GenericDraweeHierarchy hierarchy,float corner){
        hierarchy.getRoundingParams()
                .setCornersRadius(corner);
    }

    /**
     * @description 设置边框
     * @param hierarchy
     * @param resources
     * @param borderColor 边框颜色
     * @param borderWidth 边框宽度
     */
    private void setBorder(GenericDraweeHierarchy hierarchy,Resources resources,@ColorRes int borderColor, int borderWidth){
        hierarchy.getRoundingParams()
                .setBorder(resources.getColor(borderColor),borderWidth);
    }

    @Override
    public void setImageCorner(@NonNull SimpleDraweeView imageView, @Nullable Uri uri,
                               float topLeft,
                               float topRight,
                               float bottomRight,
                               float bottomLeft) {
        checkAndSetHierarchy(imageView);

        checkAndSetRoundingParams(imageView);

        imageView.getHierarchy().getRoundingParams().setCornersRadii(topLeft,topRight,bottomRight,bottomLeft);

        imageView.setImageURI(uri);
    }

    @Override
    public void setImageCircle(@NonNull SimpleDraweeView imageView, @Nullable Uri uri, float radius) {
        checkAndSetHierarchy(imageView);

        checkAndSetRoundingParams(imageView);

        imageView.getHierarchy().getRoundingParams().setRoundAsCircle(true);

        imageView.setImageURI(uri);
    }

    @Override
    public void setImageCircleWithBorder(@NonNull SimpleDraweeView imageView, @Nullable Uri uri, float radius, @ColorRes int borderColor, int borderWidth) {
        checkAndSetHierarchy(imageView);

        checkAndSetRoundingParams(imageView);

        imageView.getHierarchy().getRoundingParams().setRoundAsCircle(true);

        if (borderWidth > 0){
            setBorder(imageView.getHierarchy(),imageView.getResources(),borderColor,borderWidth);
        }

        imageView.setImageURI(uri);
    }

    @Override
    public void setImageAnimateURI(@NonNull SimpleDraweeView imageView, @Nullable String uri) {
        DraweeController controller = initControllerBuilder()
                .setImageRequest(initImageRequestBuilder(Uri.parse(uri)).build())
                .setOldController(imageView.getController())
                .build();
        imageView.setController(controller);
    }

    @Override
    public void setImageAnimateURI(@NonNull SimpleDraweeView imageView, @Nullable String uri,final NImageDownloadListener listener) {
        DraweeController controller = initControllerBuilder()
                .setImageRequest(initImageRequestBuilder(Uri.parse(uri)).build())
                .setOldController(imageView.getController())
                .setControllerListener(new BaseControllerListener<ImageInfo>(){
                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        super.onFailure(id, throwable);
                        listener.onFailure(throwable);
                    }

                    @Override
                    public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                        super.onFinalImageSet(id, imageInfo, animatable);
                        try {
                            listener.onSuccess(imageInfo,animatable);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                })
                .build();
        imageView.setController(controller);
    }

    @Override
    public void startAnimate(@NonNull SimpleDraweeView imageView) {
        if (imageView.getController() == null){
            return;
        }
        Animatable animatable = imageView.getController().getAnimatable();
        if (animatable != null){
            animatable.start();
        }
    }

    @Override
    public void stopAnimate(@NonNull SimpleDraweeView imageView) {
        if (imageView.getController() == null){
            return;
        }
        Animatable animatable = imageView.getController().getAnimatable();
        if (animatable != null){
            animatable.stop();
        }
    }

    @Override
    public void toggleAnimate(@NonNull SimpleDraweeView imageView) {
        if (imageView.getController() == null){
            return;
        }
        Animatable animatable = imageView.getController().getAnimatable();
        if (animatable == null){
            return;
        }

        if (animatable.isRunning()){
            animatable.stop();
        }else {
            animatable.start();
        }
    }

    @Override
    public void clearMemoryCaches() {
        Fresco.getImagePipeline().clearMemoryCaches();
    }

    @Override
    public void clearDiskCaches() {
        Fresco.getImagePipeline().clearDiskCaches();
    }

    @Override
    public void clearCaches() {
        Fresco.getImagePipeline().clearCaches();
    }

    @Override
    public boolean isInBitmapMemoryCache(Uri uri) {
        return Fresco.getImagePipeline().isInBitmapMemoryCache(uri);
    }

    @Override
    public boolean isInDiskCacheSync(Uri uri) {
        return Fresco.getImagePipeline().isInDiskCacheSync(uri);
    }

    @Override
    public long getDiskCacheSize() {
        return Fresco.getImagePipelineFactory().getMainFileCache().getSize();
    }

    public static ControllerListener<ImageInfo> getViewSizeControllerListener(@NonNull final SimpleDraweeView draweeView, final int width){
        return new BaseControllerListener<ImageInfo>(){
            @Override
            public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                if (imageInfo == null) {
                    return;
                }
                updateViewSize(draweeView,imageInfo,width);
            }

            @Override
            public void onIntermediateImageSet(String id, ImageInfo imageInfo) {
                super.onIntermediateImageSet(id, imageInfo);
                updateViewSize(draweeView,imageInfo,width);
            }
        };
    }

    private static void updateViewSize(@NonNull SimpleDraweeView draweeView, @Nullable ImageInfo imageInfo,int width) {
        if (imageInfo != null) {
            draweeView.getLayoutParams().width = width;
            draweeView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            draweeView.setAspectRatio((float) imageInfo.getWidth() / imageInfo.getHeight());
        }
    }

    /**
     *类型	                     SCHEME	                     示例
     远程图片	                  http://, https://	          HttpURLConnection 或者参考 使用其他网络加载方案
     本地文件	                  file://	                  FileInputStream
     Content provider	        content://	                ContentResolver
     asset目录下的资源	        asset://	                AssetManager
     res目录下的资源	            res://	                    Resources.openRawResource
     Uri中指定图片数据	         data:mime/type;base64;	     数据类型必须符合 rfc2397规定 (仅支持 UTF-8)
     */
    private ImageRequestBuilder initImageRequestBuilder(Uri uri){
        return ImageRequestBuilder.newBuilderWithSource(uri)
                .setRotationOptions(RotationOptions.autoRotate())
                .setLocalThumbnailPreviewsEnabled(true)
                .setProgressiveRenderingEnabled(true)
//                .setImageDecodeOptions()
//                .setPostprocessor()           //后处理器(修改图片)
//                .setResizeOptions()       //图片缩放
                ;
    }


    private PipelineDraweeControllerBuilder initControllerBuilder(){
        return Fresco.newDraweeControllerBuilder()
                .setAutoPlayAnimations(true)
                .setTapToRetryEnabled(true)
                .setRetainImageOnFailure(true)
                ;
    }

    /**
     *对于同一个View，请不要多次调用setHierarchy，即使这个View是可回收的。创建 DraweeHierarchy 的较为耗时的一个过程，应该多次利用。
     *注意：一个DraweeHierarchy 是不可以被多个 View 共用的！
     *如果要改变所要显示的图片可使用setController 或者 setImageURI。
     */
    private GenericDraweeHierarchyBuilder initGenericDraweeHierarchyBuilder(Resources resources){
        return new GenericDraweeHierarchyBuilder(resources)
                .setFadeDuration(300)
//                .setProgressBarImage()    //进度条
                ;
    }
}
