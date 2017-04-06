package com.hp.extracredit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

// This class is an example to show how a payoff could be showed to the user
public class PayoffContentDialogFragment extends android.support.v4.app.DialogFragment {

    public static final String TAG = "AuthenticationErrorDialog";
    private String payoffType;
    private String payoffContent;

    DialogListener dialogListener;

    public interface DialogListener {
        void onDialogDismiss();

        void onDialogShowing();
    }

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        payoffType = getArguments().getString("payoff_type");
        payoffContent = getArguments().getString("payoff_content");
    }

    @Override
    public void onResume() {
        super.onResume();
        dialogListener.onDialogShowing();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        dialogListener.onDialogDismiss();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(payoffType)
                .setMessage(payoffContent)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        return builder.create();
    }
}
