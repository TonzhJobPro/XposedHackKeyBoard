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

public class NewCrawlerProcess extends AbstractIO implements IProcess {
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

    private String[] mFuyin = {"b", "p"};

    public NewCrawlerProcess(@NonNull String inputPath, @NonNull String outputPath, @NonNull Locale locale, boolean isResume) {
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

    private boolean candidateOver6 = false;
    private boolean hackZH = true;

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
        if (candidateOver6 && hackZH) {
            candidateOver6 = false;
            return false;
        }
        final InputConnection connection = CommandProcess.getInstance().getInputConnection();
//        CharSequence afterDel = connection.getTextBeforeCursor(1000, 0);
//        if (afterDel.length() > 0) {
//            for (int i = 0; i < afterDel.length(); i++ ){
//                new SynTask(30) { // 按键
//                    @Override
//                    public void run() {
//                        CommandProcess.getInstance().pressDelete();
//                    }
//                }.synRun(mScheduler);
//            }
//        }
        connection.deleteSurroundingText(1000, 1000);
        if (status == STATUS_FOIND) {
            return true;
        }
        return false;
    }

    private int runWordInternal(String word) {
        int index = 0;
        boolean pressedShift = false;
        for (final Character c : word.toCharArray()) {
            index++;
            if (CommandProcess.getInstance().charsSecondPage.contains(c.toString())){
                new SynTask(200) { // 按键
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressShift();}
                }.synRun(mScheduler);
                sleep(50);
                pressedShift = true;
                if ("touchpal".equals(CommandProcess.getInstance().getKeyboardType())) {
                    CommandProcess.getInstance().resetKeyboardviewtoSecond();
                }
                print("press shift");
            }

            if (index == 1) {
                new SynTask(200) { // 按键
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressChar(c);
                    }
                }.synRun(mScheduler);
            } else {
                new SynTask(30) { // 按键
                    @Override
                    public void run() {
                        CommandProcess.getInstance().pressChar(c);
                    }
                }.synRun(mScheduler);
            }
            if(pressedShift){
                if ("touchpal".equals(CommandProcess.getInstance().getKeyboardType())) {
                    CommandProcess.getInstance().resetKeyboardviewtoTop();
                }
            }
        }

        CommandProcess.getInstance().waitCandidates(1000);
        List<Candidate> lastCandidates = CommandProcess.getInstance().getCandidates();
        if (mIsTouchPal && lastCandidates == null || lastCandidates.size() == 0) {
            final InputConnection connection = CommandProcess.getInstance().getInputConnection();
            CharSequence afterDel = connection.getTextBeforeCursor(1000, 0);
            if (afterDel.length() > 0) {
                for (int i = 0; i < afterDel.length(); i++ ){
                    new SynTask(30) { // 按键
                        @Override
                        public void run() {
                            CommandProcess.getInstance().pressDelete();
                        }
                    }.synRun(mScheduler);
                }
            }
            resetKeyboard();
            return STATUS_REDO;
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
                    if (candidateValue.length() > 4 && hackZH) {
                        candidateOver6 = true;
                        CommandProcess.getInstance().pressEnter();
                        return STATUS_NOT_FOUND;
                    }
                    print(candidateValue);
                    print("\n");
                }
            }
        }
        if (hackZH) {
            CommandProcess.getInstance().pressEnter();
            return STATUS_FOIND;
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
