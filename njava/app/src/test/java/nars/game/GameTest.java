package nars.game;

import jcog.signal.meter.TemporalMetrics;
import nars.*;
import nars.deriver.impl.TaskBagDeriver;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static nars.Op.GOAL;

//import tech.tablesaw.plotly.api.LinePlot;
//import tech.tablesaw.plotly.components.Figure;
//import tech.tablesaw.plotly.components.Page;

class GameTest {

	static class OneBitGame extends Game {

		Term X;
		Term Y;

		/** current input */
		float x;

		/** current output */
		float y;

		final TemporalMetrics data = new TemporalMetrics();


		OneBitGame(String id) {
			super(id);
		}

		@Override
		protected void init() {
			this.X = $.inh(id,"x");
			this.Y = $.inh(id,"y");
			sense(X, ()->x);
			action(Y, y->{
				this.y = y;
			});
			reward(()->{
				return 1-Math.abs(x - y);
			});
			data.on("x", ()->this.x);
			data.on("y", ()->this.y);

			//TODO data.commitOnFrame(this);
			onFrame(()-> data.commit(time()));
		}
	}

	@Test
    static void testOneBitGame1()  {
		NAR n = NARS.tmp(0);
		//n.log();

		int dur = 10;

		n.complexMax.set(9);
		n.time.dur(dur);

		n.beliefPriDefault.pri(0.1f);


		OneBitGame g = new OneBitGame("g");
		n.add(g);

		var d = new TaskBagDeriver /*SerialDeriver*/(NARS.Rules.nal(1,8).core().stm().temporalInduction()
				.compile(n), n);
		d.everyCycle(g.focus());



//		NAL.DEBUG = true; NAL.causeCapacity.set(4);
		g.focus().log();

//		g.focus().acceptAll(
//		NALTask.taskEternal($.impl(g.X,g.Y), BELIEF, $.t(1, n.confDefault(BELIEF)), n).withPri(1),
//			NALTask.taskEternal($.impl(g.X.neg(),g.Y.neg()), BELIEF, $.t(1, n.confDefault(BELIEF)), n).withPri(1)
//		);


		//n.want($.sim(g.X, g.Y));

//		n.want(CONJ.the(g.X, g.Y));
//		n.want(CONJ.the(g.X.neg(), g.Y.neg()));

        g.focus().onTask(x -> {
			if (((NALTask)x).stamp().length > 1) {
				n.proofPrint((NALTask) x);
				System.out.println();
			}
        }, GOAL);

        g.x = 0;  n.run(100);
		g.x = 1;  n.run(100);
		g.x = 0;  n.run(100);
		g.x = 1;  n.run(100);

//		show(LinePlot.create("title", g.data.data, "t", "y"));

	}

	@Disabled
	@Test
	void test1() {
		TemporalMetrics m = new TemporalMetrics();
		m.on("x", () -> ThreadLocalRandom.current().nextFloat());
		m.on("y", () -> ThreadLocalRandom.current().nextFloat());
		for (int i = 0; i < 100; i++) {
			m.commit(i);
		}
		//https://jtablesaw.github.io/tablesaw/userguide/TimeSeries

		//System.out.println(m.data.summary());
		//show(ScatterPlot.create("title", m.data, "t", "x"));
	}

//	static void show(Figure figure)  {
//
//		File outputFile = null;
//		try {
//			outputFile = File.createTempFile(figure.toString(), "html");
//			Page page = Page.pageBuilder(figure, "target").build();
//			String output = page.asJavascript();
//
//			try (Writer writer =
//					 new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
//				writer.write(output);
//			}
//			Runtime runtime = Runtime.getRuntime();
//			try {
//				runtime.exec("xdg-open " + outputFile);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//
//	}


}