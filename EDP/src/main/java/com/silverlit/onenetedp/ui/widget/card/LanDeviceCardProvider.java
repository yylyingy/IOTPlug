package com.silverlit.onenetedp.ui.widget.card;

import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.TextView;

import com.dexafree.materialList.card.Action;
import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.CardProvider;
import com.silverlit.onenetedp.R;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;
import net.steamcrafted.materialiconlib.MaterialIconView;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by Yangyl on 2016/7/22.
 */
public class LanDeviceCardProvider <T extends LanDeviceCardProvider>extends CardProvider {
    private static final String TAG = "LanDeviceCardProvider";

    private final static int DIVIDER_MARGIN_DP = 16;
//    private  int    SERVO_NUM;
//    private  int    DC_NUM;

//    private String mSubtitle1;
//    private String mSubtitle2;
    //    private Context mContext;
    private int    textColor;
    private String title;
    private String deviceName;
    private String ledState;
    private String ledOnString;
    private String ledOffString;
    private String adValue;
    private String adcPlugString;
    private int mLayoutId;

//    public void serMotorNum(final int servoNum,final int dcMotor){
//        SERVO_NUM = servoNum;
//        DC_NUM      = dcMotor;
//    }
    private final Map<Integer, Action> mActionMapping = new HashMap<>();

//    /**
//     * Do not use this method,it's only for {@code Card.Builder}!
//     * @param context to access the resource.
//     * */
//    void setContext(Context context) {
//        mContext = context;
//        onCreated();
//    }

    public T setTextColor(@ColorInt int color){
        textColor = color;
        return (T) this;
    }

    @ColorInt
    public int getTextColor(){
        return textColor;
    }

    /**
     *
     * @param actionViewId
     * @param action
     * @return
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public T addAction(@IdRes final int actionViewId, @NonNull final Action action) {
        mActionMapping.put(actionViewId, action);
        return (T) this;
    }

    /**
     *
     * @param actionViewId
     * @return
     */
    @Nullable
    public Action getAction(@IdRes final int actionViewId) {
        return mActionMapping.get(actionViewId);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    @Override
    public T setLayout(@LayoutRes final int layoutId) {
        mLayoutId = layoutId;
        return (T) this;
    }

    /**
     * Get the card layout as resource.
     * @return the card layout.
     * */
    @LayoutRes
    @Override
    public int getLayout(){
        return mLayoutId;
    }

    /**
     *
     */
    public T setTitle(@StringRes final int resID){
        return setTitle(getContext().getString(resID));
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public T setTitle( final String title){
        this.title = title;
        notifyDataSetChanged();
        return (T) this;
    }

    public String getTitle(){
        return title;
    }

    @SuppressWarnings("unchecked")
    public T setDeviceName(String device){
        deviceName = device;
        notifyDataSetChanged();
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setAdcPlugString(String adcPlug) {
        this.adcPlugString = adcPlug;
        return (T) this;
    }

    public String getAdcPlugString() {
        return adcPlugString;
    }

    public String getDeviceName(){
        return deviceName;
    }
    @SuppressWarnings("unchecked")
    public T setLedOffString(String ledOffString) {
        this.ledOffString = ledOffString;
        return (T) this;
    }

    @NonNull
    public String getLedOffString() {
        return ledOffString;
    }

    @SuppressWarnings("unchecked")
    public T setLedOnString(String ledOnString) {
        this.ledOnString = ledOnString;
        return (T) this;
    }

    @NonNull
    public String  getLedOnString() {
        return ledOnString;
    }

    @SuppressWarnings("unchecked")
    public T setLedState(String state){
        ledState = state;
        notifyDataSetChanged();
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setAdValue(String value){
        adValue = value;
        notifyDataSetChanged();
        return (T) this;
    }

    /**
     * Render the content and style of the card to the view.
     *
     * @param view to display the content and style on.
     * @param card to render
     * */
    @Override
    public void render(@NonNull View view, @NonNull Card card) {
        // The card background
        final CardView cardView ;
        cardView  = (CardView) findViewById(view, R.id.cardView, CardView.class);
        if (cardView != null) {
            cardView.setCardBackgroundColor(getBackgroundColor());
        }
        //icon
        final MaterialIconView iconView = (MaterialIconView) findViewById(view,R.id.icon,MaterialIconView.class);
        //Title
//        final TextView title = (TextView) findViewById(view,R.id.title,TextView.class);
        final TextView deviceName = (TextView) findViewById(view, R.id.deviceName,TextView.class);
        final TextView ledState   = (TextView) findViewById(view,R.id.ledState,TextView.class);
        final TextView adValue      = (TextView) findViewById(view ,R.id.adValue,TextView.class);

        final TextView ledOn        = (TextView) findViewById(view,R.id.ledOn,TextView.class);
        final TextView ledOff       = (TextView) findViewById(view,R.id.ledOff,TextView.class);
        final TextView adcPlug      = (TextView) findViewById(view,R.id.adcPlug,TextView.class);
//        if (title != null){
//            title.setText(getTitle());
//            title.setTextColor(getTitleColor());
//            title.setGravity(getTitleGravity());
//        }
        if (iconView != null) {
            iconView.setIcon(MaterialDrawableBuilder.IconValue.WIFI);
            iconView.setColorResource(R.color.my_blue);
        }
        if (deviceName != null){
            deviceName.setText(LanDeviceCardProvider.this.deviceName);
            deviceName.setTextColor(getTitleColor());
            deviceName.setGravity(getTitleGravity());
        }
        if (ledState != null){
            ledState.setText(LanDeviceCardProvider.this.ledState);
        }

        if (adValue != null){
            adValue.setText(LanDeviceCardProvider.this.adValue);
        }

        if (ledOn != null){
            ledOn.setText(getLedOnString());
            ledOn.setTextColor(getTextColor());
//            ledOn.setGravity(getTitleGravity());
        }
        if (ledOff != null){
            ledOff.setText(getLedOffString());
            ledOff.setTextColor(getTextColor());
//            ledOff.setGravity(getTitleGravity());
        }
        if (adcPlug != null){
            adcPlug.setText(LanDeviceCardProvider.this.adcPlugString);
            adcPlug.setTextColor(getTextColor());
//            adcPlug.setGravity(getTitleGravity());
        }

        // Actions
        for (final Map.Entry<Integer, Action> entry : mActionMapping.entrySet()) {
            final View actionViewRaw = findViewById(view, entry.getKey(), View.class);
            if (actionViewRaw != null) {
                final Action action = entry.getValue();
                action.setProvider(this);
                ((ButtonAction)action).onRender(actionViewRaw, card);
            }
        }
    }
}
