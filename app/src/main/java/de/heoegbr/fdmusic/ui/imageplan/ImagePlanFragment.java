package de.heoegbr.fdmusic.ui.imageplan;

import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.heoegbr.fdmusic.R;
import de.heoegbr.fdmusic.data.MusicConstants;
import de.heoegbr.fdmusic.player.SoundService;

/**
 *
 *
 * @author David Schneider
 */
public class ImagePlanFragment extends Fragment {

    Button previousButton;
    Button nextButton;
    ImagePlanView imagePlanView;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_image_plan, container, false);

        imagePlanView = view.findViewById(R.id.bilderplan);

        previousButton = view.findViewById(R.id.previousImageButton);
        previousButton.setOnClickListener(v -> {
            imagePlanView.skipToPreviousImage();
        });

        nextButton = view.findViewById(R.id.nextImageButton);
        nextButton.setOnClickListener(v -> {
            imagePlanView.skipToNextImage();
        });

        return view;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void setTimeInMusic(Integer timeInMusic) {
        imagePlanView.setTimeInMusic(timeInMusic);
    }

}