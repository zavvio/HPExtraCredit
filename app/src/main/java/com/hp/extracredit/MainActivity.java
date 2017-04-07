package com.hp.extracredit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
//import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hp.extracredit.ui.PostedImageActivity;
import com.hp.extracredit.ui.ScannedItemsActivity;
import com.hp.linkreadersdk.EasyReadingCallback;
import com.hp.linkreadersdk.EasyReadingFragment;
import com.hp.linkreadersdk.ErrorCode;
import com.hp.linkreadersdk.LocationHolder;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, PayoffContentDialogFragment.DialogListener, NetworkErrorDialogFragment.DialogListener
        , AuthenticationErrorDialogFragment.DialogListener {

    private static EasyReadingFragment easyReadingFragment;

    private static final int CAMERA_AND_LOCATION_PERMISSION_CODE = 1;
    private Button startScanButton;
    //private DrawerLayout drawer;

    enum AuthState {
        AUTHORIZING, AUTHORIZED, NOT_AUTHORIZED, CAMERA_FAILED, CAMERA_PERMISSION_DENIED
    }

    private static String AUTH_STATE = "AUTH_STATE";
    private static String SCAN_STATE = "SCAN_STATE";
    private AuthState authState = AuthState.AUTHORIZING;
    private boolean isScanning = false;
    private static Fragment currentFragment = null;

    // The main activity begins with the Easy Reading Fragment provided by the SDK
    // It needs to check before if version is Android M to handle camera permissions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        geolocation = new Geolocation(this);

        replaceFragmentToCurrent();
        /*
        startScanButton = (Button) findViewById(R.id.start_scanning);
        startScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isScanning = true;
                replaceFragmentToCurrent();
                startScanButton.setVisibility(View.GONE);
            }
        });

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                drawer.bringChildToFront(drawerView);
                drawer.requestLayout();
            }
        };
        drawer.setDrawerListener(toggle);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        toggle.syncState();*/

        if (savedInstanceState == null) {
            setAuthState(AuthState.AUTHORIZING);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, CAMERA_AND_LOCATION_PERMISSION_CODE);
                } else {
                    unlockDrawerAndInitializeEasyReadingFragment();
                    startGeolocation();
                }
            } else {
                unlockDrawerAndInitializeEasyReadingFragment();
                startGeolocation();
            }
        }

        //NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        //navigationView.setNavigationItemSelectedListener(this);

        setUpDrawer();
    }

    // Use the Client ID and Secret to create an instance of the preconfigured EasyReadingFragment capable of scanning
    // and presenting the rich content
    private void initializeReadingFragment() {
        easyReadingFragment = EasyReadingFragment.initWithClientID(Credentials.CLIENT_ID, Credentials.CLIENT_SECRET, new EasyReadingCallback() {
            @Override
            public void onAuthenticationSuccess() {
                MainActivity.this.onAuthenticationSuccess();
            }

            @Override
            public void onAuthenticationError(ErrorCode errorCode) {
                MainActivity.this.onAuthenticationError(errorCode);
            }

        }, this);
        currentFragment = easyReadingFragment;
    }

    // To be able to scan, the application must be authorized.
    private void onAuthenticationSuccess() {
        setAuthState(AuthState.AUTHORIZED);
    }

    private void showAuthenticationNetworkFailed() {
        NetworkErrorDialogFragment networkErrorDialogFragment = new NetworkErrorDialogFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(networkErrorDialogFragment, NetworkErrorDialogFragment.TAG);
        ft.commitAllowingStateLoss();
    }

    private void showAuthenticationFailed() {
        AuthenticationErrorDialogFragment authenticationErrorDialogFragment = new AuthenticationErrorDialogFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(authenticationErrorDialogFragment, AuthenticationErrorDialogFragment.TAG);
        ft.commitAllowingStateLoss();
    }

    public void reauthenticate() {

        setAuthState(AuthState.AUTHORIZING);
        easyReadingFragment.reauthenticate(Credentials.CLIENT_ID, Credentials.CLIENT_SECRET, new EasyReadingCallback() {
            @Override
            public void onAuthenticationSuccess() {
                MainActivity.this.onAuthenticationSuccess();
            }

            @Override
            public void onAuthenticationError(ErrorCode errorCode) {
                MainActivity.this.onAuthenticationError(errorCode);
            }

        }, this);
    }

    private void onAuthenticationError(ErrorCode errorCode) {
        android.util.Log.d("Auth", "ErrorCode " + errorCode.name());
        hideProgress();
        if (errorCode == ErrorCode.CONNECTION_ERROR) {
            showAuthenticationNetworkFailed();
        } else {
            showAuthenticationFailed();
        }
    }

    private void showProgress() {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
        progress.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
        progress.setVisibility(View.INVISIBLE);
    }

    private void setAuthState(AuthState authorizing) {
        authState = authorizing;
        switch (authorizing) {
            case AUTHORIZING:
                showProgress();
                resetMessage();
                break;
            case AUTHORIZED:
                hideProgress();
                resetMessage();
                if (!isScanning) {
                   // startScanButton.setEnabled(true);
                    //startScanButton.setVisibility(View.VISIBLE);
                }
                break;
            case NOT_AUTHORIZED:
                TextView message = (TextView) findViewById(R.id.message);
                message.setText(R.string.not_authorized);
                hideProgress();
                //startScanButton.setVisibility(View.INVISIBLE);
                break;
            case CAMERA_PERMISSION_DENIED:
                message = (TextView) findViewById(R.id.message);
                message.setText(R.string.camera_permission_denied);
                hideProgress();
                //startScanButton.setVisibility(View.INVISIBLE);
                break;
            case CAMERA_FAILED:
                message = (TextView) findViewById(R.id.message);
                message.setText(R.string.camera_failed);
                hideProgress();
                //startScanButton.setVisibility(View.INVISIBLE);
                final FrameLayout startScanFragment = (FrameLayout) findViewById(R.id.flContent);
                startScanFragment.setVisibility(View.VISIBLE);
                break;
        }
        Log.d("AuthState", authorizing + "");
    }


    private void resetMessage() {
        final TextView message = (TextView) findViewById(R.id.message);
        message.setText("");
    }

    private Geolocation geolocation;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_AND_LOCATION_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    unlockDrawerAndInitializeEasyReadingFragment();
                    if (grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        startGeolocation();
                    }
                } else {
                    setAuthState(AuthState.CAMERA_PERMISSION_DENIED);
                }
                break;
        }
    }

    private void unlockDrawerAndInitializeEasyReadingFragment() {
       // drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        initializeReadingFragment();
    }

    private void startGeolocation() {
        geolocation.requestLocationUpdates();
        easyReadingFragment.setLocationHolder(new LocationHolder() {
            @Override
            public Location getUserLocation() {
                return geolocation.getLastLocation();
            }
        });
    }

    // SDK provides re-authentication and resume functions for you to use in your error handling logic.
    @Override
    public void onReauthenticationClick() {
        reauthenticate();
    }

    @Override
    public void onCancelClick() {
        setAuthState(AuthState.NOT_AUTHORIZED);
    }

    @Override
    public void onConfirmation() {
        setAuthState(AuthState.NOT_AUTHORIZED);
    }

    @Override
    public void onBackPressed() {
      //  DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
       // if (drawer.isDrawerOpen(GravityCompat.START)) {
       //     drawer.closeDrawer(GravityCompat.START);
       // } else {
            super.onBackPressed();
        //}
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == com.hp.extracredit.R.id.default_ux) {
            currentFragment = easyReadingFragment;
        } else if (id == com.hp.extracredit.R.id.custom_ux) {
            currentFragment = new CustomUXFragment();
        }

        if (isScanning) {
            replaceFragmentToCurrent();
        }

        //DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        //drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void replaceFragmentToCurrent() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(com.hp.extracredit.R.id.flContent, new CustomUXFragment())
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        geolocation.stopLocationUpdates();
    }

    @Override
    public void onDialogDismiss() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(com.hp.extracredit.R.id.flContent);

        if (fragment instanceof CustomUXFragment) {
            ((CustomUXFragment) fragment).onDialogDismiss();
        }
    }

    @Override
    public void onDialogShowing() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(com.hp.extracredit.R.id.flContent);

        if (fragment instanceof CustomUXFragment) {
            ((CustomUXFragment) fragment).onDialogShowing();
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(AUTH_STATE, authState);
        outState.putBoolean(SCAN_STATE, isScanning);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        AuthState authState = (AuthState) savedInstanceState.getSerializable(AUTH_STATE);
        setAuthState(authState);
        isScanning = savedInstanceState.getBoolean(SCAN_STATE);
        if (isScanning) {
           // startScanButton.setVisibility(View.GONE);
        }
    }

    private void setUpDrawer() {
        new DrawerBuilder().withActivity(this).build();
        //if you want to update the items at a later time it is recommended to keep it in a variable
//        PrimaryDrawerItem item1 = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.drawer_item_home);
//        SecondaryDrawerItem item2 = new SecondaryDrawerItem().withIdentifier(2).withName(R.string.drawer_item_settings);

        // Create the AccountHeader
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.hp_light_blue)
                .addProfiles(
                        new ProfileDrawerItem().withName("Mike Penz").withEmail("Available Points: 3426").withIcon(getResources().getDrawable(R.drawable.ic_launcher)),
                        new ProfileDrawerItem().withName("John Yang").withEmail("Available Points: 546").withIcon(getResources().getDrawable(R.drawable.ic_launcher))
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();

        //create the drawer and remember the `Drawer` result object
        Drawer result = new DrawerBuilder()
                .withActivity(this)
//                .withToolbar(toolbar)
                .withAccountHeader(headerResult)
                .withTranslucentStatusBar(false)
                .withActionBarDrawerToggle(false)
                .addDrawerItems(
                        new PrimaryDrawerItem().withIdentifier(1).withName("My Scans").withIcon(R.drawable.about_icon),
//                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withIdentifier(2).withName("My Posts").withIcon(R.drawable.about_icon),
                        new PrimaryDrawerItem().withIdentifier(3).withName("My Social").withIcon(R.drawable.icon_share)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                // do something with the clicked item :D
                switch (position) {
                    case 1:
                        startActivity(new Intent(MainActivity.this, ScannedItemsActivity.class));
                        break;
                    case 2:
                        startActivity(new Intent(MainActivity.this, PostedImageActivity.class));
                        break;
                    default:
                        break;
                }
                return false;
            }
        })
                .build();

//        result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}