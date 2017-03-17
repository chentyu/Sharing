package com.example.liwensheng.sharing.model;

import android.text.TextUtils;

import com.example.liwensheng.sharing.entity.Friend;
import com.example.liwensheng.sharing.entity.UserEntity;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;


import cn.bmob.newim.BmobIM;
import cn.bmob.newim.bean.BmobIMConversation;
import cn.bmob.newim.bean.BmobIMMessage;
import cn.bmob.newim.bean.BmobIMUserInfo;
import cn.bmob.newim.event.MessageEvent;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;

/**
 * Created by liWensheng on 2017/3/17.
 */

public class UserModel extends BaseModel {
    private static UserModel ourInstance = new UserModel();

    public static UserModel getInstance() {
        return ourInstance;
    }

    private UserModel() {}



    /**查询用户信息
     * @param objectId
     * @param listener
     */
    public void queryUserInfo(String objectId, final QueryUserListener listener){
        BmobQuery<UserEntity> query = new BmobQuery<>();
        query.addWhereEqualTo("objectId", objectId);
        query.findObjects(new FindListener<UserEntity>() {
            @Override
            public void done(List<UserEntity> list, BmobException e) {
                if (e == null) {
                    if(list!=null && list.size()>0){
                        listener.internalDone(list.get(0), null);
                    }else{
                        listener.internalDone(new BmobException(000, "查无此人"));
                    }
                }
                else {
                    e.printStackTrace();
                }
            }
        });
    }

    /**更新用户资料和会话资料
     * @param event
     * @param listener
     */
    public void updateUserInfo(MessageEvent event, final UpdateCacheListener listener){
        final BmobIMConversation conversation=event.getConversation();
        final BmobIMUserInfo info =event.getFromUserInfo();
        final BmobIMMessage msg =event.getMessage();
        String username =info.getName();
        String title =conversation.getConversationTitle();
        Logger.i("" + username + "," + title);
        //sdk内部，将新会话的会话标题用objectId表示，因此需要比对用户名和会话标题--单聊，后续会根据会话类型进行判断
        if(!username.equals(title)) {
            UserModel.getInstance().queryUserInfo(info.getUserId(), new QueryUserListener() {
                @Override
                public void done(UserEntity s, BmobException e) {
                    if(e==null){
                        String name =s.getUser_name();
                        String avatar = s.getUser_icon().getFileUrl();
                        Logger.i("query success："+name+","+avatar);
                        conversation.setConversationIcon(avatar);
                        conversation.setConversationTitle(name);
                        info.setName(name);
                        info.setAvatar(avatar);
                        //更新用户资料
                        BmobIM.getInstance().updateUserInfo(info);
                        //更新会话资料-如果消息是暂态消息，则不更新会话资料
                        if(!msg.isTransient()){
                            BmobIM.getInstance().updateConversation(conversation);
                        }
                    }else{
                        Logger.e(e);
                    }
                    listener.done(null);
                }
            });
        }else{
            listener.internalDone(null);
        }
    }

    /**
     * 查询好友
     * @param listener
     */
    public void queryFriends(final FindListener<Friend> listener){
        BmobQuery<Friend> query = new BmobQuery<>();
        UserEntity user = BmobUser.getCurrentUser(UserEntity.class);
        query.addWhereEqualTo("user", user);
        query.include("friendUser");
        query.order("-updatedAt");
        query.findObjects(new FindListener<Friend>() {
            @Override
            public void done(List<Friend> list, BmobException e) {
            }
        });
    }
}
