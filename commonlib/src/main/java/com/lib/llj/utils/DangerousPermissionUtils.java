package com.lib.llj.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;


import com.llj.commonlib.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by chenyk on 2016/9/28.
 * 检查相关权限基类，适用于android 6.0 运行时权限
 */
public class DangerousPermissionUtils {
    private static final String TAG = DangerousPermissionUtils.class.getSimpleName();

    private static final int REQUEST_CODE_PERMISSON = 0x202; //权限申请的请求码

    private WeakReference<Activity> mWeakReference;

    public DangerousPermissionUtils(Activity activity) {
        if (mWeakReference == null) {
            mWeakReference = new WeakReference<>(activity);
        }
    }

    /**
     * 判断是否具有某权限
     *
     * @param perms
     * @return
     */
    public boolean hasPermissions(@NonNull String... perms) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        for (String perm : perms) {
            boolean hasPerm = (ContextCompat.checkSelfPermission(mWeakReference.get(), perm) ==
                    PackageManager.PERMISSION_GRANTED);
            if (!hasPerm) {
                return false;
            }
        }
        return true;
    }

    /**
     * 全局检查所有危险权限
     */
    public void checkGlobleDangerousPermissions() {
        checkDangerousPermissions(getAllPermissions());
    }

    /**
     * 获取应用所有危险权限集合
     *
     * @return
     */
    public String[] getAllPermissions() {
        HashMap<String, String> map = globalPermissionCheck();
        List<String> permissionsList = new ArrayList<>();
        if (map != null && map.size() > 0) {
            permissionsList.addAll(map.values());
        }
        return permissionsList.toArray(
                new String[permissionsList.size()]);
    }

    /**
     * 检查危险权限，无权限则开始申请相关权限
     */
    public void checkDangerousPermissions(String[] dangerousPermissions) {
        Log.i(TAG, "checkDangerousPermissions->" + hashCode());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        List<String> requestPermissonList = getDeniedPermissions(dangerousPermissions);
        if (requestPermissonList != null && requestPermissonList.size() > 0) {
            ActivityCompat.requestPermissions(mWeakReference.get(), requestPermissonList.toArray(
                    new String[requestPermissonList.size()]), REQUEST_CODE_PERMISSON);
        }
    }

    /**
     * 检查危险权限带请求码，无权限则开始申请相关权限
     *
     * @param dangerousPermissions
     * @param requestCode
     */
    public void checkDgPermissionsWithRequestCode(String[] dangerousPermissions, int requestCode) {
        Log.i(TAG, "checkDgPermissionsWithRequestCode->" + hashCode());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        List<String> requestPermissonList = getDeniedPermissions(dangerousPermissions);
        if (requestPermissonList != null && requestPermissonList.size() > 0) {
            ActivityCompat.requestPermissions(mWeakReference.get(), requestPermissonList.toArray(
                    new String[requestPermissonList.size()]), requestCode);
        }
    }

    /**
     * 权限授权结果回调注册，在{@link OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])}方法中调用
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void regisiterOnRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                    @NonNull int[] grantResults, @NonNull DangerousPermissionUtils.CallBack callBack) {
        Log.i(TAG, "OnRequestPermissionsResult->" + hashCode());
        if (isAllPermissionGranted(grantResults)) {
            callBack.requestAllPermissionGranted(requestCode, permissions);
        } else {
            List<String> NormalDeniedPermissions = new ArrayList<>();
            List<String> deniedPermissionsWithNoRemind = new ArrayList<>();
            for (int position = 0; position < grantResults.length; position++) {
                if (grantResults[position] != PackageManager.PERMISSION_GRANTED) {
                    //权限失败，且选择不再提醒按钮
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(mWeakReference.get(), permissions[position])) {
                        deniedPermissionsWithNoRemind.add(permissions[position]);
                    } else {
                        //权限失败，且未选择不再提醒按钮
                        NormalDeniedPermissions.add(permissions[position]);
                    }
                }
            }
            if (deniedPermissionsWithNoRemind.size() > 0) {
                callBack.requestPermissionDenied(requestCode, true, deniedPermissionsWithNoRemind.toArray(new String[deniedPermissionsWithNoRemind.size()]));
            }
            if (NormalDeniedPermissions.size() > 0) {
                callBack.requestPermissionDenied(requestCode, false, NormalDeniedPermissions.toArray(new String[NormalDeniedPermissions.size()]));
            }
        }
    }

    /**
     * 全局所需危险权限检测，并提示用户开启。
     *
     * @return return a HasMap, key means the group of permission and the value means the current permission.
     */
    private HashMap<String, String> globalPermissionCheck() {
        PackageManager packageManager = mWeakReference.get().getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(mWeakReference.get().getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] usesPermissionsArray = packageInfo.requestedPermissions;
            if (usesPermissionsArray != null && usesPermissionsArray.length > 0) {
                //因为危险权限申请为分组申请，只要该组的一个权限申请成功，那么无需申请同一组其他权限。
                return getDangerPermission(usesPermissionsArray);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.toString());
            return null;
        }
        return null;
    }

    /**
     * 获取所有需要申请的危险权限组
     *
     * @param requestedPermissions
     * @return
     */
    private HashMap<String, String> getDangerPermission(String[] requestedPermissions) {
        HashMap<String, String> permissionMap = new HashMap<>();
        String group;
        for (String permission : requestedPermissions) {
            group = getPermissionGroup(permission);
            if (!TextUtils.isEmpty(group) && !permissionMap.containsKey(group)) {
                permissionMap.put(group, permission);
            }
        }
        return permissionMap;
    }


    /**
     * 获取权限集中需要申请权限的列表,已授权的不再申请
     *
     * @return
     */
    public List<String> getDeniedPermissions(String[] dangerousPermissions) {
        Log.i(TAG, "checkDangerousPermissions->" + hashCode());
        List<String> needRequestPermissonList = new ArrayList<>();
        for (String permission : dangerousPermissions) {
            //已授权的权限，将不再申请
            if (ContextCompat.checkSelfPermission(mWeakReference.get(), permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                needRequestPermissonList.add(permission);
            }
        }
        return needRequestPermissonList;
    }

    /**
     * 检测所有的权限是否都已授权
     *
     * @param grantResults
     * @return
     */
    private boolean isAllPermissionGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取授权成功的权限集合
     *
     * @param permissions
     * @param grantResults
     * @return
     */
    private String[] getGrantedPermissions(String[] permissions, int[] grantResults) {
        List<String> grantedPermissonList = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                grantedPermissonList.add(permissions[i]);
            }
        }
        return grantedPermissonList.toArray(new String[grantedPermissonList.size()]);
    }

    /**
     * 获取未授权的权限集合
     *
     * @param permissions
     * @param grantResults
     * @return
     */
    private String[] getDeniedPermissions(String[] permissions, int[] grantResults) {
        List<String> denidePermissonList = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                denidePermissonList.add(permissions[i]);
            }
        }
        return denidePermissonList.toArray(new String[denidePermissonList.size()]);
    }


    /**
     * 启动当前应用设置页面
     */
    private void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + mWeakReference.get().getPackageName()));
        mWeakReference.get().startActivity(intent);
    }

    /**
     * 获取对应权限所属权限组用来作为Key
     *
     * @param permission
     * @return
     */
    private static String getPermissionGroup(String permission) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                return "CAMERA";
            case Manifest.permission.READ_CALENDAR:
            case Manifest.permission.WRITE_CALENDAR:
                return "CALENDAR";
            case Manifest.permission.READ_CONTACTS:
            case Manifest.permission.WRITE_CONTACTS:
            case Manifest.permission.GET_ACCOUNTS:
                return "CONTACTS";
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "LOCATION";
            case Manifest.permission.RECORD_AUDIO:
                return "RECORD";
            case Manifest.permission.READ_PHONE_STATE:
            case Manifest.permission.CALL_PHONE:
            case Manifest.permission.READ_CALL_LOG:
            case Manifest.permission.ADD_VOICEMAIL:
            case Manifest.permission.USE_SIP:
            case Manifest.permission.PROCESS_OUTGOING_CALLS:
                return "PHONEState";
            case Manifest.permission.BODY_SENSORS:
                return "SENSORS";
            case Manifest.permission.SEND_SMS:
            case Manifest.permission.RECEIVE_SMS:
            case Manifest.permission.READ_SMS:
            case Manifest.permission.RECEIVE_WAP_PUSH:
            case Manifest.permission.RECEIVE_MMS:
                return "SMS";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "STORAGE";
        }
        return "";
    }


    /**
     * 获取对应权限所属权限组的提示，可以自定义
     *
     * @param permission
     * @return
     */
    public String getPermissionStr(String permission) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                return mWeakReference.get().getString(R.string.PermissionCamera);
            case Manifest.permission.READ_CALENDAR:
            case Manifest.permission.WRITE_CALENDAR:
                return mWeakReference.get().getString(R.string.PermissionCALENDAR);
            case Manifest.permission.READ_CONTACTS:
            case Manifest.permission.WRITE_CONTACTS:
            case Manifest.permission.GET_ACCOUNTS:
                return mWeakReference.get().getString(R.string.PermissionCONTACTS);
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return mWeakReference.get().getString(R.string.PermissionLOCATION);
            case Manifest.permission.RECORD_AUDIO:
                return mWeakReference.get().getString(R.string.PermissionAUDIO);
            case Manifest.permission.READ_PHONE_STATE:
            case Manifest.permission.CALL_PHONE:
            case Manifest.permission.READ_CALL_LOG:
            case Manifest.permission.ADD_VOICEMAIL:
            case Manifest.permission.USE_SIP:
            case Manifest.permission.PROCESS_OUTGOING_CALLS:
                return mWeakReference.get().getString(R.string.PermissionPHONE);
            case Manifest.permission.BODY_SENSORS:
                return mWeakReference.get().getString(R.string.PermissionSENSORS);
            case Manifest.permission.SEND_SMS:
            case Manifest.permission.RECEIVE_SMS:
            case Manifest.permission.READ_SMS:
            case Manifest.permission.RECEIVE_WAP_PUSH:
            case Manifest.permission.RECEIVE_MMS:
                return mWeakReference.get().getString(R.string.PermissionSMS);
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return mWeakReference.get().getString(R.string.PermissionSTORAGE);
        }
        return "";
    }

    /**
     * 权限授权结果回调接口
     */
    public interface CallBack {
        /**
         * 权限授权结果
         *
         * @param permissions 如果全部授权成功，返回所有权限数组，否则，返回未授权的权限数组
         */
        void requestAllPermissionGranted(int requestCode, String[] permissions);

        void requestPermissionDenied(int requestCode, boolean isSelectNoRemind, String[] permissions);
    }


}
