package com.lak.pi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lak.pi.app.App;
import com.lak.pi.common.ActivityBase;
import com.lak.pi.dialogs.FriendRequestActionDialog;

public class MainActivity extends ActivityBase implements FriendRequestActionDialog.AlertPositiveListener {

    Toolbar mToolbar;
    LinearLayout mContainerAdmob;
    AdView mAdView;
    Fragment fragment;

    Boolean action = false;
    int page = 0;
    private AppBarLayout mAppBarLayout;
    private BottomNavigationView mNavBottomView;
    private Menu mNavMenu;
    // used to store app title`
    private CharSequence mTitle;
    private int pageId = PAGE_MEDIA_STREAM;

    private Boolean restore = false;
    //This is the handler that will manager to process the broadcast intent
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            // Extract data included in the Intent
            // String message = intent.getStringExtra("message");


            refreshBadges();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Get intent data

        Intent i = getIntent();

        pageId = i.getIntExtra("pageId", PAGE_MEDIA_STREAM);

        // Send user geolocation data to server

        App.getInstance().setLocation();

        // Initialize Admob

        //   MobileAds.initialize(this, getString(R.string.admob_app_id));

        if (savedInstanceState != null) {

            //Restore the fragment's instance
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, "currentFragment");

            restore = savedInstanceState.getBoolean("restore");
            mTitle = savedInstanceState.getString("mTitle");

        } else {

            fragment = new Fragment();

            restore = false;
            mTitle = getString(R.string.app_name);
        }

        if (fragment != null) {

            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.container_body, fragment).commit();
        }

        mToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(mTitle);
        //my credit balance
     //   TextView tvToolbartitle = mToolbar.findViewById(R.id.tvToolbartitle);
     //   tvToolbartitle.setText(mTitle);

        //because you added icon and MADE CHAGES TO REAL TOOL BAR NEED TO COPY PASTE THIS TWO LINE ON ECERY PAGE WHERE YOU WNAT TOOL BAR TITLE IS HIDE
        //

        mAppBarLayout = findViewById(R.id.appbar_layout);

        // Admob


        // Bottom Navigation

        mNavBottomView = findViewById(R.id.nav_bottom_view);
