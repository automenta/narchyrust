package nars.deriver.reaction;

import nars.$;
import nars.Deriver;
import nars.Premise;
import nars.Term;
import nars.action.Action;
import nars.control.Cause;
import nars.premise.SubPremise;

import static nars.$.intRadix;

/** stateless by default */
public abstract class NativeReaction extends MutableReaction {

	private final Term id;

	protected NativeReaction() {
		taskPattern = PremiseTask;
		beliefPattern = PremiseBelief;
		this.id =
			//$.identity(this);
			$.p(this.getClass().getSimpleName(),
					intRadix(System.identityHashCode(this), 36)
					//System.identityHashCode(this)
			);
	}

	@Override public final Term term() {
		return id;
	}

	@Deprecated protected abstract void run(Deriver d);

	@Override public Action action(Cause<Reaction<Deriver>> why) {
		return new MyNativeAction(why);
	}

	@Override public Class<? extends Reaction<Deriver>> type() {
		return getClass();
	}

	public final class MyNativeAction extends Action<Deriver> {

		private MyNativeAction(Cause<Reaction<Deriver>> why) {
			super(why);
		}

		@Override public boolean test(Deriver d) {
			d.add(new NativePremise(d.premise));
			return true;
		}

		public final class NativePremise extends SubPremise {

			NativePremise(Premise p) {
				super(MyNativeAction.this.term(), p);
			}

			@Override
            public void run(Deriver d) {
				NativeReaction.this.run(d);
			}

			@Override public Reaction reaction() { return MyNativeAction.this.why.name; }

		}
	}

}