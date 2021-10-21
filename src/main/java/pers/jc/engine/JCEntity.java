package pers.jc.engine;

import pers.jc.logic.Director;

public class JCEntity {
	public final int id = JCEngine.getAutoIncrementID(JCEntity.class);
	public Director director;
	public JCChannel channel;
	public boolean isValid;
	
	public void onLoad() {}	
	
	public void onDestroy() {}
	
	public void call(String func, Object... args) {
		sendDataText(packDataText(func, args));
	}

	public void sendDataText(String text) {
		if (isValid) channel.writeAndFlush(text);
	}

	public static String packDataText(String func, Object... args) {
		return new JCData("", JCData.TYPE_FUNCTION, func, args).stringify();
	}
}