//        BottomNavigationViewHelper.disableShiftMode(mNavBottomView);

        mNavBottomView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                hideKeyboard();

                displayFragment(item.getItemId(), item.getTitle().toString());

                mAppBarLayout.setExpanded(true, true);

                return true;
            }
        });

        mNavMenu = mNavBottomView.getMenu();


        if (!restore) {

            switch (pageId) {

                case PAGE_NOTIFICATIONS: {

                    displayFragment(mNavMenu.findItem(R.id.nav_notifications).getItemId(), getString(R.string.title_activity_notifications));

                    break;
                }

                case PAGE_MESSAGES: {

                    displayFragment(mNavMenu.findItem(R.id.nav_messages).getItemId(), getString(R.string.title_activity_dialogs));

                    break;
                }

                default: {

                    // Show default section "Media"

                    displayFragment(mNavMenu.findItem(R.id.nav_stream).getItemId(), getString(R.string.title_activity_media));

                    break;
                }
            }
        }
    }

    public void showBadge(Context context, BottomNavigationView bottomNavigationView, @IdRes int itemId, int count) {

        BottomNavigationItemView itemView = bottomNavigationView.findViewById(itemId);

        if (itemView.getChildCount() == 2) {

            View badge = LayoutInflater.from(context).inflate(R.layout.bottom_nav_notifications_badge, bottomNavigationView, false);

            // TextView text = badge.findViewById(R.id.badge_text_view);
            // text.setText(value);
            itemView.addView(badge);
        }
    }

    public void removeBadge(BottomNavigationView bottomNavigationView, @IdRes int itemId) {

        BottomNavigationItemView itemView = bottomNavigationView.findViewById(itemId);

        if (itemView.getChildCount() == 3) {

            itemView.removeViewAt(2);
        }
    }

    public void refreshBadges() {

        if (App.getInstance().getNotificationsCount() != 0) {

            showBadge(this, mNavBottomView, R.id.nav_notifications, App.getInstance().getNotificationsCount());

        } else {

            removeBadge(mNavBottomView, R.id.nav_notifications);
        }

        if (App.getInstance().getMessagesCount() != 0) {

            showBadge(this, mNavBottomView, R.id.nav_messages, App.getInstance().getMessagesCount());

        } else {

            removeBadge(mNavBottomView, R.id.nav_messages);
        }

        if (App.getInstance().getNewFriendsCount() != 0 || App.getInstance().getNewMatchesCount() != 0 || App.getInstance().getGuestsCount() != 0) {

            showBadge(this, mNavBottomView, R.id.nav_menu, App.getInstance().getNotificationsCount());

        } else {

            removeBadge(mNavBottomView, R.id.nav_menu);
        }
    }


    private int currentId = -1;

    private void displayFragment(int id, String title) {

        action = false;
        currentId = id;

        switch (id) {

            case R.id.nav_menu: {

                page = PAGE_MENU;

                mNavBottomView.getMenu().findItem(R.id.nav_menu).setChecked(true);

                fragment = new MenuFragment();

                action = true;

                break;
            }

            case R.id.nav_messages: {

                page = PAGE_MESSAGES;

                mNavBottomView.getMenu().findItem(R.id.nav_messages).setChecked(true);

                fragment = new DialogsFragment();

                action = true;

                break;
            }

            case R.id.nav_notifications: {

                page = PAGE_NOTIFICATIONS;

                mNavBottomView.getMenu().findItem(R.id.nav_notifications).setChecked(true);

                fragment = new NotificationsFragment();

                action = true;

                break;
            }

            case R.id.nav_finder: {

                page = PAGE_FINDER;

                mNavBottomView.getMenu().findItem(R.id.nav_finder).setChecked(true);

                fragment = new FinderFragment();

                action = true;

                break;
            }

            case R.id.nav_stream: {

                page = PAGE_MEDIA_STREAM;

                mNavBottomView.getMenu().findItem(R.id.nav_stream).setChecked(true);

                fragment = new FlowFragment();

                action = true;

                break;
            }
        }

        if (action && fragment != null) {

            getSupportActionBar().setDisplayShowCustomEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            if(id == R.id.nav_finder)
            {
                getSupportActionBar().setTitle("");
            }
            else {
                getSupportActionBar().setTitle(title);
            }

            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.container_body, fragment).commit();
        }
    }

    private void hideKeyboard() {

        View view = this.getCurrentFocus();

        if (view != null) {

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void updateNavItemCounter(NavigationView nav, @IdRes int itemId, int count) {

        TextView view = MenuItemCompat.getActionView(nav.getMenu().findItem(itemId)).findViewById(R.id.counter);
        view.setText(String.valueOf(count));

        if (count <= 0) {

            view.setVisibility(View.GONE);

        } else {

            view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putBoolean("restore", true);
        outState.putString("mTitle", getSupportActionBar().getTitle().toString());
        getSupportFragmentManager().putFragment(outState, "currentFragment", fragment);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onAcceptRequest(int position) {

        NotificationsFragment p = (NotificationsFragment) fragment;
        p.onAcceptRequest(position);
    }

    @Override
    public void onRejectRequest(int position) {

        NotificationsFragment p = (NotificationsFragment) fragment;
        p.onRejectRequest(position);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {

            case android.R.id.home: {

                return true;
            }

            default: {

                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }

    @Override
    public void setTitle(CharSequence title) {
        Log.e("MainActivity","Title : "+title +"  id : "+currentId);
        mTitle = title;
     //   if(R.id.nav_finder == currentId)
       // {
         //   getSupportActionBar().setTitle(" ");
      //  }
    //    else
      //  {
            getSupportActionBar().setTitle(title);
     //   }

    }
    @Override
    public void onResume() {

        super.onResume();

        refreshBadges();

        if (App.getInstance().getAdmob() == ADMOB_DISABLED) {
            if(mContainerAdmob!=null) {

                mContainerAdmob.setVisibility(View.GONE);
            }
        }

        registerReceiver(mMessageReceiver, new IntentFilter(TAG_UPDATE_BADGES));
    }

    @Override
    public void onPause() {

        super.onPause();

        unregisterReceiver(mMessageReceiver);
    }
}
