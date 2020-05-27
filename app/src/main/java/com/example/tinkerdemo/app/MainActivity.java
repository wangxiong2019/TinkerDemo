/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.tinkerdemo.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tinkerdemo.R;
import com.hero.libhero.mydb.LogUtil;
import com.hero.libhero.okhttp.OkHttpUtil;
import com.hero.libhero.okhttp.https.MyCallBack;
import com.hero.libhero.okhttp.https.ReqProgressCallBack;
import com.hero.libhero.permissions.PermissionListener;
import com.hero.libhero.permissions.PermissionsUtil;
import com.hero.libhero.utils.ActivityUtil;
import com.hero.libhero.utils.DateUtils;
import com.hero.libhero.utils.JsonUtil;
import com.hero.libhero.view.XToast;
import com.tencent.tinker.lib.library.TinkerLoadLibrary;
import com.tencent.tinker.lib.tinker.Tinker;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.loader.shareutil.ShareConstants;
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Tinker.MainActivity";

    private TextView mTvMessage = null;

    public String[] Face_Permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //1.  项目根目录apk文件夹 app-release-base.apk 是有bug的文件
        //2.  patch_new.apk  是bug解决后的热更新文件

        mContext = this;


        PermissionsUtil.requestPermission(mContext, new PermissionListener() {
            @Override
            public void permissionGranted(@NonNull String[] permission) {

                ActivityUtil.IsHasSD();

                init();


            }

            @Override
            public void permissionDenied(@NonNull String[] permission) {
                finish();
            }
        }, Face_Permissions);
    }


    //热更新文件
    String apk = ActivityUtil.mSavePath + "/patch_new.apk";

    private void init() {

        boolean isARKHotRunning = ShareTinkerInternals.isArkHotRuning();
        Log.e(TAG, "ARK HOT Running status = " + isARKHotRunning);
        Log.e(TAG, "i am on onCreate classloader:" + MainActivity.class.getClassLoader().toString());
        //test resource change
        Log.e(TAG, "i am on onCreate string:" + getResources().getString(R.string.test_resource));


        mTvMessage = findViewById(R.id.tv_message);

        Button loadPatchButton = (Button) findViewById(R.id.loadPatch);


        loadPatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(apk);
                if (file.exists() == false) {
                    Toast.makeText(MainActivity.this, "文件不存在", Toast.LENGTH_LONG).show();
                    return;
                }
                TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(), apk);

            }
        });

        Button loadLibraryButton = (Button) findViewById(R.id.loadLibrary);

        loadLibraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // #method 1, hack classloader library path
                TinkerLoadLibrary.installNavitveLibraryABI(getApplicationContext(), "armeabi");
                System.loadLibrary("stlport_shared");

                // #method 2, for lib/armeabi, just use TinkerInstaller.loadLibrary
//                TinkerLoadLibrary.loadArmLibrary(getApplicationContext(), "stlport_shared");

                // #method 3, load tinker patch library directly
