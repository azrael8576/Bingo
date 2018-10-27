package com.alex.bingo.Bean;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class NumberButton extends android.support.v7.widget.AppCompatButton {
    int number;
    boolean picked;
    int position;
    public NumberButton(Context context) {
        super(context);
    }

    public NumberButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public boolean isPicked() {
        return picked;
    }

    public void setPicked(boolean picked) {
        this.picked = picked;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
