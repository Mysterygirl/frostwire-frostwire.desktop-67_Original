package com.frostwire.gnutella.gui;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;

public class HintTextField extends JTextField implements FocusListener {

    /**
     * 
     */
    private static final long serialVersionUID = -3191287673317585610L;

    private String _hint;
    private Color _color;
    private Color _hintColor;

    public HintTextField(String hint) {
        super(hint);
        _hint = hint;
        _color = Color.BLACK;
        _hintColor = Color.LIGHT_GRAY;
        setForeground(_hintColor);
        addFocusListener(this);
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (this.getText().length() == 0) {
            setForeground(_color);
            super.setText("");
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (this.getText().length() == 0) {
            clear();
        }
    }

    @Override
    public String getText() {
        String typed = super.getText();
        return typed.equals(_hint) ? "" : typed;
    }

    public void clear() {
        if (isFocusOwner()) {
            setForeground(_color);
            super.setText("");
        } else {
            setForeground(_hintColor);
            super.setText(_hint);
        }
    }
    
    public void setHint(String text) {
    	_hint = text;
    	repaint();
    }
}
