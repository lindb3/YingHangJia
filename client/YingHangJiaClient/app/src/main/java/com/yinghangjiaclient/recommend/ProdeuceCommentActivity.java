package com.yinghangjiaclient.recommend;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jdsjlzx.interfaces.OnItemClickListener;
import com.github.jdsjlzx.recyclerview.LRecyclerView;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.github.jdsjlzx.recyclerview.ProgressStyle;
import com.github.jdsjlzx.util.RecyclerViewStateUtils;
import com.github.jdsjlzx.view.LoadingFooter;
import com.koushikdutta.ion.Ion;
import com.orhanobut.logger.Logger;
import com.yinghangjiaclient.R;
import com.yinghangjiaclient.base.ListBaseAdapter;
import com.yinghangjiaclient.bean.ItemModel;
import com.yinghangjiaclient.login.LoginActivity;
import com.yinghangjiaclient.news.NewsDetailActivity;
import com.yinghangjiaclient.util.HttpUtil;
import com.yinghangjiaclient.util.JSONUtils;
import com.yinghangjiaclient.util.StringUtils;
import com.yinghangjiaclient.util.UserUtils;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProdeuceCommentActivity extends AppCompatActivity {
    private String financelId;
    private EditText commentEdit;
    private Button comment_btn;

    private SharedPreferences sp;

    private static final int REQUEST_COUNT = 10;

    private LRecyclerView mRecyclerView = null;

    private DataAdapter mDataAdapter = null;

    private LRecyclerViewAdapter mLRecyclerViewAdapter = null;

    private boolean isRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.init("ying");
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.info_comment);
            sp = getSharedPreferences("userInfo", Activity.MODE_PRIVATE);
            Intent intent = this.getIntent();
            if (intent != null) {
                financelId = intent.getStringExtra("_id");
            }

            commentEdit = (EditText) findViewById(R.id.editText3);
            comment_btn = (Button) findViewById(R.id.radioButton4);

            mRecyclerView = (LRecyclerView) findViewById(R.id.list);

            //init data
            ArrayList<ItemModel> dataList = new ArrayList<>();

            mDataAdapter = new DataAdapter(this);
            mDataAdapter.addAll(dataList);

            mLRecyclerViewAdapter = new LRecyclerViewAdapter(this, mDataAdapter);
            mRecyclerView.setAdapter(mLRecyclerViewAdapter);

            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            mRecyclerView.setRefreshProgressStyle(ProgressStyle.BallSpinFadeLoader);
            mRecyclerView.setArrowImageView(R.drawable.ic_pulltorefresh_arrow);

            mRecyclerView.setLScrollListener(new LRecyclerView.LScrollListener() {
                @Override
                public void onRefresh() {
                    RecyclerViewStateUtils.setFooterViewState(mRecyclerView, LoadingFooter.State.Normal);
                    mDataAdapter.clear();
                    isRefresh = true;
                    new MyAsyncTask().execute();
                }

                @Override
                public void onScrollUp() {
                }

                @Override
                public void onScrollDown() {
                }

                @Override
                public void onBottom() {
                }

                @Override
                public void onScrolled(int distanceX, int distanceY) {
                }

            });
            mRecyclerView.setRefreshing(true);

            //            发表评论
            comment_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (UserUtils.isLogin(ProdeuceCommentActivity.this)) {
                        if (!StringUtils.isBlank(commentEdit.getText().toString()))
                            new putCommentAsyncTask().execute();
                        else
                            Toast.makeText(getApplicationContext(), "输入不能为空", Toast.LENGTH_SHORT).show();;
                    } else {
                        Intent intent = new Intent();
                        intent.setClass(ProdeuceCommentActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(e.getMessage());
        }
    }

    private void notifyDataSetChanged() {
        mLRecyclerViewAdapter.notifyDataSetChanged();
    }

    private void addItems(ArrayList<ItemModel> list) {
        mDataAdapter.addAll(list);
    }

    private View.OnClickListener mFooterClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RecyclerViewStateUtils.setFooterViewState(ProdeuceCommentActivity.this, mRecyclerView, REQUEST_COUNT, LoadingFooter.State.Loading, null);
            new MyAsyncTask().execute();
        }
    };

    private class DataAdapter extends ListBaseAdapter<ItemModel> {

        private LayoutInflater mLayoutInflater;

        public DataAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            mContext = context;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mLayoutInflater.inflate(R.layout.comment_listview, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ItemModel item = mDataList.get(position);

            ViewHolder viewHolder = (ViewHolder) holder;
            viewHolder.content.setText(item.bank);
            viewHolder.time.setText(item.cycle);
            viewHolder.username.setText(item.name);
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private TextView content;
            private TextView time;
            private TextView username;

            public ViewHolder(View itemView) {
                super(itemView);
                username = (TextView) itemView.findViewById(R.id.textView30);
                content = (TextView) itemView.findViewById(R.id.textView31);
                time = (TextView) itemView.findViewById(R.id.textView33);
            }
        }
    }

    public class MyAsyncTask extends AsyncTask<Void, Integer, String> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... arg0) {
            return query();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                super.onPostExecute(result);
                if (!StringUtils.isBlank(result)) {
                    if (isRefresh) {
                        mDataAdapter.clear();
                    }
                    ArrayList<ItemModel> newList = parseDataFromString(result);
                    if (newList.isEmpty()) {
                        Toast.makeText(getApplicationContext(),
                                "暂无数据", Toast.LENGTH_SHORT).show();
                    }
                    addItems(newList);
                    if (isRefresh) {
                        isRefresh = false;
                        mRecyclerView.refreshComplete();
                        notifyDataSetChanged();
                    } else {
                        RecyclerViewStateUtils.setFooterViewState(mRecyclerView, LoadingFooter.State.Normal);
                    }
                    commentEdit.setText("");
                } else {
                    if (isRefresh) {
                        isRefresh = false;
                        mRecyclerView.refreshComplete();
                        notifyDataSetChanged();
                    } else {
                        RecyclerViewStateUtils.setFooterViewState(ProdeuceCommentActivity.this, mRecyclerView, REQUEST_COUNT, LoadingFooter.State.NetWorkError, mFooterClick);
                    }
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "网络异常", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.e(e.getMessage());
            }
        }
    }

    private String query() {
        String url = HttpUtil.BASE_URL + "api/product/" + financelId;
        return HttpUtil.queryStringForGet(url);
    }

    private ArrayList<ItemModel> parseDataFromString(String result) {
        ArrayList<ItemModel> list = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject temp = jsonObject.getJSONObject("data");
            if (temp != null) {
                JSONArray commentList =  temp.getJSONArray("comments");
                if (commentList == null) return list;
                for (int i = 0; i < commentList.length(); i++) {
                    JSONObject comment = commentList.optJSONObject(i);
                    if (comment != null) {
                        ItemModel item = new ItemModel();
                        item.name = comment.getString("username");
                        item.bank = comment.getString("text");
                        item.cycle = comment.getString("time");
                        list.add(item);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Logger.e(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(e.getMessage());
        }
        return list;
    }

    public class putCommentAsyncTask extends AsyncTask<Void, Integer, String> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... arg0) {
            return putCommentquery();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (!StringUtils.isBlank(result) && result.equals("OK")) {
                Toast.makeText(getApplicationContext(),
                        "发表成功", Toast.LENGTH_SHORT).show();
                mRecyclerView.setRefreshing(true);
            } else {
                Toast.makeText(getApplicationContext(),
                        "网络异常", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String putCommentquery() {
        String url;
        String username = sp.getString("USERNAME", "");
        url = HttpUtil.BASE_URL + "api/product/comment/" + financelId;
        NameValuePair paraUsername = new BasicNameValuePair("username",
                username);
        NameValuePair paraPassword = new BasicNameValuePair("text",
                commentEdit.getText().toString());
        List<NameValuePair> para = new ArrayList<NameValuePair>();
        para.add(paraPassword);
        para.add(paraUsername);
        return HttpUtil.queryStringForPut(url, para);
    }
}