//                TinkerInstaller.loadLibraryFromTinker(getApplicationContext(), "assets/x86", "stlport_shared");

            }
        });

        Button cleanPatchButton = (Button) findViewById(R.id.cleanPatch);

        cleanPatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tinker.with(getApplicationContext()).cleanPatch();
            }
        });


        //需要重启应用 才有效
        Button killSelfButton = (Button) findViewById(R.id.killSelf);

        killSelfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareTinkerInternals.killAllOtherProcess(getApplicationContext());
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });

        Button buildInfoButton = (Button) findViewById(R.id.showInfo);

        buildInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfo(MainActivity.this);
            }
        });


        Button btn_bug = (Button) findViewById(R.id.btn_bug);

        btn_bug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<String> list=new ArrayList<>();
                list.add("111");

                //Toast.makeText(MainActivity.this, "模拟一个bug"+list.get(2), Toast.LENGTH_LONG).show();


                XToast.success(MainActivity.this, "bug清除").show();

            }
        });

        Button btn_check = (Button) findViewById(R.id.btn_check);

        btn_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //1.检查并下载
                check();
            }
        });



    }


    //模拟真实生产环境  把生成的没有bug的文件放在服务器上 然后下载 再加载 三重启应用
    private void check() {

        String http = "http://www.fastpaotui.com/App/CommonApi/GetNewHotFile";

        Map<String, Object> map = new HashMap<>();
        map.put("app_type", "user");
        map.put("today", DateUtils.getNowTime());
        map.put("version_code", ActivityUtil.GetVersionCode());

        OkHttpUtil.doPostBodyAsny(http, map, new MyCallBack() {
            @Override
            public void failBack(String res_msg, int res_code) {

            }

            @Override
            public void successBack(String res_data) {


                try {
                    ResultBean resultBean = JsonUtil.dataToClass(res_data, ResultBean.class);
                    if (resultBean.getCode() == 0) {


                        String hasnew = JsonUtil.dataToObject(resultBean.getData().toString()).getString("hasnew");

                        if (hasnew.equals("true")) {
                            String result = JsonUtil.dataToObject(resultBean.getData().toString()).getString("result");

                            String download_url = JsonUtil.dataToObject(result).getString("download_url");

                            LogUtil.e("download_url=" + download_url);


                            downLoad(download_url);

                        }

                    } else {
                        String msg = resultBean.getMessage();
                        XToast.error(mContext, msg).show();
                    }
                } catch (Exception e) {
                    LogUtil.e("Exception=" + e.getMessage());


                }
            }
        });
    }

    private void downLoad(String download_url) {


        OkHttpUtil.downLoadProgressFile(download_url, ActivityUtil.mSavePath, new ReqProgressCallBack() {
            @Override
            public void failBack(String res_msg, int res_code) {
                XToast.error(mContext, "热更新文件下载失败").show();
            }

            @Override
            public void successBack(String res_data) {
                XToast.success(mContext, "热更新文件下载完成").show();
                //2.加载热更新文件
                TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(), apk);
            }

            @Override
            public void progressBack(long total, long current, int percentage) {
               LogUtil.e( percentage + "%");
            }


        });

    }

    public boolean showInfo(Context context) {
        // add more Build Info
        final StringBuilder sb = new StringBuilder();
        Tinker tinker = Tinker.with(getApplicationContext());
        if (tinker.isTinkerLoaded()) {
            sb.append(String.format("[patch is loaded] \n"));
            sb.append(String.format("[buildConfig TINKER_ID] %s \n", BuildInfo.TINKER_ID));
            sb.append(String.format("[buildConfig BASE_TINKER_ID] %s \n", BaseBuildInfo.BASE_TINKER_ID));

            sb.append(String.format("[buildConfig MESSSAGE] %s \n", BuildInfo.MESSAGE));
            sb.append(String.format("[TINKER_ID] %s \n", tinker.getTinkerLoadResultIfPresent().getPackageConfigByName(ShareConstants.TINKER_ID)));
            sb.append(String.format("[packageConfig patchMessage] %s \n", tinker.getTinkerLoadResultIfPresent().getPackageConfigByName("patchMessage")));
            sb.append(String.format("[TINKER_ID Rom Space] %d k \n", tinker.getTinkerRomSpace()));

        } else {
            sb.append(String.format("[patch is not loaded] \n"));
            sb.append(String.format("[buildConfig TINKER_ID] %s \n", BuildInfo.TINKER_ID));
            sb.append(String.format("[buildConfig BASE_TINKER_ID] %s \n", BaseBuildInfo.BASE_TINKER_ID));

            sb.append(String.format("[buildConfig MESSSAGE] %s \n", BuildInfo.MESSAGE));
            sb.append(String.format("[TINKER_ID] %s \n", ShareTinkerInternals.getManifestTinkerID(getApplicationContext())));
        }
        sb.append(String.format("[BaseBuildInfo Message] %s \n", BaseBuildInfo.TEST_MESSAGE));

        final TextView v = new TextView(context);
        v.setText(sb);
        v.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        v.setTextColor(0xFF000000);
        v.setTypeface(Typeface.MONOSPACE);
        final int padding = 16;
        v.setPadding(padding, padding, padding, padding);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setView(v);
        final AlertDialog alert = builder.create();
        alert.show();
        return true;
    }

//    @Override
//    protected void onResume() {
//        Log.e(TAG, "i am on onResume");
////        Log.e(TAG, "i am on patch onResume");
//
//        super.onResume();
//        Utils.setBackground(false);
//
//
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        Utils.setBackground(true);
//    }
}
