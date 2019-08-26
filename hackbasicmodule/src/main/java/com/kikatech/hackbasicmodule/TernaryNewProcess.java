package com.kikatech.hackbasicmodule;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputConnection;

import com.kikatech.hackbasicmodule.bean.Candidate;
import com.kikatech.hackbasicmodule.task.IScheduler;
import com.kikatech.hackbasicmodule.task.SynTask;
import com.kikatech.hackbasicmodule.utils.IOUtils;
import com.kikatech.hackbasicmodule.utils.Logger;
import com.kikatech.hackbasicmodule.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xm on 17/4/13.
 */
public class TernaryNewProcess extends AbstractIO implements IProcess {

    private final static String SAVE_FILE = "/ternary_save.out";

    private Context mContext;

    private final boolean mIsResume;
    private final String mInputPath;
    private final String mOutputPath;
    private final Locale mLocale;
    private IScheduler mScheduler;

    private LinkedList<Word> mProcessing = new LinkedList<Word>();
    private Set<String> mWillBeProcessed = new HashSet<>();
    private Set<String> mProcessed = new HashSet<String>();
    private Set<String> mProcessedChar = new HashSet<String>();

    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private PrintStream mPrintStream;
    private BufferedReader mReader;

    public TernaryNewProcess(@NonNull String inputPath, @NonNull String outputPath, @NonNull Locale locale, boolean isResume) {
        mContext = CommandProcess.getInstance().getAppContext();
        mInputPath = inputPath;
        mOutputPath = outputPath;
        mLocale = locale;
        mIsResume = isResume;
    }

    @Override
    public void run(@NonNull IScheduler scheduler) {
        mScheduler = scheduler;

        Logger.v("[Step 1] : open input file");
        readInput();

        Logger.v("[Step 2] : open output file");
        String outputPath = mContext.getFilesDir() + "/ternary_output";
        mPrintStream = getPrintStream(outputPath);
        if (mPrintStream == null) {
            Logger.e("open output file failed");
            return;
        }
        try {
            internalRun();
        } catch (IOException e) {
            Logger.e(e);
        } finally {
            close();
        }
        CommandProcess.isGetCommand = false;
        Logger.i(Logger.TAG+"_output","finished word task");
    }

    private void internalRun() throws IOException {
        for (String line; (line = mReader.readLine()) != null; ) {
            if (!TextUtils.isEmpty(line)) {
                runLine(line);
            }
        }
    }

    private void readInput(){
        if(mIsResume){
            onRestoreState();
        }else{
            mReader = getReader(mInputPath);
            if (mReader == null) {
                Logger.e("open input file failed");
                return;
            }
        }
    }

    private void runLine(String line) {
        Logger.v("==========================Run line=========================");
        final String[] parts = line.split(" ");
        if (parts.length != 2) {
            Logger.e("must be 2 words per line: '" + line + '\'');
            return;
        }

        final InputConnection connection = CommandProcess.getInstance().getInputConnection();
        if (connection == null) {
            Logger.e("Input connection == null");
        }

        final String word1 = parts[0];
        final String word2 = parts[1];

        if (!TextUtils.isEmpty(line)) {
            final String[] mWords = StringUtils.splitToNonEmptyStrings(line, "\\s+");
            for (final String word : mWords) {
                new SynTask(10) {
                    @Override
                    public void run() {
                        connection.commitText(word, 1);
                    }
                }.synRun(mScheduler);
                CommandProcess.getInstance().waitCanditatesMethodUsed();
                new SynTask(10) {
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressSpace();
                    }
                }.synRun(mScheduler);
                CommandProcess.getInstance().waitCanditatesMethodUsed();
            }
        }

        CommandProcess.getInstance().waitCandidates(100);
        List<Candidate> lastCandidates = CommandProcess.getInstance().getCandidates();
        if (lastCandidates != null) {
            //Logger.e(word + ":" + "----获取候选词: 为空 需要查询一下");
            print("word1: %s, word2: %s, candidate: %s\n", word1, word2,
                    CommandProcess.getInstance().getWrapperCandidate());
        }
        sleep(100);
        new SynTask(30) { // 按键
            @Override
            public void run() {
                connection.deleteSurroundingText(1000,1000);
            }
        }.synRun(mScheduler);
        CommandProcess.getInstance().waitCanditatesMethodUsed();
        sleep(30);
    }

    private void resetKeyboard() {
        new SynTask(200) {
            @Override
            public void run() {
                CommandProcess.getInstance().invokeHideWindow();
            }
        }.synRun(mScheduler);
        new SynTask(200) {
            @Override
            public void run() {
                CommandProcess.getInstance().invokeShowWindow();
            }
        }.synRun(mScheduler);
    }

    private void onRestoreState(){
        BufferedReader reader = getReader(mContext.getFilesDir() + SAVE_FILE);
        if (reader == null) {
            Logger.e("onRestoreState failed");
            return;
        }
        try{
            String line = reader.readLine();
            if(line == null){
                return;
            }
            if(!TextUtils.isEmpty(line)){
                String[] array = line.split("&&&");
                for(String word : array){
                    String[] item = word.split("\\|");
                    if(item.length > 1){
                        mProcessing.add(new Word(item[0], Boolean.parseBoolean(item[1])));
                        mWillBeProcessed.add(item[0]);
                    }else{
                        mProcessing.add(new Word(item[0], false));
                        mWillBeProcessed.add(item[0]);
                    }
                }
            }
            Log.e("===================", "Processing size = " + mProcessing.size());
            line = reader.readLine();
            if(line == null){
                return;
            }
            line = reader.readLine();
            if(line == null){
                return;
            }
            if(!TextUtils.isEmpty(line)){
                String[] array = line.split("&&&");
                for(String word : array){
                    String lower = word.toLowerCase();
                    mProcessed.add(lower);
                    for(int i = 0; i < word.length(); i++){
                        mProcessedChar.add(lower.substring(0, i + 1));
                    }
                }
            }
            Log.e("===================", "Processed size = " + mProcessed.size());
        }catch (Exception e){
            Logger.e("open input file failed");
        }

    }

    public static class Word {
        String mWord;
        boolean mIsInput;

        public Word(String word, boolean isInput){
            mWord = word;
            mIsInput = isInput;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Word) {
                if (TextUtils.isEmpty(mWord)) {
                    return false;
                }
                return mWord.equals(((Word) obj).mWord);
            }
            return false;
        }
    }

    private void print(String format, Object... args) {
        String msg = String.format(format, args);
        Logger.i(Logger.TAG + "_output", msg);
        mPrintStream.print(msg);
    }
}
