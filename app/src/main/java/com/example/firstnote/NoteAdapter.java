package com.example.firstnote;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerKt;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class NoteAdapter extends BaseQuickAdapter<Note, BaseViewHolder>{
    private static final int STATE_DEFAULT = 0;
    int mEditMode = STATE_DEFAULT;
    Runnable runnable;
    Handler handler = new Handler();
    private MainActivity mainActivity; // Add this line

    public NoteAdapter(int layoutResId, @Nullable List<Note> noteList, MainActivity mainActivity) { // Modify this line
        super(layoutResId, noteList);
        this.mainActivity = mainActivity; // Add this line
    }
    public NoteAdapter(int layoutResId, @Nullable List<Note> noteList) {
        super(layoutResId, noteList);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void convert(@NonNull BaseViewHolder helper, Note item) {
        helper.setText(R.id.title_text, item.title);
        helper.setText(R.id.first_line_text, item.first_line);
        LinearLayout label_layout = helper.getView(R.id.label_layout);
        label_layout.removeAllViews();
        for(String label: item.getLabelsList()){
            TextView labelView = new TextView(mContext);
            labelView.setText(label);
            labelView.setBackgroundResource(R.drawable.label_bg);
            labelView.setPadding(10, 1, 10, 1);
            labelView.setTextColor(mContext.getResources().getColor(R.color.white));
            ((ViewGroup)helper.getView(R.id.label_layout)).addView(labelView);
        }
        helper.addOnClickListener(R.id.note_item);//添加item点击事件
        helper.addOnLongClickListener(R.id.note_item);//添加item长按事件
        helper.getView(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //删除笔记
                int position = helper.getAdapterPosition();
                Note note = getItem(position);
                if (note != null) {
                    remove(position);
                    if (runnable != null) {
                        // 移除之前的Runnable
                        handler.removeCallbacks(runnable);
                    }
                    // okhttp DELETE
                    SharedPreferences sharedPreferences = mContext.getSharedPreferences("user", 0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                    int user_id = sharedPreferences.getInt("user_id", -1);
                    String encodedTitle = Uri.encode(item.title);
                    editor.apply();
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(API.API_root + "/note/"+user_id+"/"+encodedTitle)
                            .delete()
                            .build();
                    try {
                        client.newCall(request).enqueue(
                                new Callback() {
                                    @Override
                                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                        Log.e("delete_error", e.toString());
                                    }

                                    @Override
                                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                        if (response.isSuccessful()) {
                                            Log.d("delete_success", Objects.requireNonNull(response.body()).string());
                                        } else {
                                            Log.e("delete_error", Objects.requireNonNull(response.body()).string());
                                        }
                                    }
                                }
                        );
                    } catch (Exception e) {
                        Log.e("delete_error", e.toString());
                    }
                }
            }
        });
        helper.getView(R.id.note_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转到笔记详情页面
                int position = helper.getAdapterPosition();
                Note note = getItem(position);
                if (note != null) {
                    Intent intent = new Intent(mContext, NoteActivity.class);
                    intent.putExtra("title", note.title);
                    mContext.startActivity(intent);
                    mainActivity.need_flush = true;
                }
            }
        });
        helper.getView(R.id.note_layout).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ImageButton delete_button = helper.getView(R.id.delete_button);
                Animation fadeIn = AnimationUtils.loadAnimation(helper.itemView.getContext(), R.anim.fade_in);
                delete_button.startAnimation(fadeIn);
                delete_button.setVisibility(View.VISIBLE);

                Handler handler = new Handler();
                 runnable = new Runnable() {
                    @Override
                    public void run() {
                        // 在Runnable中，设置按钮的可见性为View.GONE
                        Animation fadeOut = AnimationUtils.loadAnimation(helper.itemView.getContext(), R.anim.fade_out);
                        delete_button.startAnimation(fadeOut);
                        delete_button.setVisibility(View.GONE);
                    }
                };

                // 使用Handler的postDelayed方法来在3秒后执行Runnable
                handler.postDelayed(runnable, 3000);
                return true;
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setEditMode(int editMode) {
        mEditMode = editMode;
        notifyDataSetChanged();//刷新
    }
}
