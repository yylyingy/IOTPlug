package com.silverlit.onenetedp.views;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.OnActionClickListener;
import com.google.gson.Gson;
import com.silverlit.onenetedp.R;
import com.silverlit.onenetedp.model.CtrlBean;
import com.silverlit.onenetedp.model.OneNetEDPClient;
import com.silverlit.onenetedp.ui.widget.MaterialListViewYang;
import com.silverlit.onenetedp.ui.widget.card.ButtonAction;
import com.silverlit.onenetedp.ui.widget.card.LanDeviceCardProvider;

import java.util.ArrayList;

import butterknife.ButterKnife;

import static com.silverlit.onenetedp.utils.Util.DEV_ID;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DevsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DevsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DevsFragment extends Fragment {
    private static final String TAG = "DevsFragment";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
//    @InjectView(R.id.materialList)
    MaterialListViewYang mMaterialListViewYang;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    Gson json ;//= new Gson();
    CtrlBean.Datastreams.Datapoints<String> datapoints ;//= new CtrlBean.Datastreams.Datapoints<>();

    CtrlBean.Datastreams datastreams ;//= new CtrlBean.Datastreams(datapoints);
    CtrlBean javaBean ;//= new CtrlBean(datastreams);
    String ledCtrlStr ;//= json.toJson(javaBean, CtrlBean.class);

    private OnFragmentInteractionListener mListener;

    public DevsFragment() {
        // Required empty public constructor
        json = new Gson();
        datapoints = new CtrlBean.Datastreams.Datapoints<>();
        datapoints.setValue("LEDCTRL");
        datastreams = new CtrlBean.Datastreams(datapoints);
        datastreams.setId("LEDCTRL");
        javaBean = new CtrlBean(datastreams);
        ledCtrlStr = json.toJson(javaBean, CtrlBean.class);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DevsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DevsFragment newInstance(String param1, String param2) {
        DevsFragment fragment = new DevsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devs, container, false);
        // Inflate the layout for this fragment
        Log.d(TAG,"onCreateView");
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        ButterKnife.inject(getActivity());
        mMaterialListViewYang = (MaterialListViewYang) getActivity().findViewById(R.id.materialList);
        Log.d(TAG,"onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG,"onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG,"onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG,"onAttach");
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG,"onDetach");
        mListener = null;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.d(TAG,"hidden:" + hidden);
    }

    public boolean addOneItem(final String device){
//        保证device唯一性
        if (mMaterialListViewYang.getAdapter().getItemCount() > 0) {
            for (int i = 0; i < mMaterialListViewYang.getColumnCount(); i++) {
                if (device.equals(mMaterialListViewYang.getAdapter().getCard(i).getTag())) {
                    return false;
                }
            }
        }
        Card card ;
        Card.Builder builder = new Card.Builder(getContext());
        builder.setTag(device);
        builder.setDismissible();
        final LanDeviceCardProvider cardProvider = new LanDeviceCardProvider();
        cardProvider.setLayout(R.layout.lan_device_cardprovider_layout);
        cardProvider.setAdcPlugString("ADC ON");
        cardProvider.setDeviceName(device);
        cardProvider.setAdValue("NULL");
        cardProvider.setLedState("OFF");
        cardProvider.setSubtitleColor(getResources().getColor(R.color.red));
        cardProvider.setTextColor(getResources().getColor(R.color.colorWhite));
        cardProvider.addAction(R.id.ledOn,new ButtonAction(getContext())
                        .setText("LEDCTRL")  //开灯
                        .setTextResourceColor(R.color.button_flat)
                        .setListener(new OnActionClickListener() {
                            @Override
                            public void onActionClicked(View view, Card card) {
                                Toast.makeText(getActivity(), "" + ledCtrlStr, Toast.LENGTH_SHORT).show();
                                Log.d(TAG,ledCtrlStr);
//                                Toast.makeText(getContext(),"test",Toast.LENGTH_SHORT).show();
                                OneNetEDPClient.getInstance().saveData(device,
                                        1, null, ledCtrlStr.getBytes());
//                        mDrawerLayout.openDrawer(GravityCompat.START);
//                                TxData data = new TxData(device,true,ADCControlCode);
//                                presenter.send(data);
                                //发送开灯指令
                            }
                        })
        );


        cardProvider.endConfig();
        builder.withProvider(cardProvider);
        card  = builder.build();
        mMaterialListViewYang.getAdapter().add(card);
        return true;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
