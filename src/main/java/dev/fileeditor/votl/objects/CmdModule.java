package dev.fileeditor.votl.objects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum CmdModule {
	WEBHOOK("modules.webhook", 1),
	MODERATION("modules.moderation", 2),
	STRIKES("modules.strikes", 3),
	VERIFICATION("modules.verification", 4),
	TICKETING("modules.ticketing", 5),
	VOICE("modules.voice", 6),
	REPORT("modules.report", 7),
	ROLES("modules.roles", 8);
	
	private final String path;
	private final int value;

	public static final Set<CmdModule> ALL = new HashSet<CmdModule>();
	private static final Map<Integer, CmdModule> BY_VALUE = new HashMap<Integer, CmdModule>();

	static {
		for (CmdModule ct : CmdModule.values()) {
			ALL.add(ct);
			BY_VALUE.put(ct.getValue(), ct);
		}
	}
	
	CmdModule(String path, int value) {
		this.path = path;
		this.value = (int) Math.pow(2, value-1);
	}

	public String getPath() {
		return path;
	}

	public int getValue() {
		return value;
	}

	public static Set<CmdModule> decodeModules(int data) {
		Set<CmdModule> modules = new HashSet<>(values().length);
		for (CmdModule v : values()) {
			if ((data & v.value) == v.value) modules.add(v);
		}
		return modules;
	}

	public static int encodeModules(Set<CmdModule> actions) {
		int data = 0;
		for (CmdModule v : actions) {
			data += v.value;
		}
		return data;
	}

	public static CmdModule byValue(int type) {
		return BY_VALUE.get(type);
	}
}