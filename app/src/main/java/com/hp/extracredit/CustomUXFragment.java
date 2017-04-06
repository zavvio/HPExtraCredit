package com.hp.extracredit;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.hp.linkreadersdk.AuthenticationCallback;
import com.hp.linkreadersdk.CameraError;
import com.hp.linkreadersdk.CaptureManager;
import com.hp.linkreadersdk.CaptureViewCallback;
import com.hp.linkreadersdk.ErrorCode;
import com.hp.linkreadersdk.Injector;
import com.hp.linkreadersdk.LinkReaderSDK;
import com.hp.linkreadersdk.Manager;
import com.hp.linkreadersdk.camera.CameraView;
import com.hp.linkreadersdk.enums.CaptureStates;
import com.hp.linkreadersdk.payload.TriggerType;
import com.hp.linkreadersdk.payoff.Contact;
import com.hp.linkreadersdk.payoff.DetectionCallback;
import com.hp.linkreadersdk.payoff.Email;
import com.hp.linkreadersdk.payoff.Layout;
import com.hp.linkreadersdk.payoff.Payoff;
import com.hp.linkreadersdk.payoff.PayoffError;
import com.hp.linkreadersdk.payoff.ResolveError;
import com.hp.linkreadersdk.payoff.Web;

public class CustomUXFragment extends Fragment implements DetectionCallback {

    private static final int CAMERA_PERMISSION_CODE = 1;
    Manager manager;
    CaptureManager captureManager;

    private static PayoffContentDialogFragment payoffContentDialogFragment;
    private FrameLayout cameraFrame;
    private CameraView cameraView;
    private boolean isPresenting = false;

    public CustomUXFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinkReaderSDK.initialize(this.getContext());
        manager = Injector.getObjectGraph().get(Manager.class);
        captureManager = Injector.getObjectGraph().get(CaptureManager.class);
    }

    private CaptureViewCallback getCaptureViewCallback() {

        CaptureViewCallback captureViewCallback = new CaptureViewCallback() {

            @Override
            public void didChangeFromState(CaptureStates fromState, CaptureStates toState) {
                if (toState.equals(CaptureStates.CAMERA_RUNNING) && !fromState.equals(CaptureStates.SCANNER_RUNNING) && !isDialogShowing()) {
                    startScanning();
                }
            }

            @Override
            public void cameraFailedError(CameraError lrCameraError) {
                Log.d("Error", "Camera failed to open");
            }
        };

        return captureViewCallback;
    }

    private void startScanning() {
        captureManager.startScanning(CustomUXFragment.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    captureManager.startSession(cameraView);
                } else {
                    Log.d("Error", "Camera permission denied by user");
                }
                break;
        }
    }

    private View fragmentCustomView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentCustomView = inflater.inflate(R.layout.fragment_custom_ux, container, false);

        cameraFrame = (FrameLayout) fragmentCustomView.findViewById(R.id.custom_ux_camera);
        cameraView = new CameraView(getContext());
        cameraFrame.addView(cameraView);

        manager.retrieveState(savedInstanceState);
        captureManager.retrieveSate(savedInstanceState);

        if (!manager.isAuthorized()) {
            manager.authorizeWithClientID(Credentials.CLIENT_ID, Credentials.CLIENT_SECRET, new AuthenticationCallback() {
                @Override
                public void onAuthenticationSuccess() {
                    captureManager.setCaptureViewCallback(getCaptureViewCallback());
                    captureManager.startSession(cameraView);
                }

                @Override
                public void onAuthenticationFailed(ErrorCode errorCode) {
                    Log.d("Error", "Auth Failed");
                }
            }, getContext());
        } else {
            captureManager.setCaptureViewCallback(getCaptureViewCallback());
        }

        if (payoffContentDialogFragment == null) {
            payoffContentDialogFragment = new PayoffContentDialogFragment();
        }

        return fragmentCustomView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (manager.isAuthorized()) {
            captureManager.startSession(cameraView);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        captureManager.stopSession();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        manager.saveState(outState);
        captureManager.saveState(outState);
    }

    @Override
    public void didFindTrigger(TriggerType triggerType) {
        Log.d("MARKDETECTED", triggerType.name());
    }

    // The class DetectionCallback provides the payoff and this methods show what could be done with the different types
    @Override
    public void didFindPayoff(Payoff payoff) {
        Log.d("Payoff", payoff.getPayoffType().toString());
        String payoffContent = "";

        switch (payoff.getPayoffType()) {
            case WEB:
                Web web = (Web) payoff;
                payoffContent = web.getUrl();
                break;
            case CONTACT:
                Contact contact = (Contact) payoff;
                int size = contact.getAddresses().size();
                payoffContent = contact.getName();
                for (int i = 0; i < size; i++) {
                    payoffContent += " - " + contact.getAddresses().get(i).getAddress();
                }
                break;
            case EMAIL:
                Email email = (Email) payoff;
                payoffContent = email.getEmailAddress();
                break;
            case LAYOUT:
                Layout layout = (Layout) payoff;
                payoffContent = layout.getLabel() + " with " + layout.getActions().size() + " actions";
                break;
        }

        showPayoffContentDialog(payoff.getPayoffType().name(), payoffContent);
    }

    // This method is an example of how to create a simple dialog to show the payoff content
    private void showPayoffContentDialog(String payoffType, String payoffContent) {

        Bundle args = new Bundle();
        args.putString("payoff_type", payoffType);
        args.putString("payoff_content", payoffContent);
        payoffContentDialogFragment.setArguments(args);

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.add(payoffContentDialogFragment, PayoffContentDialogFragment.TAG);

        ft.commitAllowingStateLoss();
    }

    // ResolverError types: NETWORK_ERROR, LINK_NO_LONGER_ACTIVE, LINK_OUT_OF_RANGE,
    //                      UNEXPECTED, NETWORK_TIMEOUT, UNKNOWN_HOST, HTTP_ERROR;
    @Override
    public void errorOnPayoffResolving(ResolveError resolveError) {
        Log.d("ERRORRESOLVING", resolveError.getErrorCode().toString());
        showPayoffContentDialog("Error on Payoff Resolving", resolveError.getErrorCode().toString());
    }

    // PayoffError types: UNSUPPORTED_TYPE, INVALID_PAYOFF, UNEXPECTED;
    @Override
    public void payoffError(PayoffError payoffError) {
        Log.d("ERROR", payoffError.toString());
        showPayoffContentDialog("Payoff Error", payoffError.getErrorCode().toString());
    }

    public void onDialogDismiss() {
        isPresenting = false;
        startScanning();
    }

    public void onDialogShowing() {
        isPresenting = true;
        captureManager.stopScanning();
    }

    public boolean isDialogShowing() {
        return isPresenting;
    }
}
