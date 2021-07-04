package de.heoegbr.fdmusic.ui.setup;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import de.heoegbr.fdmusic.R;
import de.heoegbr.fdmusic.data.MusicConstants;
import io.noties.markwon.Markwon;

public class SetupWelcomeFragment extends Fragment {
    private TextView textView;
    private Switch aggreeSwitch;
    private TextView aggreeSwitchTitle;
    private Markwon markwon;
    private EditText textField;
    private TextWatcher textInputListener;

    private int textResourceId;
    private boolean switchEnabled;
    private int switchTitleId;
    private CompoundButton.OnCheckedChangeListener listener;
    private boolean initialState;
    private boolean textFieldEnabled;

    public SetupWelcomeFragment(@NotNull int textResourceId, boolean switchEnabled, int switchTitleId,
                                CompoundButton.OnCheckedChangeListener listener, boolean initialState,
                                boolean textFieldEnabled, TextWatcher textInputListener) {
        this.textResourceId = textResourceId;
        this.switchEnabled = switchEnabled;
        this.switchTitleId = switchTitleId;
        this.listener = listener;
        this.initialState = initialState;
        this.textFieldEnabled = textFieldEnabled;
        this.textInputListener = textInputListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_setup_content, container, false);

        textView = root.findViewById(R.id.setup_fragment_text);
        markwon = Markwon.create(getContext());
        markwon.setMarkdown(textView, getString(textResourceId));

        textField = root.findViewById(R.id.setup_fragment_text_input);
        if (textFieldEnabled) {
            textField.setVisibility(View.VISIBLE);
            textField.addTextChangedListener(textInputListener);

            // FIXME remove asap
            textField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    try {
                        Thread.sleep(10);
                        if (MusicConstants.THIS_IS_A_HACK)
                            textField.setCompoundDrawablesWithIntrinsicBounds(
                                    0,0,R.drawable.ic_check_cycle,0);
                        else
                            textField.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        aggreeSwitch = root.findViewById(R.id.setup_fragment_switch);
        aggreeSwitchTitle = root.findViewById(R.id.setup_fragment_switch_title);
        if (switchEnabled) {
            aggreeSwitch.setVisibility(View.VISIBLE);
            aggreeSwitch.setOnCheckedChangeListener(listener);
            aggreeSwitch.setChecked(initialState);
            aggreeSwitchTitle.setVisibility(View.VISIBLE);
            aggreeSwitchTitle.setText(switchTitleId);
        } else {
            aggreeSwitch.setVisibility(View.INVISIBLE);
            aggreeSwitchTitle.setVisibility(View.INVISIBLE);
        }

        return root;
    }

    void resetSwitch() {
        aggreeSwitch.setChecked(false);
    }

}
