package com.macrogrh.fastscroll;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<Item> mItems;

    public ItemAdapter(List<Item> items) {
        mItems = items;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle;
        public TextView textViewBody;

        public ViewHolder(View itemView) {
            super(itemView);

            textViewTitle = (TextView) itemView.findViewById(R.id.title);
            textViewBody = (TextView) itemView.findViewById(R.id.body);
        }
    }

    @Override
    public ItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View contactView = inflater.inflate(R.layout.item_row, parent, false);

        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(ItemAdapter.ViewHolder viewHolder, int position) {
        Item item = mItems.get(position);

        TextView textView = viewHolder.textViewTitle;
        textView.setText(item.getTitle());
        TextView textViewBody = viewHolder.textViewBody;
        textViewBody.setText(item.getBody());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}
