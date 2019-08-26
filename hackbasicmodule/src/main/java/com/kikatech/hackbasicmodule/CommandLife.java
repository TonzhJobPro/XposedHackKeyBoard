package com.kikatech.hackbasicmodule;

import android.inputmethodservice.InputMethodService;
import android.support.v4.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kikatech.hackbasicmodule.bean.Candidate;
import com.kikatech.hackbasicmodule.bean.KeyboardConfig;

import java.util.List;

/**
 * Created by xm on 17/4/14.
 */

public interface CommandLife extends Runnable {

    void onCreate(InputMethodService service);

    void onDestory();

    List<Candidate> getCandidates();

    void setCandidates(List<Candidate> candidates, List<Pair<TextView, ViewGroup>> candidateTextViews);

    InputMethodService getInputMethodService();

    void setInputMethodService(InputMethodService service);

    void setMainKeyboardView(View view);

    void pressSpace();

    void pressQuotation();

    void pressDoubleQuotation();

    void pressEnter();

    void pressSymbol();

    void pressShift();

    void pressDelete();

    void pressChar(Character character);

}
