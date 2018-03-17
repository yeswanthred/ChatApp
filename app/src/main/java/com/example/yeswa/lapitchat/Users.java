package com.example.yeswa.lapitchat;

/**
 * Created by yeswa on 03-03-2018.
 */

public class Users {

    String name;
    String image;
    String status;

    String thumb_image;

    public Users() {

    }

    public Users(String name, String image, String status, String thumb_image) {
        this.name = name;
        this.image = image;
        this.status = status;
        this.thumb_image = thumb_image;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setThumb_img(String thumb_image) {
        this.thumb_image = thumb_image;
    }

    public String getThumb_image() {
        return thumb_image;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getStatus() {
        return status;
    }


}
