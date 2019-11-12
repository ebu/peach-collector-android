package ch.ebu.peachdemo;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.ArrayList;

import ch.ebu.peachcollector.EventContextComponent;
import ch.ebu.peachcollector.Event;


public class RecommendationsFragment extends Fragment {

    private ImageButton[] imageButtons;

    public RecommendationsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recommendations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ArrayList<String> items = new ArrayList<>();
        items.add("media00");
        items.add("media01");
        items.add("media02");
        items.add("media03");

        final EventContextComponent component = new EventContextComponent();
        component.name = "MainCarousel";
        component.type = "carousel";
        component.version = "1.0";


        Event.sendRecommendationDisplayed("reco00", items , 4, null, null, component);


        View.OnClickListener buttonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = 0;
                for (int i = 0; i < 4; i++) {
                    ImageButton button = imageButtons[i];
                    if (button == v) index = i;
                }
                Event.sendRecommendationHit("reco00", items , 4, index, null, null, component);

                if(index == 0){
                    getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new MediaFragment(), "media").addToBackStack("media").commit();
                }
                else if (index == 1){
                    getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new AudioFragment(), "audio").addToBackStack("audio").commit();
                }
            }};


        ImageButton buttonReco00 = view.findViewById(R.id.reco00);
        ImageButton buttonReco01 = view.findViewById(R.id.reco01);
        ImageButton buttonReco02 = view.findViewById(R.id.reco02);
        ImageButton buttonReco03 = view.findViewById(R.id.reco03);

        imageButtons = new ImageButton[]{buttonReco00, buttonReco01, buttonReco02, buttonReco03};
        for (ImageButton button : imageButtons) {
            button.setOnClickListener(buttonListener);
        }

    }
}
