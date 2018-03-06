package com.lib.llj.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.llj.commonlib.R;


/**
 * Created by wzh on 2017/10/12.
 * 当有内容输入时，在输入框末尾出现删除按钮的EditText
 */

public class ClearEditText extends AppCompatEditText implements View.OnFocusChangeListener {

    private Drawable mLeft;
    private Drawable mClear;
    public ClearEditText(Context context) {
        super(context);
        initAttrs(null);
    }

    public ClearEditText(Context context, AttributeSet attrs) {
        super(context,attrs);
        initAttrs(attrs);
    }

    public ClearEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context,attrs,defStyleAttr);
        initAttrs(attrs);
    }

    private void initAttrs(AttributeSet attrs){
        if(attrs!=null){
            TypedArray array=getContext().obtainStyledAttributes(attrs, R.styleable.ClearEditText);
            mLeft=array.getDrawable(R.styleable.ClearEditText_drawableLeft);
            array.recycle();
        }
        if(mLeft!=null){
            mLeft.setBounds(0,0,mLeft.getMinimumWidth(),mLeft.getMinimumWidth());
            setCompoundDrawablesWithIntrinsicBounds(mLeft,null,null,null);
        }

        mClear=getResources().getDrawable(R.drawable.clear);
        mClear.setBounds(0,0,mClear.getMinimumWidth(),mClear.getMinimumHeight());
        this.setOnFocusChangeListener(this);
        this.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length()>0){
                    showClearDrawable();
                }else {
                    resetDrawable();
                }
            }
        });
    }

    private void resetDrawable(){
        if(mLeft!=null){
            setCompoundDrawablesWithIntrinsicBounds(mLeft,null,null,null);
        }else {
            setCompoundDrawablesWithIntrinsicBounds(null,null,null,null);
        }
    }

    private void showClearDrawable(){
        if(mLeft!=null){
            setCompoundDrawablesWithIntrinsicBounds(mLeft,null,mClear,null);
        }else {
            setCompoundDrawablesWithIntrinsicBounds(null,null,mClear,null);
        }
    }





    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()== MotionEvent.ACTION_DOWN){
            float x=event.getRawX();
            if(getText().toString().length()>0){
                if(x<getRight()&&x>getRight()-mClear.getMinimumWidth()){
                    setText("");
                    resetDrawable();
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        String context=getText().toString();
        if(hasFocus){
            if(context.length()>0){
                showClearDrawable();
            }else {
                resetDrawable();
            }
        }else {
            resetDrawable();
        }
    }
}
