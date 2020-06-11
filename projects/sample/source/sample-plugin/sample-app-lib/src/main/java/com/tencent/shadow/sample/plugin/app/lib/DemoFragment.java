package com.tencent.shadow.sample.plugin.app.lib;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.tencent.shadow.sample.plugin.app.lib.usecases.activity.TestActivityOnCreate;
import com.tencent.shadow.sample.plugin.app.lib.usecases.dialog.TestDialog;

/**
 * @description : 示例插件Fragment
 * @date : 2020/6/9 11:45 AM 
 * @version :
 */
public class DemoFragment extends Fragment implements View.OnClickListener{

    private static final String TAG = "DemoFragment";

    public static Fragment newInstance() {
        return new DemoFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"DemoFragment onCreate");
    }

    View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_item_list, null);
        Log.d(TAG, "DemoFragment onCreateView view : " + view);
        initView();
        return view;
    }

    private void initView() {
        if (view == null) {
            return;
        }
        View viewDialog = view.findViewById(R.id.btn_dialog);
        if (viewDialog != null) {
            viewDialog.setOnClickListener(this);
        }
        View viewActivity = view.findViewById(R.id.btn_activity);
        if (viewActivity != null) {
            viewActivity.setOnClickListener(this);
        }
        View viewText = view.findViewById(R.id.btn_text);
        if (viewText != null) {
            viewText.setOnClickListener(this);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG,"DemoFragment onViewCreated");
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"DemoFragment onResume");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG,"DemoFragment onStart");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"DemoFragment onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG,"DemoFragment onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG,"DemoFragment onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"DemoFragment onDestroy");
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_activity) {
            startActivity(new Intent(getActivity(), TestActivityOnCreate.class));
        } else if (id == R.id.btn_dialog) {
            new TestDialog(getActivity()).show();
        } else if (id == R.id.btn_text) {
            Button btn_text = view.findViewById(R.id.btn_text);
            btn_text.setText(getActivity().getString(R.string.app_name));
        }
    }
}
