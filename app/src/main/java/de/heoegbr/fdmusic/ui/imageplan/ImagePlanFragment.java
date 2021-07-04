package de.heoegbr.fdmusic.ui.imageplan;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import de.heoegbr.fdmusic.R;

/**
 *
 *
 * @author David Schneider
 */
public class ImagePlanFragment extends Fragment {

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_image_plan, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void setTimeInMusic(Integer timeInMusic) {
        ImagePlanView view = (ImagePlanView) this.getActivity().findViewById(R.id.bilderplan);
        view.setTimeInMusic(timeInMusic);
    }

}