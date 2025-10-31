/*
 * SND_JAVA.java
 * Copyright (C) 2004
 * 
 * $Id: SND_JAVA.java,v 1.1 2004-07-09 06:50:48 hzi Exp $
 */
/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package jake2.sound.jsound;

import jake2.Globals;
import jake2.game.cvar_t;
import jake2.qcommon.Cvar;

import javax.sound.sampled.*;

/**
 * SND_JAVA
 */
public class SND_JAVA extends Globals {

	private static boolean snd_inited;

	private static cvar_t sndbits;

	public static class dma_t {
		int channels;
		int samples;
		int submission_chunk; 
		
		int samplebits;
		int speed;
		public byte[] buffer;
	}
  	public static final SND_DMA.dma_t dma = new dma_t();
  	
  	static class SoundThread extends Thread {
  		final byte[] b;
		final SourceDataLine l;
		int pos;
		boolean running;
  		SoundThread(byte[] buffer, SourceDataLine line) {
  			b = buffer;
  			l = line;
  		}
  		@Override
        public void run() {
  			running = true;
  			while (running) {
  				line.write(b, pos, 512);
  				pos = (pos+512) % b.length;
  			}
  		}
  		synchronized void stopLoop() {
  			running = false;
  		}
  		int getSamplePos() {
  			return pos >> 1;
  		}
  	}
  	private static SoundThread thread;
	private static SourceDataLine line;


	static boolean SNDDMA_Init() {

		if (snd_inited)
			return true;

		if (sndbits == null) {
			sndbits = Cvar.Get("sndbits", "16", CVAR_ARCHIVE);
			cvar_t sndspeed = Cvar.Get("sndspeed", "0", CVAR_ARCHIVE);
			cvar_t sndchannels = Cvar.Get("sndchannels", "1", CVAR_ARCHIVE);
		}


		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 22050, 16, 1, 2, 22050, false);
		DataLine.Info dinfo = new DataLine.Info(SourceDataLine.class, format);
		
		try {
			line = (SourceDataLine) AudioSystem.getLine(dinfo);
		} catch (LineUnavailableException e4) {
			return false; 
		}
		
		dma.buffer = new byte[65536];				
		dma.channels = format.getChannels();
		dma.samplebits = format.getSampleSizeInBits();
		dma.samples = dma.buffer.length / format.getFrameSize();
		dma.speed = (int) format.getSampleRate();
		
		dma.submission_chunk = 1;
		
		try {
			line.open(format, 4096);
		} catch (LineUnavailableException e5) {
			return false;
		}

		line.start();
		thread = new SoundThread(dma.buffer, line);
		
		thread.start();
				
		snd_inited = true;
		return true;

	}

	public static int SNDDMA_GetDMAPos() {
		return thread.getSamplePos();
	}

	static void SNDDMA_Shutdown() {
		thread.stopLoop();
		line.stop();
		line.flush();
		line.close();
		line=null;
		snd_inited = false;		
	}

	/*
	==============
	SNDDMA_Submit

	Send sound to device if buffer isn't really the dma buffer
	===============
	*/
	static void SNDDMA_Submit() {

	}

	static void SNDDMA_BeginPainting() {}

















}
