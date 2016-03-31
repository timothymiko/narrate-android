package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.providers.TagsDao;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by timothymiko on 6/10/14.
 */
public class TagsPickerDialog extends MaterialDialogFragment {

    public interface TagsChangeListener {
        void onTagsChanged(List<String> tags);
    }

    private List<String> tags;
    private TagsChangeListener mCallback;

    private Dialog mRoot;
    private View mAddBtn;

    private TagsListAdapter adapter;
    private AutoCompleteTextView editText;
    private LinearLayout list;

    private List<String> mAutoCompleteTags;
    private ArrayAdapter<String> mAutocompleteAdapter;

    public static TagsPickerDialog newInstance(TagsChangeListener listener, List<String> tags) {
        TagsPickerDialog picker = new TagsPickerDialog();
        picker.tags = tags != null ? tags : new ArrayList<String>();
        picker.mCallback = listener;
        return picker;
    }

    public void setTags(List<String> tags) {
        if (this.tags == null)
            tags = new ArrayList<>();

        this.tags.clear();
        this.tags.addAll(tags);

        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new RetrieveTagsTask().execute();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() != null) {
            setTitle(R.string.tags);
            setContentView(R.layout.tags_picker_layout);

            mRoot = super.onCreateDialog(savedInstanceState);

            mRoot.findViewById(R.id.dialog_buttons_layout).setVisibility(View.GONE);

            editText = (AutoCompleteTextView) mRoot.findViewById(R.id.editText);
            editText.setImeActionLabel("Add", KeyEvent.KEYCODE_ENTER);
            editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    addTag();
                    return true;
                }
            });
            editText.getBackground().setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);

            if (mAutocompleteAdapter != null)
                editText.setAdapter(mAutocompleteAdapter);

            list = (LinearLayout) mRoot.findViewById(R.id.list);
            adapter = new TagsListAdapter(getActivity(), R.layout.tags_list_item, tags);
            adapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    updateListViews();
                }
            });
            updateListViews();
            mAddBtn = mRoot.findViewById(R.id.left_button);
            mAddBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addTag();
                }
            });

            return mRoot;
        } else
            return null;
    }

    private void updateListViews() {
        list.removeAllViews();

        for ( int i = 0; i < adapter.getCount(); i++ )
            list.addView(adapter.getView(i, null, null), i);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mCallback.onTagsChanged(tags);
    }

    private void addTag() {
        String input;
        if ((input = editText.getText().toString()).length() > 0)
            tags.add(input);
        editText.setText("");
        adapter.notifyDataSetChanged();
    }

    private class RetrieveTagsTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... params) {
            return TagsDao.getTags();
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            super.onPostExecute(strings);

            if (mAutocompleteAdapter == null) {
                mAutoCompleteTags = new ArrayList<>(strings);
                mAutocompleteAdapter = new ArrayAdapter<String>(getActivity(), R.layout.simple_list_item_1, mAutoCompleteTags);
                editText.setAdapter(mAutocompleteAdapter);
            }
        }
    }

    private class TagsListAdapter extends ArrayAdapter<String> {

        private class TagViewHolder {
            TextView text;
            ImageView image;
        }

        LayoutInflater inflater;

        public TagsListAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TagViewHolder viewHolder;
            final String item = getItem(position);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.tags_list_item, parent, false);

                viewHolder = new TagViewHolder();
                viewHolder.text = (TextView) convertView.findViewById(R.id.text);
                viewHolder.image = (ImageView) convertView.findViewById(R.id.image);
                viewHolder.image.setColorFilter(getContext().getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);

                convertView.setTag(viewHolder);
            } else
                viewHolder = (TagViewHolder) convertView.getTag();

            viewHolder.text.setText(item);
            viewHolder.image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(item);
                    notifyDataSetChanged();
                }
            });

            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }
    }
}
