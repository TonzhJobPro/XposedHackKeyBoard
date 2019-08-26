package com.kikatech.hackbasicmodule;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.inputmethod.InputConnection;

import com.kikatech.hackbasicmodule.bean.Candidate;
import com.kikatech.hackbasicmodule.task.IScheduler;
import com.kikatech.hackbasicmodule.task.SynTask;
import com.kikatech.hackbasicmodule.utils.IOUtils;
import com.kikatech.hackbasicmodule.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ryanlin on 06/07/2017.
 */

public class NewCrawlerNewProcess extends AbstractIO implements IProcess {
    private final static String SAVE_FILE = "/crawler_save.out";

    private Context mContext;

    private final boolean mIsResume;
    private final String mInputPath;
    private final String mOutputPath;
    private final Locale mLocale;
    private IScheduler mScheduler;

    private char[] mRecursiveChar = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private String mStartStr = "a";
    private String mFinalStr = "ab";

    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private PrintStream mPrintStream;

    private StringBuilder mCurrentString;
    private boolean mIsTouchPal;

    public NewCrawlerNewProcess(@NonNull String inputPath, @NonNull String outputPath, @NonNull Locale locale, boolean isResume) {
        mContext = CommandProcess.getInstance().getAppContext();
        mInputPath = inputPath;
        mOutputPath = outputPath;
        mLocale = locale;
        mIsResume = isResume;
    }

    @Override
    public void run(@NonNull IScheduler scheduler) {
        mScheduler = scheduler;
        mIsTouchPal = "touchpal".equals(CommandProcess.getInstance().getKeyboardType());

        readInput();

        Logger.v("[Step 2] : open output file");
        String outputPath = mContext.getFilesDir() + "/crawler_output";
        mPrintStream = getPrintStream(outputPath);
        if (mPrintStream == null) {
            Logger.e("open output file failed");
            return;
        }
        try {
            Logger.v("[Step 3] : start internal run()");
            internalRun();
        } catch (IOException e) {
            Logger.e(e);
        } finally {
            close();
        }
        CommandProcess.isGetCommand = false;
        Logger.i(Logger.TAG + "_output", "finished word task");
    }

    private void readInput() {
        if (mIsResume) {
            onRestoreState();
        } else {
            BufferedReader reader = getReader(mInputPath);
            initialCondition(reader);
        }
        mCurrentString = new StringBuilder(mStartStr);
    }

    private void initialCondition(BufferedReader reader) {
        if (reader == null) {
            Logger.e("open input file failed");
            return;
        }
        try {
            synchronized (this) {
                int lineCount = 0;
                for (String line; (line = reader.readLine()) != null; ) {
                    if (!TextUtils.isEmpty(line) && !TextUtils.isEmpty(line.trim())) {
                        line = line.trim();
                        if (lineCount == 0) {
                            mStartStr = line;
                        } else if (lineCount == 1) {
                            mFinalStr = line;
                        } else {
                            String lineNew = line;
                            mRecursiveChar = new char[lineNew.length()];
                            for (int i = 0; i < lineNew.length(); i++) {
                                mRecursiveChar[i] = lineNew.charAt(i);
                            }
                        }
                        lineCount++;
                    }
                }
            }
        } catch (Exception e) {
            Logger.e("open input file failed");
        }
    }

    private char findNextChar(char curChar) {
        for (int i = 0; i < mRecursiveChar.length; i++) {
            if (mRecursiveChar[i] == curChar && i + 1 < mRecursiveChar.length) {
                return mRecursiveChar[i + 1];
            }
        }
        return '\0';
    }

    private void internalRun() throws IOException {
        int index = 0;
        while (mCurrentString.length() > 0) {
            index++;
            boolean result = runWord(mCurrentString.toString());
            if (result) {
                mCurrentString.append(mRecursiveChar[0]);
            } else {
                char nextChar = '\0';
                while (mCurrentString.length() > 0
                        && (nextChar = findNextChar(mCurrentString.charAt(mCurrentString.length() - 1))) == '\0') {
                    mCurrentString.deleteCharAt(mCurrentString.length() - 1);
                }

                if (mCurrentString.length() == 0 || mFinalStr.equals(mCurrentString.toString())
                        || nextChar == '\0') {
                    break;
                }

                mCurrentString.replace(mCurrentString.length() - 1, mCurrentString.length(),
                        String.valueOf(nextChar));
            }

            if (index % 50 == 0) {
                onSaveState();
            }
            if (index % 300 == 0 && mIsTouchPal) {
                resetKeyboard();
            }
        }

        print("Finished!!!\n");
    }

