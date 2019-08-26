package com.kikatech.hackkeyboardxposedmodule;

import android.app.Application;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.inputmethodservice.InputMethodService;
import android.os.Environment;
import android.support.v4.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.kikatech.hackbasicmodule.CommandProcess;
import com.kikatech.hackbasicmodule.bean.Candidate;
import com.kikatech.hackbasicmodule.bean.KeyboardConfig;
import com.kikatech.hackbasicmodule.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.SELinuxHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.services.BaseService;

/**
 * Created by xm on 17/4/13.
 */

public class XposedModuleMain implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private ClassLoader mAppClassLoader;
    private Application mApplication;
    private Object commandProcessObj;

    public static KeyboardConfig mKeyboardConfig;

    public static XSharedPreferences prefs;
    private View mMainKeyboard;
    private View mCandicateView;

    public XposedModuleMain() {
        Logger.v("XposedModuleMain constructor");
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        loadPrefs();
    }

    private void loadPrefs() {
        prefs = new XSharedPreferences(Common.MY_PACKAGE_NAME, Common.PREFS);
        prefs.makeWorldReadable();
    }


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        Logger.i(loadPackageParam.packageName);
        prefs.reload();
        if (!prefs.getString("packageName","").equals(loadPackageParam.packageName)) {
            return;
        }
        Logger.v("we found packageName");
        if (loadConfigFile()) {
            Logger.v("load config success");
        }
        mAppClassLoader = loadPackageParam.classLoader;
        hookApplication();
        hookInputMethodService(mKeyboardConfig.getInputmethodServiceClazz());
        hookMainKeyboardView(mKeyboardConfig.getMainKeyboardViewClazz());
        hookCandidateView(mKeyboardConfig.getCandidateViewClazz(),mKeyboardConfig.getKeyboardType());
        hookTouch();
    }

    private void requestWRITEEXTERNALSTORAGEPermission(){
        Logger.i("WRITE_EXTERNAL_STORAGE permission has not been granted. request WRITEEXTERNALSTORAGE Permission");

    }
    private boolean loadConfigFile() {
//
//
//        //tonzh
//        if (ActivityCompat.checkSelfPermission(mApplication, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            // Camera permission has not been granted.
//
//            //未授权，提起权限申请
//            requestWRITEEXTERNALSTORAGEPermission();
//        } else {
//            //权限已授权，功能操作
//            Logger.i("WRITE_EXTERNAL_STORAGE permission has already been granted.");
//        }


        BaseService appDataFileService = SELinuxHelper.getAppDataFileService();
        File directory = Environment.getExternalStorageDirectory();
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(appDataFileService.getFileInputStream(directory + "/layout_info"));
            Gson gson = new Gson();
            mKeyboardConfig = gson.fromJson(new JsonReader(reader), KeyboardConfig.class);
            if (mKeyboardConfig != null) {
                Logger.v(mKeyboardConfig.toString());
                return true;
            } else {
                Logger.v("com.kikatech.hackbasicmodule.bean.KeyboardConfig is null");
                return false;
            }
        } catch (NullPointerException e) {
            Logger.e(e);
            return false;
        } catch (JsonIOException e) {
            Logger.e(e);
            return false;
        } catch (JsonSyntaxException e) {
            Logger.e(e);
            return false;
        } catch (IOException e) {
            Logger.e(e);
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }
        }
    }


    private void hookApplication() {
        Logger.v("hookApplication");
        XposedBridge.hookAllMethods(XposedHelpers.findClass("android.app.Application", mAppClassLoader), "onCreate", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.v("[Module-Application] before " + param.method.getName());
                mApplication = (Application) param.thisObject;
            }
        });
    }

    private void hookInputMethodService(String inputMethodServiceName) {
        Logger.v("hookInputMethodService : " + inputMethodServiceName);
        String hackmethod = "onCreate";
        if (mKeyboardConfig.getKeyboardType().equals("gboard")){
            hackmethod = "a";
        }

        Class<?> inputMethodServiceClazz = XposedHelpers.findClass(inputMethodServiceName, mAppClassLoader);
        try {
//            XposedHelpers.findAndHookMethod(inputMethodServiceClazz, "onCreate", new XC_MethodHook() {
            XposedHelpers.findAndHookMethod(inputMethodServiceClazz, hackmethod, new XC_MethodHook() {
                @Override

                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Logger.v("[Module-InputMethodService-onCreate] before");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Logger.v("[Module-InputMethodService-onCreate] after");
                    InputMethodService inputMethodService = (InputMethodService) param.thisObject;
                    Logger.e("get service", inputMethodService.toString());
                    CommandProcess.getInstance().onCreate(inputMethodService);
                    CommandProcess.getInstance().setKeyboardType(mKeyboardConfig.getKeyboardType());
                    callSetKeyboardConfig();
                }
            });
        }
        catch (Exception e){
            Logger.e("hack service: onCreate", e);
        }
        try {
            XposedBridge.hookAllMethods(inputMethodServiceClazz.getSuperclass(), "hideWindow", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Logger.v("[Module-InputMethodService-hideWindow] before");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Logger.v("[Module-InputMethodService-hideWindow] after");
                }
            });
        }
        catch (Exception e){
            Logger.e("hack service: hideWindow", e);
        }

        try {
            Method hideWindowMethod = inputMethodServiceClazz.getSuperclass().getMethod("hideWindow");
            CommandProcess.getInstance().setHideWindowMethod(hideWindowMethod);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        Method[] methods = inputMethodServiceClazz.getDeclaredMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers())) {
                // XposedBridge.hookMethod(method, mMyMethodHooker);
                if ("showWindow".equals(method.getName())) {
                    CommandProcess.getInstance().setShowWindowMethod(method);
                }
            }
        }
    }

    private XC_MethodHook mMyMethodHooker = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Logger.v("[Module-InputMethodService-" + param.method.getName() + "] before");
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            super.afterHookedMethod(param);
            Logger.v("[Module-InputMethodService-" + param.method.getName() + "] after");
        }
    };

    private void callSetKeyboardConfig() {
        Logger.v("callSetKeyboardConfig");
        CommandProcess.getInstance().setKeyboardConfig(mKeyboardConfig);
    }


    private void hookMainKeyboardView(String mainKeyboardViewName) {
        Logger.v("hookMainKeyboardView : " + mainKeyboardViewName);
        final Class<?> mainKeyboard = XposedHelpers.findClass(mainKeyboardViewName, mAppClassLoader);
        XposedBridge.hookAllConstructors(mainKeyboard, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Logger.v("[hookMainKeyboardView-Constructor] after");
                    CommandProcess.getInstance().setMainKeyboardView((View) param.thisObject);
                    mMainKeyboard = (View)param.thisObject;
                }
            }
        );
    }

    /**
     * 可以辅助查询事件分发的层级关系和分发的类
     */
    private void hookTouch() {
        Logger.v("hookTouch");
        XposedBridge.hookAllMethods(View.class, "dispatchTouchEvent", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                Logger.v("[Touch] dispatchTouchEvent: " + param.thisObject + " => " + param.args[0]);
            }
        });

        XposedBridge.hookAllMethods(View.class, "onTouchEvent", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                Logger.v("[Touch] onTouchEvent: " + param.thisObject + " => " + param.args[0]);
            }
        });
    }

    private void hookCandidateView(final String candicateViewName, final String keyboardType) {
        Logger.i("keyboardType : " + keyboardType);
        switch (keyboardType) {
            case "swiftkey":
                handleSwiftKeyCandidate(candicateViewName);
                break;
            case "touchpal":
                handleTouchPal(candicateViewName);
                break;
            case "gboard":
                handleGBoardCandidate(candicateViewName);
                break;
            case "kika":
                handleKikaCandidate(candicateViewName);
                break;
            case "emojiPro":
                handleKikaCandidate(candicateViewName);
                break;
            case "kikaindia":
                handleKikaIndiaCandidate(candicateViewName);
                break;
            case "ghindi":
                handleGHindiCandidate(candicateViewName);
                break;
            case "facemoji":
                handleFacemojiCandidate(candicateViewName);
                break;
            case "typany":
                handleTypanyCandidate(candicateViewName);
                break;
            case "kikaoem":
                handleKikaCandidate(candicateViewName);
                break;
        }
    }

    private void handleTouchPal(String candidateViewName) {
        Logger.v("handleTouchPal");

        final String paraClassName = "com.cootek.smartinput5.engine.CandidateManager$ICandidateProvider";

        final Class<?> a = XposedHelpers.findClass(candidateViewName, mAppClassLoader);
        try {
            XposedHelpers.findAndHookMethod(a, "onCandidateUpdated",
                    boolean.class,
                    mAppClassLoader.loadClass(paraClassName),
                    boolean.class,
                    mAppClassLoader.loadClass(paraClassName),
                    boolean.class,
                    mAppClassLoader.loadClass(paraClassName),
                    boolean.class,
                    new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            Logger.v("afterHookedMethod onCandidateUpdated");
//                            Field field = XposedHelpers.findField(a, "mWordContainer");
//                            CommandProcess.getInstance().setCandidateView((View) field.get(param.thisObject));
                            CommandProcess.getInstance().setCandidateView((View) param.thisObject);

//                            Method getFirstIndexMethod = XposedHelpers.findMethodExact(
//                                    mAppClassLoader.loadClass(paraClassName), "getFirstIndex");
                            // Logger.v("param.args[1] a = " + param.args[1].getClass().getSimpleName());
                            // Logger.v("getFirstIndexMethod a = " + getFirstIndexMethod.getName());
//                            Object firstIndex = getFirstIndexMethod.invoke(param.args[1]);
//                            int i = (Integer) firstIndex;
//                            Logger.v("firstIndex = " + i);

                            Method getMethod = XposedHelpers.findMethodExact(
                                    mAppClassLoader.loadClass(paraClassName), "get", int.class);
                            Method getDisplayStringMethod = XposedHelpers.findMethodExact(
                                    mAppClassLoader.loadClass("com.cootek.smartinput5.engine.CandidateItem"),
                                    "getDisplayString");

                            List<Candidate> list = new ArrayList<>();
                            Object CandidateItem;
                            int index = 0;
                            while ((CandidateItem = getMethod.invoke(param.args[1], index)) != null) {
                                String candidate = (String) getDisplayStringMethod.invoke(CandidateItem);
                                list.add(new Candidate(candidate));

//                                if (index < 3) {
//                                    Logger.v("Candidate[" + index + "] = " + candidate);
//                                }
                                index++;
                            }
//                            CommandProcess.getInstance().setCandidates(list, null);
                            Logger.v("Total candidate: " + list.toString());
                        }
                    }
            );
        } catch(ClassNotFoundException e) {
            Logger.v("Touchpal ClassNotFoundException");
        }

        String candidateName = "com.cootek.smartinput5.ui.TopScrollView";

        final Class<?> b = XposedHelpers.findClass(candidateName, mAppClassLoader);
        try {
            XposedHelpers.findAndHookMethod(b, "b",
//                    ArrayList.class,
//                    int.class,
                    Canvas.class,
                    String.class,
                    float.class,
                    float.class,
                    Paint.class,
                    new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            List<Candidate> list = new ArrayList<>();
                            Logger.v("draw " + param.args[1].toString());
                            CommandProcess.getInstance().setTouchoalCandidates(param.args[1].toString());
                        }
                    }
            );
        } catch(Exception e) {
            Logger.v("Touchpal ClassNotFoundException");
        }
    }

    private void handleGHindiCandidate(String candidateViewName) {
        Logger.v("handleGHindiCandidate");
        Class<?> a = XposedHelpers.findClass(candidateViewName, mAppClassLoader);
        try {
            //mAppClassLoader.loadClass("com.google.android.apps.inputmethod.libs.framework.core.Candidate");
//            XposedHelpers.findAndHookMethod(a, "appendTextCandidates",
            XposedHelpers.findAndHookMethod(a, "appendTextCandidates",
                    List.class,
                    mAppClassLoader.loadClass("com.google.android.apps.inputmethod.libs.framework.core.Candidate"),
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                List<Object> candidates = (List<Object>) param.args[0];
                                List<Candidate> list = new ArrayList<Candidate>();
                                for (Object obj : candidates) {
                                    //Candidate : text = 'an' : rank = 0 : position = 0 : autoCorrection = false
                                    String str = obj.toString();
                                    String[] split = str.split(":");
                                    String[] split1 = split[1].trim().replace("'", "").split("=");
                                    String suggestion = split1[1].trim();
                                    list.add(new Candidate(suggestion));
                                }
                                CommandProcess.getInstance().setCandidates(list, null);
                                Logger.v(list.toString());
                            } catch (Exception e){
                                Logger.e("handleGHindiCandidate faield");
                                Logger.e(e);
                            }
                        }
                    }
            );
        } catch(ClassNotFoundException e) {
            Logger.e(e);

        }
    }

    private void handleKikaIndiaCandidate(String candidateViewName) {
        Logger.v("handleKikaCandidate");
        String[] split = candidateViewName.split(";");
        Logger.i(Arrays.toString(split));
        String candidateViewNameClazz = split[0];
        String methodName = "showSuggestWord";
        String paramName = "com.android.inputmethod.latin.SuggestedWords";
        if (split.length > 1) {
            methodName = split[1];
            paramName = split[2];
        }
        final Class<?> a = XposedHelpers.findClass(candidateViewNameClazz, mAppClassLoader);
        try {
            XposedHelpers.findAndHookMethod(a, methodName,
                    mAppClassLoader.loadClass(paramName),
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Field field = XposedHelpers.findField(a, "mWordContainer");
                            CommandProcess.getInstance().setCandidateView((View) field.get(param.thisObject));
                            ViewGroup viewGroup = (ViewGroup) field.get(param.thisObject);
                            LinearLayout layout = (LinearLayout) ((ViewGroup) viewGroup.getChildAt(0)).getChildAt(0);
                            List<Candidate> list = new ArrayList<Candidate>();
                            for (int i =0; i < layout.getChildCount(); i++) {
                                View childView = layout.getChildAt(i);
                                if (childView instanceof TextView) {
                                    CharSequence text = ((TextView) childView).getText();
                                    Logger.v("Ryan text = " + text);
                                    if (i == 1) {
                                        list.add(0, new Candidate(text.toString()));
                                    } else {
                                        list.add(new Candidate(text.toString()));
                                    }
                                }
                            }
                            CommandProcess.getInstance().setCandidates(list, null);
                        }
                    }
            );
        } catch (ClassNotFoundException e) {
            Logger.e(e);
        }
    }

    private void handleKikaOemCandidate(String candidateViewName) {
        Logger.v("handleKikaCandidate");
        String[] split = candidateViewName.split(";");
        Logger.i(Arrays.toString(split));
        String candidateViewNameClazz = split[0];
        String methodName = "showSuggestWord";
        String paramName = "com.android.inputmethod.latin.SuggestedWords";
        if (split.length > 1) {
            methodName = split[1];
            paramName = split[2];
        }
        final Class<?> a = XposedHelpers.findClass(candidateViewNameClazz, mAppClassLoader);
        try {
            XposedHelpers.findAndHookMethod(a, methodName,
                    mAppClassLoader.loadClass(paramName),
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Field field = XposedHelpers.findField(a, "mWordContainer");
                            CommandProcess.getInstance().setCandidateView((View) field.get(param.thisObject));
                            ViewGroup viewGroup = (ViewGroup) field.get(param.thisObject);
                            LinearLayout layout = (LinearLayout) ((ViewGroup) viewGroup.getChildAt(0)).getChildAt(0);
                            List<Candidate> list = new ArrayList<Candidate>();
                            for (int i =0; i < layout.getChildCount(); i++) {
                                View childView = layout.getChildAt(i);
                                if (childView instanceof TextView) {
                                    CharSequence text = ((TextView) childView).getText();
                                    Logger.v("Ryan text = " + text);
                                    if (i == 1) {
                                        list.add(0, new Candidate(text.toString()));
                                    } else {
                                        list.add(new Candidate(text.toString()));
                                    }
                                }
                            }
                            CommandProcess.getInstance().setCandidates(list, null);
                        }
                    }
            );
        } catch (ClassNotFoundException e) {
            Logger.e(e);
        }
    }

    private void handleKikaCandidate(String candidateViewName) {
        Logger.v("handleKikaCandidate");
        String[] split = candidateViewName.split(";");
        Logger.i(Arrays.toString(split));
        String candidateViewNameClazz = split[0];
        String methodName = "layoutWords";
//        String paramName = "com.android.inputmethod.latin.SuggestedWords"; tonzh
        String paramName = "com.android.inputmethod.core.engine.suggest.SuggestedWords";
        if (split.length > 1) {
            methodName = split[1];
            paramName = split[2];
        }
//        tonzh
//        methodName = "layoutSuggestedWord";
        final Class<?> a = XposedHelpers.findClass(candidateViewNameClazz, mAppClassLoader);
//        try {
//            XposedHelpers.findAndHookMethod(a, methodName,
//                    mAppClassLoader.loadClass(paramName),
////                    boolean.class,
////                    method layoutSuggestedWord
//                    int.class,
//                    boolean.class,
//                    new XC_MethodHook() {
//                        @Override
//                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                            Field field = XposedHelpers.findField(a, "mWordContainer");
//                            CommandProcess.getInstance().setCandidateView((View) field.get(param.thisObject));
//                            ViewGroup viewGroup = (ViewGroup) field.get(param.thisObject);
//                            List<Candidate> list = new ArrayList<Candidate>();
//                            for (int i =0; i < viewGroup.getChildCount(); i++) {
//                                View childView = viewGroup.getChildAt(i);
//                                if (childView instanceof TextView) {
//                                    CharSequence text = ((TextView) childView).getText();
//                                    if (i == 1) {
//                                        list.add(0, new Candidate(text.toString()));
//                                    } else {
//                                        list.add(new Candidate(text.toString()));
//                                    }
//                                }
//                            }
//                            CommandProcess.getInstance().setCandidates(list, null);
//                        }
//                    }
//            );
//        } catch (Exception e) {
//            Logger.e("hack mWordContainer failed");
//            Logger.e(e);
//        }
//        methodName = "setSuggestWord";
//        paramName = "com.android.inputmethod.core.engine.suggest.SuggestedWords";
        final String paramName1 = paramName;
        try {
            XposedHelpers.findAndHookMethod(a, methodName,
                    mAppClassLoader.loadClass(paramName),
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try{

                                Field field = XposedHelpers.findField(a, "mWordContainer");
                                CommandProcess.getInstance().setCandidateView((View) field.get(param.thisObject));

//                                String paramName = "com.kikatech.inputmethod.SuggestedWords";


                                Logger.e("get setSuggestWord args0" + param.args[0].toString());
                                Class  csug = (mAppClassLoader.loadClass(paramName1));
                                Field fildAutoCorrec = csug.getField("mWillAutoCorrect");
                                boolean auto = (Boolean) fildAutoCorrec.get(param.args[0]);
                                Logger.v("is auto： " + auto);
//                                删除final属性
                                Field fildWords = csug.getDeclaredField("mSuggestedWordInfoList");
                                Field modifiersField = Field.class.getDeclaredField("accessFlags");
                                modifiersField.setAccessible(true);
                                modifiersField.setInt(fildWords, fildWords.getModifiers() & ~Modifier.FINAL);
                                fildWords.setAccessible(true);

                                List<Object> words = (List<Object>) fildWords.get(param.args[0]);
                                List<Candidate> list = new ArrayList<Candidate>();

                                Class csuginfo = (mAppClassLoader.loadClass(paramName1 + "$SuggestedWordInfo"));
                                Field fildmword = csuginfo.getField("mWord");
                                Logger.v("print origin words: ", words.toString());
                                for (int i = 0; i < words.size(); i++){
                                    if (i == 0 && auto){
                                        String word1 = (String) fildmword.get(words.get(0));
                                        String word0 = (String) fildmword.get(words.get(1));
                                        list.add(new Candidate(word0));
                                        list.add(new Candidate(word1));
                                        i++;
                                    }
                                    else {
                                        String word0 = (String) fildmword.get(words.get(i));
                                        list.add(new Candidate(word0));
                                    }
//                                    String word0 = (String) fildmword.get(words.get(i));
//                                    list.add(new Candidate(word0));


                                }
                                Logger.v("setIkeyCandidates: " + list.toString());
                                CommandProcess.getInstance().setCandidates(list, null);
                            }
                            catch (Exception e){
                                Logger.e("hack setSuggestWord failed!");
                                Logger.e(e);
                            }
                        }
                    }
            );
        } catch (Exception e) {
            Logger.e("hack mSuggestedWords failed!");
            Logger.e(e);
        }
    }

    private void handleGBoardCandidate(String candicateViewName) {
        Logger.v("handleGBoardCandidate");
        //com.google.android.apps.inputmethod.libs.framework.keyboard.widget.FixedSizeCandidatesHolderView
        //com.google.android.apps.inputmethod.latin.keyboard.widget.LatinFixedCountCandidatesHolderView
        Class<?> a = XposedHelpers.findClass(candicateViewName, mAppClassLoader);
//        XposedHelpers.findAndHookMethod(a, "appendCandidates", List.class, new XC_MethodHook()
        XposedHelpers.findAndHookMethod(a, "a", List.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    mCandicateView = (View) param.thisObject;
                    //                Logger.e("tonzh hanleGboad", mCandicateView.getParent() + "; " + mCandicateView.getVisibility());
                    CommandProcess.getInstance().setCandidateView((View) param.thisObject);
                    List<Object> list = (List<Object>) param.args[0];
                    List<Candidate> candidates = new ArrayList<Candidate>();
                    for (Object obj : list) {
                        String string = obj.toString();
                        String[] split = string.split(":");
                        String[] split2 = split[1].split(" \'");
                        String[] split3 = split2[1].split("\' ");
                        String candidate = split3[0].replace("\u200b", "");
                        Logger.v("candidate = " + candidate);
                        candidates.add(new Candidate(candidate));
                    }

                    List<Pair<TextView, ViewGroup>> textViewList = new ArrayList<>();
                    ViewGroup vg = (ViewGroup) mCandicateView;
                    for (int i = 0; i < vg.getChildCount(); i++) {
                        View childView = vg.getChildAt(i);
                        TextView textView = getFirstTextViewInHierarchy(childView);

//                        Logger.v("Ryan textView.text = " + textView.getText());
                        textView.setTag(vg);
                        View softKeyboardView = mCandicateView;
                        while (!"SoftKeyboardView".equals(softKeyboardView.getClass().getSimpleName())) {
                            softKeyboardView = (View) softKeyboardView.getParent();
                        }
                        textViewList.add(new Pair<>(textView, (ViewGroup) softKeyboardView));
                    }
                    CommandProcess.getInstance().setCandidates(candidates, textViewList);
                    Logger.v("print gb candidate" + candidates.toString());
                } catch (Exception e){
                    Logger.e("hack handleGBoardCandidate failed!");
                    Logger.e(e);
                }
            }
        });
    }

    private TextView getFirstTextViewInHierarchy(View view) {

        if (view instanceof TextView) {
            return (TextView) view;
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View result = getFirstTextViewInHierarchy(vg.getChildAt(i));
                if (result != null) return (TextView) result;
            }
        }

        return null;
    }

    //hack swift 7.1.4.9
    private void handleSwiftKeyCandidate(String candicateViewName) {
        Logger.v("handleSwiftKeyCandidate");
        Class<?> kClass = XposedHelpers.findClass(candicateViewName, this.mAppClassLoader);
        XposedBridge.hookAllConstructors(kClass, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.v("setCandidateView");
                CommandProcess.getInstance().setCandidateView((View) param.thisObject);
            }
        });
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("candicate ViewName ");
        stringBuilder.append(kClass.toString());
        Logger.e(stringBuilder.toString());
        try {
            XposedHelpers.findAndHookMethod(kClass, "a", new Object[]{"ead", new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("param.args[0]:");
                        stringBuilder.append(param.args[0].toString());
                        Logger.e(stringBuilder.toString());
                        Field field = null;
                        for (Field item : param.args[0].getClass().getDeclaredFields()) {
                            if (item.getName().equals("a")) {
                                field = item;
                            }
                        }
                        List<Object> candidates = (List) field.get(param.args[0]);
                        List<Candidate> candidateList = new ArrayList();
                        for (Object object : candidates) {
                            candidateList.add(new Candidate(XposedHelpers.findMethodBestMatch(object.getClass(), "getCorrectionSpanReplacementText", new Class[0]).invoke(object, new Object[0]).toString()));
                        }
                        CommandProcess.getInstance().setCandidates(candidateList, null);
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("print swift candidate： ");
                        stringBuilder2.append(candidateList.toString());
                        Logger.v(stringBuilder2.toString());
                    } catch (Exception e) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("hachSwiftCandidate error: ");
                        stringBuilder3.append(e.toString());
                        Logger.e(stringBuilder3.toString());
                    }
                }
            }});
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("hachSwiftCandidate error: ");
            stringBuilder2.append(e.toString());
            Logger.e(stringBuilder2.toString());
        }
    }

    private void handleFacemojiCandidate(String candicateViewName) {
        final Class<?> a = XposedHelpers.findClass("com.baidu.simeji.inputview.suggestions.MainSuggestionView", mAppClassLoader);
        XposedBridge.hookAllMethods(a, "setSuggestions", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Field field = XposedHelpers.findField(a, "e");
                int length = Array.getLength(field.get(param.thisObject));
                List<Candidate> candidates = new ArrayList<Candidate>();
                for (int i = 0; i< length; i++) {
                    Object obj = Array.get(field.get(param.thisObject), i);
                    TextView textView = (TextView) obj;
                    Logger.v("facemoji text = " + textView.getText().toString());
                    candidates.add(new Candidate(textView.getText().toString().replace("\"", "")));
                }

                CommandProcess.getInstance().setCandidates(candidates, null);
            }
        });
    }

    private void handleTypanyCandidate(String candicateViewName) {
        Logger.v("handleTypanyKeyCandidate candicateViewName = " + candicateViewName);
        final Class<?> candidateViewClazz = XposedHelpers.findClass(candicateViewName, mAppClassLoader);

        XposedBridge.hookAllMethods(candidateViewClazz, "setContent", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.v("handleTypanyKeyCandidate afterHookedMethod");
                List<Candidate> candidateList = new ArrayList<Candidate>();
                Field field = candidateViewClazz.getField("a");

                Button button = (Button) Array.get(field.get(param.thisObject), 0);
                candidateList.add(new Candidate(button.getText().toString()));
                Logger.v("handleTypanyKeyCandidate setContent 1 = " + button.getText());

                button = (Button) Array.get(field.get(param.thisObject), 1);
                candidateList.add(new Candidate(button.getText().toString()));
                Logger.v("handleTypanyKeyCandidate setContent 2 = " + button.getText());

                button = (Button) Array.get(field.get(param.thisObject), 2);
                candidateList.add(new Candidate(button.getText().toString()));
                Logger.v("handleTypanyKeyCandidate setContent 3 = " + button.getText());

                CommandProcess.getInstance().setCandidates(candidateList, null);
            }
        });
    }
}
