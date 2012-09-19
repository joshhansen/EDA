package cc.mallet.pipe;

import cc.mallet.types.Instance;

public class SourceSetter extends Pipe {
	private static final long serialVersionUID = 1L;

	@Override
	public Instance pipe(Instance inst) {
		inst.setSource(inst.getName());
		return inst;
	}
}
