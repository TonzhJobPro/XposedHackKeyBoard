package com.kikatech.hackbasicmodule;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.kikatech.hackbasicmodule.bean.Candidate;
import com.kikatech.hackbasicmodule.task.IScheduler;
import com.kikatech.hackbasicmodule.task.SynTask;
import com.kikatech.hackbasicmodule.utils.IOUtils;
import com.kikatech.hackbasicmodule.utils.Logger;

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
public class CrawlerProcess extends AbstractIO implements IProcess {

    private final static String SAVE_FILE = "/crawler_save.out";

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

    public CrawlerProcess(@NonNull String inputPath, @NonNull String outputPath, @NonNull Locale locale, boolean isResume) {
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
        String outputPath = mContext.getFilesDir() + "/crawler_output";
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

    private void readInput(){
        if(mIsResume){
            onRestoreState();
        }else{
            BufferedReader reader = getReader(mInputPath);
            if (reader == null) {
                Logger.e("open input file failed");
                return;
            }
            try{
                synchronized (this){
                    for (String line; (line = reader.readLine()) != null; ) {
                        if(!TextUtils.isEmpty(line) && !TextUtils.isEmpty(line.trim())){
                            line = line.trim();
                            if(!line.contains(" ") && line.length() <= 5){
                                mProcessing.addLast(new Word(line.trim(), true));
                                mWillBeProcessed.add(line.trim());
                            }
                        }
                    }
                }
            }catch (Exception e){
                Logger.e("open input file failed");
            }
        }
    }

    private void internalRun() throws IOException {
        int index = 0;
        for(;mProcessing.size() > 0;) {
            index++;
            Word w = mProcessing.pollFirst();
            if(!TextUtils.isEmpty(w.mWord)){
                if(!mProcessed.contains(w.mWord.toLowerCase())){
                    mProcessed.add(w.mWord.toLowerCase());
                    runLine(w);
                }
            }
            if(index % 50 == 0){
                final LinkedList<Word> processing = new LinkedList<Word>();
                final Set<String> processed = new HashSet<String>();
                for(Word e : mProcessing){
                    processing.add(e);
                }
                for(String e : mProcessed){
                    processed.add(e);
                }
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onSaveState(processing, processed);
                    }
                });
            }
        }
    }

    private void runLine(Word word) {
        print("word: %s input: %s \n", word.mWord, String.valueOf(word.mIsInput));
        if (!runWord(word)) {
            print("Retry %s 1st time.\n", word.mIsInput);
            if (!runWord(word)) {
                print("Retry %s 2nd time.\n", word.mIsInput);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!runWord(word)) {
                    print("Retry %s 3rd time.\n", word.mIsInput);
                    runWord(word);
                }
            }
        }
        Logger.i("press enter start");
        CommandProcess.getInstance().pressEnter();
//        CommandProcess.getInstance().pressSpace();
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

    private int mWordCount = 0;
    private boolean runWord(final Word w) {
        mWordCount++;
        if (mWordCount % 300 == 0
                && "touchpal".equals(CommandProcess.getInstance().getKeyboardType())) {
            resetKeyboard();
        }
        int index = 0;
        for (final Character c : w.mWord.toCharArray()) { // 逐个输入字符
            index++;
            if(index == 1){
                new SynTask(200) { // 按键
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressChar(c);
                    }
                }.synRun(mScheduler);
            }else{
                new SynTask(30) { // 按键
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressChar(c);
                    }
                }.synRun(mScheduler);
            }
//            if (!mIsInputOk) {
//                Logger.v("Ryan input not ok.");
//                return true;
//            }
            String chars = w.mWord.substring(0, index).toLowerCase();
            if(!mProcessedChar.contains(chars)){
                mProcessedChar.add(chars);
//                new SynTask(200) { // 获取候选词
//                    @Override
//                    public void run() {
//                        List<Candidate> lastCandidates = CommandProcess.getInstance().getCandidates();
//                        if (lastCandidates == null) {
//                            //Logger.e(word + ":" + "----获取候选词: 为空 需要查询一下");
//                            return;
//                        }
//                        for (int i = 0; i < lastCandidates.size(); i++) {
//                            String candidateValue = lastCandidates.get(i).getValue();
//                            if(!TextUtils.isEmpty(candidateValue) && !TextUtils.isEmpty(candidateValue.trim())){
//                                candidateValue = candidateValue.trim();
//                                if(!candidateValue.contains(" ") && candidateValue.length() <= 5 && !w.mWord.equalsIgnoreCase(candidateValue)){
//                                    mProcessing.addFirst(new Word(candidateValue, false));
//                                    print(candidateValue);
//                                    print("\n");
//                                }
//                            }
//                        }
//                    }
//                }.synRun(mScheduler);
                CommandProcess.getInstance().waitCandidates(1000);
                List<Candidate> lastCandidates = CommandProcess.getInstance().getCandidates();
                if (lastCandidates == null || lastCandidates.size() == 0
                        && "touchpal".equals(CommandProcess.getInstance().getKeyboardType())) {
                    mProcessedChar.remove(chars);
                    resetKeyboard();
                    return false;
                }
                if (lastCandidates != null) {
                    for (int i = 0; i < lastCandidates.size(); i++) {
                        String candidateValue = lastCandidates.get(i).getValue();
                        if(!TextUtils.isEmpty(candidateValue) && !TextUtils.isEmpty(candidateValue.trim())){
                            candidateValue = candidateValue.trim();
                            if(!candidateValue.contains(" ") && !chars.equalsIgnoreCase(candidateValue)
                                    && !candidateValue.contains("-") && candidateValue.length() < 20
                                    && !mProcessed.contains(candidateValue.toLowerCase())
                                    && !mWillBeProcessed.contains(candidateValue.toLowerCase())) {
                                mWillBeProcessed.add(candidateValue.toLowerCase());
                                Word newWord = new Word(candidateValue, false);
                                    // hashset, hashcode of string
                                mProcessing.addFirst(newWord);

                                print(candidateValue);
                                print("\n");
                            }
                        }
                    }
                }
            }
        }
        // fetch bigram word
        CommandProcess.getInstance().pressSpace();
        CommandProcess.getInstance().waitCandidates(200, 300);
        List<Candidate> lastCandidates = CommandProcess.getInstance().getCandidates();
        if (lastCandidates != null) {
            //Logger.e(word + ":" + "----获取候选词: 为空 需要查询一下");
            print("word: %s, input: %s, candidate: %s\n", w.mWord, String.valueOf(w.mIsInput),
                    CommandProcess.getInstance().getWrapperCandidate());
        }
