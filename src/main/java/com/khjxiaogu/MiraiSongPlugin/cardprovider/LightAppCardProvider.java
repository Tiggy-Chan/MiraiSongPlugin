package com.khjxiaogu.MiraiSongPlugin.cardprovider;

import com.google.gson.JsonObject;
import com.khjxiaogu.MiraiSongPlugin.MusicCardProvider;
import com.khjxiaogu.MiraiSongPlugin.MusicInfo;
import com.khjxiaogu.MiraiSongPlugin.Utils;

import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.LightApp;
import net.mamoe.mirai.message.data.MessageChain;

public class LightAppCardProvider implements MusicCardProvider {

	public LightAppCardProvider() {
	}

	@Override
	public MessageChain process(MusicInfo mi,Contact ct) {
		JsonObject appmsg = new JsonObject();
		appmsg.addProperty("app", "com.tencent.structmsg");
		JsonObject config=new JsonObject();
		appmsg.add("config",config);
		config.addProperty("autosize",true);
		config.addProperty("forward",true);
		config.addProperty("ctime",1604074080);
		config.addProperty("type","normal");
		config.addProperty("token","30bba236d84394d3922e3965c11c3514");
		appmsg.addProperty("view", "music");
		appmsg.addProperty("ver", "0.0.0.1");
		appmsg.addProperty("desc", "音乐");
		appmsg.addProperty("prompt","[分享]"+ mi.title);
		JsonObject meta = new JsonObject();
		appmsg.add("meta", meta);
		JsonObject music = new JsonObject();
		meta.add("music", music);
		music.addProperty("action", "");
		music.addProperty("android_pkg_name", "");
		music.addProperty("app_type", 1);
		music.addProperty("appid", mi.appid);
		music.addProperty("preview", mi.purl);
		music.addProperty("desc", mi.desc);
		music.addProperty("jumpUrl", mi.jurl);
		music.addProperty("musicUrl", mi.murl);
		music.addProperty("sourceMsgId", "0");
		music.addProperty("source_icon", mi.icon);
		music.addProperty("source_url", "");
		music.addProperty("sourceMsgId","0");
		music.addProperty("tag", mi.source);
		music.addProperty("title", mi.title);
		return new LightApp(appmsg.toString()).plus(mi.jurl);
	}

}
