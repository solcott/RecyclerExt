package com.devbrackets.android.recyclerextdemo;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.devbrackets.android.recyclerext.adapter.RecyclerCursorAdapter;
import com.devbrackets.android.recyclerextdemo.database.DBHelper;
import com.devbrackets.android.recyclerextdemo.database.ItemDAO;
import com.devbrackets.android.recyclerextdemo.viewholder.SimpleTextViewHolder;


/**
 * An example of the RecyclerView Cursor Adapter
 */
public class CursorFragment extends Fragment {
    private DBHelper dbHelper;

    private RecyclerView recyclerView;
    private CursorAdapter cursorAdapter;

    public static CursorFragment newInstance() {
        return new CursorFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recycler_fragment, container, false);
        recyclerView = (RecyclerView)view.findViewById(R.id.recyclerext_fragment_recycler);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupDB();
        setupRecyclerExt();
    }

    private void setupDB() {
        dbHelper = new DBHelper(getActivity());
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        //create and save some dummy items...
        for (int i = 1; i < 20; i++) {
            ItemDAO item = new ItemDAO("Cursor Item " + i);
            item.save(database);
        }

        database.close();
    }

    private void setupRecyclerExt() {
        cursorAdapter = new CursorAdapter(getActivity(), ItemDAO.findCursorAll(dbHelper.getWritableDatabase()));

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(cursorAdapter);
    }


















    private class CursorAdapter extends RecyclerCursorAdapter<SimpleTextViewHolder> {
        private LayoutInflater inflater;

        public CursorAdapter(Context context, Cursor cursor) {
            super(cursor);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public SimpleTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.simple_drag_item, null);

            return new SimpleTextViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SimpleTextViewHolder holder, int position) {
            Cursor c = getCursor(position);
            if (c != null) {
                ItemDAO item = new ItemDAO(c);
                holder.setText(item.getText() != null ? item.getText() : "");
                holder.setPosition(position);
            }
        }

        @Override
        public void onContentChanged() {
            //Purposefully left blank
        }
    }
}
