package com.lak.pi;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.balysv.materialripple.MaterialRippleLayout;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.lak.pi.adapter.GiftsSelectListAdapter;
import com.lak.pi.model.BaseGift;
import com.lak.pi.util.Helper;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.lak.pi.adapter.CommentsListAdapter;
import com.lak.pi.app.App;
import com.lak.pi.constants.Constants;
import com.lak.pi.dialogs.CommentActionDialog;
import com.lak.pi.dialogs.CommentDeleteDialog;
import com.lak.pi.dialogs.MixedCommentActionDialog;
import com.lak.pi.dialogs.MyCommentActionDialog;
import com.lak.pi.dialogs.MyPhotoActionDialog;
import com.lak.pi.dialogs.PhotoActionDialog;
import com.lak.pi.dialogs.PhotoDeleteDialog;
import com.lak.pi.dialogs.PhotoReportDialog;
import com.lak.pi.model.Comment;
import com.lak.pi.model.Image;
import com.lak.pi.util.Api;
import com.lak.pi.util.CommentInterface;
import com.lak.pi.util.CustomRequest;
import com.lak.pi.view.ResizableImageView;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import github.ankushsachdeva.emojicon.EditTextImeBackListener;
import github.ankushsachdeva.emojicon.EmojiconEditText;
import github.ankushsachdeva.emojicon.EmojiconGridView;
import github.ankushsachdeva.emojicon.EmojiconTextView;
import github.ankushsachdeva.emojicon.EmojiconsPopup;
import github.ankushsachdeva.emojicon.emoji.Emojicon;


public class ViewImageFragment extends Fragment implements Constants, SwipeRefreshLayout.OnRefreshListener, CommentInterface {

    private ProgressDialog pDialog;

    private MaterialRippleLayout mLikeButton;
    private ImageView mLikeIcon;

    private LinearLayout mStatusContainer;
    private ImageView mStatusIcon;
    private TextView mStatusTitle;

    private AdView mAdView;
    private Toolbar mToolbar;

    private RelativeLayout mBannerContainer;
    private LinearLayout mCommentsContainer, mLikesContainer;

    SwipeRefreshLayout mContentContainer;
    RelativeLayout mErrorScreen, mLoadingScreen, mEmptyScreen;
    LinearLayout mCommentFormContainer;
    CoordinatorLayout mContentScreen;

    EmojiconEditText mCommentText;

    private RecyclerView mRecyclerView;
    private NestedScrollView mNestedView;

    LinearLayout giftButton;
    Button mRetryBtn;

    private LinearLayout mEmojiButton, mSendComment;
    ImageView mEmojiButtonIcon;

    TextView mFullnameTitle, mUsernameTitle, mModeTitle, mItemTimeAgo, mItemLikesCount, mItemCommentsCount;
    ResizableImageView mItemImg;
    CircularImageView mPhotoImage, mVerifiedIcon, mOnlineIcon;

    EmojiconTextView mItemText;

    ImageView mItemPlay;

    ImageLoader imageLoader = App.getInstance().getImageLoader();


    private ArrayList<Comment> itemsList;
    private CommentsListAdapter itemsAdapter;

    Image item = new Image();

    long itemId = 0, replyToUserId = 0;
    int arrayLength = 0;
    String commentText;

    private Boolean loading = false;
    private Boolean restore = false;
    private Boolean preload = false;

    EmojiconsPopup popup;

    private Boolean loadingComplete = false;

    public ViewImageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        setHasOptionsMenu(true);

        initpDialog();

        Intent i = getActivity().getIntent();

        itemId = i.getLongExtra("itemId", 0);

        itemsList = new ArrayList<Comment>();
        itemsAdapter = new CommentsListAdapter(getActivity(), itemsList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_view_image, container, false);

        popup = new EmojiconsPopup(rootView, getActivity());

        popup.setSizeForSoftKeyboard();

        popup.setOnEmojiconClickedListener(new EmojiconGridView.OnEmojiconClickedListener() {

            @Override
            public void onEmojiconClicked(Emojicon emojicon) {

                mCommentText.append(emojicon.getEmoji());
            }
        });

