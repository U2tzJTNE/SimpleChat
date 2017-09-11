package cn.bmob.imdemo.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;


import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * [从本地选择图片以及拍照工具类，完美适配2.0-5.0版本]
 *
 * @author huxinwu
 * @version 1.0
 * @date 2015-1-7
 **/
public class PhotoUtils {

    private final String tag = PhotoUtils.class.getSimpleName();

    /**
     * 裁剪图片成功后返回
     **/
    public static final int INTENT_CROP = 2;
    /**
     * 拍照成功后返回
     **/
    public static final int INTENT_TAKE = 3;
    /**
     * 选择成功后返回
     **/
    public static final int INTENT_SELECT = 4;


    public boolean isCrop = true;

    //public static final String CROP_FILE_NAME = "crop_file.jpg";

    /**
     * PhotoUtils对象
     **/
    private OnPhotoResultListener onPhotoResultListener;
    private Uri image_uri;

    public PhotoUtils(OnPhotoResultListener onPhotoResultListener) {
        this.onPhotoResultListener = onPhotoResultListener;
    }

    /**
     * 拍照
     */
    public void takePicture(Activity activity) {
        try {
//            //每次选择图片吧之前的图片删除
//            clearCropFile(buildUri(activity));

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
            image_uri = buildUri(activity);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
            if (!isIntentAvailable(activity, intent)) {
                return;
            }
            activity.startActivityForResult(intent, INTENT_TAKE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * 选择一张图片
     * 图片类型，这里是image/*，当然也可以设置限制
     * 如：image/jpeg等
     *
     * @param activity Activity
     */
    @SuppressLint("InlinedApi")
    public void selectPicture(Activity activity) {
        try {
//            //每次选择图片吧之前的图片删除
//            clearCropFile(buildUri(activity));

            Intent intent = new Intent(Intent.ACTION_PICK, null);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");

            if (!isIntentAvailable(activity, intent)) {
                return;
            }
            activity.startActivityForResult(intent, INTENT_SELECT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 构建uri
     *
     * @param activity
     * @return
     */
    private Uri buildUri(Activity activity) {
        String image_name = System.currentTimeMillis()+".jpg";
        if (CommonUtils.checkSDCard()) {
            File image_dir = new File(Environment.getExternalStorageDirectory()+ "/SIMPLE_CHAT_IMAGES");
            if (!image_dir.exists()) {
                image_dir.mkdir();
            }
            return Uri.fromFile(image_dir).buildUpon().appendPath(image_name).build();
        } else {
            File image_dir = new File(activity.getCacheDir()+ "/SIMPLE_CHAT_IMAGES");
            if (!image_dir.exists()) {
                image_dir.mkdir();
            }
            return Uri.fromFile(image_dir).buildUpon().appendPath(image_name).build();
        }
    }

    /**
     * 判断Intent是否有效
     *
     * @param intent
     * @return
     */
    protected boolean isIntentAvailable(Activity activity, Intent intent) {
        PackageManager packageManager = activity.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private boolean corp(Activity activity, Uri uri) {
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setDataAndType(uri, "image/*");
        cropIntent.putExtra("crop", "true");
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1);
        cropIntent.putExtra("outputX", 200);
        cropIntent.putExtra("outputY", 200);
        cropIntent.putExtra("return-data", false);
        cropIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        Uri cropuri = buildUri(activity);
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, cropuri);
        if (!isIntentAvailable(activity, cropIntent)) {
            return false;
        } else {
            try {
                activity.startActivityForResult(cropIntent, INTENT_CROP);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * 返回结果处理
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (onPhotoResultListener == null) {
            Log.e(tag, "onPhotoResultListener is not null");
            return;
        }

        switch (requestCode) {
            //拍照
            case INTENT_TAKE:
                //判断文件是否存在
                if (new File(image_uri.getPath()).exists()) {

                    if (isCrop) {
                        if (corp(activity, image_uri)) {
                            return;
                        }
                    } else {
                        onPhotoResultListener.onPhotoResult(image_uri);
                    }
                    onPhotoResultListener.onPhotoCancel();
                }
                break;

            //选择图片
            case INTENT_SELECT:
                if (data != null && data.getData() != null) {
                    Uri imageUri = data.getData();
                    if (isCrop) {
                        if (corp(activity, imageUri)) {
                            return;
                        }
                    } else {
                        Uri url = imageUri;
                        onPhotoResultListener.onPhotoResult(imageUri);
                    }
                }
                onPhotoResultListener.onPhotoCancel();
                break;

            //截图
            case INTENT_CROP:
                if (resultCode == Activity.RESULT_OK && new File(image_uri.getPath()).exists()) {
                    onPhotoResultListener.onPhotoResult(image_uri);
                }
                break;
        }
    }

//    /**
//     * 删除文件
//     *
//     * @param uri
//     * @return
//     */
//    public boolean clearCropFile(Uri uri) {
//        if (uri == null) {
//            return false;
//        }
//
//        File file = new File(uri.getPath());
//        if (file.exists()) {
//            boolean result = file.delete();
//            if (result) {
//                Log.i(tag, "Cached crop file cleared.");
//            } else {
//                Log.e(tag, "Failed to clear cached crop file.");
//            }
//            return result;
//        } else {
//            Log.w(tag, "Trying to clear cached crop file but it does not exist.");
//        }
//
//        return false;
//    }

    /**
     * [回调监听类]
     *
     * @author huxinwu
     * @version 1.0
     * @date 2015-1-7
     **/
    public interface OnPhotoResultListener {
        void onPhotoResult(Uri uri);

        void onPhotoCancel();
    }

    public OnPhotoResultListener getOnPhotoResultListener() {
        return onPhotoResultListener;
    }

    public void setOnPhotoResultListener(OnPhotoResultListener onPhotoResultListener) {
        this.onPhotoResultListener = onPhotoResultListener;
    }

}