    private static final int STATUS_FOIND = 0;
    private static final int STATUS_NOT_FOUND = 1;
    private static final int STATUS_REDO = 2;
    private boolean runWord(String word) {
        print("word: %s \n", mCurrentString);
        int status = runWordInternal(word);
        if (status == STATUS_REDO && mIsTouchPal) {
            status = runWordInternal(word);
            if (status == STATUS_REDO) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                status = runWordInternal(word);
            }
        }
        final InputConnection connection = CommandProcess.getInstance().getInputConnection();

//        new SynTask(30) { // 按键
//            @Override
//            public void run() {
//                connection.deleteSurroundingText(1000,1000);
//            }
//        }.synRun(mScheduler);

        CharSequence afterDel = connection.getTextBeforeCursor(1000, 0);
        if (afterDel.length() > 0) {
            for (int i = 0; i < afterDel.length() + 1; i++ ){
                new SynTask(30) { // 按键
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressDelete();
                    }
                }.synRun(mScheduler);
            }
        }

        sleep(30);
        if (status == STATUS_FOIND) {
            return true;
        }
        return false;
    }

    private int runWordInternal(final String word) {
        final InputConnection inputConnection = CommandProcess.getInstance().getInputConnection();
        int index = 0;
        String inputWord = "";
        new SynTask(100){
            @Override
            public void run(){ inputConnection.commitText(word, 1); }
        }.synRun(mScheduler);
//        for (final Character c : word.toCharArray()) {
//            index++;
//            int delay = index == 1 ? 200 : 100;
//            int candidateDelay = index == 1 ? 300 : 100;
//            Logger.v("press '" + c + "'");
//            inputWord += c.toString();
//            inputConnection.finishComposingText();
//            if("swiftkey".equals(CommandProcess.getInstance().getKeyboardType())) {
//                final String inputConnect = inputWord;
//                new SynTask(50) {
//                    @Override
//                    public void run() {
//                        inputConnection.commitText(inputConnect, 1);
//                    }
//                }.synRun(mScheduler);
//                sleep(50);
//            }
//            else if ("gboard".equals(CommandProcess.getInstance().getKeyboardType())) {
//                new SynTask(delay) {
//                    @Override
//                    public void run() {
//                        inputConnection.commitText(c.toString(), 1);
//                    }
//                }.synRun(mScheduler);
//                sleep(50);
//            }
//            else if (mIsTouchPal) {
//                new SynTask(100) {
//                    @Override
//                    public void run() {
//                        inputConnection.commitText(c.toString(), 1);
//                    }
//                }.synRun(mScheduler);
//                sleep(100);
//
//            }
//            else {
//                new SynTask(50) {
//                    @Override
//                    public void run() {
//                        inputConnection.commitText(c.toString(), 1);
//                    }
//                }.synRun(mScheduler);
//                sleep(50);
//            }
//        }

        CommandProcess.getInstance().waitCandidates(300);
        List<Candidate> lastCandidates = CommandProcess.getInstance().getCandidates();
        if (mIsTouchPal && lastCandidates == null || lastCandidates.size() == 0) {
            resetKeyboard();
            return STATUS_NOT_FOUND;
        }

        int startWithCount = 0;
        for (int i = 0; i < lastCandidates.size(); i++) {
            String candidateValue = lastCandidates.get(i).getValue();
            if (!TextUtils.isEmpty(candidateValue) && !TextUtils.isEmpty(candidateValue.trim())) {
                candidateValue = candidateValue.trim();
                String noCaseValue = candidateValue.toLowerCase();
                if (!candidateValue.contains(" ") && !noCaseValue.equals(word)) {
                    if (noCaseValue.startsWith(word) && candidateValue.length() > word.length()) {
                        startWithCount++;
                    }
                    print(candidateValue);
                    print("\n");
                }
            }
        }

        boolean foundStartWith;
        if (word.length() <= 2) {
            foundStartWith = true;
        } else if (word.length() <= 4) {
            foundStartWith = startWithCount >= 1;
        } else if (word.length() <= 7) {
            foundStartWith = startWithCount >= 2;
        } else {
            foundStartWith = false;
        }
        return foundStartWith ? STATUS_FOIND : STATUS_NOT_FOUND;
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

    private void onSaveState(/*LinkedList<CrawlerProcess.Word> processing, Set<String> processed*/) {
        String outputPath = mContext.getFilesDir() + SAVE_FILE;
        PrintStream ps = IOUtils.getPrintStream(outputPath, false);
        ps.println(mCurrentString.toString());
        ps.println(mFinalStr);
        for (char c : mRecursiveChar) {
            ps.print(c);
        }

        IOUtils.close(ps);
    }

    private void onRestoreState() {
        BufferedReader reader = getReader(mContext.getFilesDir() + SAVE_FILE);
        initialCondition(reader);
    }

    private void print(String format, Object... args) {
        String msg = String.format(format, args);
        Logger.i(Logger.TAG + "_output", msg);
        mPrintStream.print(msg);
    }
}
