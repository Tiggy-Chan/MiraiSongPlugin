package com.khjxiaogu.MiraiSongPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import com.khjxiaogu.MiraiSongPlugin.cardprovider.AmrVoiceProvider;
import com.khjxiaogu.MiraiSongPlugin.cardprovider.LightAppCardProvider;
import com.khjxiaogu.MiraiSongPlugin.cardprovider.LightAppXCardProvider;
import com.khjxiaogu.MiraiSongPlugin.cardprovider.PlainMusicInfoProvider;
import com.khjxiaogu.MiraiSongPlugin.cardprovider.ShareCardProvider;
import com.khjxiaogu.MiraiSongPlugin.cardprovider.SilkVoiceProvider;
import com.khjxiaogu.MiraiSongPlugin.cardprovider.XMLCardProvider;
import com.khjxiaogu.MiraiSongPlugin.musicsource.BaiduMusicSource;
import com.khjxiaogu.MiraiSongPlugin.musicsource.BiliBiliMusicSource;
import com.khjxiaogu.MiraiSongPlugin.musicsource.KugouMusicSource;
import com.khjxiaogu.MiraiSongPlugin.musicsource.LocalFileSource;
import com.khjxiaogu.MiraiSongPlugin.musicsource.NetEaseAdvancedRadio;
import com.khjxiaogu.MiraiSongPlugin.musicsource.NetEaseHQMusicSource;
import com.khjxiaogu.MiraiSongPlugin.musicsource.NetEaseMusicSource;
import com.khjxiaogu.MiraiSongPlugin.musicsource.NetEaseRadioSource;
import com.khjxiaogu.MiraiSongPlugin.musicsource.QQMusicSource;

import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.console.plugins.ConfigSection;
import net.mamoe.mirai.console.plugins.PluginBase;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.Events;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.message.FriendMessageEvent;
import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.MessageEvent;
import net.mamoe.mirai.message.TempMessageEvent;

// TODO: Auto-generated Javadoc
/**
 * 插件主类
 * 
 * @author khjxiaogu
 *         file: MiraiSongPlugin.java
 *         time: 2020年8月26日
 */
public class MiraiSongPlugin extends PluginBase {
	private final String spliter=" ";
	// 请求音乐的线程池。
	private Executor exec = Executors.newFixedThreadPool(8);

	/** 命令列表. */
	public static final Map<String, BiConsumer<MessageEvent, String[]>> commands = new ConcurrentHashMap<>();

	/** 音乐来源. */
	public static final Map<String, MusicSource> sources = Collections.synchronizedMap(new LinkedHashMap<>());

	/** 外观来源 */
	public static final Map<String, MusicCardProvider> cards = new ConcurrentHashMap<>();
	static {
		// 注册音乐来源
		sources.put("QQ音乐", new QQMusicSource());
		// sources.put("QQ音乐HQ",new QQMusicHQSource());//这个音乐源已被tx禁用。
		sources.put("网易", new NetEaseMusicSource());
		sources.put("Bilibili",new BiliBiliMusicSource());
		sources.put("网易电台节目", new NetEaseAdvancedRadio());
		sources.put("网易电台", new NetEaseRadioSource());
		sources.put("网易HQ", new NetEaseHQMusicSource());
		
		sources.put("酷狗", new KugouMusicSource());
		sources.put("千千", new BaiduMusicSource());
		sources.put("本地", new LocalFileSource());
		
		cards.put("LightApp",new XMLCardProvider());
		cards.put("XML",new XMLCardProvider());
		cards.put("Silk",new SilkVoiceProvider());
		cards.put("AMR",new AmrVoiceProvider());
		cards.put("Share",new ShareCardProvider());
		cards.put("Message",new PlainMusicInfoProvider());
		cards.put("LightAppX",new LightAppXCardProvider());
	}
	static {
		HttpURLConnection.setFollowRedirects(true);
	}

	/**
	 * 使用现有的来源和外观制作指令执行器
	 * 
	 * @param source 音乐来源名称
	 * @param card   音乐外观名称
	 * @return return 返回一个指令执行器，可以注册到命令列表里面
	 */
	public BiConsumer<MessageEvent, String[]> makeTemplate(String source, String card) {
		if (source.equals("all"))
			return makeSearchesTemplate(card);
		MusicCardProvider cb = cards.get(card);
		if (cb == null)
			throw new IllegalArgumentException("card template not exists");
		MusicSource mc = sources.get(source);
		if (mc == null)
			throw new IllegalArgumentException("music source not exists");
		return (event, args) -> {
			String sn;
			try {
				sn = URLEncoder.encode(String.join(spliter, Arrays.copyOfRange(args, 1, args.length)), "UTF-8");
			} catch (UnsupportedEncodingException ignored) {
				return;
			}
			exec.execute(() -> {
				MusicInfo mi;
				try {
					mi = mc.get(sn);
				} catch (Throwable t) {
					this.getLogger().debug(t);
					Utils.getRealSender(event).sendMessage("无法找到歌曲。");
					return;
				}
				try {
					Utils.getRealSender(event).sendMessage(cb.process(mi, Utils.getRealSender(event)));
				} catch (Throwable t) {
					this.getLogger().debug(t);
					Utils.getRealSender(event).sendMessage("无法生成分享。");
					return;
				}
			});
		};
	}

