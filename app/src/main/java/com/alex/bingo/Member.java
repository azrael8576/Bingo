package com.alex.bingo;

public class Member {
    String uid;
    String nickname;
    String displayName;
    int avatarId;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(int avatar) {
        this.avatarId = avatar;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
