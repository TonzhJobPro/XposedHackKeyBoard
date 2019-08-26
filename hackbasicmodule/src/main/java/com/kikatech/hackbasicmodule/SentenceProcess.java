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

public class SentenceProcess extends AbstractIO implements IProcess {

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

    public SentenceProcess(@NonNull String inputPath, @NonNull String outputPath, @NonNull Locale locale, boolean isResume) {
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
        for (String line; (line = mReader.readLine()) != null; ) {
            runLine(line);
        }
    }

    private boolean runLine(String line) {
        print("SENTENCE: %s\n", line);
        for (String word : StringUtils.splitToNonEmptyStrings(line, "\\s+")) {
            print("WORD: %s\n", word);
            runWord(word);
        }

        Logger.i("Log current sentences.");
        new SynTask(20) {
            @Override
            public void run() {
                InputConnection inputConnection = CommandProcess.getInstance().getInputConnection();
                if (inputConnection != null) {
                    CharSequence output = inputConnection.getTextBeforeCursor(1000, 0);
                    print("SENTENCE OUTPUT: %s\n", output);
                }
            }
        }.synRun(mScheduler);

        Logger.i("press enter start");
        new SynTask(20) {
            @Override
            public void run() {
                CommandProcess.getInstance().pressEnter();
            }
        }.synRun(mScheduler);
//        tonzh fix pressEnter suggestion word still on
        Logger.i("press enter start");
        new SynTask(20) {
            @Override
            public void run() {
                CommandProcess.getInstance().pressEnter();
            }
        }.synRun(mScheduler);
        CommandProcess.getInstance().waitCandidates(100);

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
        //判断word是以标点符号开头，且不是单独标点，认为无效字符，空格结束输入
        if (".,!?;:".contains(word.substring(0, 1)) && word.length() > 1) {
            new SynTask(20) {
                @Override
                public void run() {
                    CommandProcess.getInstance().pressSpace();
                }
            }.synRun(mScheduler);
            return true;
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
                    if (gotSuggestiongFromBigram) {
                        if ("swiftkey".equals(CommandProcess.getInstance().getKeyboardType()) ||
                                "kika".equals(CommandProcess.getInstance().getKeyboardType())) {
                            // 选择输入
                            if (foundIndex == 0) {
                                foundIndex = 1;
                            } else if (foundIndex == 1) {
                                foundIndex = 0;
                            }
//                        print(word + " foundIndex: " + foundIndex);
                            CommandProcess.getInstance().selectCandidate(foundIndex);
                        } else if ("gboard".equals(CommandProcess.getInstance().getKeyboardType())) {
                            CommandProcess.getInstance().selectCandidate(word);
                        }
                    }
                }
            }
        }.synRun(mScheduler);

        //判断当前找到二元词，并且swift 已经点击二元词，直接结束word输入
        Logger.v("gotSuggestiongFromBigram = " + gotSuggestiongFromBigram);
        if (gotSuggestiongFromBigram) {
            if ("swiftkey".equals(CommandProcess.getInstance().getKeyboardType())
                    || "kika".equals(CommandProcess.getInstance().getKeyboardType())) {
                sleep(100);
                return true;
            }
            if ("gboard".equals(CommandProcess.getInstance().getKeyboardType())) {
                new SynTask(0) {
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressSpace();
                    }
                }.synRun(mScheduler);
                sleep(100);
                return true;
            }
        }

        int index = 0;
        for (final Character c : word.toCharArray()) { // 逐个输入字
            Logger.v("press '" + c + "'");
            foundIndex = -1;
            boolean pressSymbol = false;
            int delay = index == 0 ? 200 : 20;
            int candidateDelay = index == 0 ? 300 : 100;
            index++;
//            if (c == '\'' || c == '\"' || c == '!' || c == '?' || c == '$' || c == '&') {
            if ("\'\"?!$&0123456789".contains(c.toString())) {
                new SynTask(delay) {
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressSymbol();
                    }
                }.synRun(mScheduler);
                if ("touchpal".equals(CommandProcess.getInstance().getKeyboardType())) {
                    CommandProcess.getInstance().resetKeyboardviewtoSecond();
                }
                sleep(200);
                pressSymbol = true;
            }

            new SynTask(delay) { // 按键
                @Override
                public void run() {
                    CommandProcess.getInstance().pressChar(c);
                }
            }.synRun(mScheduler);

            Logger.v("pressSymbol = " + pressSymbol);
            if (pressSymbol) {
                sleep(100);
                new SynTask(20) {
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressSymbol();
                    }
                }.synRun(mScheduler);
                if ("touchpal".equals(CommandProcess.getInstance().getKeyboardType())) {
                    CommandProcess.getInstance().resetKeyboardviewtoTop();
                }
            }
            if (".,?!$&".contains(c.toString()) && word.length() == 1) {
                Logger.v("return via .,?!$&");
                return true;
            }

            CommandProcess.getInstance().waitCandidates(candidateDelay);
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

        // [3]
        // select from candidate list, or enter a space to start next word [S]
        Logger.v("foundIndex = " + foundIndex);
        new SynTask(20) {
            @Override
            public void run() {
                if (foundIndex >= 0) {
                    if (foundIndex == 0) {
                        foundIndex = 1;
                    } else if (foundIndex == 1) {
                        foundIndex = 0;
                    }

                    if ("gboard".equals(CommandProcess.getInstance().getKeyboardType())) {
                        CommandProcess.getInstance().selectCandidate(word);
                        //                        单个字母单词上屏
                        if (word.length() == 1) {
                            CommandProcess.getInstance().pressSpace();
                        }
                    } else if ("touchpal".equals(CommandProcess.getInstance().getKeyboardType())) {
                        //touchpal 临时处理
                        CommandProcess.getInstance().pressSpace();
                    } else {
                        CommandProcess.getInstance().selectCandidate(foundIndex);
                        Logger.i("select found in " + foundIndex);
                    }
                } else {
                    CommandProcess.getInstance().pressSpace();
                    Logger.i("select not found, " + foundIndex);
                }
            }
        }.synRun(mScheduler);
//        sleep(100);

        if ((foundIndex >= 0 && "gboard".equals(CommandProcess.getInstance().getKeyboardType()) && (word.length() == 1))
                || "0123456789".contains(word.substring(word.length() - 1, word.length()))) {
            new SynTask(20) { // 按键
                @Override
                public void run() {
                    CommandProcess.getInstance().pressSpace();
                }
            }.synRun(mScheduler);
        }
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
