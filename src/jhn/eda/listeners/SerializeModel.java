package jhn.eda.listeners;

import java.io.File;

import jhn.eda.Paths;
import jhn.util.Util;

public class SerializeModel extends IntervalListener {
	private final String runDir;
	public SerializeModel(int printInterval, String run) {
		super(printInterval);
		this.runDir = run;
		
		File dir = new File(Paths.modelDir(run));
		if(!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	@Override
	protected void iterationEndedAtInterval(int iteration) throws Exception {
		Util.serialize(eda, Paths.modelFilename(runDir, iteration));
	}
	
}