        popup.setOnEmojiconBackspaceClickedListener(new EmojiconsPopup.OnEmojiconBackspaceClickedListener() {

            @Override
            public void onEmojiconBackspaceClicked(View v) {

                KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                mCommentText.dispatchKeyEvent(event);
            }
        });

        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {

                setIconEmojiKeyboard();
            }
        });

        popup.setOnSoftKeyboardOpenCloseListener(new EmojiconsPopup.OnSoftKeyboardOpenCloseListener() {

            @Override
            public void onKeyboardOpen(int keyBoardHeight) {

            }

            @Override
            public void onKeyboardClose() {

                if (popup.isShowing())

                    popup.dismiss();
            }
        });

        popup.setOnEmojiconClickedListener(new EmojiconGridView.OnEmojiconClickedListener() {

            @Override
            public void onEmojiconClicked(Emojicon emojicon) {

                mCommentText.append(emojicon.getEmoji());
            }
        });

        popup.setOnEmojiconBackspaceClickedListener(new EmojiconsPopup.OnEmojiconBackspaceClickedListener() {

            @Override
            public void onEmojiconBackspaceClicked(View v) {

                KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                mCommentText.dispatchKeyEvent(event);
            }
        });

        if (savedInstanceState != null) {

            restore = savedInstanceState.getBoolean("restore");
            loading = savedInstanceState.getBoolean("loading");
            preload = savedInstanceState.getBoolean("preload");

            replyToUserId = savedInstanceState.getLong("replyToUserId");

        } else {

            restore = false;
            loading = false;
            preload = false;

            replyToUserId = 0;
        }

        if (loading) {

            showpDialog();
        }

        mToolbar = rootView.findViewById(R.id.toolbar);

        ((AppCompatActivity)getActivity()).setSupportActionBar(mToolbar);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setHomeButtonEnabled(true);

        mAdView = rootView.findViewById(R.id.adView);

        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        giftButton=rootView.findViewById(R.id.giftButton);
        mNestedView = rootView.findViewById(R.id.nested_view);

        final GridLayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 1);

        mRecyclerView.setLayoutManager(mLayoutManager);

        itemsAdapter.setOnMoreButtonClickListener(new CommentsListAdapter.OnItemMenuButtonClickListener() {

            @Override
            public void onItemClick(View v, Comment obj, int actionId, int position) {

                switch (actionId){

                    case R.id.action_remove: {

                        FragmentManager fm = getActivity().getSupportFragmentManager();

                        CommentDeleteDialog alert = new CommentDeleteDialog();

                        Bundle b = new Bundle();
                        b.putInt("position", position);
                        b.putLong("itemId", obj.getId());

                        alert.setArguments(b);
                        alert.show(fm, "alert_dialog_comment_delete");

                        break;
                    }

                    case R.id.action_reply: {

                        if (App.getInstance().getId() != 0) {

                            replyToUserId = obj.getOwner().getId();

                            mCommentText.setText("@" + obj.getOwner().getUsername() + ", ");
                            mCommentText.setSelection(mCommentText.getText().length());

                            mCommentText.requestFocus();

                        }

                        break;
                    }
                }
            }
        });

        mRecyclerView.setAdapter(itemsAdapter);

        mRecyclerView.setNestedScrollingEnabled(false);

        mStatusContainer = rootView.findViewById(R.id.status_container);
        mStatusIcon = rootView.findViewById(R.id.status_icon);
        mStatusTitle = rootView.findViewById(R.id.status_label);

        mLikeButton = rootView.findViewById(R.id.like_button);
        mLikeIcon = rootView.findViewById(R.id.like_icon);

        mLikeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (item.isMyLike()) {

                    mLikeIcon.setColorFilter(ContextCompat.getColor(getActivity(), R.color.grey_40), android.graphics.PorterDuff.Mode.SRC_IN);

                    item.setMyLike(false);

                    item.setLikesCount(item.getLikesCount() - 1);

                } else {

                    mLikeIcon.setColorFilter(ContextCompat.getColor(getActivity(), R.color.fab_like), android.graphics.PorterDuff.Mode.SRC_IN);

                    item.setMyLike(true);

                    item.setLikesCount(item.getLikesCount() + 1);
                }

                like();
            }
        });

        mLikeButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    animateIcon(mLikeIcon);
                }

                return false;
            }
        });

        mEmptyScreen = rootView.findViewById(R.id.emptyScreen);
        mErrorScreen = rootView.findViewById(R.id.errorScreen);
        mLoadingScreen = rootView.findViewById(R.id.loadingScreen);
        mContentContainer = rootView.findViewById(R.id.refresh_view);
        mContentContainer.setOnRefreshListener(this);

        mContentScreen = rootView.findViewById(R.id.content_screen);
        mCommentFormContainer = rootView.findViewById(R.id.commentFormContainer);

        mBannerContainer = rootView.findViewById(R.id.banner_container);
        mCommentsContainer = rootView.findViewById(R.id.comments_container);
        mLikesContainer = rootView.findViewById(R.id.likes_container);

        mCommentText = rootView.findViewById(R.id.commentText);
        mSendComment = rootView.findViewById(R.id.sendButton);
        mEmojiButton = rootView.findViewById(R.id.emojiButton);
        mEmojiButtonIcon = rootView.findViewById(R.id.emojiButtonIcon);

        giftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choiceGiftDialog();
            }
        });
        mSendComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                send();
            }
        });

        mRetryBtn = rootView.findViewById(R.id.retryBtn);

        mRetryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (App.getInstance().isConnected()) {

                    showLoadingScreen();

                    getItem();
                }
            }
        });



        mFullnameTitle = rootView.findViewById(R.id.fullname_label);
        mUsernameTitle = rootView.findViewById(R.id.username_label);

        mPhotoImage = rootView.findViewById(R.id.photo_image);
        mVerifiedIcon = rootView.findViewById(R.id.verified_icon);
        mOnlineIcon = rootView.findViewById(R.id.online_icon);

        mModeTitle = rootView.findViewById(R.id.mode_label);

        mItemText = rootView.findViewById(R.id.itemText);
        mItemTimeAgo = rootView.findViewById(R.id.date_label);
        mItemLikesCount = rootView.findViewById(R.id.likes_count_label);
        mItemCommentsCount = rootView.findViewById(R.id.comments_count_label);

        mItemImg = rootView.findViewById(R.id.itemImage);
        mItemPlay = rootView.findViewById(R.id.itemPlay);

        if (!EMOJI_KEYBOARD) {

            mEmojiButton.setVisibility(View.GONE);
        }
        imageViewer = rootView.findViewById(R.id.imageViewer);



        mEmojiButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (!popup.isShowing()) {

                    if (popup.isKeyBoardOpen()){

                        popup.showAtBottom();
                        setIconSoftKeyboard();

                    } else {

                        mCommentText.setFocusableInTouchMode(true);
                        mCommentText.requestFocus();
                        popup.showAtBottomPending();

                        final InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(mCommentText, InputMethodManager.SHOW_IMPLICIT);
                        setIconSoftKeyboard();
                    }

                } else {

                    popup.dismiss();
                }
            }
        });

        EditTextImeBackListener er = new EditTextImeBackListener() {

            @Override
            public void onImeBack(EmojiconEditText ctrl, String text) {

                hideEmojiKeyboard();
            }
        };

        mCommentText.setOnEditTextImeBackListener(er);

        if (!restore) {

            if (App.getInstance().isConnected()) {

                showLoadingScreen();
                getItem();

            } else {

                showErrorScreen();
            }

        } else {

            if (App.getInstance().isConnected()) {

                if (!preload) {

                    loadingComplete();
                    updateItem();

                } else {

                    showLoadingScreen();
                }

            } else {

                showErrorScreen();
            }
        }

        // Inflate the layout for this fragment
        return rootView;
    }
    RelativeLayout imageViewer;
     public void hideEmojiKeyboard() {

        popup.dismiss();
    }

    public void setIconEmojiKeyboard() {

        mEmojiButtonIcon.setBackgroundResource(R.drawable.ic_emoji);
    }

    public void setIconSoftKeyboard() {

        mEmojiButtonIcon.setBackgroundResource(R.drawable.ic_keyboard);
    }

    public void onDestroyView() {

        super.onDestroyView();

        hidepDialog();
    }

    protected void initpDialog() {

        pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage(getString(R.string.msg_loading));
        pDialog.setCancelable(false);
    }

    protected void showpDialog() {

        if (!pDialog.isShowing()) pDialog.show();
    }

    protected void hidepDialog() {

        if (pDialog.isShowing()) pDialog.dismiss();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putBoolean("restore", true);
        outState.putBoolean("loading", loading);
        outState.putBoolean("preload", preload);

        outState.putLong("replyToUserId", replyToUserId);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRefresh() {

        if (App.getInstance().isConnected()) {

            mContentContainer.setRefreshing(true);
            getItem();

        } else {

            mContentContainer.setRefreshing(false);
        }
    }

    public String getItemModeText(int postMode) {

        switch (postMode) {

            case 0: {

                return getString(R.string.label_image_for_public);
            }

            default: {

                return getString(R.string.label_image_for_friends);
            }
        }
    }

    private void showAdBanner() {

        mBannerContainer.setVisibility(View.VISIBLE);

        if (App.getInstance().getAdmob() == ADMOB_ENABLED && App.getInstance().getAllowAdBannerInGalleryItem() == 1) {

            AdRequest adRequest = new AdRequest.Builder().build();

            mAdView.setAdListener(new AdListener() {

                @Override
                public void onAdLoaded() {

                    super.onAdLoaded();

                    mBannerContainer.setVisibility(View.VISIBLE);

                    Log.e("ADMOB", "onAdLoaded");
                }

                @Override
                public void onAdFailedToLoad(int i) {

                    super.onAdFailedToLoad(i);

                    mBannerContainer.setVisibility(View.GONE);

                    Log.e("ADMOB", "onAdFailedToLoad");
                }
            });

            mAdView.loadAd(adRequest);

        } else {

            Log.e("ADMOB", "ADMOB_DISABLED");

            mBannerContainer.setVisibility(View.GONE);
        }
    }

    public void updateItem() {

        if (imageLoader == null) {

            imageLoader = App.getInstance().getImageLoader();
        }

        showAdBanner();
        updateCounters();
        updateStatus();

        mItemPlay.setVisibility(View.GONE);

        mVerifiedIcon.setVisibility(View.GONE);
        mOnlineIcon.setVisibility(View.GONE);

        mFullnameTitle.setText(item.getOwner().getFullname());
        mUsernameTitle.setText("@" + item.getOwner().getUsername());

        mModeTitle.setText(getItemModeText(item.getAccessMode()));

        if (item.getOwner().getVerify() == 1) {

            mVerifiedIcon.setVisibility(View.VISIBLE);
        }

        if (item.getOwner().getLowPhotoUrl().length() != 0 &&
                (App.getInstance().getSettings().isAllowShowNotModeratedProfilePhotos() || App.getInstance().getId() == item.getId() || item.getModerateAt() != 0)) {

            mPhotoImage.setVisibility(View.VISIBLE);

            imageLoader.get(item.getOwner().getLowPhotoUrl(), ImageLoader.getImageListener(mPhotoImage, R.drawable.profile_default_photo, R.drawable.profile_default_photo));

        } else {

            mPhotoImage.setVisibility(View.VISIBLE);
            mPhotoImage.setImageResource(R.drawable.profile_default_photo);
        }

        mPhotoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), ProfileActivity.class);
                intent.putExtra("profileId", item.getOwner().getId());
                startActivity(intent);
            }
        });

        mFullnameTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), ProfileActivity.class);
                intent.putExtra("profileId", item.getOwner().getId());
                startActivity(intent);
            }
        });

        mLikesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), LikersActivity.class);
                intent.putExtra("itemId", item.getId());
                startActivity(intent);
            }
        });

        if (item.isMyLike()) {

            mLikeIcon.setColorFilter(ContextCompat.getColor(getActivity(), R.color.fab_like), android.graphics.PorterDuff.Mode.SRC_IN);

        } else {

            mLikeIcon.setColorFilter(ContextCompat.getColor(getActivity(), R.color.grey_40), android.graphics.PorterDuff.Mode.SRC_IN);
        }

        mItemTimeAgo.setText(item.getTimeAgo());
        mItemTimeAgo.setVisibility(View.VISIBLE);

        if (item.getComment().length() > 0) {

            mItemText.setText(item.getComment().replaceAll("<br>", "\n"));

            mItemText.setVisibility(View.VISIBLE);

        } else {

            mItemText.setVisibility(View.GONE);
        }

        if (item.getItemType() == Constants.GALLERY_ITEM_TYPE_VIDEO && item.getVideoUrl().length() > 0) {

            mItemImg.setImageResource(R.drawable.ic_video_preview);
            mItemImg.setVisibility(View.VISIBLE);
            mItemPlay.setVisibility(View.VISIBLE);

            if(item.getVideoUrl().contains("youtu")){
                Glide.with(context)
                        .load(item.getOriginImgUrl())
                        .into(mItemImg);
            }else{
                Glide.with(context)
                        .load(item.getVideoUrl())
                        .into(mItemImg);
            }

        } else {
            mItemPlay.setVisibility(View.GONE);

            if (item.getItemType() == Constants.GALLERY_ITEM_TYPE_IMAGE && item.getImgUrl().length() > 0) {

                imageLoader.get(item.getImgUrl(), ImageLoader.getImageListener(mItemImg, R.drawable.img_loading, R.drawable.img_loading));
                mItemImg.setVisibility(View.VISIBLE);
            }
        }

            if(item.getImgUrl() != null && !item.getImgUrl().isEmpty())
            Picasso.with(context)
                    .load(item.getImgUrl())
                    .resize(getResources().getDisplayMetrics().widthPixels - 70, 500)
                    .into(mItemImg);






        mItemImg.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (item.getItemType() == Constants.GALLERY_ITEM_TYPE_VIDEO) {
                    if(item.getVideoUrl().contains("youtu")){
                          Intent intent = YouTubeStandalonePlayer.createVideoIntent((ViewImageActivity)context, getString(R.string.api_key_youtube),getVideoId(item.getVideoUrl()));
                 // Intent i = new Intent(getActivity(), VideoViewActivity.class);
                 //   i.putExtra("videoUrl", item.getVideoUrl());
                   startActivity(intent);
                    }else{
                        Intent intent = new Intent(getActivity(), VideoViewActivity.class);
                        intent.putExtra("videoUrl", item.getVideoUrl());
                        startActivity(intent);
                    }


                } else {

                    Intent i = new Intent(getActivity(), PhotoViewActivity.class);
                    i.putExtra("imgUrl", item.getImgUrl());
                    startActivity(i);
                }
            }
        });
    }

    private final static String expression = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
    public static String getVideoId(String videoUrl) {
        if (videoUrl == null || videoUrl.trim().length() <= 0){
            return null;
        }
        Pattern pattern = Pattern.compile(expression);
        Matcher matcher = pattern.matcher(videoUrl);
        try {
            if (matcher.find())
                return matcher.group();
        } catch (ArrayIndexOutOfBoundsException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void updateCounters() {

        mItemLikesCount.setText(Integer.toString(item.getLikesCount()));

        if (item.getLikesCount() > 0) {

            mLikesContainer.setVisibility(View.VISIBLE);
            mItemLikesCount.setVisibility(View.VISIBLE);

        } else {

            mLikesContainer.setVisibility(View.GONE);
            mItemLikesCount.setVisibility(View.GONE);
        }

        mItemCommentsCount.setText(Integer.toString(item.getCommentsCount()));

        if (item.getCommentsCount() > 0) {

            mCommentsContainer.setVisibility(View.VISIBLE);
            mItemCommentsCount.setVisibility(View.VISIBLE);

        } else {

            mCommentsContainer.setVisibility(View.GONE);
            mItemCommentsCount.setVisibility(View.GONE);
        }
    }

    private void updateStatus() {

        mStatusContainer.setVisibility(View.GONE);

        if (item.getOwner().getId() == App.getInstance().getId()) {

            if (item.getRemoveAt() != 0) {

                mStatusIcon.setImageResource(R.drawable.ic_rejected);
                mStatusTitle.setText(getString(R.string.msg_gallery_item_status_rejected));

            } else {

                if (item.getModerateAt() == 0) {

                    mStatusIcon.setImageResource(R.drawable.ic_wait);
                    mStatusTitle.setText(getString(R.string.msg_gallery_item_status_wait));

                } else {

                    mStatusIcon.setImageResource(R.drawable.ic_accept);
                    mStatusTitle.setText(getString(R.string.msg_gallery_item_status_approved));
                }
            }

            mStatusContainer.setVisibility(View.VISIBLE);
        }

        mItemLikesCount.setText(Integer.toString(item.getLikesCount()));

        if (item.getLikesCount() > 0) {

            mLikesContainer.setVisibility(View.VISIBLE);
            mItemLikesCount.setVisibility(View.VISIBLE);

        } else {

            mLikesContainer.setVisibility(View.GONE);
            mItemLikesCount.setVisibility(View.GONE);
        }

        mItemCommentsCount.setText(Integer.toString(item.getCommentsCount()));

        if (item.getCommentsCount() > 0) {

            mCommentsContainer.setVisibility(View.VISIBLE);
            mItemCommentsCount.setVisibility(View.VISIBLE);

        } else {

            mCommentsContainer.setVisibility(View.GONE);
            mItemCommentsCount.setVisibility(View.GONE);
        }
    }

    public void getItem() {

        CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_GALLERY_GET_ITEM, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        if (!isAdded() || getActivity() == null) {

                            Log.e("ERROR", "ViewImageFragment Not Added to Activity");

                            return;
                        }

                        try {

                            arrayLength = 0;

                            if (!response.getBoolean("error")) {

                                itemsList.clear();

                                itemId = response.getInt("itemId");

                                if (response.has("items")) {

                                    JSONArray itemsArray = response.getJSONArray("items");

                                    arrayLength = itemsArray.length();

                                    if (arrayLength > 0) {

                                        for (int i = 0; i < itemsArray.length(); i++) {

                                            JSONObject itemObj = (JSONObject) itemsArray.get(i);

                                            item = new Image(itemObj);

                                            updateItem();
                                        }
                                    }
                                }

                                if (response.has("comments") && item.getOwner().getAllowPhotosComments() == 1) {

                                    JSONObject commentsObj = response.getJSONObject("comments");

                                    if (commentsObj.has("items")) {

                                        JSONArray commentsArray = commentsObj.getJSONArray("items");

                                        arrayLength = commentsArray.length();

                                        if (arrayLength > 0) {

                                            for (int i = commentsArray.length() - 1; i > -1 ; i--) {

                                                JSONObject itemObj = (JSONObject) commentsArray.get(i);

                                                Comment comment = new Comment(itemObj);

                                                itemsList.add(comment);
                                            }
                                        }
                                    }
                                }

                                loadingComplete();

                            } else {

                                showErrorScreen();
                            }

                        } catch (JSONException e) {

                            showErrorScreen();

                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                if (!isAdded() || getActivity() == null) {

                    Log.e("ERROR", "ViewImageFragment Not Added to Activity");

                    return;
                }

                showErrorScreen();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountId", Long.toString(App.getInstance().getId()));
                params.put("accessToken", App.getInstance().getAccessToken());
                params.put("itemId", Long.toString(itemId));
                params.put("language", "en");

                return params;
            }
        };

        App.getInstance().addToRequestQueue(jsonReq);
    }

    public void send() {

        commentText = mCommentText.getText().toString();
        commentText = commentText.trim();

        /* Ghanshyam For Encoding use - StringEscapeUtils.escapeJava(String text) */

        commentText = StringEscapeUtils.escapeJava(commentText); // Ghanshyam

        if (App.getInstance().isConnected() && App.getInstance().getId() != 0 && commentText.length() > 0) {

            loading = true;

            showpDialog();

            CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_COMMENTS_NEW, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            if (!isAdded() || getActivity() == null) {

                                Log.e("ERROR", "ViewImageFragment Not Added to Activity");

                                return;
                            }

                            try {

                                if (!response.getBoolean("error")) {

                                    if (response.has("comment")) {

                                        JSONObject commentObj = response.getJSONObject("comment");

                                        Comment comment = new Comment(commentObj);

                                        itemsList.add(comment);

                                        itemsAdapter.notifyDataSetChanged();

                                        mCommentText.setText("");
                                        replyToUserId = 0;

                                        mNestedView.post(new Runnable() {

                                            @Override
                                            public void run() {
                                                // Select the last row so it will scroll into view...
                                                mNestedView.fullScroll(View.FOCUS_DOWN);

                                                item.setCommentsCount(item.getCommentsCount() + 1);

                                                updateCounters();
                                            }
                                        });
                                    }

                                    Toast.makeText(getActivity(), getString(R.string.msg_comment_has_been_added), Toast.LENGTH_SHORT).show();

                                }

                            } catch (JSONException e) {

                                e.printStackTrace();

                            } finally {

                                loading = false;

                                hidepDialog();

                                Log.e("ERROR", response.toString());
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    if (!isAdded() || getActivity() == null) {

                        Log.e("ERROR", "ViewImageFragment Not Added to Activity");

                        return;
                    }

                    Log.e("ERROR", error.toString());

                    loading = false;

                    hidepDialog();
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("accountId", Long.toString(App.getInstance().getId()));
                    params.put("accessToken", App.getInstance().getAccessToken());

                    params.put("itemId", Long.toString(item.getId()));
                    params.put("commentText", commentText);

                    params.put("replyToUserId", Long.toString(replyToUserId));

                    return params;
                }
            };

            int socketTimeout = 0;//0 seconds - change to what you want
            RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

            jsonReq.setRetryPolicy(policy);

            App.getInstance().addToRequestQueue(jsonReq);
        }
    }

    public void onPhotoDelete(final int position) {

        Api api = new Api(getActivity());

        api.photoDelete(item.getId());

        getActivity().finish();
    }

    public void onPhotoReport(int position, int reasonId) {

        if (App.getInstance().isConnected()) {

            Api api = new Api(getActivity());

            api.photoReport(item.getId(), reasonId);

        } else {

            Toast.makeText(getActivity(), getText(R.string.msg_network_error), Toast.LENGTH_SHORT).show();
        }
    }

    public void remove(int position) {

        FragmentManager fm = getActivity().getSupportFragmentManager();

        PhotoDeleteDialog alert = new PhotoDeleteDialog();

        Bundle b = new Bundle();
        b.putInt("position", 0);

        alert.setArguments(b);
        alert.show(fm, "alert_dialog_photo_delete");
    }

    public void report(int position) {

        FragmentManager fm = getActivity().getSupportFragmentManager();

        PhotoReportDialog alert = new PhotoReportDialog();

        Bundle b  = new Bundle();
        b.putInt("position", position);
        b.putInt("reason", 0);

        alert.setArguments(b);
        alert.show(fm, "alert_dialog_photo_report");
    }

    public void action(int position) {

        if (item.getOwner().getId() == App.getInstance().getId()) {

            /** Getting the fragment manager */
            FragmentManager fm = getActivity().getSupportFragmentManager();

            /** Instantiating the DialogFragment class */
            MyPhotoActionDialog alert = new MyPhotoActionDialog();

            /** Creating a bundle object to store the selected item's index */
            Bundle b  = new Bundle();

            /** Storing the selected item's index in the bundle object */
            b.putInt("position", position);

            /** Setting the bundle object to the dialog fragment object */
            alert.setArguments(b);

            /** Creating the dialog fragment object, which will in turn open the alert dialog window */

            alert.show(fm, "alert_my_post_action");

        } else {

            /** Getting the fragment manager */
            FragmentManager fm = getActivity().getSupportFragmentManager();

            /** Instantiating the DialogFragment class */
            PhotoActionDialog alert = new PhotoActionDialog();

            /** Creating a bundle object to store the selected item's index */
            Bundle b  = new Bundle();

            /** Storing the selected item's index in the bundle object */
            b.putInt("position", position);

            /** Setting the bundle object to the dialog fragment object */
            alert.setArguments(b);

            /** Creating the dialog fragment object, which will in turn open the alert dialog window */

            alert.show(fm, "alert_post_action");
        }
    }

    public void loadingComplete() {

        itemsAdapter.notifyDataSetChanged();

        showContentScreen();

        if (mContentContainer.isRefreshing()) {

            mContentContainer.setRefreshing(false);
        }
    }

    public void showLoadingScreen() {

        preload = true;

        mContentScreen.setVisibility(View.GONE);
        mErrorScreen.setVisibility(View.GONE);
        mEmptyScreen.setVisibility(View.GONE);

        mLoadingScreen.setVisibility(View.VISIBLE);
    }

    public void showEmptyScreen() {

        mContentScreen.setVisibility(View.GONE);
        mLoadingScreen.setVisibility(View.GONE);
        mErrorScreen.setVisibility(View.GONE);

        mEmptyScreen.setVisibility(View.VISIBLE);
    }

    public void showErrorScreen() {

        mContentScreen.setVisibility(View.GONE);
        mLoadingScreen.setVisibility(View.GONE);
        mEmptyScreen.setVisibility(View.GONE);

        mErrorScreen.setVisibility(View.VISIBLE);
    }

    public void showContentScreen() {

        preload = false;

        mLoadingScreen.setVisibility(View.GONE);
        mErrorScreen.setVisibility(View.GONE);
        mEmptyScreen.setVisibility(View.GONE);

        mContentScreen.setVisibility(View.VISIBLE);

        if (item.getOwner().getAllowPhotosComments() == COMMENTS_DISABLED) {

            mCommentFormContainer.setVisibility(View.GONE);
        }

        loadingComplete = true;

        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void commentAction(int position) {

        final Comment comment = itemsList.get(position);

        if (comment.getOwner().getId() == App.getInstance().getId() && item.getOwner().getId() == App.getInstance().getId()) {

            FragmentManager fm = getActivity().getSupportFragmentManager();

            MyCommentActionDialog alert = new MyCommentActionDialog();

            Bundle b  = new Bundle();

            b.putInt("position", position);

            alert.setArguments(b);

            alert.show(fm, "alert_dialog_my_comment_action");

        } else if (comment.getOwner().getId() != App.getInstance().getId() && item.getOwner().getId() == App.getInstance().getId()) {

            FragmentManager fm = getActivity().getSupportFragmentManager();

            MixedCommentActionDialog alert = new MixedCommentActionDialog();

            Bundle b = new Bundle();

            b.putInt("position", position);

            alert.setArguments(b);

            alert.show(fm, "alert_dialog_mixed_comment_action");

        } else if (comment.getOwner().getId() == App.getInstance().getId() && item.getOwner().getId() != App.getInstance().getId()) {

            FragmentManager fm = getActivity().getSupportFragmentManager();

            MyCommentActionDialog alert = new MyCommentActionDialog();

            Bundle b  = new Bundle();

            b.putInt("position", position);

            alert.setArguments(b);

            alert.show(fm, "alert_dialog_my_comment_action");

        } else {

            /** Getting the fragment manager */
            FragmentManager fm = getActivity().getSupportFragmentManager();

            /** Instantiating the DialogFragment class */
            CommentActionDialog alert = new CommentActionDialog();

            /** Creating a bundle object to store the selected item's index */
            Bundle b  = new Bundle();

            /** Storing the selected item's index in the bundle object */
            b.putInt("position", position);

            /** Setting the bundle object to the dialog fragment object */
            alert.setArguments(b);

            /** Creating the dialog fragment object, which will in turn open the alert dialog window */

            alert.show(fm, "alert_dialog_comment_action");
        }
    }

    public void onCommentReply(final int position) {

        if (item.getOwner().getAllowPhotosComments() == COMMENTS_ENABLED) {

            final Comment comment = itemsList.get(position);

            replyToUserId = comment.getOwner().getId();

            mCommentText.setText("@" + comment.getOwner().getUsername() + ", ");
            mCommentText.setSelection(mCommentText.getText().length());

            mCommentText.requestFocus();

        } else {

            Toast.makeText(getActivity(), getString(R.string.msg_comments_disabled), Toast.LENGTH_SHORT).show();
        }
    }

    public void onCommentRemove(int position) {

        /** Getting the fragment manager */
        FragmentManager fm = getActivity().getSupportFragmentManager();

        /** Instantiating the DialogFragment class */
        CommentDeleteDialog alert = new CommentDeleteDialog();

        /** Creating a bundle object to store the selected item's index */
        Bundle b  = new Bundle();

        /** Storing the selected item's index in the bundle object */
        b.putInt("position", position);

        /** Setting the bundle object to the dialog fragment object */
        alert.setArguments(b);

        /** Creating the dialog fragment object, which will in turn open the alert dialog window */

        alert.show(fm, "alert_dialog_comment_delete");
    }

    public void onCommentDelete(final int position) {

        final Comment comment = itemsList.get(position);

        itemsList.remove(position);
        itemsAdapter.notifyDataSetChanged();

        Api api = new Api(getActivity());

        api.imagesCommentDelete(comment.getId());

        item.setCommentsCount(item.getCommentsCount() - 1);

        updateCounters();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);

        menu.clear();

        inflater.inflate(R.menu.menu_view_item, menu);

//        MainMenu = menu;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        super.onPrepareOptionsMenu(menu);

        if (loadingComplete) {

            if (App.getInstance().getId() != item.getOwner().getId()) {

                menu.removeItem(R.id.action_delete);

            } else {

                menu.removeItem(R.id.action_report);
            }

            //show all menu items
            hideMenuItems(menu, true);

        } else {

            //hide all menu items
            hideMenuItems(menu, false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_delete: {

                remove(0);

                return true;
            }

            case R.id.action_report: {

                report(0);

                return true;
            }

            default: {

                return super.onOptionsItemSelected(item);
            }
        }
    }

    private void hideMenuItems(Menu menu, boolean visible) {

        for (int i = 0; i < menu.size(); i++){

            menu.getItem(i).setVisible(visible);
        }
    }

    public void like() {

        CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_GALLERY_LIKE, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        if (!isAdded() || getActivity() == null) {

                            Log.e("ERROR", "ViewImageFragment Not Added to Activity");

                            return;
                        }

                        try {

                            if (!response.getBoolean("error")) {

                                item.setLikesCount(response.getInt("likesCount"));
                                item.setMyLike(response.getBoolean("myLike"));
                            }

                        } catch (JSONException e) {

                            e.printStackTrace();

                        } finally {

                            updateCounters();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                if (!isAdded() || getActivity() == null) {

                    Log.e("ERROR", "ViewImageFragment Not Added to Activity");

                    return;
                }

                Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountId", Long.toString(App.getInstance().getId()));
                params.put("accessToken", App.getInstance().getAccessToken());
                params.put("itemId", Long.toString(item.getId()));

                return params;
            }
        };

        App.getInstance().addToRequestQueue(jsonReq);
    }

    private void animateIcon(ImageView icon) {

        ScaleAnimation scale = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(175);
        scale.setInterpolator(new LinearInterpolator());

        icon.startAnimation(scale);
    }
    private Context context;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    private void choiceGiftDialog() {

        final GiftsSelectListAdapter giftsAdapter;

        giftsAdapter = new GiftsSelectListAdapter(getActivity(), App.getInstance().getGiftsList());

        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_gifts);
        dialog.setCancelable(true);

        final ProgressBar mProgressBar = (ProgressBar) dialog.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        TextView mDlgTitle = (TextView) dialog.findViewById(R.id.title_label);
        mDlgTitle.setText(R.string.dlg_choice_gift_title);

        TextView mDlgSubtitle = (TextView) dialog.findViewById(R.id.subtitle_label);
        mDlgSubtitle.setText(String.format(Locale.getDefault(), getString(R.string.account_balance_label), App.getInstance().getBalance()));

        AppCompatButton mDlgBalanceButton = (AppCompatButton) dialog.findViewById(R.id.balance_button);
        mDlgBalanceButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent i = new Intent(getActivity(), BalanceActivity.class);
                startActivityForResult(i, 1945);

                dialog.dismiss();
            }
        });

        AppCompatButton mDlgCancelButton = (AppCompatButton) dialog.findViewById(R.id.cancel_button);
        mDlgCancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                dialog.dismiss();
            }
        });

        NestedScrollView mDlgNestedView = (NestedScrollView) dialog.findViewById(R.id.nested_view);
        final RecyclerView mDlgRecyclerView = (RecyclerView) dialog.findViewById(R.id.recycler_view);

        final LinearLayoutManager mLayoutManager = new GridLayoutManager(getActivity(), Helper.getStickersGridSpanCount(getActivity()));
        mDlgRecyclerView.setLayoutManager(mLayoutManager);
        mDlgRecyclerView.setHasFixedSize(true);
        mDlgRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mDlgRecyclerView.setAdapter(giftsAdapter);

        mDlgRecyclerView.setNestedScrollingEnabled(true);

        giftsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onChanged() {

                super.onChanged();

                if (App.getInstance().getGiftsList().size() != 0) {

                    mDlgRecyclerView.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        });

        giftsAdapter.setOnItemClickListener(new GiftsSelectListAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(View view, BaseGift obj, int position) {

                if (App.getInstance().getBalance() >= obj.getCost()) {

                    Intent intent = new Intent(getActivity(), SendGiftActivity.class);
                    intent.putExtra("giftId", obj.getId());
                    intent.putExtra("giftTo",  item.getOwner().getId());
                    intent.putExtra("giftCost", obj.getCost());
                    intent.putExtra("imgUrl", obj.getImgUrl());
                    //startActivityForResult(intent, PROFILE_NEW_GIFT);
                    startActivity(intent);

                    dialog.dismiss();

                } else {

                    Toast.makeText(getActivity(), getString(R.string.error_credits), Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (App.getInstance().getGiftsList().size() == 0) {

            mDlgRecyclerView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);

            Api api = new Api(getActivity());
            api.getGifts(giftsAdapter);
        }

        dialog.show();

        doKeepDialog(dialog);
    }

    private static void doKeepDialog(Dialog dialog){

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);
    }

}