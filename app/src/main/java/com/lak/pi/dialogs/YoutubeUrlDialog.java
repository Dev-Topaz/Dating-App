package com.lak.pi.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.lak.pi.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeUrlDialog extends DialogFragment implements View.OnClickListener {
    YoutubeVideoAddListener youtubeVideoAddListener;
    private Context context;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        try {

            youtubeVideoAddListener = (YoutubeVideoAddListener) context;

        } catch(ClassCastException e){

            // The hosting activity does not implemented the interface AlertPositiveListener
            throw new ClassCastException(context.toString() + " must implement AlertPositiveListener");
        }
    }
    private EditText urlContainer;
    private Button btnOkay;

    public interface YoutubeVideoAddListener
    {
        public void addYoutubeVideo(String videoId);
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = (Dialog) super.onCreateDialog(savedInstanceState);
        dialog.setContentView(R.layout.dialog_youtube_url);
        urlContainer = dialog.findViewById(R.id.youtubeURL);
        btnOkay = dialog.findViewById(R.id.btnOkay);
        btnOkay.setOnClickListener(this);
        return dialog;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id)
        {
            case R.id.btnOkay:
                String url = urlContainer.getEditableText().toString();
                String videoId = getVideoId(url);
                if(checkUrl(url) && videoId!=null) {
                    dismiss();
                    youtubeVideoAddListener.addYoutubeVideo(videoId);
                }
                else
                    Toast.makeText(context, "Invalid URL", Toast.LENGTH_SHORT).show();
                break;

        }
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

    private boolean checkUrl(String url) {
        if(!TextUtils.isEmpty(url) || url != null)
            return true;
        return false;
    }
}
