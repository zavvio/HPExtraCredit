//
//  NetworkErrorDialogFragment.java
//  com.hp.linkreadersdk
//  LinkReaderSDK
//
//  Copyright (c) 2015 HP. All rights reserved.

package com.hp.extracredit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;


public class NetworkErrorDialogFragment extends DialogFragment {

    public static final String TAG = "NetworkErrorDialog";

    public interface DialogListener {
        void onReauthenticationClick();

        void onCancelClick();
    }

    DialogListener dialogListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            dialogListener = (DialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.network_error_title)
                .setMessage(getResources().getText(R.string.network_error_body))
                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialogListener.onReauthenticationClick();
                    }
                })
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialogListener.onCancelClick();
                    }
                });
        return builder.create();
    }
}
