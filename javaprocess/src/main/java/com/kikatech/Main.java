package com.kikatech;

import com.google.gson.Gson;
import com.kikatech.bean.KeyInfo;
import com.kikatech.bean.KeyboardConfig;
import com.kikatech.bean.KeyboardPointerInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage : java -cp Main ${inputFilePath} ${outputFilePath}");
        }
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            Gson gson = new Gson();
            br = new BufferedReader(new FileReader(new File(args[0])));
            bw = new BufferedWriter(new FileWriter(new File(args[1])));
            List<KeyboardPointerInfo> list = new ArrayList<>();
            for (String line; (line = br.readLine()) != null; ) {
                if(line.startsWith("#")) continue;
                String[] strings = line.split("TOUCH\\|");
                if (strings.length != 2) continue;
                KeyboardPointerInfo pointerInfo = gson.fromJson(strings[1], KeyboardPointerInfo.class);
                list.add(pointerInfo);
            }
            List<KeyInfo> keys = new ArrayList<>();
            for (KeyboardPointerInfo info : list) {
                keys.add(info.getKeyInfo(1504));
            }
            String packageName = "";
            String inputmethodServiceClazz = "";
            String mainKeyboardViewClazz = "";
            String candidateViewClazz = "";
            KeyboardConfig keyboardConfig = new KeyboardConfig(packageName, inputmethodServiceClazz, mainKeyboardViewClazz, candidateViewClazz, keys);
            String json = gson.toJson(keyboardConfig);
            System.out.println(json);
            bw.write(json);
            bw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {}
            }

            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {}
            }
        }
    }
}
