package com.kikatech.hackbasicmodule;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import com.kikatech.hackbasicmodule.bean.Candidate;
import com.kikatech.hackbasicmodule.bean.KeyInfo;
import com.kikatech.hackbasicmodule.bean.KeyboardConfig;
import com.kikatech.hackbasicmodule.task.HandlerScheduler;
import com.kikatech.hackbasicmodule.utils.Logger;
import com.kikatech.hackbasicmodule.utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xm on 17/4/13.
 */

public class CommandProcess extends AbstractIO implements CommandLife {

    final static long MIN_WAIT_TIME = 50;
    private Handler mHandler;
    private final String mCommandFilePath;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private InputMethodService mInputMethodService;
    private List<Candidate> mCandidateList = new ArrayList<>();
    private List<Pair<TextView, ViewGroup>> mCandidateTextViewList;
    private View mMainKeyboardView;
    private static WeakReference<KeyboardConfig> mKeyboardConfig;
    long mCandidateTime = 0;
    long mCandidateWaitTime = 0;
    final private Object mCandidateLock = new Object();

    private static CommandProcess instance;
    private static InputMethodService mService;
    private static View mCandidateView;
    private String mKeyboardType;

    private Method mHideWindowMethod;
    private Method mShowWindowMethod;
//    public TestCallback testCallback;

//  tonzh test
    private boolean candidateChanged = false;
    private boolean candidateMethodUsed = false;
    //    测试词典引擎响应时间
    private boolean testEngineTime = false;
    private PrintStream mPrintStream;

    public void openEngineTimeFiles(){
        if (testEngineTime){
            Context appContext = getAppContext();
            String outputPath = appContext.getFilesDir() + "/Word_Engine_time";
            mPrintStream = getPrintStream(outputPath);
        }
    }

    private void print(String format, Object... args) {
        String msg = String.format(format, args);
        mPrintStream.print(msg + "\n");
    }

    public List<View> mMainKeyboardViewList = new ArrayList<View>() {};

    // 用于没有写sd卡权限的方案
    public static boolean isGetCommand = false;


    private CommandProcess() {
        mCommandFilePath = null;
    }


    private CommandProcess(String commandFilePath) {
        mCommandFilePath = commandFilePath;
    }

    public static CommandProcess getInstance() {
        synchronized (CommandProcess.class) {
            if (instance == null) {
                synchronized (CommandProcess.class) {
                    instance = new CommandProcess();
                }
            }
        }
        return instance;
//        return instance.get();
    }

//    public void setTestCallback(TestCallback testCallback) {
//        this.testCallback = testCallback;
//    }

    @Override
    public void onCreate(InputMethodService service) {
        mService = service;
        mHandler = new Handler();
        mHandler.post(this);

        isGetCommand = false;
    }

