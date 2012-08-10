package jhn.eda.listeners;

import java.io.File;

import jhn.eda.Paths;
import jhn.util.Util;

public class SerializeModel extends IntervalListener {
	private final int run;
	public SerializeModel(int printInterval, int run) {
		super(printInterval);
		this.run = run;
		
		File dir = new File(Paths.modelDir(run));
		if(!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	@Override
	protected void iterationEndedAtInterval(int iteration) throws Exception {
		Util.serialize(eda, Paths.modelFilename(run, iteration));
	}
	
}
