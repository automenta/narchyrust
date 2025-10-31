package nars.func.language;

import com.google.common.io.Resources;
import jcog.data.list.Lst;
import jcog.exe.Loop;
import jcog.io.Twokenize;
import nars.*;
import nars.concept.Operator;
import nars.func.language.util.Twenglish;
import nars.task.util.TaskException;
import nars.term.Functor;
import nars.term.atom.Atomic;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;

import static nars.Op.BELIEF;


/**
 * sequential hearing and reading input abilities
 * TODO extend NARPart, add Loop as field
 */
public class NARHear extends Loop {

	static final Atomic START = Atomic.atomic("start");

	private final List<Term> tokens;

	@Nullable private final Term src;
	private final Focus f;

	//public final Off onReset;
	int token;

	float priorityFactor = 1f;
	float confFactor = 1f;



	public NARHear(Focus f, List<Term> msg, @Nullable String src, int wordDelayMS) {
		this(f, msg, src!=null ? Atomic.atom(src) : null, wordDelayMS);
	}

	public NARHear(Focus f, List<Term> msg, @Nullable Term src, int wordDelayMS) {
		super();
		this.f = f;
        this.src = src;
		//onReset = nar.eventClear.onWeak(this::onReset);
		tokens = msg;

		if (wordDelayMS > 0) {
			setPeriodMS(wordDelayMS);
		}
//

//		Term prev = null;
//		for (Term x : msg) {
//			hear(prev, x);
//			prev = x;
//		}

	}

	public static Loop hear(Focus f, String msg, String src, int wordDelayMS) {
		return hear(f, msg, src, wordDelayMS, 1);
	}

	/**
	 * set wordDelayMS to 0 to disable twenglish function
	 */
	public static Loop hear(Focus f, String msg, String src, int wordDelayMS, float pri) {
		return hearIfNotNarsese(f, msg, src, m -> hearText(f, msg, src, wordDelayMS, pri));
	}

	public static Loop hearText(Focus f, String msg, @Nullable String src, int wordDelayMS, float pri) {
		assert (wordDelayMS > 0);
		List<Term> tokens = tokenize(msg);
		if (!tokens.isEmpty()) {
			NARHear hear = new NARHear(f, tokens, src, wordDelayMS);
			hear.priorityFactor = pri;
			return hear;
		} else {
			return null;
		}
	}

	public static Loop hearIfNotNarsese(Focus f, String msg, String src, Function<String, Loop> ifNotNarsese) {

		List<Task> parsed = new Lst<>();

		List<Narsese.NarseseException> errors = new Lst<>();

		try {
			Narsese.tasks(msg, parsed, f.nar);
		} catch (Narsese.NarseseException ignored) {

		}

		if (!parsed.isEmpty() && errors.isEmpty()) {
			f.acceptAll(parsed);
			return null;
		} else {
			return ifNotNarsese.apply(msg);
		}
	}


//    protected void onReset(Timed n) {
//        stop();
//        onReset.close();
//    }

	public static List<Term> tokenize(String msg) {
		List<Term> list = Twokenize.tokenize(msg).stream().map(Twenglish::spanToTerm).toList();
		return list;
	}

	public static void readURL(Focus f) {
		f.nar.add(Atomic.atom("readURL"), (t, n) -> {
			Term[] args = Functor.args(t.term()).arrayClone();
			try {
				return readURL(f, $.unquote(args[0]));
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		});
	}

	public static Task readURL(Focus f, String url) throws IOException {


		String html = Resources.toString(new URL(url), Charset.defaultCharset());

		html = StringEscapeUtils.unescapeHtml4(html);
		String strippedText = html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ").toLowerCase();


		hear(f, strippedText, url, 250, 0.1f);

		return Operator.log(f.nar.time(), "Reading " + url + ':' + strippedText.length() + " characters");
	}

	public static void hear(Focus f, String t, String source) {
		NAR nar = f.nar;
		try {
//			try {
//				throw new TODO();
//				//nar.input(t);
//			} catch (Narsese.NarseseException e) {

				try {
					hear(f, t, source, 0);
				} catch (Exception e1) {
					nar.input(Operator.log(nar.time(), e1));
				}

//			}
		} catch (TaskException tt) {
			nar.input(Operator.log(nar.time(), $.p(t, tt.toString())));
		}

	}

	public static void hear(String text, String src, NAR nar) {

		Focus f = nar.main();
		hearIfNotNarsese(f, text, src, (t) ->
			new NARHear(f, tokenize(t.toLowerCase()), src, (int)(nar.dur() * 2)));
	}

	@Override
	public boolean next() {
		if (token >= tokens.size()) {
			stop();
			return true;
		}


		hear(token > 0 ? tokens.get(token - 1) : START, tokens.get(token++));
		return true;
	}

	private void hear(Term prev, Term next) {

		Term term =
			src != null ?
				$.func("that", next, src) :
				$.func("that", next);

		long now = f.nar.time();
		//            new TruthletTask(
		//                target,
		//                BELIEF,
		//                Truthlet.impulse(
		//                        now, now+1 /* TODO use realtime to translate wordDelayMS to cycles */, 1f, 0f,
		//                        c2w(nar.confDefault(BELIEF)*confFactor)
		//                ),
		//                nar)
		NAR nar = f.nar;
		f.accept(
				NALTask.taskUnsafe(term, BELIEF, $.t(1, (nar.confDefault(BELIEF) * confFactor)), now,
					Math.round(now + nar.dur()), nar.evidence()).withPri(nar.priDefault(BELIEF) * priorityFactor)
		);
	}
}