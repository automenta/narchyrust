package nars.time.part;

import nars.$;
import nars.NAR;

import java.util.function.Consumer;

public final class DurNARConsumer extends DurLoop {

	final Consumer<NAR> r;

	public DurNARConsumer(Consumer<NAR> r) {
		super($.identity(r));
		this.r = r;
	}

	@Override
	public void accept(NAR n) {
		r.accept(n);
	}



}