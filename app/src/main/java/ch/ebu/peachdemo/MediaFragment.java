package ch.ebu.peachdemo;


import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ForwardingPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.ui.StyledPlayerView;

import ch.ebu.peachcollector.EventContextComponent;
import ch.ebu.peachcollector.Event;
import ch.ebu.peachcollector.PeachCollector;
import ch.ebu.peachcollector.PeachPlayerTracker;


/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class MediaFragment extends Fragment {

    private StyledPlayerView videoView;

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

        final ExoPlayer player = new ExoPlayer.Builder(getContext()).build();
        videoView = view.findViewById(R.id.player);
        videoView.setPlayer(player);

        //Location of Media File
        Uri uri = Uri.parse("android.resource://" + getContext().getPackageName() + "/" + R.raw.peach);

        // Build the media item.
        MediaItem mediaItem = MediaItem.fromUri(uri);
        // Set the media item to be played.
        player.setMediaItem(mediaItem);
        // Prepare the player.
        player.prepare();
        // Start the playback.
        player.play();

        PeachPlayerTracker.setPlayer(player);
        PeachPlayerTracker.trackMedia("test0001", null, null, null);

    }
}
