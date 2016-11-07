package com.linyuzai.demo4likeornotview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.linyuzai.likeornot.BaseAdapter;
import com.linyuzai.likeornot.LikeOrNotView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private LikeOrNotView mLikeOrNotView;

    private ImageView mBackImageView;
    private ImageView mNopeImageView;
    private ImageView mLikeImageView;
    private ImageView mStarImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLikeOrNotView = (LikeOrNotView) findViewById(R.id.like_or_not);

        mBackImageView = (ImageView) mLikeOrNotView.findViewById(R.id.back);
        mNopeImageView = (ImageView) mLikeOrNotView.findViewById(R.id.nope);
        mLikeImageView = (ImageView) mLikeOrNotView.findViewById(R.id.like);
        mStarImageView = (ImageView) mLikeOrNotView.findViewById(R.id.star);

        mBackImageView.setOnClickListener(this);
        mNopeImageView.setOnClickListener(this);
        mLikeImageView.setOnClickListener(this);
        mStarImageView.setOnClickListener(this);

        mLikeOrNotView.setOnItemClickListener(new LikeOrNotView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(MainActivity.this, position + "", Toast.LENGTH_SHORT).show();
            }
        });
        mLikeOrNotView.setOnLikeOrNotListener(new LikeOrNotView.OnLikeOrNotListener() {
            @Override
            public void onLike(View view, int position) {
                Log.e("onLike", position + "");
            }

            @Override
            public void onNope(View view, int position) {
                Log.e("onNope", position + "");
            }

            @Override
            public void onAnimationEnd() {
                //Log.e("onAnimationEnd", "onAnimationEnd");
            }
        });
        mLikeOrNotView.setViewStateCallback(new LikeOrNotView.ViewStateCallback() {
            @Override
            public void onViewReleased(View view) {
                //Log.e("onViewReleased", "onViewReleased");
            }

            @Override
            public void onViewPositionChanged(View view, int left, int top, float scale) {
                //Log.e("onViewPositionChanged", "onViewPositionChanged");
            }
        });
        mLikeOrNotView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 10;
            }

            @Override
            public View getView(View convertView, ViewGroup parent, final int position) {
                if (convertView == null)
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_like_or_not, parent, false);
                TextView textView = (TextView) convertView.findViewById(R.id.text);
                textView.setText(position + "");
                /*convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, position + "", Toast.LENGTH_SHORT).show();
                    }
                });*/
                //View view = View.inflate(parent.getContext(),R.layout.item_like_or_not,null);
                return convertView;
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                mLikeOrNotView.back();
                break;
            case R.id.like:
                mLikeOrNotView.like();
                break;
            case R.id.nope:
                mLikeOrNotView.nope();
                break;
            case R.id.star:
                Toast.makeText(this, "Star", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
