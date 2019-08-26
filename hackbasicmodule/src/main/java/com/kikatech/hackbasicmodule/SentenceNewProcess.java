package com.kikatech.hackbasicmodule;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputConnection;

import com.kikatech.hackbasicmodule.bean.Candidate;
import com.kikatech.hackbasicmodule.task.IScheduler;
import com.kikatech.hackbasicmodule.task.SynTask;
import com.kikatech.hackbasicmodule.utils.Logger;
import com.kikatech.hackbasicmodule.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xm on 17/4/13.
 */

public class SentenceNewProcess extends AbstractIO implements IProcess {

    private final static String SAVE_FILE = "/sentence_save.out";

    private final static String TAG = "SentenceProcess";

    private static final int NOT_MATCH = 0;
    private static final int EXACTLY_MATCH = 1;
    private static final int IGNORE_CASE_MATCH = 2;

    private Context mContext;
    private final boolean mIsResume;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private final String mInputPath;
    private final String mOutputPath;
    private final Locale mLocale;
    private IScheduler mScheduler;
    private BufferedReader mReader;
    private PrintStream mPrintStream;
    private boolean gotSuggestiongFromBigram;
    private int foundIndex = -1;

    private int mWordCount = 0;
    private InputConnection inputConnection;
    private boolean noSpacelang = false;
    private String noSpaceline = "";

    public SentenceNewProcess(@NonNull String inputPath, @NonNull String outputPath, @NonNull Locale locale, boolean isResume) {
        mInputPath = inputPath;
        mOutputPath = outputPath;
        mLocale = locale;
        mIsResume = isResume;
        Log.e(TAG, "SentenceProcess startup " + mIsResume);
    }

    @Override
    public void run(@NonNull IScheduler scheduler) {
        mScheduler = scheduler;

        Logger.v("[Step 1] : open input file");
        readInput();

        Logger.v("[Step 2] : open output file");
        mContext = CommandProcess.getInstance().getAppContext().getApplicationContext();
        String outputPath = mContext.getFilesDir() + "/sentence_output";
        mPrintStream = getPrintStream(outputPath);
        if (mPrintStream == null) {
            Logger.e("open output file failed");
            return;
        }
        try {
            Logger.v("start internalRun");
            internalRun();
        } catch (IOException e) {
            Logger.e(e);
        } finally {
            close();
        }
        CommandProcess.isGetCommand = false;
        new SynTask(20) {
            @Override
            public void run() {
                Logger.i(Logger.TAG + "_output", "finished sentence task");
            }
        }.synRun(mScheduler);
    }

    private void readInput() {
        mReader = getReader(mInputPath);
        if (mReader == null) {
            Logger.e("open input file failed");
            return;
        }
    }

    private void internalRun() throws IOException {
        Logger.v("run in internalRun");
        for (String line; (line = mReader.readLine()) != null; ) {
            runLine(line);
        }
        Logger.v("finish internalRun");
    }


    private boolean runLine(String line) {
        //tonzh
        noSpaceline = "";
        inputConnection = CommandProcess.getInstance().getInputConnection();
        print("SENTENCE: %s\n", line);
        for (String word : StringUtils.splitToNonEmptyStrings(line, "\\s+")) {
            print("WORD: %s\n", word);
            runWord(word);
//            inputConnection.commitText(word,1);
            new SynTask(50) {
                @Override
                public void run() {
                    CommandProcess.getInstance().pressSpace();
                }
            }.synRun(mScheduler);
//            CommandProcess.getInstance().waitCandidates(300);
        }

        Logger.i("Log current sentences.");
        new SynTask(20) {
            @Override
            public void run() {
                if (inputConnection != null) {
                    CharSequence output = inputConnection.getTextBeforeCursor(1000, 0);
                    print("SENTENCE OUTPUT: %s\n", output);
                }
            }
        }.synRun(mScheduler);

        Logger.i("press enter start");
//        new SynTask(20) {
//            @Override
//            public void run() {
//                CommandProcess.getInstance().pressEnter();
//            }
//        }.synRun(mScheduler);
//        tonzh fix pressEnter suggestion word still on
        Logger.i("press enter start");
        new SynTask(20) {
            @Override
            public void run() {
                inputConnection.deleteSurroundingText(1000, 1000);
            }
        }.synRun(mScheduler);
        return true;
    }

