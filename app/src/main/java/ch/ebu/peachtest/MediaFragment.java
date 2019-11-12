package ch.ebu.peachtest;


import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.VideoView;

import ch.ebu.peachcollector.EventContextComponent;
import ch.ebu.peachcollector.Event;


/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class MediaFragment extends Fragment {

    private ImageButton playerButton;
    private MediaController mediaController;
    private VideoView videoView;

    private EventContextComponent component = new EventContextComponent();

    public MediaFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        component.name = "MainPlayer";
        component.type = "media_player";
        component.version = "2.3.0";

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerButton = view.findViewById(R.id.playerButton);
        playerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    playerButton.setImageResource(R.drawable.icon_play);
                    Event.sendMediaPause("media00", null, null, null);
                }
                else {
                    videoView.start();
                    playerButton.setImageResource(R.drawable.icon_pause);
                    Event.sendMediaPlay("media00", null, null, null);
                }
            }
        });

        videoView = view.findViewById(R.id.player);
        //Set MediaController  to enable play, pause, forward, etc options.
        mediaController = new MediaController(getContext());
        mediaController.setAnchorView(videoView);
        //Location of Media File
        Uri uri = Uri.parse("android.resource://" + getContext().getPackageName() + "/" + R.raw.peach);
        //Starting VideView By Setting MediaController and URI
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(uri);
        videoView.requestFocus();
        //videoView.start();




    }
}