    @Override
    public void run() {
        if (mHandler == null) {
            Logger.e("[CommandProcess-run] handler is null, handler must exist");
            return;
        }
        mHandler.postDelayed(this, 200);
        final File commandFile;
        if (mCommandFilePath == null) {
            commandFile = new File("/sdcard/input");
        } else {
            commandFile = new File(mCommandFilePath);
        }
        if (!commandFile.isFile()) {
            return;
        }
        if (isGetCommand) {
            return;
        }

        Logger.v("get command file");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(commandFile));
            for (String line; (line = br.readLine()) != null; ) {
                if (TextUtils.isEmpty(line)) continue;
                executeCommand(line);
            }
        } catch (FileNotFoundException e) {
            Logger.e(e);
        } catch (IOException e) {
            Logger.e(e);
        } finally {
            boolean delete = commandFile.delete();
            if (!delete) {
                Logger.e("无法删除文件:" + commandFile.getAbsolutePath());
                Logger.e("查看hack输入法是否有权限,尝试通过adb删除");
                Logger.i(Logger.TAG_PRINT, "delete file");
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
    }

    private void executeCommand(String line) {
        final String[] strings = StringUtils.splitToNonEmptyStrings(line, "\\s+");
        switch (strings[0]) {
            case "do_sentence":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new SentenceProcess(strings[1], strings[2], new Locale(strings[3]), false).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "resume_sentence":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new SentenceProcess(strings[1], strings[2], new Locale(strings[3]), true).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "do_sentencenew":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new SentenceNewProcess(strings[1], strings[2], new Locale(strings[3]), false).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "resume_sentencenew":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new SentenceNewProcess(strings[1], strings[2], new Locale(strings[3]), true).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "do_word":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new WordProcess(strings[1], strings[2], new Locale(strings[3])).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "do_wordnew":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new WordNewProcess(strings[1], strings[2], new Locale(strings[3])).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "do_wordnewIndic":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new WordNewIndicProcess(strings[1], strings[2], new Locale(strings[3])).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "do_crawlernew":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new NewCrawlerNewProcess(strings[1], strings[2], new Locale(strings[3]), false).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "do_crawler":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new NewCrawlerProcess(strings[1], strings[2], new Locale(strings[3]), false).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "resume_crawler":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new NewCrawlerProcess(strings[1], strings[2], new Locale(strings[3]), true).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "do_next":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new NextProcess(strings[1], strings[2], new Locale(strings[3]), false).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "do_nextnew":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new NextNewProcess(strings[1], strings[2], new Locale(strings[3]), false).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            case "resume_next":
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        new NextProcess(strings[1], strings[2], new Locale(strings[3]), true).run(new HandlerScheduler(mHandler));
                    }
                });
                break;
            default:
                Logger.e("unknown command " + strings[0]);
        }
    }


    @Override
    public List<Candidate> getCandidates() {
        synchronized (mCandidateLock) {
            return mCandidateList;
        }
    }

    public String getWrapperCandidate() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getCandidates().size(); i++) {
            if (i == 0) {
                sb.append('1');
            } else {
                sb.append('0');
            }
            sb.append(':');
            sb.append(getCandidates().get(i).getValue());
            sb.append(' ');
        }
        return sb.toString();
    }

    public int  getCountCandidate(){
        return mCandidateList.size();
    }

    public void setTouchoalCandidates(String candidate0){
        try {
            if (candidate0.length() == 0 ) {
                return;
            }
            Logger.v("set touchpalCandidate: " + candidate0);
    //        synchronized (mCandidateLock){
    //            if(mCandidateList.size() != -1){
    //                mCandidateLock.notifyAll();
    //            }
    //            mCandidateList.add(candidate0);


                mCandidateList.add(new Candidate(candidate0));
                Logger.v("count candidate: " + mCandidateList.size());
        }
        catch (Exception e){
            Logger.v("add candidate error: " + e.toString());
        }
    }

    public void clearCandidateList(){
        mCandidateList.clear();
    }
    @Override
    public void setCandidates(List<Candidate> candidates, List<Pair<TextView, ViewGroup>> candidateTextViews) {
        if (testEngineTime) {
            //    测试词典引擎响应时间
            Logger.e("Tonzh testEngineTime setCandidates: " + System.currentTimeMillis());
            print("Tonzh testEngineTime setCandidates: " + System.currentTimeMillis());
        }
        synchronized (mCandidateLock){
            if(!isTheSame(mCandidateList, candidates)){
                mCandidateTime = System.currentTimeMillis();
                mCandidateLock.notifyAll();
                candidateChanged = true;
            }
            mCandidateList = candidates;
            mCandidateTextViewList = candidateTextViews;
        }
        candidateMethodUsed = true;
    }

    private boolean isTheSame(List<Candidate> candidates1, List<Candidate> candidates2){
        if(candidates1 == candidates2){
            return true;
        }
        if(candidates1 != null && candidates2 != null){
            if(candidates1.size() != candidates2.size()){
                return false;
            }else{
                for(int i = 0; i < candidates1.size(); i++){
                    Candidate c1 = candidates1.get(i);
                    Candidate c2 = candidates2.get(i);
                    if(!(c1 != null && c1.equals(c2) || c1 == null && c2 == null)){
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public void waitCanditatesChange(){
        long startWait = System.currentTimeMillis();
        while (candidateChanged == false){
            try {
                Thread.sleep(MIN_WAIT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long thisWait = System.currentTimeMillis();
            if ((thisWait -startWait) > 1000){
                break;
            }
        }
        candidateChanged = false;
    }

    public void waitCanditatesMethodUsed(){
        long startWait = System.currentTimeMillis();
        while (candidateMethodUsed == false){
            try {
                Thread.sleep(MIN_WAIT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long thisWait = System.currentTimeMillis();
            if ((thisWait -startWait) > 1000){
                break;
            }
        }
        candidateMethodUsed = false;
    }

    public void waitCandidates(long time){
        mCandidateWaitTime = System.currentTimeMillis();
        try {
            Thread.sleep(MIN_WAIT_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (mCandidateLock){
            try {
                if(mCandidateWaitTime > mCandidateTime){
                    mCandidateLock.wait(time - MIN_WAIT_TIME);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void waitCandidates(long minTime, long time){
        mCandidateWaitTime = System.currentTimeMillis();
        try {
            Thread.sleep(minTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (mCandidateLock){
            try {
//                mCandidateWaitTime = System.currentTimeMillis();
//                mCandidateLock.wait(MIN_WAIT_TIME);
                if(mCandidateWaitTime > mCandidateTime){
                    mCandidateLock.wait(time - minTime);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void setCandidateView(View view) {
        mCandidateView = view;
    }

    @Override
    public InputMethodService getInputMethodService() {
        Logger.v("[CommandProcess-getInputMethodService] " + ((mInputMethodService == null) ? mInputMethodService.getClass().getSimpleName() : null));
        return mInputMethodService;
    }

    @Override
    public void setInputMethodService(InputMethodService service) {
        Logger.e("[CommandProcess-setInputMethodService] " + ((service == null) ? service.getClass().getSimpleName() : null));
        mInputMethodService = service;
    }

    @Override
    public void setMainKeyboardView(View view) {
        if (view == null) {
            Logger.e("don't found main-keyboard-view");
        } else {
            Logger.v("main-keyboard-view " + view.getClass().getSimpleName());
        }
        mMainKeyboardView = view;
        mMainKeyboardViewList.add(view);
        Log.e("tonzh set", mMainKeyboardView.toString());
    }

    public void resetMainKeyboardView(){
        Log.e("tonzh reset", "START resetMainKeyboardView");
        if (mMainKeyboardViewList.size() > 0) {
            for (int i = 0; i < mMainKeyboardViewList.size(); i++) {
                View v = mMainKeyboardViewList.get(i);
                if (v != null) {
                    Log.e("Tonzh reet : ", v.getParent() + "; " + v.getVisibility());
                    if (v.getParent() != null && v.getVisibility() == View.VISIBLE) {
                        mMainKeyboardView = v;
                    }
                }

            }
        }
    }

    public void resetMainKeyboardView(int count){
        if (mMainKeyboardView.getParent() == null || mMainKeyboardView.getVisibility() != View.VISIBLE) {
            resetMainKeyboardView();

            int idx = 0;
            Log.e("tonzh reset", "START resetMainKeyboardView");
            if (mMainKeyboardViewList.size() > 0) {
                for (int i = 0; i < mMainKeyboardViewList.size(); i++) {
                    View v = mMainKeyboardViewList.get(i);
                    if (v != null) {
                        Log.e("Tonzh reet : ", v.getParent() + "; " + v.getVisibility());
                        if (v.getParent() != null && v.getVisibility() == View.VISIBLE) {
                            idx += 1;
                        }
                    }
                    if (idx == count) {
                        mMainKeyboardView = v;
                    }
                }

            }
        }
    }

    public void setKeyboardConfig(KeyboardConfig config) {
        if (config == null) {
            Logger.e("don't found config");
        } else {
            Logger.v("setKeyboardConfig " + config.getClass().getSimpleName());
        }
        mKeyboardConfig = new WeakReference<KeyboardConfig>(config);
    }

    public void resetKeyboardviewtoTop() {
        Log.e("tonzh", "set top");
        mMainKeyboardView = mMainKeyboardViewList.get(0);
    }

    public boolean checkCharin(char character) {
        KeyInfo keyInfo = mKeyboardConfig.get().getKeyInfo(character);
        if (keyInfo == null)
        {
            Logger.e("can't find character : " + character);
            return false;
        }
        return true;
    }

    public void resetKeyboardviewtoSecond() {
        Log.e("tonzh", "set second");
        if (mMainKeyboardViewList.size() > 2)
        {
            mMainKeyboardView = mMainKeyboardViewList.get(2);
            Log.e("tonzh", "set second pass " + mMainKeyboardView.getParent());

            for (int i = 0; i < mMainKeyboardViewList.size(); i++)
            {
                Log.e("tonzh", "print mmList " + mMainKeyboardViewList.get(i).getParent());
            }
        }
    }
    @Override
    public void pressChar(final Character character) {
//        if (checkViChar(character)){
//            dealViWord(character.toString());
//            sleep(50);
//            return;
//        }

        if (character.equals(' ')) {
            pressSpace();
            return;
        }

        if (mMainKeyboardView == null) {
            Logger.e("[pressChar-%s] mMainKeyboardView is null", character);
            return;
        }

        if (mKeyboardConfig == null || mKeyboardConfig.get() == null) {
            Logger.e("[pressChar-%s] mKeyboardConfig is null", character);
            return;
        }
        Log.e("Tonzh: ", mMainKeyboardView.getParent() + "; " + mMainKeyboardView.getVisibility() + ";" + mMainKeyboardView.getId());
//        Log.e("Tonzh: ", mCandidateView.getParent() + "; " + mCandidateView.getVisibility());
        if ("gboard".equals(CommandProcess.getInstance().getKeyboardType())) {
            if (mMainKeyboardView.getParent() == null || mMainKeyboardView.getVisibility() != View.VISIBLE) {
                resetMainKeyboardView();
            }
        }
//        mMainKeyboardView = mMainKeyboardViewList.get(0);
        if (character == '\''){
            pressQuotation();
        }
        else if (character == '\"'){
            pressDoubleQuotation();
        }
        else {
            KeyInfo keyInfo = mKeyboardConfig.get().getKeyInfo(character);
            if (keyInfo == null) {
                Logger.e("can't find character : " + character);
                return;
            }

//            String symbols = "0123456789!@#$%&*()_+-=:;?";
//            if (symbols.indexOf(character) != -1) {
//                //pressSymbol();
//                Logger.i(keyInfo.getX() + ", " + keyInfo.getY());
//                Operations.touchView(mMainKeyboardView, "d", keyInfo.getX(), keyInfo.getY());
//                Operations.touchView(mMainKeyboardView, "u", keyInfo.getX(), keyInfo.getY());
//                //pressSymbol();
//            }
            else {
                Logger.i(keyInfo.getX() + ", " + keyInfo.getY());
                if (testEngineTime) {
                    //    测试词典引擎响应时间
                    Logger.e("Tonzh testEngineTime char down: " + System.currentTimeMillis());
                    print("Tonzh testEngineTime char down: " + System.currentTimeMillis());
                }
                Operations.touchView(mMainKeyboardView, "d", keyInfo.getX(), keyInfo.getY());
                Operations.touchView(mMainKeyboardView, "u", keyInfo.getX(), keyInfo.getY());
            }
        }
    }

    @Override
    public void pressSpace() {
        if (mMainKeyboardView == null) {
            Logger.e("[pressChar-space] mMainKeyboardView is null");
            return;
        }

        if ("gboard".equals(CommandProcess.getInstance().getKeyboardType())) {
            if (mMainKeyboardView.getParent() == null || mMainKeyboardView.getVisibility() != View.VISIBLE) {
                resetMainKeyboardView();
            }
        }

        if (mKeyboardConfig == null) {
            Logger.e("[pressChar-space] mKeyboardConfig is null");
            return;
        }
        KeyInfo spaceKey = mKeyboardConfig.get().getSpaceKey();
        if (spaceKey == null) {
            Logger.e("can't find spaceKey");
            return;
        }
        if (testEngineTime) {
            //    测试词典引擎响应时间
            Logger.e("Tonzh testEngineTime space down: " + System.currentTimeMillis());
            print("Tonzh testEngineTime space down: " + System.currentTimeMillis());
        }
        Logger.e("press space in mainView :" + mMainKeyboardView.toString());
        Operations.touchView(mMainKeyboardView, "d", spaceKey.getX(), spaceKey.getY());
        Operations.touchView(mMainKeyboardView, "u", spaceKey.getX(), spaceKey.getY());
    }

    @Override
    public  void pressQuotation() {
        if (mMainKeyboardView == null) {
            Logger.e("[pressChar-space] mMainKeyboardView is null");
            return;
        }

        if (mKeyboardConfig == null) {
            Logger.e("[pressChar-space] mKeyboardConfig is null");
            return;
        }
        Logger.v(mKeyboardConfig.toString());

        KeyInfo quotationKey = mKeyboardConfig.get().getQuotationKey();
        if (quotationKey == null) {
            Logger.e("can't find QuotationKey");
            return;
        }
        Logger.i("Xposed_hack_module_output input \"\'\" ");
        Logger.i("pressQuotation : x-%d, y-%d", quotationKey.getX(), quotationKey.getY());
        Operations.touchView(mMainKeyboardView, "d", quotationKey.getX(), quotationKey.getY());
        Operations.touchView(mMainKeyboardView, "u", quotationKey.getX(), quotationKey.getY());
    }

    @Override
    public  void pressDoubleQuotation() {
        if (mMainKeyboardView == null) {
            Logger.e("[pressChar-space] mMainKeyboardView is null");
            return;
        }

        if (mKeyboardConfig == null) {
            Logger.e("[pressChar-space] mKeyboardConfig is null");
            return;
        }
        Logger.v(mKeyboardConfig.toString());

        KeyInfo doublequotationKey = mKeyboardConfig.get().getDoubleQuotationKey();
        if (doublequotationKey == null) {
            Logger.e("can't find doubleQuotationKey");
            return;
        }

        Logger.i("pressDoubleQuotation : x-%d, y-%d", doublequotationKey.getX(), doublequotationKey.getY());
        Operations.touchView(mMainKeyboardView, "d", doublequotationKey.getX(), doublequotationKey.getY());
        Operations.touchView(mMainKeyboardView, "u", doublequotationKey.getX(), doublequotationKey.getY());
    }

    @Override
    public void pressEnter() {
        if (mMainKeyboardView == null) {
            Logger.e("[pressChar-space] mMainKeyboardView is null");
            return;
        }

        if ("gboard".equals(CommandProcess.getInstance().getKeyboardType())) {
            if (mMainKeyboardView.getParent() == null || mMainKeyboardView.getVisibility() != View.VISIBLE) {
                resetMainKeyboardView();
            }
        }

        if (mKeyboardConfig == null) {
            Logger.e("[pressChar-space] mKeyboardConfig is null");
            return;
        }
        Logger.v(mKeyboardConfig.toString());

        KeyInfo enterKey = mKeyboardConfig.get().getEnterKey();
        if (enterKey == null) {
            Logger.e("can't find enterKey");
            return;
        }
        Logger.i("pressEnter : x-%d, y-%d", enterKey.getX(), enterKey.getY());
        Operations.touchView(mMainKeyboardView, "d", enterKey.getX(), enterKey.getY());
        Operations.touchView(mMainKeyboardView, "u", enterKey.getX(), enterKey.getY());

    }

    @Override
    public void pressSymbol() {
        if (mMainKeyboardView == null) {
            Logger.e("[pressChar-space] mMainKeyboardView is null");
            return;
        }

        if ("gboard".equals(CommandProcess.getInstance().getKeyboardType())) {
            if (mMainKeyboardView.getParent() == null || mMainKeyboardView.getVisibility() != View.VISIBLE) {
                resetMainKeyboardView();
            }
        }

        if (mKeyboardConfig == null) {
            Logger.e("[pressChar-space] mKeyboardConfig is null");
            return;
        }
        Logger.v(mKeyboardConfig.toString());

        KeyInfo symbolKey = mKeyboardConfig.get().getSymbolKey();
        if (symbolKey == null) {
            Logger.e("can't find symbolKey");
            return;
        }
        Logger.i("pressSymbol : x-%d, y-%d", symbolKey.getX(), symbolKey.getY());

        Operations.touchView(mMainKeyboardView, "d", symbolKey.getX(), symbolKey.getY());
        Operations.touchView(mMainKeyboardView, "u", symbolKey.getX(), symbolKey.getY());
    }

    @Override
    public void pressDelete() {
        if (mMainKeyboardView == null) {
            Logger.e("[pressChar-delete] mMainKeyboardView is null");
            return;
        }

        if (mKeyboardConfig == null) {
            Logger.e("[pressChar-delete] mKeyboardConfig is null");
            return;
        }
        Logger.v(mKeyboardConfig.toString());

        KeyInfo delKey = mKeyboardConfig.get().getDeleteKey();
        if (delKey == null) {
            Logger.e("can't find deleteKey");
            return;
        }
        Logger.i("pressDelete : x-%d, y-%d", delKey.getX(), delKey.getY());
        Operations.touchView(mMainKeyboardView, "d", delKey.getX(), delKey.getY());
        Operations.touchView(mMainKeyboardView, "u", delKey.getX(), delKey.getY());
    }

    @Override
    public void pressShift() {
        if (mMainKeyboardView == null) {
            Logger.e("[pressChar-space] mMainKeyboardView is null");
            return;
        }

        if (mKeyboardConfig == null) {
            Logger.e("[pressChar-space] mKeyboardConfig is null");
            return;
        }
        KeyInfo shiftKey = mKeyboardConfig.get().getShiftKey();
        if (shiftKey == null) {
            Logger.e("can't find shiftKey");
            return;
        }
        Operations.touchView(mMainKeyboardView, "d", shiftKey.getX(), shiftKey.getY());
        Operations.touchView(mMainKeyboardView, "u", shiftKey.getX(), shiftKey.getY());
    }

    @Override
    public void onDestory() {
        mHandler.removeCallbacks(this);
        mHandler = null;
        mInputMethodService = null;
    }

    public Context getAppContext() {
        return mService;
    }

    public void setKeyboardType(String type) {
        mKeyboardType = type;
    }

    public String getKeyboardType() {
        return mKeyboardType;
    }

    public void selectCandidate(final String candidateStr) {
        if (mCandidateTextViewList == null) {
            return;
        }

        boolean isFound = false;
        for (Pair pair : mCandidateTextViewList) {
            TextView textView = (TextView) pair.first;
            ViewGroup viewGroup = (ViewGroup) pair.second;
            if (candidateStr.equals(textView.getText().toString())) {
                int[] pos = new int[2];
                textView.getLocationInWindow(pos);
                Operations.touchView(viewGroup, "d", pos[0] + textView.getWidth() / 2, textView.getHeight() / 2);
                Operations.touchView(viewGroup, "u", pos[0] + textView.getWidth() / 2, textView.getHeight() / 2);
                isFound = true;
                break;
            }
        }

        if (!isFound) {
            for (Pair pair : mCandidateTextViewList) {
                TextView textView = (TextView) pair.first;
                ViewGroup viewGroup = (ViewGroup) pair.second;
                if (candidateStr.toLowerCase().equals(textView.getText().toString().toLowerCase())) {
                    int[] pos = new int[2];
                    textView.getLocationInWindow(pos);
                    Operations.touchView(viewGroup, "d", pos[0] + textView.getWidth() / 2, textView.getHeight() / 2);
                    Operations.touchView(viewGroup, "u", pos[0] + textView.getWidth() / 2, textView.getHeight() / 2);
                    isFound = true;
                    break;
                }
            }
        }

        if (!isFound) {
            pressSpace();
        }
    }

    public void selectCandidate(final int position) {
        if (mCandidateView == null) {
            Logger.e("selectCandidate view is null");
            return;
        }
        if ("gboard".equals(getKeyboardType()) || "touchpal".equals(getKeyboardType())) {
            Logger.i("%s tap space, position %d", getKeyboardType(), position);
            CommandProcess.getInstance().pressSpace();
        } else {
            dispathTouchEvent(position, mCandidateView);
//            CommandProcess.getInstance().pressSpace();
        }
    }

    private void dispathTouchEvent(final int position, final View view) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int kSize = 3;
                //直接设置候选词位置有三个，否则swift数字上屏有问题
                //Math.min(3, mCandidateList.size());
//                float x = (view.getWidth() / kSize) * (.5f + position);
                int x =  (int) ((view.getWidth() / kSize) * (.5f + position));
                int y = (int) (view.getHeight() * .5f);
                Logger.e("select: x " + x + "；Select: y  " + view.getHeight() * .5f);
                Logger.e("getTop: " + view.getTop() * 1.0f );
                Logger.e("getBotton: " + view.getBottom() + "; getLeft: " + view.getLeft() + "; getRight: " + view.getRight());
//                MotionEvent downEvent = MotionEvent.obtain(
//                        System.currentTimeMillis(),
//                        System.currentTimeMillis() * 10,
//                        MotionEvent.ACTION_DOWN,
//                        x,
//                        view.getHeight() * .5f,
//                        0);
//                downEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
//                view.dispatchTouchEvent(downEvent);
//                MotionEvent upEvent = MotionEvent.obtain(
//                        System.currentTimeMillis(),
//                        System.currentTimeMillis() * 10,
//                        MotionEvent.ACTION_UP,
//                        x,
//                        view.getHeight() * .5f,
//                        0);
////                upEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
////                view.dispatchTouchEvent(upEvent);

                Operations.touchView(view, "d", x, y);
                Operations.touchView(view, "u", x, y);
            }
        });
    }

    public void setHideWindowMethod(Method method) {
        mHideWindowMethod = method;
    }
    public void setShowWindowMethod(Method method) {
        mShowWindowMethod = method;
    }

    public void invokeHideWindow() {
        try {
            mHideWindowMethod.invoke(mService);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    public void invokeShowWindow() {
        try {
            mShowWindowMethod.invoke(mService, false);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public InputMethodService getService() {
        if (mService != null) {
            return mService;
        }
        return null;
    }

    public InputConnection getInputConnection() {
        if (mService != null) {
            return mService.getCurrentInputConnection();
        }
        return null;
    }

    private final Map<String,String> viDict = new HashMap<String, String>() {
        {
            put("ă", "aw");
            put("ơ", "ow");
            put("ư", "uw");
            put("â", "aa");
            put("đ", "dd");
            put("ê", "ee");
            put("ô", "oo");
            put("à", "af");
            put("ằ", "awf");
            put("ầ", "aaf");
            put("è", "ef");
            put("ề", "eef");
            put("ì", "if");
            put("ò", "of");
            put("ồ", "oof");
            put("ờ", "owf");
            put("ù", "uf");
            put("ừ", "uwf");
            put("ỳ", "yf");
            put("á", "as");
            put("ắ", "aws");
            put("ấ", "aas");
            put("é", "es");
            put("ế", "ees");
            put("í", "is");
            put("ó", "os");
            put("ố", "oos");
            put("ớ", "ows");
            put("ú", "us");
            put("ứ", "uws");
            put("ý", "ys");
            put("ả", "ar");
            put("ẳ", "awr");
            put("ẩ", "aar");
            put("ẻ", "er");
            put("ể", "eer");
            put("ỉ", "ir");
            put("ỏ", "or");
            put("ổ", "oor");
            put("ở", "owr");
            put("ủ", "ur");
            put("ử", "uwr");
            put("ỷ", "yr");
            put("ã", "ax");
            put("ẵ", "awx");
            put("ẫ", "aax");
            put("ẽ", "ex");
            put("ễ", "eex");
            put("ĩ", "ix");
            put("ỗ", "ox");
            put("ỡ", "oox");
            put("ở", "owx");
            put("ũ", "ux");
            put("ữ", "uwx");
            put("ỹ", "yx");
            put("ạ", "aj");
            put("ặ", "awj");
            put("ậ", "aaj");
            put("ẹ", "ej");
            put("ệ", "eej");
            put("ị", "ij");
            put("ọ", "oj");
            put("ộ", "ooj");
            put("ợ", "owj");
            put("ụ", "uj");
            put("ự", "uwj");
            put("ỵ", "yj");
        }
    };

    public boolean checkViChar(Character c){
        if (viDict.containsKey(c.toString())){
            return true;
        }
        return false;
    }

    public String checkViWord(String word) {

        String result = "";
        for (final Character c : word.toCharArray()){
            if (viDict.containsKey(c.toString())){
                result += viDict.get(c.toString());
            }
            else {
                result += c.toString();
            }
        }
        return result;
    }

    public void dealViWord(String word) {

        String result = "";
        for (final Character c : word.toCharArray()){
            if (viDict.containsKey(c.toString())){
                result += viDict.get(c.toString());
            }
            else {
                result += c.toString();
            }
        }
        for (final Character c: result.toCharArray()){
            pressChar(c);
            sleep(30);
        }
        return;
    }

    private String thSecondPage = "+๑๒๓๔ู฿๕๖๗๘๙๐“ฎฑธํ๊ณฯญฐฤฆฏโฌ็๋ษศซฅ()ฉฮฺ์?ฒฬฦḯḰḱḲḳḴḵḶḷḸḹḺḻḼḽḾḿṀṁṂṃṄṅṆṇṈṉṊṋṌṍṎṏṐṑṒṓṔṕṖ".replace("\\u0020", "");
    private String mySecendPage_gb = "ဍဣဤ၌ဥ၍ဿဏဗှီ္ွံဲဒဓဇဌဃဠဎဉဦဧ".replace("\\u0020", "");
    private String mySecendPage_swft = "ဈဢဎဣဤ၌ဉဿဏဧဗှီ၍ါံဲဦဨဳဓဇဌဃဠဝဴ".replace("\\u0020", "");
    private String mySecendPage_tp = "ဍဎဋၓၔၕရဈဝဣ၎ဤ၌ဥ၍ဿဏဗှီ္ွံဲဒဓဂဇဌဃဠဟဉ၏".replace("\\u0020", "");
    private String mySecendPage_tp_mj = "ဍ႑ဋ၎ဪဩရဂဧဟဈ႒ဎဣဤႆ၌၌ဥဏဗွီၤြံဲုဓဒဇဌဃဠဝ၏".replace("\\u0020", "");

    private String urdoSecendPage_tp = "ْؤٰڑٹِأئۃۂآصڈًغھضخُذژثظَںّ";
    private String bengaliSecondPage_tp = "ৰৱৠৡৢৣৄৗৎঃং়ঁ";
    private String gujaratiSecondPage_tp = "₹ૐ૱઼ઁ";
    public String charsSecondPage = thSecondPage + mySecendPage_tp_mj + urdoSecendPage_tp + bengaliSecondPage_tp + gujaratiSecondPage_tp;
}