    private boolean runWord(final String word) {
        Logger.v("------------- word = " + word + " -------------");
        mWordCount++;
        if (mWordCount % 300 == 0
                && "touchpal".equals(CommandProcess.getInstance().getKeyboardType())) {
            resetKeyboard();
        }
        gotSuggestiongFromBigram = false;
        inputConnection = CommandProcess.getInstance().getInputConnection();
        if (inputConnection == null) {
            Logger.e("Input connection == null; reset");
            inputConnection = CommandProcess.getInstance().getInputConnection();
        }

        CommandProcess.getInstance().waitCandidates(100);
        new SynTask(10) { // 获取二元推荐
            @Override
            public void run() {
                List<Candidate> candidates = CommandProcess.getInstance().getCandidates();
                if (candidates == null || candidates.isEmpty()) {
                    print("SUGGESTION: (got=false) EMPTY!!! \n");
                } else {
                    StringBuilder sb = new StringBuilder();
                    int foundIndex = -1;
                    int viewCandidatesSize = Math.min(3, candidates.size());
                    int matchResult = NOT_MATCH;
                    for (int i = 0; i < viewCandidatesSize; i++) {
                        int tag = 0;
                        String candidate = candidates.get(i).getValue();
                        if (matchResult != EXACTLY_MATCH) { // Not found yet.
                            int result = matchCandidate(candidate, word);
                            if (result == EXACTLY_MATCH || (result == IGNORE_CASE_MATCH)) {
                                tag = 1;
                                foundIndex = i;
                                gotSuggestiongFromBigram = true;
                            }
                            matchResult = result;
                        }
                        Logger.v("get bigram : candidate = " + candidate + " matchResult = " + matchResult);
                        sb.append(String.format(Locale.ENGLISH, "%d:%s ", tag, candidate));
                    }
                    print(String.format("SUGGESTION: (got=%s) %s\n", String.valueOf(gotSuggestiongFromBigram), sb.toString()));
                }
            }
        }.synRun(mScheduler);

        //判断当前找到二元词，输入空格
        Logger.v("gotSuggestiongFromBigram = " + gotSuggestiongFromBigram);
        if (gotSuggestiongFromBigram) {

            new SynTask(10) {
                @Override
                public void run() {
                    inputConnection.commitText(word, 1);
                }
            }.synRun(mScheduler);

            return true;
        }

        int index = 0;
        String inputWord = "";
        for (final Character c : word.toCharArray()) { // 逐个输入字
            index++;
            int delay = index == 1 ? 200 : 100;
            int candidateDelay = index == 1 ? 300 : 100;
            Logger.v("press '" + c + "'");
            noSpaceline += c.toString();
            if (noSpacelang){
                inputWord = noSpaceline;
            }
            else {
                inputWord += c.toString();
            }
//            inputConnection.finishComposingText();
            if("swiftkey".equals(CommandProcess.getInstance().getKeyboardType()) || "gboard".equals(CommandProcess.getInstance().getKeyboardType()) ||
                    "kika".equals(CommandProcess.getInstance().getKeyboardType()) || "kikaoem".equals(CommandProcess.getInstance().getKeyboardType())) {
                final String inputConnect = inputWord;
                foundIndex = -1;
                new SynTask(50) {
                    @Override
                    public void run() {
                        inputConnection.commitText(inputConnect, 1);
                    }
                }.synRun(mScheduler);
                sleep(50);
            }
            else {
                new SynTask(50) {
                    @Override
                    public void run() {
                        inputConnection.commitText(c.toString(), 1);
                    }
                }.synRun(mScheduler);
                sleep(50);
//                final String inputConnect = inputWord;
//                foundIndex = -1;
//                new SynTask(50) {
//                    @Override
//                    public void run() {
//                        inputConnection.commitText(inputConnect, 1);
//                    }
//                }.synRun(mScheduler);
//                sleep(50);
            }


            if (".,?!$&".contains(c.toString()) && word.length() == 1) {
                Logger.v("return via .,?!$&");
                return true;
            }

            CommandProcess.getInstance().waitCandidates(300);
            List<Candidate> lastCandidates = CommandProcess.getInstance().getCandidates();
            //修改逻辑，如果候选词为空，跳过判断候选词，继续输入（之前逻辑直接跳出输入）
            if (lastCandidates == null || lastCandidates.isEmpty()) {
                Logger.e(word + ":" + c + "----获取候选词: 为空 需要查询一下");
                continue;
            }
            StringBuilder sb = new StringBuilder();
            boolean isDefault = false;
            boolean found = false;
            int matchResult = NOT_MATCH;
            int min_v = 3;
            if ("kikaoem".equals(CommandProcess.getInstance().getKeyboardType())) {min_v = 5;}

            int viewCandidatesSize = Math.min(min_v, lastCandidates.size());
            for (int i = 0; i < viewCandidatesSize; i++) {
                String candidate = lastCandidates.get(i).getValue();
                if (matchResult != EXACTLY_MATCH) { // Not found yet.
                    int result = matchCandidate(candidate, word);
                    if (result == EXACTLY_MATCH || (result == IGNORE_CASE_MATCH)) {
                        foundIndex = i;
                        found = true;
                        if ("gboard".equals(CommandProcess.getInstance().getKeyboardType())) {
                            isDefault = i == 1;
                        } else {
                            isDefault = i == 0;
                        }
                    }
                    matchResult = result;
                }
                Logger.v("get candidate : candidate = " + candidate + " matchResult = " + matchResult);
                int tag = 0;
                if ("gboard".equals(CommandProcess.getInstance().getKeyboardType())) {
                    if (i == 1) {
                        tag = 1;
                        sb.insert(0, String.format("%d:%s ", tag, lastCandidates.get(i).getValue()));
                    } else {
                        sb.append(String.format("%d:%s ", tag, lastCandidates.get(i).getValue()));
                    }
                } else {
                    //touch 推荐逻辑待定
                    if (i == 0) {
                        tag = 1;
                    }
                    sb.append(String.format("%d:%s ", tag, lastCandidates.get(i).getValue()));
                }
            }
            print("CANDIDATE: (found=%b, default=%b) %s\n", found, isDefault, sb.toString());
        }

//        inputConnection.finishComposingText();

        // [3]
        // select from candidate list, or enter a space to start next word [S]
//        CommandProcess.getInstance().selectCandidate(word);
//        inputConnection.finishComposingText();
        Logger.v("foundIndex = " + foundIndex);
        return true;
    }

    private int matchCandidate(String candidate, String word) {
        if (TextUtils.isEmpty(candidate) || TextUtils.isEmpty(word)) {
            return NOT_MATCH;
        }

        if (word.equals(candidate)) {
            return EXACTLY_MATCH;
        }
        if (word.toLowerCase().equals(candidate.toLowerCase())) {
            return IGNORE_CASE_MATCH;
        }
        return NOT_MATCH;
    }

    private void print(String format, Object... args) {
        String msg = String.format(format, args);
        Logger.i(Logger.TAG + "_output", msg);
        mPrintStream.print(msg);
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
}
