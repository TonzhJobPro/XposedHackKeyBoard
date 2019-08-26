package com.kikatech.hackbasicmodule;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
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

/**
 * Created by xm on 17/4/13.
 */
public class WordNewIndicProcess extends AbstractIO implements IProcess {

    private final String mInputPath;
    private final String mOutputPath;
    private final Locale mLocale;
    private IScheduler mScheduler;
    private BufferedReader mReader;
    private PrintStream mPrintStream;
    private int foundIndex;
    private boolean found;
    private boolean isDefault;

    private static final int NOT_MATCH = 0;
    private static final int EXACTLY_MATCH = 1;
    private static final int IGNORE_CASE_MATCH = 2;
    private boolean gotSuggestiongFromBigram;

    public WordNewIndicProcess(@NonNull String inputPath, @NonNull String outputPath, @NonNull Locale locale) {
        mInputPath = inputPath;
        mOutputPath = outputPath;
        mLocale = locale;
    }

    @Override
    public void run(@NonNull IScheduler scheduler) {
        mScheduler = scheduler;

        Logger.v("[Step 1] : open input file");
        mReader = getReader(mInputPath);
        if (mReader == null) {
            Logger.e("open input file failed");
            return;
        }

        Logger.v("[Step 2] : open output file");
        Context appContext = CommandProcess.getInstance().getAppContext();
        String outputPath = appContext.getFilesDir() + "/word_output";
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
            runLine(line);
        }
    }

    private void runLine(String line) {
//        final String[] parts = StringUtils.splitToNonEmptyStrings(line, "\\s+");

        Logger.v("==========================Run line=========================");
        final String[] parts = line.split("\t");
        if (parts.length != 3) {
            Logger.e("must be 3 words per line: '" + line + '\'');
            return;
        }

        for (String s : parts) {
            Logger.v("runLine parts s = " + s);
        }
        final InputConnection connection = CommandProcess.getInstance().getInputConnection();
        if (connection == null) {
            Logger.e("Input connection == null");
        }

        final String prefix = parts[0];
        final String input = parts[1];
        final String desire = parts[2];
        print("TASK: Prefix = '%s', input=%s desire=%s\n", prefix, input, desire);

        if (!TextUtils.isEmpty(prefix)) {
            final String[] prefixSplits = StringUtils.splitToNonEmptyStrings(prefix, "\\s+");
            for (final String prefixFrag : prefixSplits) {
                Logger.v("prefixFrag = " + prefixFrag);

                new SynTask(10) {
                    @Override
                    public void run() {
                        connection.commitText(prefixFrag, 1);
                    }
                }.synRun(mScheduler);
                new SynTask(10) {
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressSpace();
                    }
                }.synRun(mScheduler);
                sleep(50);
            }
        }

        CommandProcess.getInstance().waitCandidates(100);
        gotSuggestiongFromBigram = false;
        new SynTask(10) { // 获取二元推荐
            @Override
            public void run() {
                List<Candidate> candidates = CommandProcess.getInstance().getCandidates();
                if (candidates == null || candidates.isEmpty()) {
                    print("SUGGESTION: (got=false) EMPTY!!! \n");
                } else {
                    StringBuilder sb = new StringBuilder();
                    int foundIndex = -1;
                    int viewCandidatesSize = Math.min(100, candidates.size());
                    int matchResult = NOT_MATCH;
                    for (int i = 0; i < viewCandidatesSize; i++) {
                        int tag = 0;
                        String candidate = candidates.get(i).getValue();
                        if (matchResult != EXACTLY_MATCH) { // Not found yet.
                            int result = matchCandidate(candidate, desire);
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

        int index = 0;
        String tempChars = "";
        for (final Character c : input.toCharArray()) { // 逐个输入字符
            found = false;
            isDefault = false;
            index++;
            connection.finishComposingText();
            tempChars += c.toString();

            if ("ghindi".equals(CommandProcess.getInstance().getKeyboardType())) {
//                connection.setComposingRegion(index, 0);
                connection.setComposingText(tempChars, 0);
            }
            else {
                new SynTask(50) { // 按键
                    @Override
                    public void run() {
                        connection.commitText(c.toString(), 1);
                    }
                }.synRun(mScheduler);
                sleep(50);
            }

            CommandProcess.getInstance().waitCandidates(10000);
            List<Candidate> lastCandidates = CommandProcess.getInstance().getCandidates();
            if (lastCandidates == null) {
                Logger.e(input + ":" + c + "-获取候选词: 为空 需要查询一下");
                CommandProcess.getInstance().pressEnter();
                return;
            }
            for (int i = 0; i < lastCandidates.size(); i++) {
                if (desire.equalsIgnoreCase(lastCandidates.get(i).getValue())) {
                    if (i < 3) {
                        found = true;
                        foundIndex = i;
                    }
                    isDefault = i == 0;
                }
            }
            print("CANDIDATE: (found=%b, default=%b) %s\n", found, isDefault, CommandProcess.getInstance().getWrapperCandidate());
        }
//        // tonzh 点击后退删除上屏词
//        for (int i = 0; i < input.length() ; i++) {
//            CommandProcess.getInstance().pressDelete();
//        }
        new SynTask(20) {
            @Override
            public void run() {
                CommandProcess.getInstance().pressEnter();
            }
        }.synRun(mScheduler);
        sleep(100);
    }

    private void print(String format, Object... args) {
        String msg = String.format(format, args);
        Logger.i(Logger.TAG + "_output", msg);
        mPrintStream.print(msg);
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

}
