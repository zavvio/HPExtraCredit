//
//  AuthenticationErrorDialogFragment.java
//  com.hp.linkreadersdk
//  LinkReaderSDK
//
//  Copyright (c) 2015 HP. All rights reserved.
//

package com.hp.extracredit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class AuthenticationErrorDialogFragment extends DialogFragment {

    public static final String TAG = "AuthenticationErrorDialog";

    public interface DialogListener {
        void onConfirmation();
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
                .setTitle(R.string.auth_error)
                .setMessage(getResources().getText(R.string.invalid_credentials))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialogListener.onConfirmation();
                    }
                });
        return builder.create();
    }

}