//        new SynTask(200) { // 获取候选词
//            @Override
//            public void run() {
//                List<Candidate> lastCandidates = CommandProcess.getInstance().getCandidates();
//                if (lastCandidates == null) {
//                    //Logger.e(word + ":" + "----获取候选词: 为空 需要查询一下");
//                    return;
//                }
//                print("word: %s, input: %s, candidate: %s\n", w.mWord, String.valueOf(w.mIsInput),
//                        CommandProcess.getInstance().getWrapperCandidate());
//            }
//        }.synRun(mScheduler);
        return true;
    }

    private void onSaveState(LinkedList<Word> processing, Set<String> processed){
        String outputPath = mContext.getFilesDir() + SAVE_FILE;
        PrintStream ps = IOUtils.getPrintStream(outputPath, false);
        StringBuffer sb = new StringBuffer();
        for(Word word : processing){
            if(sb.length() > 0){
                sb.append("&&&");
            }
            if(!TextUtils.isEmpty(word.mWord)){
                sb.append(word.mWord);
                sb.append("|");
                sb.append(String.valueOf(word.mIsInput));
            }
        }
        ps.print(sb.toString());
        ps.println();
        ps.print("==================================================");
        ps.println();
        sb = new StringBuffer();
        for(String word : processed){
            if(sb.length() > 0){
                sb.append("&&&");
            }
            if(!TextUtils.isEmpty(word)){
                sb.append(word);
            }
        }
        ps.print(sb.toString());
        IOUtils.close(ps);
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

    private boolean runWord_ZH(final Word w) {
        mWordCount++;
        if (mWordCount % 300 == 0
                && "touchpal".equals(CommandProcess.getInstance().getKeyboardType())) {
            resetKeyboard();
        }
        int index = 0;
        for (final Character c : w.mWord.toCharArray()) { // 逐个输入字符
            index++;
            if(index == 1){
                new SynTask(200) { // 按键
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressChar(c);
                    }
                }.synRun(mScheduler);
            }else{
                new SynTask(30) { // 按键
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressChar(c);
                    }
                }.synRun(mScheduler);
            }

            String chars = w.mWord.substring(0, index).toLowerCase();
            if(!mProcessedChar.contains(chars)){
                mProcessedChar.add(chars);

                CommandProcess.getInstance().waitCandidates(1000);
                List<Candidate> lastCandidates = CommandProcess.getInstance().getCandidates();
                if (lastCandidates == null || lastCandidates.size() == 0
                        && "touchpal".equals(CommandProcess.getInstance().getKeyboardType())) {
                    mProcessedChar.remove(chars);
                    resetKeyboard();
                    return false;
                }
                if (lastCandidates != null) {
                    for (int i = 0; i < lastCandidates.size(); i++) {
                        String candidateValue = lastCandidates.get(i).getValue();
                        if(!TextUtils.isEmpty(candidateValue) && !TextUtils.isEmpty(candidateValue.trim())){
                            candidateValue = candidateValue.trim();
                            if(!candidateValue.contains(" ") && !chars.equalsIgnoreCase(candidateValue)
                                    && !candidateValue.contains("-") && candidateValue.length() < 20
                                    && !mProcessed.contains(candidateValue.toLowerCase())
                                    && !mWillBeProcessed.contains(candidateValue.toLowerCase())) {
                                mWillBeProcessed.add(candidateValue.toLowerCase());
                                Word newWord = new Word(candidateValue, false);
                                // hashset, hashcode of string
                                mProcessing.addFirst(newWord);

                                print(candidateValue);
                                print("\n");
                            }
                        }
                    }
                }
            }
        }
        // fetch bigram word
        CommandProcess.getInstance().pressSpace();
        CommandProcess.getInstance().waitCandidates(200, 300);
        List<Candidate> lastCandidates = CommandProcess.getInstance().getCandidates();
        if (lastCandidates != null) {
            //Logger.e(word + ":" + "----获取候选词: 为空 需要查询一下");
            print("word: %s, input: %s, candidate: %s\n", w.mWord, String.valueOf(w.mIsInput),
                    CommandProcess.getInstance().getWrapperCandidate());
        }

        CommandProcess.getInstance().pressEnter();
        return true;
    }
}
