package com.kikatech;

import java.io.*;

public class RunWord
{
    public static void main(String[] args)
            throws Throwable
    {
        if (args.length != 3) {
            System.err.println("Usage: java ${inFilePath} ${deviceId} ${languageCode}");
            return;
        }

        String inFilePath = args[0];
        String deviceId = args[1];
        String languageCode = args[2];

        BufferedReader wordSrc = new BufferedReader(new FileReader(new File(inFilePath)));

        Runtime.getRuntime().exec(String.format("adb -s %s shell rm /sdcard/word_output", new Object[] { deviceId })).waitFor();
        while (true)
        {
            File tmp = File.createTempFile("word", "part");
            try {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp)));
                try
                {
                    int lineCount = 0;
                    String line;
                    while ((lineCount < 100) && ((line = wordSrc.readLine()) != null)) {
                        lineCount++;
                        writer.write(line);
                        writer.write(10);
                    }

                    if (lineCount == 0)
                    {
                        writer.close();

                        return;
                    }
                } finally {
                    writer.close();
                }

                Runtime.getRuntime().exec(String.format("adb -s %s push %s /sdcard/word_input", new Object[] { deviceId, tmp.getAbsolutePath() }))
                        .waitFor();
            } finally {
                tmp.delete();
            }

            Runtime.getRuntime().exec(String.format("adb -s %s shell input tap 500 500", new Object[] { deviceId })).waitFor();
            Thread.sleep(2000L);

            Runtime.getRuntime().exec(String.format("adb -s %s logcat -c", new Object[] { deviceId }))
                    .waitFor();

            File tmpInput = File.createTempFile("runword-", "-part");
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(tmpInput));
                try {
                    bw.write(String.format("do_word /sdcard/word_input /sdcard/word_output %s", new Object[] { languageCode }));
                } finally {
                    bw.close();
                }
                Runtime.getRuntime().exec(String.format("adb -s %s push %s /sdcard/input", new Object[] { deviceId, tmpInput.getAbsolutePath() })).waitFor();
            } finally {
                tmpInput.delete();
            }

            Process logcat = Runtime.getRuntime().exec(String.format("adb -s %s logcat -s Xposed_hack_module_output", new Object[] { deviceId }));
            BufferedReader rd = new BufferedReader(new InputStreamReader(logcat.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                System.out.println(line);
                if (line.contains("finished word task")) {
                    break;
                }

                if (line.contains("tap 0")) {
                    executeInput(String.format("adb -s %s shell input tap %d %d", new Object[]{deviceId, 283, 1493}));
                } else if (line.contains("tap 1")) {
                    executeInput(String.format("adb -s %s shell input tap %d %d", new Object[]{deviceId,715,1477}));
                } else if (line.contains("tap 2")) {
                    executeInput(String.format("adb -s %s shell input tap %d %d", new Object[]{deviceId,1089,1498}));
                }

                if (line.contains("delete file")) {
                    executeInput(String.format("adb -s %s shell rm /sdcard/input", new Object[] {deviceId}));
                    executeInput(String.format("adb -s %s shell rm /sdcard/word_input", new Object[] {deviceId}));
                }
            }
            logcat.destroy();

            Runtime.getRuntime().exec(String.format("adb -s %s shell input keyevent ENTER", new Object[] { deviceId })).waitFor();
            Runtime.getRuntime().exec(String.format("adb -s %s shell input keyevent BACK", new Object[] { deviceId })).waitFor();
            Thread.sleep(2000L);
        }
    }
    private static Process executeInput(String format) throws IOException {
        return Runtime.getRuntime().exec(format);
    }
}