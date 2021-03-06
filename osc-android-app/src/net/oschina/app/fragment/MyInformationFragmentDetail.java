package net.oschina.app.fragment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.oschina.app.AppContext;
import net.oschina.app.R;
import net.oschina.app.api.remote.OSChinaApi;
import net.oschina.app.base.BaseFragment;
import net.oschina.app.bean.MyInformation;
import net.oschina.app.bean.Result;
import net.oschina.app.bean.ResultBean;
import net.oschina.app.bean.User;
import net.oschina.app.ui.dialog.CommonDialog;
import net.oschina.app.ui.dialog.DialogHelper;
import net.oschina.app.ui.empty.EmptyLayout;
import net.oschina.app.util.FileUtil;
import net.oschina.app.util.ImageUtils;
import net.oschina.app.util.StringUtils;
import net.oschina.app.util.UIHelper;
import net.oschina.app.util.XmlUtils;

import org.apache.http.Header;
import org.kymjs.kjframe.KJBitmap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.loopj.android.http.AsyncHttpResponseHandler;

/**
 * 登录   用户信息详情  “我的资料”
 * <p> 相册 与 图片裁剪都选用第三方
 * @author FireAnt（http://my.oschina.net/LittleDY）
 * @version 创建时间：2015年1月6日 上午10:33:18
 * 
 */

public class MyInformationFragmentDetail extends BaseFragment {

    public static final int ACTION_TYPE_ALBUM = 0;
    public static final int ACTION_TYPE_PHOTO = 1;

    @InjectView(R.id.iv_avatar)
    ImageView mUserFace;

    @InjectView(R.id.tv_name)
    TextView mName;

    @InjectView(R.id.tv_join_time)
    TextView mJoinTime;

    @InjectView(R.id.tv_location)
    TextView mFrom;

    @InjectView(R.id.tv_development_platform)
    TextView mPlatFrom;

    @InjectView(R.id.tv_academic_focus)
    TextView mFocus;

    @InjectView(R.id.error_layout)
    EmptyLayout mErrorLayout;

    private User mUser;

    private boolean isChangeFace = false;

    private String theLarge;

    private final static int CROP = 200;

    private final static String FILE_SAVEPATH = Environment
            .getExternalStorageDirectory().getAbsolutePath()
            + "/OSChina/Portrait/";
    private Uri origUri;
    private Uri cropUri;
    private File protraitFile;
    private Bitmap protraitBitmap;
    private String protraitPath;

    private final AsyncHttpResponseHandler mHandler = new AsyncHttpResponseHandler() {

        @Override
        public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
            mErrorLayout.setErrorType(EmptyLayout.HIDE_LAYOUT);
            mUser = XmlUtils.toBean(MyInformation.class,
                    new ByteArrayInputStream(arg2)).getUser();
            if (mUser != null) {
                fillUI();
            } else {
                this.onFailure(arg0, arg1, arg2, null);
            }
        }