	/**
	 * 自动搜索所有源并且以指定外观返回
	 * 
	 * @param card 音乐外观名称
	 * @return return 返回一个指令执行器，可以注册到命令列表里面
	 */
	public BiConsumer<MessageEvent, String[]> makeSearchesTemplate(String card) {
		MusicCardProvider cb = cards.get(card);
		if (cb == null)
			throw new IllegalArgumentException("card template not exists");
		return (event, args) -> {
			String sn;
			try {
				sn = URLEncoder.encode(String.join(spliter, Arrays.copyOfRange(args, 1, args.length)), "UTF-8");
			} catch (UnsupportedEncodingException ignored) {
				return;
			}
			exec.execute(() -> {
				for (MusicSource mc : sources.values()) {
					if(!mc.isVisible())continue;
					MusicInfo mi;
					try {
						mi = mc.get(sn);
					} catch (Throwable t) {
						this.getLogger().debug(t);
						continue;
					}
					try {
						/*if(mi.source=="QQ音乐"&&cb instanceof LightAppCardProvider)
							Utils.getRealSender(event).sendMessage(cards.get("XML").process(mi, Utils.getRealSender(event)));
						else*/
							Utils.getRealSender(event).sendMessage(cb.process(mi, Utils.getRealSender(event)));
							return;
					} catch (Throwable t) {
						this.getLogger().debug(t);
						Utils.getRealSender(event).sendMessage("无法生成分享。");
					}
				}
				Utils.getRealSender(event).sendMessage("无法找到歌曲。");
			});
		};
	}

	@SuppressWarnings("resource")
	@Override
	public void onEnable() {
		Config cfg;
		if (!new File(this.getDataFolder(), "config.yml").exists()) {
			try {
				new FileOutputStream(new File(this.getDataFolder(), "config.yml"))
						.write(Utils.readAll(this.getResources("config.yml")));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		cfg = super.loadConfig("config.yml");
		
		if (!cfg.contains("adddefault")||cfg.getBoolean("adddefault")) {
			commands.put("音乐",makeSearchesTemplate("LightApp"));
			commands.put("外链",makeSearchesTemplate("Message"));
			commands.put("语音",makeSearchesTemplate("AMR"));
			commands.put("QQ", makeTemplate("QQ音乐", "XML"));
			commands.put("bili", makeTemplate("Bilibili", "XML"));
			commands.put("网易", makeTemplate("网易", "LightApp"));
			commands.put("网易电台", makeTemplate("网易电台节目", "LightApp"));
			commands.put("酷狗", makeTemplate("酷狗", "LightApp"));
			commands.put("千千", makeTemplate("千千", "LightApp"));
			commands.put("文件", makeTemplate("本地", "AMR"));
			commands.put("点歌", (event, args) -> {
				String sn;
				try {
					sn = URLEncoder.encode(String.join(spliter, Arrays.copyOfRange(args, 3, args.length)), "UTF-8");
				} catch (UnsupportedEncodingException ignored) {
					return;
				}
				exec.execute(() -> {
					try {
						MusicSource ms = sources.get(args[1]);
						if (ms == null) {
							Utils.getRealSender(event).sendMessage("无法找到源");
							return;
						}
						MusicCardProvider mcp = cards.get(args[2]);
						if (mcp == null) {
							Utils.getRealSender(event).sendMessage("无法找到模板");
							return;
						}
						Utils.getRealSender(event).sendMessage(mcp.process(ms.get(sn),Utils.getRealSender(event)));
					} catch (Throwable e) {
						this.getLogger().debug(e);
						Utils.getRealSender(event).sendMessage("无法找到歌曲");
					}
				});
			});
		}
		if (cfg.contains("extracommands")) {
			ConfigSection excs= cfg.getConfigSection("extracommands");
			for (String cmd : excs.keySet()) {
				commands.put(cmd.toString(), makeTemplate(excs.getConfigSection(cmd).getString("source"),
						excs.getConfigSection(cmd).getString("card")));
			}
		}
		AmrVoiceProvider.ffmpeg = SilkVoiceProvider.ffmpeg = new File(cfg.getString("ffmpeg_path"));
		AmrVoiceProvider.autoSize = cfg.contains("amrqualityshift")&&cfg.getBoolean("amrqualityshift");
		AmrVoiceProvider.wideBrand = (!cfg.contains("amrwb"))||cfg.getBoolean("amrwb");
		SilkVoiceProvider.silk = new File(cfg.getString("silkenc_path"));
		Events.registerEvents(this, new SimpleListenerHost(this.getCoroutineContext()) {
			@EventHandler
			public void onGroup(GroupMessageEvent event) {
				String[] args = Utils.getPlainText(event.getMessage()).split(spliter);
				BiConsumer<MessageEvent, String[]> exec = commands.get(args[0]);
				if (exec != null)
					exec.accept(event, args);
			}
			@EventHandler
			public void onFriend(FriendMessageEvent event) {
				String[] args = Utils.getPlainText(event.getMessage()).split(spliter);
				BiConsumer<MessageEvent, String[]> exec = commands.get(args[0]);
				if (exec != null)
					exec.accept(event, args);
			}
			@EventHandler
			public void onTemp(TempMessageEvent event) {
				String[] args = Utils.getPlainText(event.getMessage()).split(spliter);
				BiConsumer<MessageEvent, String[]> exec = commands.get(args[0]);
				if (exec != null)
					exec.accept(event, args);
			}
		});

		getLogger().info("插件加载完毕!");
	}

}
