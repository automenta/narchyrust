package nars.game.sensor;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.game.Game;
import nars.term.atom.Int;
import org.junit.jupiter.api.Test;

import static jcog.Str.n4;

class FreqVectorSensorTest {

	@Test
	void test1() {
		NAR n = NARS.tmp();
		Game g = new Game("hear");
		FreqVectorSensor f = new FreqVectorSensor(16, 1024, 0, 1024, i -> $.inh("x", Int.i(i)), n);
		g.addSensor(f);

		int bufferSize = 256;
		int x = 0;
		int buffers = 16;
		float freq = 32f/bufferSize;
		float[] buf = new float[bufferSize];
		for (int b = 0; b < buffers; b++) {
			for (int j = 0; j < bufferSize; j++) {
				buf[x%bufferSize] = (float) Math.sin(x*freq*2*3.14159f);
				//buf[x%bufferSize] += (float) Math.sin(x*freq/2*2*3.14159f)*0.5f;
				x++;
			}
			f.input(buf);
			n.run(1);
			System.out.println(n.time()+"\t:"+ n4(f.freq()));
		}

	}
}