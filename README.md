# Demo4LikeOrNotView
右划喜欢左划不喜欢

![like_or_not_1.gif](http://upload-images.jianshu.io/upload_images/2113387-96588e7db454540a.gif?imageMogr2/auto-orient/strip)

![like_or_not_2.gif](http://upload-images.jianshu.io/upload_images/2113387-cd0388a31225b471.gif?imageMogr2/auto-orient/strip)

```
<dependency>
  <groupId>com.linyuzai</groupId>
  <artifactId>likeornot</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>

compile 'com.linyuzai:likeornot:1.0.0'

jceter还在审核，应该
```

```
<com.linyuzai.likeornot.LikeOrNotView xmlns:like_or_not="http://schemas.android.com/apk/res-auto"    
    android:id="@+id/like_or_not"    
    android:layout_width="match_parent"           
    android:layout_height="match_parent" 
    android:background="#eeeeee"    
    like_or_not:animator_duration="300"    
    like_or_not:drag_scale="0.35"    
    like_or_not:move_multiplier="3"    
    like_or_not:rotation_direction="clockwise"    
    like_or_not:rotation_range="5"    
    like_or_not:style_rotatable="true"    
    like_or_not:style_stratified="true">    
    <LinearLayout        
        android:layout_width="match_parent"        
        android:layout_height="match_parent"        
        android:layout_marginTop="@dimen/activity_horizontal_margin"        
        android:gravity="center"        
        android:orientation="horizontal">        
    <ImageView            
        android:id="@+id/back"            
        android:layout_width="50dp"            
        android:layout_height="50dp"            
        android:layout_marginRight="40dp"            
        android:layout_marginTop="20dp"            
        android:scaleType="centerInside"            
        android:src="@mipmap/back" />        
    <ImageView            
        android:id="@+id/nope"            
        android:layout_width="50dp"            
        android:layout_height="50dp"            
        android:layout_marginRight="40dp"            
        android:scaleType="centerInside"            
        android:src="@mipmap/nope" />        
    <ImageView            
        android:id="@+id/like"            
        android:layout_width="50dp"            
        android:layout_height="50dp"            
        android:layout_marginRight="40dp"            
        android:scaleType="centerInside"            
        android:src="@mipmap/like" />        
    <ImageView            
        android:id="@+id/star"            
        android:layout_width="50dp"            
        android:layout_height="50dp"            
        android:layout_marginTop="20dp"            
        android:scaleType="centerInside"            
        android:src="@mipmap/star" />    
    </LinearLayout>
</com.linyuzai.likeornot.LikeOrNotView>
```
>里面需要一个子控件，就是图中下面四个按钮的那块。如果不需要那块东西，就随便放个view上去，因为我默认是有一个子view。说明一下那个，自定义的属性

style_rotatable，拖动的时候可旋转<br>
style_stratified，是否有层次效果<br>
rotation_direction，旋转方向，默认右划顺时针<br>
rotation_range，旋转范围，会乘一定比例，默认5<br>
animator_duration，非手动拖动时动画持续时间，默认300ms<br>
drag_scale，设置有效滑动距离，为屏幕宽度*drag_scale，默认0.35<br>
move_multiplier，松手后view滑动距离和用手滑动距离的倍数，说简单点，当你发现手一甩，view没甩出屏幕，就把这个参数加大<br>
>然后设置一下Adapter（和Listener）

```
mLikeOrNotView.setAdapter(new BaseAdapter() {    
    @Override    
    public int getCount() {        
        return 10;    
    }    

    @Override    
    public View getView(View convertView, ViewGroup parent, final int position) {        
        if (convertView == null)          
            convertView=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_like_or_not, parent, false);        
        TextView textView = (TextView) convertView.findViewById(R.id.text);        
        textView.setText(position + "");              
        return convertView;    
    }
});

mLikeOrNotView.setOnItemClickListener(new LikeOrNotView.OnItemClickListener() {    
    @Override    
    public void onItemClick(View view, int position) {   
     
    }
});

mLikeOrNotView.setOnLikeOrNotListener(new LikeOrNotView.OnLikeOrNotListener() {    
    @Override    
    public void onLike(View view, int position) {        
        
    }    

    @Override    
    public void onNope(View view, int position) {        
        
    }    

    @Override    
    public void onAnimationEnd() {        
        
    }
});

mLikeOrNotView.setViewStateCallback(new LikeOrNotView.ViewStateCallback() {    
    @Override    
    public void onViewReleased(View view) {        
  
    }    
    @Override    
    public void onViewPositionChanged(View view, int left, int top, float scale) {        

    }
});
```
