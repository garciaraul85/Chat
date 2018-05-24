package com.chat.chat.view;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.TextView;

import com.chat.chat.R;
import com.chat.chat.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatMessage> items;
    private OnItemClickListener listener;

    public ChatAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(ChatMessage item);
    }

    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;

        v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatMessage model = items.get(position);

        holder.bind(items.get(position), listener);

        // Set their text
        boolean isValid = URLUtil.isValidUrl(model.getMessageText());
        if (isValid) {
            holder.messageText.setText(R.string.download_share_file);
            holder.sharedLink.setText(model.getMessageText());
        } else {
            holder.messageText.setText(model.getMessageText());
        }
        holder.messageUser.setText(model.getMessageUser());

        // Format the date before showing it
        holder.messageTime.setText(DateFormat.format("dd-MM-yyyy (HH:mm:ss)",
                model.getMessageTime()));
    }

    @Override
    public int getItemCount() {
        if (items != null) {
            return items.size();
        } else {
            return 0;
        }
    }

    private ChatMessage getItem(int position) {
        return items.get(position);
    }


    public void setItems(List<ChatMessage> items) {
        if (items == null) {
            return;
        }

        this.items = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    /**
     * Inner Class for a recycler view
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView messageUser;
        TextView messageTime;
        TextView sharedLink;

        ViewHolder(View v) {
            super(v);
            messageText = v.findViewById(R.id.message_text);
            messageUser = v.findViewById(R.id.message_user);
            messageTime = v.findViewById(R.id.message_time);
            sharedLink  = v.findViewById(R.id.file_url);
        }

        public void bind(final ChatMessage item, final OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}