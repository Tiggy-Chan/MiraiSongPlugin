package com.khjxiaogu.MiraiSongPlugin.test;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.khjxiaogu.MiraiSongPlugin.MusicInfo;
import com.khjxiaogu.MiraiSongPlugin.MusicSource;
import com.khjxiaogu.MiraiSongPlugin.musicsource.BiliBiliMusicSource;

public class Main {
	public static final Map<String, MusicSource> sources = Collections.synchronizedMap(new LinkedHashMap<>());
	static {
	}

	public Main() {
	}

	public static void main(String[] args) throws Exception {
		MusicInfo mi = new BiliBiliMusicSource().get(URLEncoder.encode("小丑的品格"));
		StringBuilder tsb = new StringBuilder().append("歌名：").append(mi.title).append("\n作者：").append(mi.desc)
				.append("\n封面：" + mi.purl).append("\n外链：" + mi.murl).append("\n链接：" + mi.jurl)
				.append("\n来自：" + mi.source);
		System.out.println(tsb);
	}

}
