package com.silverlit.onenetedp.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Created by Yangyl on 2016/4/12.
 */
public class MyDialog extends AlertDialog {
    /**
     * Creates an alert dialog that uses the default alert dialog theme.
     * <p>
     * The default alert dialog theme is defined by
     * {@link android.R.attr#alertDialogTheme} within the parent
     * {@code context}'s theme.
     *
     * @param context the parent context
     * @see android.R.styleable#Theme_alertDialogTheme
     */
    public MyDialog(Context context) {
        super(context);
    }


    @Override
    public void setIcon(Drawable icon) {
        super.setIcon(icon);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
    }

    @Override
    public void setView(View view) {
        super.setView(view);
    }

    @Override
    public void create() {
        super.create();
    }

    @Override
    public void show() {
        super.show();
    }
}