        @Override
        public void onFailure(int arg0, Header[] arg1, byte[] arg2,
                Throwable arg3) {
            mErrorLayout.setErrorType(EmptyLayout.NETWORK_ERROR);
        }

    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(
                R.layout.fragment_my_information_detail, container, false);
        initView(view);
        initData();
        return view;
    }

    /**
     * 头像点击监听
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.iv_avatar:
            showClickAvatar();
            break;

        default:
            break;
        }
    }

    public void showClickAvatar() {
        if (mUser == null) {
            AppContext.showToast("");
            return;
        }
        final CommonDialog dialog = DialogHelper
                .getPinterestDialogCancelable(getActivity());
        dialog.setTitle("选择操作");
        dialog.setNegativeButton(R.string.cancle, null);
        dialog.setItemsWithoutChk(
                getResources().getStringArray(R.array.avatar_option),
                new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        if (position == 0) {    //更换头像
                            handleSelectPicture();
                        } else {                   //查看大头像
                            if (mUser == null) {
                                dialog.dismiss();
                                return;
                            }
                            UIHelper.showUserAvatar(getActivity(),
                                    mUser.getPortrait());
                        }
                        dialog.dismiss();
                    }
                });
        dialog.show();
    }

    private void handleSelectPicture() {
        final CommonDialog dialog = DialogHelper
                .getPinterestDialogCancelable(getActivity());
        dialog.setTitle(R.string.choose_picture);  // 选择图片
        dialog.setNegativeButton(R.string.cancle, null);
        dialog.setItemsWithoutChk(
                getResources().getStringArray(R.array.choose_picture),
                new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        dialog.dismiss();
                        goToSelectPicture(position);
                    }
                });
        dialog.show();
    }
/**
 *选择图片
 * @param position
 */
    private void goToSelectPicture(int position) {
        switch (position) {
        case ACTION_TYPE_ALBUM:  //相册
            startImagePick();
            break;
        case ACTION_TYPE_PHOTO:  //相机
            startTakePhoto();
            break;
        default:
            break;
        }
    }

    @Override
    public void initView(View view) {
        ButterKnife.inject(this, view);
        mErrorLayout.setOnLayoutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRequiredData();
            }
        });

        mUserFace.setOnClickListener(this);
    }

    @Override
    public void initData() {
        sendRequiredData();
    }

    public void fillUI() {
        new KJBitmap().displayWithLoadBitmap(mUserFace, mUser.getPortrait(),
                R.drawable.widget_dface);
        mName.setText(mUser.getName());
        mJoinTime.setText(StringUtils.friendly_time(mUser.getJointime()));
        mFrom.setText(mUser.getFrom());
        mPlatFrom.setText(mUser.getDevplatform());
        mFocus.setText(mUser.getExpertise());
    }

    public void sendRequiredData() {
        mErrorLayout.setErrorType(EmptyLayout.NETWORK_LOADING);
        OSChinaApi.getMyInformation(AppContext.getInstance().getLoginUid(),
                mHandler);
    }

    /**
     * 上传新照片
     */
    private void uploadNewPhoto() {
        showWaitDialog("正在上传头像...");

        // 获取头像缩略图
        if (!StringUtils.isEmpty(protraitPath) && protraitFile.exists()) {
            protraitBitmap = ImageUtils
                    .loadImgThumbnail(protraitPath, 200, 200);
        } else {
            AppContext.showToast("图像不存在，上传失败");
        }
        if (protraitBitmap != null) {

            try {
                OSChinaApi.updatePortrait(AppContext.getInstance()
                        .getLoginUid(), protraitFile,
                        new AsyncHttpResponseHandler() {

                            @Override
                            public void onSuccess(int arg0, Header[] arg1,
                                    byte[] arg2) {
                                Result res = XmlUtils.toBean(ResultBean.class,
                                        new ByteArrayInputStream(arg2))
                                        .getResult();
                                if (res.OK()) {
                                    AppContext.showToast("更换成功");
                                    // 显示新头像
                                    mUserFace.setImageBitmap(protraitBitmap);
                                    isChangeFace = true;
                                } else {
                                    AppContext.showToast(res.getErrorMessage());
                                }
                            }

                            @Override
                            public void onFailure(int arg0, Header[] arg1,
                                    byte[] arg2, Throwable arg3) {
                                AppContext.showToast("更换头像失败");
                            }

                            @Override
                            public void onFinish() {
                                hideWaitDialog();
                            }
                        });
            } catch (FileNotFoundException e) {
                AppContext.showToast("图像不存在，上传失败");
            }
        }
    }

    /**
     * 选择图片裁剪  相册
     * <p> api 19以下与  19及以上相册 的调用不同
     * 
     * @param output
     */
    private void startImagePick() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {  //
            intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "选择图片"),
                    ImageUtils.REQUEST_CODE_GETIMAGE_BYCROP);
        } else {
            intent = new Intent(Intent.ACTION_PICK,
                    Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "选择图片"),
                    ImageUtils.REQUEST_CODE_GETIMAGE_BYCROP);
        }
    }

    private void startTakePhoto() {
        Intent intent;
        // 判断是否挂载了SD卡
        String savePath = "";
        String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            savePath = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/oschina/Camera/";
            File savedir = new File(savePath);
            if (!savedir.exists()) {
                savedir.mkdirs();
            }
        }

        // 没有挂载SD卡，无法保存文件  直接返回  savePath = "";也为空
        if (StringUtils.isEmpty(savePath)) {
            AppContext.showToastShort("无法保存照片，请检查SD卡是否挂载");
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date());
        String fileName = "osc_" + timeStamp + ".jpg";// 照片命名
        File out = new File(savePath, fileName);
        Uri uri = Uri.fromFile(out);
        origUri = uri;

        theLarge = savePath + fileName;// 该照片的绝对路径

        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent,
                ImageUtils.REQUEST_CODE_GETIMAGE_BYCAMERA);
    }

    // 裁剪头像的绝对路径
    private Uri getUploadTempFile(Uri uri) {
        String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            File savedir = new File(FILE_SAVEPATH);
            if (!savedir.exists()) {
                savedir.mkdirs();
            }
        } else {
            AppContext.showToast("无法保存上传的头像，请检查SD卡是否挂载");
            return null;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date());
        String thePath = ImageUtils.getAbsolutePathFromNoStandardUri(uri);

        // 如果是标准Uri
        if (StringUtils.isEmpty(thePath)) {
            thePath = ImageUtils.getAbsoluteImagePath(getActivity(), uri);
        }
        String ext = FileUtil.getFileFormat(thePath);
        ext = StringUtils.isEmpty(ext) ? "jpg" : ext;
        // 照片命名
        String cropFileName = "osc_crop_" + timeStamp + "." + ext;
        // 裁剪头像的绝对路径
        protraitPath = FILE_SAVEPATH + cropFileName;
        protraitFile = new File(protraitPath);

        cropUri = Uri.fromFile(protraitFile);
        return this.cropUri;
    }

    /**
     * 拍照后裁剪
     * <p> 裁剪功能也是用的第三方
     * 
     * @param data
     *            原始图片
     * @param output
     *            裁剪后图片
     */
    private void startActionCrop(Uri data) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(data, "image/*");
        intent.putExtra("output", this.getUploadTempFile(data));
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);// 裁剪框比例
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", CROP);// 输出图片大小  pix
        intent.putExtra("outputY", CROP);
        intent.putExtra("scale", true);// 去黑边
        intent.putExtra("scaleUpIfNeeded", true);// 去黑边
        startActivityForResult(intent,
                ImageUtils.REQUEST_CODE_GETIMAGE_BYSDCARD);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
            final Intent imageReturnIntent) {
        if (resultCode != Activity.RESULT_OK)
            return;

        switch (requestCode) {
        case ImageUtils.REQUEST_CODE_GETIMAGE_BYCAMERA:
            startActionCrop(origUri);// 拍照后裁剪
            break;
        case ImageUtils.REQUEST_CODE_GETIMAGE_BYCROP:
            startActionCrop(imageReturnIntent.getData());// 选图后裁剪
            break;
        case ImageUtils.REQUEST_CODE_GETIMAGE_BYSDCARD: //剪裁后触发
            uploadNewPhoto();
            break;
        }
    }
}
