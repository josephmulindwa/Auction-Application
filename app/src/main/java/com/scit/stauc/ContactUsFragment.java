package com.scit.stauc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import util.AppUtils;

public class ContactUsFragment extends Fragment {

    public static ContactUsFragment newInstance(){
        return new ContactUsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_contact_us, container, false);
        Button sendMessageButton = v.findViewById(R.id.send_message_button);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MessageActivity.sendMessage(getActivity(), AppUtils.HELP_CENTER_ID, "Contact Help Center");
            }
        });
        return v;
    }

}
